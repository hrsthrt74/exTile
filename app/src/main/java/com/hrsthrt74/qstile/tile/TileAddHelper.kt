package com.hrsthrt74.qstile.tile

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.util.Log
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.function.Consumer

/**
 * 「把 exTile 磁贴加入控制中心（QS 磁贴集合）」的统一入口。
 *
 * 系统自 SDK 33 起提供了 [StatusBarManager.requestAddTileService]：
 * 由系统弹出确认框，用户同意后磁贴即被加入 `sysui_qs_tiles`，
 * 不依赖 Shizuku / WRITE_SECURE_SETTINGS（本应用唯一的提权需求留给磁贴切换）。
 *
 * 之所以抽成独立工具类：OOBE 的 P3「配置磁贴」页与磁贴编辑页的「更多」菜单
 * 都需要这一动作，且行为必须完全一致（含回调判定 + 延迟拉起控制中心）。
 * 两边各写一份迟早会漂移（例如一边忘记处理 ALREADY_ADDED）。
 */
object TileAddHelper {

    private const val TAG = "TileAddHelper"

    /**
     * 磁贴添加成功后，等系统的确认弹窗退场再拉起控制中心，避免两个面板动画打架。
     * 数值沿用 OOBE 的实现（400ms），改这里即两处同时生效。
     */
    private const val EXPAND_PANEL_DELAY_MS = 400L

    /**
     * 请求系统把 exTile 磁贴加入当前 QS 磁贴集合。
     *
     * 系统会弹出确认框，用户同意后磁贴即加入集合；回调仅作兜底，无需强提示。
     *
     * @param context 任意 Context（内部统一取 applicationContext，避免回调持有 Activity 造成泄漏）
     * @param tileLabel 磁贴显示名（由调用方用 `stringResource` 传入，避免在普通函数里
     *   用 `context.getString` 触发「Querying resource values」lint 警告）
     * @param scope 用于「延迟拉起控制中心」的协程作用域，应绑定调用页面的生命周期
     * @param onAdded 磁贴已加入集合（新加入或本来就在）时回调，供调用方置灰按钮等
     */
    fun requestAdd(
        context: Context,
        tileLabel: String,
        scope: CoroutineScope,
        onAdded: () -> Unit = {},
    ) {
        val appContext = context.applicationContext
        // 个别定制系统可能拿不到 StatusBarManager，直接放弃（不影响其它功能）
        val statusBarManager = appContext.getSystemService(StatusBarManager::class.java) ?: run {
            Log.w(TAG, "requestAdd skipped: StatusBarManager unavailable")
            return
        }
        val componentName = ComponentName(appContext, ExTileService::class.java)
        val tileIcon = Icon.createWithResource(appContext, R.drawable.ic_tile)
        statusBarManager.requestAddTileService(
            componentName,
            tileLabel,
            tileIcon,
            appContext.mainExecutor,
            Consumer { result ->
                // result 为 StatusBarManager 的回调常量：
                // TILE_ADD_REQUEST_RESULT_TILE_ADDED(2) / TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED(1)
                // 两者都表示磁贴已在集合中；其余（未添加 0 或各种错误码）不做任何处理
                if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
                ) {
                    onAdded()
                    // 顺带把控制中心拉起来，方便用户接着点「编辑」排布磁贴。
                    // 理想情况是直接进入系统的磁贴编辑页，但该页面是控制中心内部 View，
                    // shell 权限下没有任何入口（详见 SecureSettingsHelper.openQsTileEditor
                    // 的逆向注释），因此这里实际执行的是「展开控制面板」的回退分支。
                    // 先等系统的添加确认弹窗退场，避免两个面板的动画打架。
                    scope.launch {
                        delay(EXPAND_PANEL_DELAY_MS)
                        SecureSettingsHelper.openQsTileEditor()
                    }
                }
            }
        )
    }
}
