package com.hrsthrt74.qstile.viewmodel

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrsthrt74.qstile.data.BackupInfo
import com.hrsthrt74.qstile.data.BackupRepository
import com.hrsthrt74.qstile.data.BackupSnapshot
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.CreativeConfig
import com.hrsthrt74.qstile.data.ThemeRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SettingsScreen 的 ViewModel，负责管理设置页面的状态和业务逻辑。
 *
 * 主要职责：
 * - 管理所有设置项的状态
 * - 处理备份/恢复/导入操作
 * - 加载应用列表等耗时数据
 * - 提供工具函数（JSON解析等）
 */
class SettingsViewModel : ViewModel() {

    // ==================== 核心配置状态 ====================

    /** 当前磁贴配置 */
    var config by mutableStateOf(TileConfig())
        private set

    /** 系统磁贴配置（sysui_qs_tiles） */
    var currentSysuiTiles by mutableStateOf("")
        private set

    /** 备份 JSON 文本 */
    var backupText by mutableStateOf("")

    /** 设置是否已加载完成 */
    var settingsLoaded by mutableStateOf(false)
        private set

    // ==================== 通用设置状态 ====================

    /** 长按 exTile 磁贴行为 */
    var longPressBehavior by mutableIntStateOf(ConfigRepository.LongPressBehavior.OPEN_EXTILE)

    /** 自定义应用包名 */
    var customAppPackage by mutableStateOf("")

    /** 磁贴名预设选项 */
    var tileLabelPreset by mutableIntStateOf(ConfigRepository.TileLabelPreset.MORE_TILES)

    /** 自定义磁贴名 */
    var tileLabelCustom by mutableStateOf("")

    /** 磁贴图标选项 */
    var tileIcon by mutableIntStateOf(ConfigRepository.TileIcon.DEFAULT)

    /** 自定义磁贴名输入（临时） */
    var customTileLabelInput by mutableStateOf("")

    /** 无字模式同步 */
    var wordlessModeSync by mutableStateOf(false)

    /** 融合设备中心同步 */
    var smartDeviceControlSync by mutableStateOf(false)

    /** 自动收起 */
    var autoCollapseOnClose by mutableStateOf(false)

    /**
     * 平板固定磁贴预设（仅小米平板有意义，见 [ConfigRepository.TabletFixedTiles]）。
     * 变更时会顺带迁移已保存的磁贴列表，因此不直接用 `saveXxx` 那种一行写入的写法。
     */
    var tabletFixedTiles by mutableIntStateOf(ConfigRepository.TabletFixedTiles.WIFI_BT)
        private set

    // ==================== 创造模式状态 ====================

    /** 创造模式开关（开启后会写系统布局，因此状态由写入结果驱动，不做乐观翻转） */
    var creativeModeEnabled by mutableStateOf(false)
        private set

    /** 是否正在开启 / 关闭创造模式（期间禁用开关，避免连续点击产生并发写入） */
    var creativeModeBusy by mutableStateOf(false)
        private set

    /**
     * 创造模式配置（两层布局 + 每层开关 + 生效层）。
     *
     * 设置页自己只用得着 [CreativeConfig.enabled]，但**备份必须带上完整的两层数据**，
     * 否则用户备份再恢复后精心调好的两层就没了。因此这里保存整份配置，
     * 打开备份 Sheet 前会再刷新一次（编辑页可能刚改过）。
     */
    var creativeConfig by mutableStateOf(CreativeConfig())
        private set

    // ==================== 应用选择器状态 ====================

    /** 可启动应用列表（null 表示未加载） */
    var launchableApps by mutableStateOf<List<LaunchableApp>?>(null)
        private set

    /** 应用搜索关键字 */
    var appSearchQuery by mutableStateOf("")

    // ==================== 备份相关状态 ====================

    /** 导入备份 JSON */
    var importBackupJson by mutableStateOf("")

    /** 本地备份列表 */
    var backupList by mutableStateOf<List<BackupInfo>>(emptyList())
        private set

