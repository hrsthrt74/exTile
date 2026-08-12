package com.hrsthrt74.qstile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tile_config")

object ConfigRepository {
    private val EXPANDED_TILES = stringPreferencesKey("expanded_tiles")
    private val COLLAPSED_TILES = stringPreferencesKey("collapsed_tiles")
    private val IS_EXPANDED = booleanPreferencesKey("is_expanded")
    private val LONG_PRESS_BEHAVIOR = intPreferencesKey("long_press_behavior")
    private val LONG_PRESS_CUSTOM_APP = stringPreferencesKey("long_press_custom_app")
    private val WORDLESS_MODE_SYNC = booleanPreferencesKey("wordless_mode_sync")
    private val SMART_DEVICE_CONTROL_SYNC = booleanPreferencesKey("smart_device_control_sync")
    private val OOBE_COMPLETED = booleanPreferencesKey("oobe_completed")
    /** 收起 QS 面板时自动收起磁贴布局的开关 key */
    private val AUTO_COLLAPSE_ON_CLOSE = booleanPreferencesKey("auto_collapse_on_close")

    private val DEFAULT_EXPANDED = listOf("wifi", "bt", "cell", "airplane", "flashlight", "hotspot")
    private val DEFAULT_COLLAPSED = listOf("wifi", "bt", "cell")

    /** 长按 exTile 磁贴的行为选项 */
    object LongPressBehavior {
        const val OPEN_EXTILE = 0      // 跳转到 exTile 应用
        const val OPEN_SETTINGS = 1    // 跳转到系统设置
        const val OPEN_CUSTOM_APP = 2  // 跳转到自定义应用

        /** 合法的取值范围，用于数据校验 */
        const val MAX = OPEN_CUSTOM_APP
    }

    fun getConfigFlow(context: Context): Flow<TileConfig> {
        return context.dataStore.data.map { preferences ->
            TileConfig(
                expandedTiles = preferences[EXPANDED_TILES]?.split(",")?.filter { it.isNotBlank() } ?: DEFAULT_EXPANDED,
                collapsedTiles = preferences[COLLAPSED_TILES]?.split(",")?.filter { it.isNotBlank() } ?: DEFAULT_COLLAPSED,
                isExpanded = preferences[IS_EXPANDED] ?: false
            )
        }
    }

    suspend fun getConfig(context: Context): TileConfig {
        return getConfigFlow(context).first()
    }

