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
import com.hrsthrt74.qstile.data.ConfigRepository
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
        ABOUT       // 关于
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
        settingsLoaded = true
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
     * 生成备份 JSON
     */
    fun generateBackupJson(): String {
        return buildString {
            append("{\n")
            append("  \"expandedTiles\": [${config.expandedTiles.joinToString(", ") { "\"$it\"" }}],\n")
            append("  \"collapsedTiles\": [${config.collapsedTiles.joinToString(", ") { "\"$it\"" }}],\n")
            append("  \"currentSysuiTiles\": \"$currentSysuiTiles\"\n")
            append("}")
        }
    }

    /**
     * 保存备份到本地
     * @return 文件名，失败返回 null
     */
    suspend fun saveBackup(context: Context): String? {
        return BackupRepository.saveBackup(
            context = context,
            expandedTiles = config.expandedTiles,
            collapsedTiles = config.collapsedTiles
        )
    }

    /**
     * 从 JSON 恢复配置
     * @return 是否成功
     */
    suspend fun restoreFromJson(context: Context, json: String): Boolean {
        val restored = parseBackupJson(json)
        if (restored != null) {
            ConfigRepository.saveExpandedTiles(context, restored.expandedTiles)
            ConfigRepository.saveCollapsedTiles(context, restored.collapsedTiles)
            config = restored
            return true
        }
        return false
    }

    /**
     * 从本地备份恢复
     */
    suspend fun restoreFromBackup(context: Context, backup: BackupInfo): Boolean {
        val restored = BackupRepository.loadBackup(context, backup.fileName)
        if (restored != null) {
            ConfigRepository.saveExpandedTiles(context, restored.expandedTiles)
            ConfigRepository.saveCollapsedTiles(context, restored.collapsedTiles)
            config = restored
            return true
        }
        return false
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

    /**
     * 从文件导入配置
     */
    suspend fun importFromFile(context: Context, json: String): Boolean {
        val restored = parseBackupJson(json)
        if (restored != null) {
            ConfigRepository.saveExpandedTiles(context, restored.expandedTiles)
            ConfigRepository.saveCollapsedTiles(context, restored.collapsedTiles)
            config = restored
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

    /**
     * 解析备份 JSON 字符串为 TileConfig
     */
    private fun parseBackupJson(json: String): TileConfig? {
        return try {
            val expandedMatch = Regex("\"expandedTiles\":\\s*\\[([^\\]]+)\\]").find(json)
            val collapsedMatch = Regex("\"collapsedTiles\":\\s*\\[([^\\]]+)\\]").find(json)
            if (expandedMatch != null && collapsedMatch != null) {
                val expanded = expandedMatch.groupValues[1].split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }
                val collapsed = collapsedMatch.groupValues[1].split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }
                TileConfig(expandedTiles = expanded, collapsedTiles = collapsed)
            } else null
        } catch (_: Exception) { null }
    }
}