    /** 待删除的备份 */
    var deleteTargetBackup by mutableStateOf<BackupInfo?>(null)

    // ==================== UI 状态 ====================

    /** 当前展开的设置分类 */
    var expandedCategory by mutableStateOf<SettingsCategory?>(null)

    // ==================== 数据模型 ====================

    /** 应用选择器数据模型 */
    data class LaunchableApp(
        val packageName: String,
        val label: String,
        val icon: Drawable
    )

    /** 设置页可展开分类的标识 */
    enum class SettingsCategory {
        GENERAL,    // 通用
        APPEARANCE, // 外观
        DATA,       // 数据
        ABOUT,      // 关于
        ADVANCED    // 高级（创造模式 / 从系统导入 / 隐私统计 / 重跑引导）
    }

    // ==================== 初始化方法 ====================

    /**
     * 加载所有设置项（首次进入页面时调用）
     */
    suspend fun loadSettings(context: Context) {
        config = ConfigRepository.getConfig(context)
        longPressBehavior = ConfigRepository.getLongPressBehavior(context)
        customAppPackage = ConfigRepository.getLongPressCustomApp(context)
        tileLabelPreset = ConfigRepository.getTileLabelPreset(context)
        tileLabelCustom = ConfigRepository.getTileLabelCustom(context)
        tileIcon = ConfigRepository.getTileIcon(context)
        wordlessModeSync = ConfigRepository.getWordlessModeSync(context)
        smartDeviceControlSync = ConfigRepository.getSmartDeviceControlSync(context)
        autoCollapseOnClose = ConfigRepository.getAutoCollapseOnClose(context)
        // 平板固定磁贴预设（仅小米平板会在设置页显示该选项，其余设备读取到的只是无害的默认值）
        tabletFixedTiles = ConfigRepository.getTabletFixedTiles(context)
        // 创造模式：开关与两层数据一起读，供设置页开关与「备份」两处使用
        creativeConfig = ConfigRepository.getCreativeConfig(context)
        creativeModeEnabled = creativeConfig.enabled
        settingsLoaded = true
    }

    /**
     * 开启 / 关闭创造模式。
     *
     * 开关**不做乐观翻转**：状态只在系统写入成功后才变，因为这两个动作都有系统副作用
     * （开启 = 把层 1 写进 sysui_qs_tiles；关闭 = 把普通布局写回去）。写入失败时开关保持原位，
     * 由调用方弹 Toast 说明原因，避免出现「界面说开了、系统没变」的错位。
     *
     * @param enabled 目标状态
     * @param onResult 结果回调（调用方据此弹对应文案的 Toast）
     */
    fun setCreativeMode(
        context: Context,
        enabled: Boolean,
        onResult: (ConfigRepository.CreativeModeResult) -> Unit,
    ) {
        if (creativeModeBusy) return
        creativeModeBusy = true
        viewModelScope.launch {
            val result = if (enabled) {
                ConfigRepository.enterCreativeMode(context).also { r ->
                    if (r == ConfigRepository.CreativeModeResult.Success) {
                        creativeModeEnabled = true
                        // 备份读的是 creativeConfig，开关变化后同步一次
                        creativeConfig = ConfigRepository.getCreativeConfig(context)
                    }
                }
            } else {
                if (ConfigRepository.exitCreativeMode(context)) {
                    creativeModeEnabled = false
                    creativeConfig = creativeConfig.copy(enabled = false)
                    ConfigRepository.CreativeModeResult.Success
                } else {
                    ConfigRepository.CreativeModeResult.WriteFailed
                }
            }
            creativeModeBusy = false
            onResult(result)
        }
    }

    /**
     * 加载系统磁贴配置（后台执行，不阻塞页面渲染）
     */
    fun loadSystemTiles(context: Context) {
        viewModelScope.launch {
            currentSysuiTiles = SecureSettingsHelper.getSysuiQsTiles(context) ?: ""
        }
    }