    suspend fun saveExpandedTiles(context: Context, tiles: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[EXPANDED_TILES] = tiles.joinToString(",")
        }
    }

    suspend fun saveCollapsedTiles(context: Context, tiles: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[COLLAPSED_TILES] = tiles.joinToString(",")
        }
    }

    suspend fun saveIsExpanded(context: Context, isExpanded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_EXPANDED] = isExpanded
        }
    }

    /**
     * 获取长按 exTile 磁贴的行为配置（int 值）
     * @param context Context
     * @return 行为配置，见 [LongPressBehavior]
     */
    suspend fun getLongPressBehavior(context: Context): Int {
        return context.dataStore.data.map { preferences ->
            val value = preferences[LONG_PRESS_BEHAVIOR] ?: LongPressBehavior.OPEN_EXTILE
            // 校验取值范围，防止旧版本残留数据导致索引越界
            if (value in 0..LongPressBehavior.MAX) value else LongPressBehavior.OPEN_EXTILE
        }.first()
    }

    /**
     * 保存长按 exTile 磁贴的行为配置
     * @param context Context
     * @param behavior 行为配置，见 [LongPressBehavior]
     */
    suspend fun saveLongPressBehavior(context: Context, behavior: Int) {
        context.dataStore.edit { preferences ->
            preferences[LONG_PRESS_BEHAVIOR] = behavior
        }
    }

    /**
     * 获取长按 exTile 磁贴时跳转的自定义应用包名
     * @param context Context
     * @return 应用包名，未设置时返回空字符串
     */
    suspend fun getLongPressCustomApp(context: Context): String {
        return context.dataStore.data.map { preferences ->
            preferences[LONG_PRESS_CUSTOM_APP] ?: ""
        }.first()
    }

    /**
     * 保存长按 exTile 磁贴时跳转的自定义应用包名
     * @param context Context
     * @param packageName 应用包名
     */
    suspend fun saveLongPressCustomApp(context: Context, packageName: String) {
        context.dataStore.edit { preferences ->
            preferences[LONG_PRESS_CUSTOM_APP] = packageName
        }
    }

    suspend fun toggleExpanded(context: Context): Boolean {
        val config = getConfig(context)
        val newIsExpanded = !config.isExpanded
        saveIsExpanded(context, newIsExpanded)
        return newIsExpanded
    }

    /**
     * 获取「展开收起同时控制无字模式」开关状态
     * @param context Context
     * @return 是否启用：启用后展开/收起「通用」分类会同步写入 secure settings 的 wordless_mode
     */
    suspend fun getWordlessModeSync(context: Context): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[WORDLESS_MODE_SYNC] ?: false
        }.first()
    }

    /**
     * 保存「展开收起同时控制无字模式」开关状态
     * @param context Context
     * @param enabled 是否启用
     */
    suspend fun saveWordlessModeSync(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WORDLESS_MODE_SYNC] = enabled
        }
    }

    /**
     * 获取「展开收起同时控制融合设备中心」开关状态
     * @param context Context
     * @return 是否启用：启用后磁贴展开/收起会同步写入 secure settings 的 smart_device_control
     */
    suspend fun getSmartDeviceControlSync(context: Context): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[SMART_DEVICE_CONTROL_SYNC] ?: false
        }.first()
    }

    /**
     * 保存「展开收起同时控制融合设备中心」开关状态
     * @param context Context
     * @param enabled 是否启用
     */
    suspend fun saveSmartDeviceControlSync(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SMART_DEVICE_CONTROL_SYNC] = enabled
        }
    }

    // ==================== 收起面板时自动收起 ====================

    /**
     * 获取「收起面板时自动收起」开关状态
     * @param context Context
     * @return 是否启用：启用后用户收起 QS 面板时，若磁贴处于展开布局会自动执行收起操作
     */
    suspend fun getAutoCollapseOnClose(context: Context): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_COLLAPSE_ON_CLOSE] ?: false
        }.first()
    }

    /**
     * 保存「收起面板时自动收起」开关状态
     * @param context Context
     * @param enabled 是否启用
     */
    suspend fun saveAutoCollapseOnClose(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_COLLAPSE_ON_CLOSE] = enabled
        }
    }

    // ==================== OOBE（首次使用引导） ====================

    /**
     * 获取「OOBE 是否已完成」标志流。
     * 默认 false（首次安装/未完成引导时显示引导页面）。
     * @param context Context
     * @return OOBE 完成状态流
     */
    fun getOobeCompletedFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[OOBE_COMPLETED] ?: false
        }
    }

    /**
     * 保存「OOBE 是否已完成」标志。
     * @param context Context
     * @param completed true 表示引导已完成（下次启动直接进主页）；false 可触发「重新运行引导」
     */
    suspend fun saveOobeCompleted(context: Context, completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[OOBE_COMPLETED] = completed
        }
    }

    /**
     * 从系统导入磁贴配置（OOBE 首次配置专用）。
     * 复用 [SecureSettingsHelper.getCurrentTiles] 读取系统 sysui_qs_tiles 完整列表
     * （读取 secure settings 无需任何权限）。
     *
     * 规则：
     * 1. 完整列表原样保存为展开配置 expandedTiles —— 小米固定磁贴（手机 wifi/cell，平板 wifi/bt）默认
     *    就在最前，edit 默认在最后，无需任何移动处理，与编辑页派生逻辑天然对齐；
     * 2. 收起配置 collapsedTiles = [0..exTileIndex]（exTile 及之前的保留部分）+ 末尾补 "edit"
     *    （系统列表末尾通常有 edit，取前段后需补回，保证收起时仍有编辑入口）；
     * 3. isExpanded 保存为 true —— 系统当前显示的正是完整（展开）布局，若存 false 则用户首次点击
     *    exTile 时写入 expanded（系统无变化），体验困惑。
     *
     * @param context Context
     * @return [SystemImportResult]：Success(保留数/收纳数 + 两组磁贴列表) / NoExtile / ReadFailed
     */
    suspend fun importSystemTiles(context: Context): SystemImportResult {
        // 读取系统完整列表（读 secure settings 无需任何权限）
        val tiles = SecureSettingsHelper.getCurrentTiles(context)
        if (tiles.isEmpty()) return SystemImportResult.ReadFailed
        // 校验是否含 exTile 锚点，否则无法确定展开/收起的边界
        val extileIndex = tiles.indexOf(TileCatalog.EXTILE_CUSTOM)
        if (extileIndex < 0) return SystemImportResult.NoExtile

        // 展开 = 系统完整列表原样保存
        val expandedTiles = tiles
        // 收起 = exTile 及之前的保留部分 + 末尾补回编辑入口
        val collapsedTiles = tiles.take(extileIndex + 1) + "edit"

        // 保存四组值（展开/收起/展开状态）
        saveExpandedTiles(context, expandedTiles)
        saveCollapsedTiles(context, collapsedTiles)
        saveIsExpanded(context, true)

        return SystemImportResult.Success(
            keepCount = collapsedTiles.size,
            hideCount = expandedTiles.size - collapsedTiles.size,
            expandedTiles = expandedTiles,
            collapsedTiles = collapsedTiles,
        )
    }

    /**
     * 系统导入结果。
     * @see importSystemTiles
     */
    sealed class SystemImportResult {
        /**
         * 导入成功。
         * @param keepCount 保留磁贴数（收起时也可见）
         * @param hideCount 收纳磁贴数（仅展开时可见）
         * @param expandedTiles 展开配置
         * @param collapsedTiles 收起配置
         */
        data class Success(
            val keepCount: Int,
            val hideCount: Int,
            val expandedTiles: List<String>,
            val collapsedTiles: List<String>,
        ) : SystemImportResult()

        /** 系统中未找到 exTile 磁贴（锚点缺失） */
        data object NoExtile : SystemImportResult()

        /** 读取系统配置失败 */
        data object ReadFailed : SystemImportResult()
    }
}
