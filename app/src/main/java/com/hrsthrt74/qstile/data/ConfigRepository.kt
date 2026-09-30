package com.hrsthrt74.qstile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hrsthrt74.qstile.R
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
    private val TILE_LABEL_PRESET = intPreferencesKey("tile_label_preset")
    private val TILE_LABEL_CUSTOM = stringPreferencesKey("tile_label_custom")
    private val TILE_ICON = intPreferencesKey("tile_icon")
    private val WORDLESS_MODE_SYNC = booleanPreferencesKey("wordless_mode_sync")
    private val SMART_DEVICE_CONTROL_SYNC = booleanPreferencesKey("smart_device_control_sync")
    private val OOBE_COMPLETED = booleanPreferencesKey("oobe_completed")
    /** 收起 QS 面板时自动收起磁贴布局的开关 key */
    private val AUTO_COLLAPSE_ON_CLOSE = booleanPreferencesKey("auto_collapse_on_close")
    /** MS Clarity 匿名统计的用户同意状态 key */
    private val CLARITY_CONSENT = booleanPreferencesKey("clarity_consent")
    /** 自动检查更新开关 key（默认关闭） */
    private val AUTO_CHECK_UPDATE = booleanPreferencesKey("auto_check_update")

    private val DEFAULT_EXPANDED = listOf("wifi", "bt", "cell", "airplane", "flashlight", "hotspot")
    private val DEFAULT_COLLAPSED = listOf("wifi", "bt", "cell")

    /** 长按 exTile 磁贴的行为选项 */
    object LongPressBehavior {
        const val OPEN_EXTILE = 0      // 跳转到 exTile 应用
        const val OPEN_SETTINGS = 1    // 跳转到系统设置
        const val OPEN_CUSTOM_APP = 2  // 跳转到自定义应用
        const val OPEN_DEVICE_CENTER = 3 // 跳转到融合设备中心（小米互联服务）

        /** 合法的取值范围，用于数据校验 */
        const val MAX = OPEN_DEVICE_CENTER
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

    /** 磁贴名字（QS 面板中 exTile 磁贴显示的标签）的预设选项 */
    object TileLabelPreset {
        const val MORE_TILES = 0      // 更多磁贴
        const val EXPAND_COLLAPSE = 1 // 展开 / 收起
        const val SWITCH_TILES = 2    // 切换磁贴
        const val CUSTOM = 3          // 自定义

        /** 合法的取值范围，用于数据校验 */
        const val MAX = CUSTOM

        /** 预设选项对应的磁贴名字文案 */
        val labels = listOf("更多磁贴", "展开 / 收起", "切换磁贴", "自定义")

        /**
         * 根据预设选项与当前展开状态计算磁贴名字。
         * 无需 I/O，纯静态计算，供 ExTileService 直接调用。
         * @param preset 预设选项，见 [TileLabelPreset]
         * @param isExpanded 当前是否处于展开状态
         * @param customName 自定义名字（仅 CUSTOM 预设使用）
         * @return 磁贴名字文案
         */
        fun resolveLabel(preset: Int, isExpanded: Boolean, customName: String): String {
            return when (preset) {
                MORE_TILES -> labels[0]
                EXPAND_COLLAPSE -> if (isExpanded) "收起" else "展开"
                SWITCH_TILES -> labels[2]
                CUSTOM -> if (customName.isNotBlank()) customName.trim() else labels[0]
                else -> labels[0]
            }
        }
    }

    /** exTile 磁贴图标（QS 面板中 exTile 磁贴显示的图标）的预设选项 */
    object TileIcon {
        const val DEFAULT = 0      // 默认图标（manifest 中声明的 ic_tile）
        const val ADD = 1          // Add
        const val ADD_CIRCLE = 2   // AddCircle
        const val ALL = 3          // All
        const val GRID_VIEW = 4    // GridView
        const val EXPAND_MORE = 5  // ExpandMore

        /** 合法的取值范围，用于数据校验 */
        const val MAX = EXPAND_MORE

        /** 预设选项对应的显示文案（与 SettingsScreen 下拉项顺序一致） */
        val labels = listOf("默认", "Add", "AddCircle", "All", "GridView", "ExpandMore")

        /**
         * 根据预设选项返回对应的 drawable 资源 id。
         * 纯静态查询，供 ExTileService / SettingsScreen 直接调用。
         * @param preset 预设选项，见 [TileIcon]
         * @return drawable 资源 id；DEFAULT 或非法值时返回 null（表示回落 manifest 默认图标）
         */
        fun iconRes(preset: Int): Int? = when (preset) {
            DEFAULT -> null
            ADD -> R.drawable.tile_icon_add
            ADD_CIRCLE -> R.drawable.tile_icon_add_circle
            ALL -> R.drawable.tile_icon_all
            GRID_VIEW -> R.drawable.tile_icon_grid_view
            EXPAND_MORE -> R.drawable.tile_icon_expand_more
            else -> null
        }
    }

    /**
     * 获取磁贴名字的预设选项（int 值）
     * @param context Context
     * @return 预设选项，见 [TileLabelPreset]
     */
    suspend fun getTileLabelPreset(context: Context): Int {
        return context.dataStore.data.map { preferences ->
            val value = preferences[TILE_LABEL_PRESET] ?: TileLabelPreset.MORE_TILES
            // 校验取值范围，防止旧版本残留数据导致索引越界
            if (value in 0..TileLabelPreset.MAX) value else TileLabelPreset.MORE_TILES
        }.first()
    }

    /**
     * 保存磁贴名字的预设选项
     * @param context Context
     * @param preset 预设选项，见 [TileLabelPreset]
     */
    suspend fun saveTileLabelPreset(context: Context, preset: Int) {
        context.dataStore.edit { preferences ->
            preferences[TILE_LABEL_PRESET] = preset
        }
    }

    /**
     * 获取自定义磁贴名字
     * @param context Context
     * @return 自定义名字，未设置时返回空字符串
     */
    suspend fun getTileLabelCustom(context: Context): String {
        return context.dataStore.data.map { preferences ->
            preferences[TILE_LABEL_CUSTOM] ?: ""
        }.first()
    }

    /**
     * 保存自定义磁贴名字
     * @param context Context
     * @param name 自定义名字
     */
    suspend fun saveTileLabelCustom(context: Context, name: String) {
        context.dataStore.edit { preferences ->
            preferences[TILE_LABEL_CUSTOM] = name
        }
    }

    /**
     * 获取 exTile 磁贴图标的预设选项（int 值）
     * @param context Context
     * @return 预设选项，见 [TileIcon]
     */
    suspend fun getTileIcon(context: Context): Int {
        return context.dataStore.data.map { preferences ->
            val value = preferences[TILE_ICON] ?: TileIcon.DEFAULT
            // 校验取值范围，防止旧版本残留数据导致索引越界
            if (value in 0..TileIcon.MAX) value else TileIcon.DEFAULT
        }.first()
    }

    /**
     * 保存 exTile 磁贴图标的预设选项
     * @param context Context
     * @param preset 预设选项，见 [TileIcon]
     */
    suspend fun saveTileIcon(context: Context, preset: Int) {
        context.dataStore.edit { preferences ->
            preferences[TILE_ICON] = preset
        }
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

    // ==================== MS Clarity 匿名统计同意 ====================

    /**
     * 获取「MS Clarity 匿名统计」同意状态流。
     * 默认 false（首次安装/未同意时不收集数据）。
     * @param context Context
     * @return 同意状态流
     */
    fun getClarityConsentFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[CLARITY_CONSENT] ?: false
        }
    }

    /**
     * 获取「MS Clarity 匿名统计」同意状态（挂起函数）。
     * @param context Context
     * @return 是否同意
     */
    suspend fun getClarityConsent(context: Context): Boolean {
        return getClarityConsentFlow(context).first()
    }

    /**
     * 保存「MS Clarity 匿名统计」同意状态。
     * @param context Context
     * @param consent true 表示同意收集数据；false 表示不同意
     */
    suspend fun saveClarityConsent(context: Context, consent: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CLARITY_CONSENT] = consent
        }
    }

    // ==================== 自动检查更新 ====================

    /**
     * 获取「自动检查更新」开关状态流。
     * 默认 false（可选项，用户不主动开启就不联网检查）。
     * @param context Context
     * @return 开关状态流
     */
    fun getAutoCheckUpdateFlow(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_CHECK_UPDATE] ?: false
        }
    }

    /**
     * 获取「自动检查更新」开关状态（挂起函数）。
     * @param context Context
     * @return 是否开启自动检查更新
     */
    suspend fun getAutoCheckUpdate(context: Context): Boolean {
        return getAutoCheckUpdateFlow(context).first()
    }

    /**
     * 保存「自动检查更新」开关状态。
     * @param context Context
     * @param enabled true 表示启动时自动检查；false 表示不检查
     */
    suspend fun saveAutoCheckUpdate(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CHECK_UPDATE] = enabled
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