    /**
     * 加载可启动应用列表
     */
    fun loadLaunchableApps(context: Context) {
        if (launchableApps != null) return // 已加载

        viewModelScope.launch {
            launchableApps = withContext(Dispatchers.IO) {
                getLaunchableApps(context)
            }
        }
    }

    /**
     * 加载本地备份列表
     */
    fun loadBackupList(context: Context) {
        viewModelScope.launch {
            backupList = BackupRepository.getBackupList(context)
        }
    }

    // ==================== 业务逻辑方法 ====================

    /**
     * 打开备份 Sheet 前刷新一次配置快照。
     *
     * 设置页的 `loadSettings` 只在页面首次组合时执行，而 Pager 会保留页面：
     * 用户去编辑页改完磁贴再切回来，内存里的 config / creativeConfig 仍是旧值。
     * 备份必须是「此刻的配置」，所以生成 JSON 前重新读一遍 DataStore。
     */
    suspend fun refreshBackupSource(context: Context) {
        config = ConfigRepository.getConfig(context)
        creativeConfig = ConfigRepository.getCreativeConfig(context)
        currentSysuiTiles = SecureSettingsHelper.getSysuiQsTiles(context) ?: ""
    }

    /**
     * 生成备份 JSON（供「复制到剪贴板」「另存为文件」使用）。
     *
     * 格式与本地备份文件完全一致：普通配置 + 创造模式（有数据时才带 `creative` 字段）。
     */
    fun generateBackupJson(): String {
        return BackupRepository.buildBackupJson(
            expandedTiles = config.expandedTiles,
            collapsedTiles = config.collapsedTiles,
            // 空配置（从没用过创造模式）不写 creative 字段：
            // 否则恢复这份备份会把用户后来调好的两层清空
            creative = creativeConfig.takeIf { it.hasData },
            // 当前系统列表只作排查参考；解析时忽略
            currentSysuiTiles = currentSysuiTiles
        )
    }

    /**
     * 保存备份到本地（含创造模式的两层数据）
     * @return 文件名，失败返回 null
     */
    suspend fun saveBackup(context: Context): String? {
        return BackupRepository.saveBackup(
            context = context,
            expandedTiles = config.expandedTiles,
            collapsedTiles = config.collapsedTiles,
            creative = creativeConfig.takeIf { it.hasData }
        )
    }

    /**
     * 恢复结果。
     *
     * 之所以不沿用 Boolean：创造模式的恢复比「写得进去 / 写不进去」多一种**部分成功**——
     * 配置数据（含两层布局）已经落库，只是当前系统布局没能切换过去（缺 WRITE_SECURE_SETTINGS）。
     * 这时 UI 必须给出与「格式错误」不同的提示，否则用户会以为整个恢复失败了。
     */
    sealed class RestoreResult {
        /** 全部恢复成功 */
        data object Success : RestoreResult()

        /** JSON 内容非法（不是 JSON / 缺 expandedTiles 或 collapsedTiles） */
        data object InvalidFormat : RestoreResult()

        /** 备份文件读不出来（不存在 / IO 失败），仅本地备份列表会出现 */
        data object LoadFailed : RestoreResult()

        /**
         * 普通配置已恢复，但当前正开着创造模式，普通布局要退出创造模式后才生效。
         *
         * 典型场景：升级到带创造模式的版本后导入旧版本备份。此时界面（磁贴面板）不会有
         * 任何变化，必须明确告诉用户「已经恢复成功了」，否则会以为导入失败反复重试。
         */
        data object RestoredCreativeActive : RestoreResult()

        /** 配置已恢复，但创造模式布局未能写入系统（层数据已在，检查权限后开关一次创造模式即可） */
        data object CreativeApplyFailed : RestoreResult()
    }

    /**
     * 从 JSON 文本恢复（粘贴 JSON / 从文件导入共用）。
     *
     * 兼容旧备份：没有 `creative` 字段时只恢复普通配置，创造模式配置原样保留。
     */
    suspend fun restoreFromJson(context: Context, json: String): RestoreResult {
        val snapshot = BackupRepository.parseBackupJson(json)
            ?: return RestoreResult.InvalidFormat
        return applyBackup(context, snapshot)
    }

