package com.hrsthrt74.qstile.tile

import android.content.Context
import android.graphics.drawable.Icon
import android.os.SystemClock
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.StatsRepository
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class ExTileService : TileService() {
    companion object {
        private const val TAG = "ExTileService"

        /**
         * 首次确认延迟（毫秒）。
         *
         * 背景：`onStopListening()` **并不等于「面板已收起」**。
         * HyperOS 的磁贴列表是 RecyclerView（日志里表现为 MainPanelAdapter / QSItemViewHolder），
         * 磁贴一旦被滚出可视区域就会停止监听；此外「上滑准备关闭面板但松手回弹」
         * 这类被打断的手势也会先停监听、再重新监听。用户实际反馈的现象就是：
         * 在控制中心里滑动一下、面板还没收起，磁贴就被自动折叠了。
         *
         * 因此这里不能收到回调就立刻写 `sysui_qs_tiles`，必须先等一段时间，
         * 看这期间面板是否又恢复监听（说明面板一直开着），再决定要不要折叠。
         *
         * 时长取 1.2 秒的依据：排查用户日志时，误触发后 1.58 秒用户又点了磁贴，
         * 只要确认窗口覆盖住这段「用户还在面板里操作」的间隔，
         * 后续那次 onStartListening / onClick 就能把误判作废。
         */
        private const val AUTO_COLLAPSE_CONFIRM_DELAY_MS = 1200L

        /**
         * 二次确认延迟（毫秒）。首次等待结束后再静默观察 0.8 秒（合计 2 秒），
         * 期间只要出现「重新监听 / 用户点击磁贴」就放弃本次自动收起。
         *
         * 窗口偏长是有意为之：
         * - 确认期间用户看不到任何东西（写入发生在面板关闭之后），等待没有感知成本；
         * - 窗口越长，越容易被用户后续的真实交互打断，误判率越低；
         * - 代价仅是「面板关闭后 2 秒内重新打开」不会折叠，这本身也是合理行为。
         */
        private const val AUTO_COLLAPSE_SETTLE_DELAY_MS = 800L

        /**
         * 点击冷却（毫秒）：点击磁贴会写 `sysui_qs_tiles`，SystemUI 随即重建整个磁贴列表，
         * 重建过程本身也可能对我们的磁贴触发一次 `onStopListening`。
         * 这段时间内不执行自动收起，避免「刚点开就被自己写回的收起布局覆盖」这种自触发。
         */
        private const val AUTO_COLLAPSE_CLICK_COOLDOWN_MS = 1200L

        /**
         * 监听代次。`onStartListening` / `onStopListening` / `onClick` 都会让它自增，
         * 延时任务在真正执行前比对自己拿到的那一代，代次变了就说明期间有新的监听事件，
         * 本次判定已经过期，直接作废（等价于「取消」）。
         *
         * 放在伴生对象而不是实例字段：面板关闭后本服务可能被系统解绑销毁，
         * 下次打开面板会创建新实例，代次必须跨实例连续才有意义。
         */
        private val listeningEpoch = AtomicInteger(0)

        /** 最近一次点击磁贴的时刻（`SystemClock.elapsedRealtime()`），用于点击冷却判断 */
        @Volatile
        private var lastClickAt = 0L

        /** 待执行的自动收起任务；收到新监听事件时取消，避免重复写 `sysui_qs_tiles` */
        @Volatile
        private var pendingCollapseJob: Job? = null

        /**
         * 进程级协程作用域，专门跑「延时确认 + 自动收起」。
         *
         * 不能挂在服务实例的 scope 上：面板关闭后 SystemUI 会解绑本服务，
         * 实例随之销毁，绑在实例上的延时任务会在写入前被一起取消，
         * 导致「自动收起」时灵时不灵。
         */
        private val pendingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** 取消尚未执行的自动收起（有新监听事件 / 新点击时调用） */
        private fun cancelPendingCollapse() {
            pendingCollapseJob?.cancel()
            pendingCollapseJob = null
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartListening() {
        super.onStartListening()
        // 面板开始监听 = 面板打开了（或磁贴重新滚回可视区域）：
        // 这是新一代监听，任何还在排队的自动收起都必须作废，
        // 否则会把刚打开的（或用户正在看的）面板又收掉。
        listeningEpoch.incrementAndGet()
        cancelPendingCollapse()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // 记录点击时刻并推进代次：点击说明用户此刻正在面板里操作，
        // 排队中的自动收起应立即作废，随后 1.2 秒内也不再触发。
        lastClickAt = SystemClock.elapsedRealtime()
        listeningEpoch.incrementAndGet()
        cancelPendingCollapse()

        scope.launch {
            try {
                // OOBE 未完成时不执行任何操作：
                // 首次使用引导期间用户可能已把磁贴添加到面板但尚未完成配置导入，
                // 此时点击磁贴会以默认配置写 sysui_qs_tiles，覆盖用户当前的系统布局。
                // 「重新运行引导」期间（标志被重置为 false）同样拦截，保持行为一致。
                val oobeCompleted = ConfigRepository.getOobeCompletedFlow(this@ExTileService).first()
                if (!oobeCompleted) {
                    Log.d(TAG, "OOBE not completed, ignore tile click")
                    return@launch
                }

                val config = ConfigRepository.getConfig(this@ExTileService)
                // onClick = 切换：当前展开则收起，当前收起则展开
                val newIsExpanded = !config.isExpanded
                applyLayout(this@ExTileService, newIsExpanded, config)
                updateTileState()
            } catch (e: Exception) {
                Log.e(TAG, "onClick failed", e)
            }
        }
    }

    /**
     * 面板停止监听本磁贴时回调。
     *
     * 注意：**这个回调不等于「面板已收起」**。HyperOS 上以下情况都会触发它：
     * 1. 面板真的关闭（本意要处理的情况）；
     * 2. 磁贴在控制中心里被滚出可视区域（用户滑动列表）；
     * 3. 上滑关闭面板的手势被松手回弹（面板仍然开着）；
     * 4. 面板内磁贴列表重建（例如其他应用改动了磁贴、列表刷新）。
     *
     * 所以这里只登记一次「疑似关闭」，交给延时任务去确认；确认条件是
     * 「这一代监听是否还是最新的」+「用户最近有没有点过磁贴」，
     * 两者都通过后才真正写入收起布局。整个确认过程只有计时和一次 DataStore 读取，
     * 不执行任何 shell / dumpsys 之类的耗时操作，不会拖慢磁贴交互。
     */
    override fun onStopListening() {
        super.onStopListening()

        // 取得本次停止监听对应的代次，并让上一轮待执行的收起作废
        val epoch = listeningEpoch.incrementAndGet()
        cancelPendingCollapse()

        pendingCollapseJob = pendingScope.launch {
            try {
                // 第一步：等一段时间。若期间面板重新监听（onStartListening）或用户点了磁贴，
                // 代次会变，说明面板一直开着，直接放弃。
                delay(AUTO_COLLAPSE_CONFIRM_DELAY_MS)
                if (!isEpochStillValid(epoch)) {
                    Log.d(TAG, "Auto-collapse canceled after confirm delay (panel still active)")
                    return@launch
                }

                // 第二步：再静默观察一小段时间，仍无任何监听活动才认为面板已收起，
                // 避免「滚动刚停下来」这种边界情况被误判。
                delay(AUTO_COLLAPSE_SETTLE_DELAY_MS)
                if (!isEpochStillValid(epoch)) {
                    Log.d(TAG, "Auto-collapse canceled after settle delay (panel still active)")
                    return@launch
                }

                // 使用应用上下文：本服务实例可能已被销毁，
                // 而 ContentResolver / DataStore 都是应用级对象，拿 applicationContext 最稳。
                val context = applicationContext

                val autoCollapse = ConfigRepository.getAutoCollapseOnClose(context)
                if (!autoCollapse) return@launch

                val config = ConfigRepository.getConfig(context)
                // 仅在展开状态下触发收起；已是收起状态则跳过（幂等，避免重复写 sysui_qs_tiles）
                if (!config.isExpanded) return@launch

                Log.d(TAG, "Auto-collapsing after confirmed QS panel close")
                applyLayout(context, newIsExpanded = false, config = config)
                // 这里不再调用 updateTileState()：面板已经关闭，qsTile 通常也已失效，
                // 再刷一次只会让 SystemUI 多重绘一遍；磁贴文案会在下次 onStartListening 时刷新。
            } catch (e: Exception) {
                Log.e(TAG, "onStopListening auto-collapse failed", e)
            }
        }
    }

    /**
     * 判断延时任务拿到的代次是否仍然有效。
     *
     * 两个条件必须同时满足：
     * 1. 代次没变 —— 期间没有新的 `onStartListening` / `onStopListening` / `onClick`；
     * 2. 距离上次点击磁贴已超过冷却时间 —— 用户不是正在面板里点磁贴。
     *
     * @param epoch 任务启动时取得的监听代次
     * @return true 表示可以继续执行自动收起
     */
    private fun isEpochStillValid(epoch: Int): Boolean {
        if (listeningEpoch.get() != epoch) return false
        val sinceClick = SystemClock.elapsedRealtime() - lastClickAt
        return sinceClick >= AUTO_COLLAPSE_CLICK_COOLDOWN_MS
    }

    /**
     * 将磁贴布局切换到指定状态并同步所有关联设置。
     * onClick（手动切换）和 onStopListening（自动收起）共用此函数，
     * 避免重复代码。执行顺序：
     * 1. 写 sysui_qs_tiles（目标布局）
     * 2. 保存 isExpanded 标志
     * 3. 记录统计（展开/收起）
     * 4. 按开关同步无字模式 / 融合设备中心
     *
     * @param context Context（自动收起路径传 applicationContext：服务实例可能已销毁）
     * @param newIsExpanded 目标展开状态：true = 展开，false = 收起
     * @param config 当前配置（用于获取展开/收起磁贴列表，避免重复读取）
     */
    private suspend fun applyLayout(
        context: Context,
        newIsExpanded: Boolean,
        config: com.hrsthrt74.qstile.data.TileConfig
    ) {
        val tilesToSet = if (newIsExpanded) {
            config.expandedTiles
        } else {
            config.collapsedTiles
        }

        val success = SecureSettingsHelper.setCurrentTiles(context, tilesToSet)

        if (success) {
            ConfigRepository.saveIsExpanded(context, newIsExpanded)
            Log.d(TAG, "Layout applied: ${if (newIsExpanded) "expanded" else "collapsed"}")

            // 记录统计：仅统计布局切换成功的情况（失败不算）
            if (newIsExpanded) {
                StatsRepository.recordExpand(context)
            } else {
                StatsRepository.recordCollapse(context)
            }

            // 「展开收起同时控制无字模式」：开启后，磁贴切换布局时同步 wordless_mode
            // 展开 → wordless_mode=0（显示文字），收起 → wordless_mode=1（无字模式）
            if (ConfigRepository.getWordlessModeSync(context)) {
                SecureSettingsHelper.putSecureSetting(
                    context,
                    "wordless_mode",
                    if (newIsExpanded) "0" else "1"
                )
            }

            // 「展开收起同时控制融合设备中心」：开启后，磁贴切换布局时同步 smart_device_control
            // 展开 → smart_device_control=1，收起 → smart_device_control=0（与无字模式相反）
            if (ConfigRepository.getSmartDeviceControlSync(context)) {
                SecureSettingsHelper.putSecureSetting(
                    context,
                    "smart_device_control",
                    if (newIsExpanded) "1" else "0"
                )
            }
        } else {
            Log.e(TAG, "Failed to set sysui_qs_tiles")
        }
    }

    private fun updateTileState() {
        scope.launch {
            try {
                val config = ConfigRepository.getConfig(this@ExTileService)
                val tile = qsTile ?: return@launch

                tile.state = if (config.isExpanded) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                // 磁贴名字：读取预设选项与自定义名字，结合当前展开状态计算显示文案
                val labelPreset = ConfigRepository.getTileLabelPreset(this@ExTileService)
                val customLabel = ConfigRepository.getTileLabelCustom(this@ExTileService)
                tile.label = ConfigRepository.TileLabelPreset.resolveLabel(
                    preset = labelPreset,
                    isExpanded = config.isExpanded,
                    customName = customLabel
                )

                // 磁贴图标：读取预设选项，映射为对应 drawable 资源；默认时用 ic_tile 显式设置。
                // TileService 在整个生命周期内复用同一个 Tile 对象：若默认分支不设置 icon，
                // 会残留上一次的非默认图标，导致切回默认后不刷新；而显式 setIcon(null) 在
                // 部分系统上 SystemUI 不回落默认图标，反而渲染灰色占位。故统一显式设置。
                val tileIconRes = ConfigRepository.TileIcon.iconRes(
                    ConfigRepository.getTileIcon(this@ExTileService)
                ) ?: R.drawable.ic_tile
                tile.icon = Icon.createWithResource(this@ExTileService, tileIconRes)

                tile.updateTile()
            } catch (e: Exception) {
                Log.e(TAG, "updateTileState failed", e)
            }
        }
    }
}