    /**
     * 从本地备份列表恢复
     */
    suspend fun restoreFromBackup(context: Context, backup: BackupInfo): RestoreResult {
        val snapshot = BackupRepository.loadBackup(context, backup.fileName)
            ?: return RestoreResult.LoadFailed
        return applyBackup(context, snapshot)
    }

    /**
     * 把一份备份快照写入 DataStore，并处理创造模式的系统副作用。
     *
     * 普通配置与旧版一致：**只写 DataStore，不动系统布局**（下次点磁贴才应用）。
     * 创造模式必须额外考虑系统，因为它有一个「当前生效层」：
     * - 备份里 enabled=true：把备份的生效层写进系统（否则开关说开着、面板还是普通布局）；
     * - 备份里 enabled=false 但当前开着：把普通布局写回系统（相当于恢复时退出创造模式）；
     * - 两边都是关闭：没有任何系统副作用，纯配置恢复。
     *
     * enabled 采用「系统写成功才翻转」的写法，与 [setCreativeMode] 保持一致，
     * 避免出现「界面说开了、系统没变」的错位。
     */
    private suspend fun applyBackup(context: Context, snapshot: BackupSnapshot): RestoreResult {
        // 1. 普通配置先落库
        ConfigRepository.saveExpandedTiles(context, snapshot.config.expandedTiles)
        ConfigRepository.saveCollapsedTiles(context, snapshot.config.collapsedTiles)
        config = snapshot.config

        // 当前设备是否开着创造模式（后面判断系统副作用时还要用，只读一次）
        val wasEnabled = ConfigRepository.getCreativeConfig(context).enabled

        // 2. 没有创造模式数据（旧版本备份，或「从未用过创造模式」的空配置）：
        //    到此为止，不要碰当前的创造模式配置——当成「清空两层」是最坏的结果。
        val creative = snapshot.creative?.takeIf { it.hasData } ?: run {
            refreshSystemTiles(context)
            // 当前开着创造模式时，刚写入的普通配置在面板上还看不到（系统显示的是某一层）。
            // 这不是失败，但必须单独提示，否则用户会以为导入没生效。
            return if (wasEnabled) {
                RestoreResult.RestoredCreativeActive
            } else {
                RestoreResult.Success
            }
        }

        // 3. 两层数据先落库；enabled 暂存当前值，等系统写入成功后再翻
        val staged = creative.copy(enabled = wasEnabled)
        ConfigRepository.saveCreativeConfig(context, staged)
        creativeConfig = staged

        // 4. 没有系统副作用的情形：备份关闭、当前也关闭
        if (!creative.enabled && !wasEnabled) {
            refreshSystemTiles(context)
            return RestoreResult.Success
        }

        // 5. 系统布局同步
        val applied = if (creative.enabled) {
            ConfigRepository.applyCreativeLayer(context, creative.layer(creative.activeLayer))
        } else {
            ConfigRepository.applyNormalLayout(context)
        }
        if (!applied) {
            // 层数据已在 DataStore 里，只是没能切系统；enabled 保持原值不翻转
            return RestoreResult.CreativeApplyFailed
        }

        if (creative.enabled != wasEnabled) {
            ConfigRepository.saveCreativeEnabled(context, creative.enabled)
        }
        creativeConfig = creative
        refreshSystemTiles(context)
        return RestoreResult.Success
    }

    /** 刷新系统磁贴列表快照（恢复 / 切层后系统内容已变） */
    private suspend fun refreshSystemTiles(context: Context) {
        currentSysuiTiles = SecureSettingsHelper.getSysuiQsTiles(context) ?: ""
    }

    /**
     * 删除备份
     */
    suspend fun deleteBackup(context: Context, backup: BackupInfo): Boolean {
        val success = BackupRepository.deleteBackup(context, backup.fileName)
        if (success) {
            backupList = BackupRepository.getBackupList(context)
        }
        return success
    }

    /**
     * 从系统导入磁贴
     */
    suspend fun importFromSystem(context: Context): Boolean {
        val tiles = SecureSettingsHelper.getCurrentTiles(context)
        if (tiles.isNotEmpty()) {
            ConfigRepository.saveExpandedTiles(context, tiles)
            config = config.copy(expandedTiles = tiles)
            return true
        }
        return false
    }

    // ==================== 设置保存方法 ====================

    fun saveLongPressBehavior(context: Context, index: Int) {
        longPressBehavior = index
        viewModelScope.launch {
            ConfigRepository.saveLongPressBehavior(context, index)
        }
    }

    fun saveCustomAppPackage(context: Context, packageName: String) {
        customAppPackage = packageName
        viewModelScope.launch {
            ConfigRepository.saveLongPressCustomApp(context, packageName)
        }
    }

    fun saveTileLabelPreset(context: Context, index: Int) {
        tileLabelPreset = index
        viewModelScope.launch {
            ConfigRepository.saveTileLabelPreset(context, index)
        }
    }

    fun saveTileLabelCustom(context: Context, name: String) {
        tileLabelCustom = name
        viewModelScope.launch {
            ConfigRepository.saveTileLabelCustom(context, name)
        }
    }

    fun saveTileIcon(context: Context, index: Int) {
        tileIcon = index
        viewModelScope.launch {
            ConfigRepository.saveTileIcon(context, index)
        }
    }

    fun saveWordlessModeSync(context: Context, enabled: Boolean) {
        wordlessModeSync = enabled
        viewModelScope.launch {
            ConfigRepository.saveWordlessModeSync(context, enabled)
        }
    }

    fun saveSmartDeviceControlSync(context: Context, enabled: Boolean) {
        smartDeviceControlSync = enabled
        viewModelScope.launch {
            ConfigRepository.saveSmartDeviceControlSync(context, enabled)
        }
    }

    fun saveAutoCollapseOnClose(context: Context, enabled: Boolean) {
        autoCollapseOnClose = enabled
        viewModelScope.launch {
            ConfigRepository.saveAutoCollapseOnClose(context, enabled)
        }
    }

    /**
     * 切换平板固定磁贴预设。
     *
     * 与普通开关不同，这个动作会顺带迁移「已保存的磁贴列表」（把旧固定磁贴换成新的），
     * 因此**不做乐观翻转**：等 DataStore 写入成功后再更新内存状态，失败时保持原位并由
     * 调用方弹 Toast，避免出现「界面说切了、列表还是旧的」的错位。
     *
     * @param preset 目标预设，见 [ConfigRepository.TabletFixedTiles]
     * @param onResult 写入结果回调（true=成功）
     */
    fun saveTabletFixedTiles(context: Context, preset: Int, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = ConfigRepository.setTabletFixedTiles(context, preset)
            if (success) {
                tabletFixedTiles = preset
            }
            onResult(success)
        }
    }

    // ==================== 分类展开逻辑 ====================

    /**
     * 切换分类展开状态（手风琴模式）
     */
    fun toggleCategory(category: SettingsCategory) {
        expandedCategory = if (expandedCategory == category) null else category
    }

    // ==================== 清理方法 ====================

    fun clearAppSearchQuery() {
        appSearchQuery = ""
    }

    fun clearImportBackupJson() {
        importBackupJson = ""
    }

    fun clearCustomTileLabelInput() {
        customTileLabelInput = ""
    }

    fun clearDeleteTargetBackup() {
        deleteTargetBackup = null
    }

    // ==================== 私有工具方法 ====================

    /**
     * 获取所有可启动的应用
     */
    private fun getLaunchableApps(context: Context): List<LaunchableApp> {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0).mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val packageName = activityInfo.packageName
            val label = try {
                activityInfo.loadLabel(pm).toString()
            } catch (e: Exception) {
                packageName
            }
            val icon = try {
                activityInfo.loadIcon(pm)
            } catch (e: Exception) {
                null
            }
            if (icon != null) LaunchableApp(packageName, label, icon) else null
        }.distinctBy { it.packageName }.sortedBy { it.label }
    }
}
