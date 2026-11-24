package com.hrsthrt74.qstile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hjq.device.compat.DeviceOs
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

    // ==================== 创造模式（两套独立布局） ====================
    // 全部使用独立 key，与普通模式的 expanded/collapsed/is_expanded 完全隔离：
    // 开启创造模式只切这组 key，退出后普通配置原样可用。

    /** 创造模式总开关 key */
    private val CREATIVE_MODE_ENABLED = booleanPreferencesKey("creative_mode_enabled")

    /** 当前生效层 key（1 基，非法值读取时收敛到 1..CREATIVE_LAYER_COUNT） */
    private val CREATIVE_MODE_ACTIVE_LAYER = intPreferencesKey("creative_mode_active_layer")

    /** 层 1 / 层 2 的完整磁贴列表（逗号拼接，含固定磁贴与 edit） */
    private val CREATIVE_LAYER1_TILES = stringPreferencesKey("creative_layer1_tiles")
    private val CREATIVE_LAYER2_TILES = stringPreferencesKey("creative_layer2_tiles")

    /** 层 1 / 层 2 的「显示磁贴名」开关（true → wordless_mode=0） */
    private val CREATIVE_LAYER1_SHOW_LABEL = booleanPreferencesKey("creative_layer1_show_label")
    private val CREATIVE_LAYER2_SHOW_LABEL = booleanPreferencesKey("creative_layer2_show_label")

    /** 层 1 / 层 2 的「显示融合设备中心」开关（true → smart_device_control=1） */
    private val CREATIVE_LAYER1_DEVICE_CENTER = booleanPreferencesKey("creative_layer1_device_center")
    private val CREATIVE_LAYER2_DEVICE_CENTER = booleanPreferencesKey("creative_layer2_device_center")

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

        /**
         * 创造模式下的磁贴名字计算。
         *
         * 只有「展开 / 收起」这个预设在创造模式下语义失效（点击不再展开/收起，而是切层），
         * 改为显示当前层号（如「层 2」）；其余预设与展开状态无关，直接复用 [resolveLabel]。
         *
         * @param preset 预设选项，见 [TileLabelPreset]
         * @param layer 当前生效层（1 基）
         * @param customName 自定义名字（仅 CUSTOM 预设使用）
         * @param layerText 层号文案（由调用方用资源格式化，如「层 2」）
         * @return 磁贴名字文案
         */
        fun resolveCreativeLabel(
            preset: Int,
            layer: Int,
            customName: String,
            layerText: String
        ): String {
            return when (preset) {
                EXPAND_COLLAPSE -> layerText
                // 其余预设与「是否展开」无关，传 false 不影响结果（CUSTOM 仍走自定义名）
                else -> resolveLabel(preset, false, customName)
            }.ifBlank { layerText }
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
        // 校验是否含 exTile 锚点，否则无法确定展开/收起的边界。
        // 用规范化比较兼容相对/完整类名两种 spec 写法，以及 debug 变体的 .debug 包名后缀。
        val extileIndex = tiles.indexOfFirst { CustomTileUtils.isExTileSpec(it) }
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

    // ==================== 创造模式（两套独立布局） ====================

    /**
     * 创造模式配置流。任意 key 变化都会重新发射（DataStore 的固有行为），
     * 但 [CreativeConfig] 是 data class，值不变时 Compose 的结构化相等会拦掉重组。
     */
    fun getCreativeConfigFlow(context: Context): Flow<CreativeConfig> {
        return context.dataStore.data.map { preferences ->
            CreativeConfig(
                enabled = preferences[CREATIVE_MODE_ENABLED] ?: false,
                // 旧版本/脏数据可能留下越界层号，统一收敛
                activeLayer = (preferences[CREATIVE_MODE_ACTIVE_LAYER] ?: 1)
                    .coerceIn(1, CREATIVE_LAYER_COUNT),
                layers = listOf(
                    CreativeLayer(
                        tiles = parseTileList(preferences[CREATIVE_LAYER1_TILES]),
                        showTileLabels = preferences[CREATIVE_LAYER1_SHOW_LABEL] ?: true,
                        showDeviceCenter = preferences[CREATIVE_LAYER1_DEVICE_CENTER] ?: false,
                    ),
                    CreativeLayer(
                        tiles = parseTileList(preferences[CREATIVE_LAYER2_TILES]),
                        showTileLabels = preferences[CREATIVE_LAYER2_SHOW_LABEL] ?: true,
                        showDeviceCenter = preferences[CREATIVE_LAYER2_DEVICE_CENTER] ?: false,
                    ),
                ),
            )
        }
    }

    /** 一次性读取创造模式配置 */
    suspend fun getCreativeConfig(context: Context): CreativeConfig {
        return getCreativeConfigFlow(context).first()
    }

    /** 逗号分隔字符串 → 磁贴列表（空条目直接丢弃） */
    private fun parseTileList(raw: String?): List<String> {
        return raw?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    /** 层号（1 基）→ 对应的列表 key；非法层号按层 1 处理 */
    private fun tilesKeyOf(layer: Int) =
        if (layer == 2) CREATIVE_LAYER2_TILES else CREATIVE_LAYER1_TILES

    private fun showLabelKeyOf(layer: Int) =
        if (layer == 2) CREATIVE_LAYER2_SHOW_LABEL else CREATIVE_LAYER1_SHOW_LABEL

    private fun deviceCenterKeyOf(layer: Int) =
        if (layer == 2) CREATIVE_LAYER2_DEVICE_CENTER else CREATIVE_LAYER1_DEVICE_CENTER

    /** 保存创造模式总开关 */
    suspend fun saveCreativeEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CREATIVE_MODE_ENABLED] = enabled
        }
    }

    /** 保存当前生效层（越界自动收敛，避免写入脏数据） */
    suspend fun saveCreativeActiveLayer(context: Context, layer: Int) {
        val safe = layer.coerceIn(1, CREATIVE_LAYER_COUNT)
        context.dataStore.edit { preferences ->
            preferences[CREATIVE_MODE_ACTIVE_LAYER] = safe
        }
    }

    /** 保存指定层的完整磁贴列表（只写这一个 key，避免整份重写引发多余重组） */
    suspend fun saveCreativeLayerTiles(context: Context, layer: Int, tiles: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[tilesKeyOf(layer)] = tiles.joinToString(",")
        }
    }

    /** 保存指定层的「显示磁贴名」开关 */
    suspend fun saveCreativeLayerShowLabels(context: Context, layer: Int, show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[showLabelKeyOf(layer)] = show
        }
    }

    /** 保存指定层的「显示融合设备中心」开关 */
    suspend fun saveCreativeLayerShowDeviceCenter(context: Context, layer: Int, show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[deviceCenterKeyOf(layer)] = show
        }
    }

    /** 整层写入（磁贴列表 + 两个开关），首次开启时种子化两层用，一次 edit 写完避免中间态 */
    suspend fun saveCreativeLayer(context: Context, layer: Int, value: CreativeLayer) {
        context.dataStore.edit { preferences ->
            preferences[tilesKeyOf(layer)] = value.tiles.joinToString(",")
            preferences[showLabelKeyOf(layer)] = value.showTileLabels
            preferences[deviceCenterKeyOf(layer)] = value.showDeviceCenter
        }
    }

    /**
     * 一次性写入整份创造模式配置（恢复备份用）。
     *
     * 必须用**单次** `edit` 写完所有 key：DataStore 每次 edit 都会让所有订阅流重新发射，
     * 拆成「开关 / 层 1 / 层 2」多次写会让编辑页看到两层数据新旧混杂的中间态。
     *
     * @param context Context
     * @param config 完整的创造模式配置（越界层号自动收敛）
     */
    suspend fun saveCreativeConfig(context: Context, config: CreativeConfig) {
        context.dataStore.edit { preferences ->
            preferences[CREATIVE_MODE_ENABLED] = config.enabled
            preferences[CREATIVE_MODE_ACTIVE_LAYER] = config.activeLayer
                .coerceIn(1, CREATIVE_LAYER_COUNT)
            for (layer in 1..CREATIVE_LAYER_COUNT) {
                val value = config.layer(layer)
                preferences[tilesKeyOf(layer)] = value.tiles.joinToString(",")
                preferences[showLabelKeyOf(layer)] = value.showTileLabels
                preferences[deviceCenterKeyOf(layer)] = value.showDeviceCenter
            }
        }
    }

    /**
     * 保证磁贴列表里存在**当前变体**的 exTile 磁贴（创造模式下它是唯一的「切层」入口，
     * 缺了就切不回来）。
     *
     * 只认当前变体（[CustomTileUtils.isExTileSpec]），不能用「任意变体」判断：
     * Release 与 Debug 是两个独立应用、各有一份自己的 DataStore，另一个变体的磁贴点下去
     * 切的是它自己的布局，对本应用的层毫无影响——它在层里就只是一张普通磁贴，用户应该能删。
     * （曾经用 isAnyExTileSpec 判断，导致 debug 版用户从系统导入 Release 磁贴后删不掉。）
     *
     * 缺失时补到 "edit" 之前；没有 "edit" 则追加到末尾。
     * 纯函数，供 [ConfigRepository] 与 ExTileService / ViewModel 共用。
     */
    fun ensureExTile(tiles: List<String>): List<String> {
        if (tiles.any { CustomTileUtils.isExTileSpec(it) }) return tiles
        val result = tiles.toMutableList()
        val editIndex = result.indexOf("edit")
        if (editIndex >= 0) result.add(editIndex, TileCatalog.EXTILE_CUSTOM)
        else result.add(TileCatalog.EXTILE_CUSTOM)
        return result
    }

    /**
     * 把某一层写进系统，并同步该层的「当切换到此层时……」两个开关。
     *
     * 从 `ExTileService.applyCreativeLayer` 抽出：点击切层、自动回到层 1、**恢复备份**
     * 三处都要做同一件事，分散实现容易出现「恢复备份后无字模式没跟着变」这类不一致。
     *
     * @param context Context（自动收起路径传 applicationContext）
     * @param layer 目标层配置
     * @return 是否成功写入系统布局；层数据为空时直接返回 false（避免把面板写成只剩 exTile）
     */
    suspend fun applyCreativeLayer(context: Context, layer: CreativeLayer): Boolean {
        // 空层不写系统：既保护「恢复了个残缺备份」的情况，也避免误清空用户面板
        if (layer.tiles.isEmpty()) return false
        // 兜底：层里必须始终存在 exTile，否则切过去就再也切不回来了
        val tiles = ensureExTile(layer.tiles)
        if (!SecureSettingsHelper.setCurrentTiles(context, tiles)) return false

        // 「显示磁贴名」= 无字模式的反向：显示名字 → wordless_mode=0，隐藏 → 1。
        // wordless_mode 是 HyperOS 专属 Secure Setting，非小米机型不写无关的 key。
        // 这里直接用 DeviceOs 判断（只读 Build 属性，成本极低），不构造整个 DeviceProfile：
        // 磁贴点击路径上禁止任何可能触发 binder 调用的重活。
        if (DeviceOs.isMiui() || DeviceOs.isHyperOs()) {
            SecureSettingsHelper.putSecureSetting(
                context,
                "wordless_mode",
                if (layer.showTileLabels) "0" else "1"
            )
        }

        // 「显示融合设备中心」：显示 → smart_device_control=1，隐藏 → 0
        SecureSettingsHelper.putSecureSetting(
            context,
            "smart_device_control",
            if (layer.showDeviceCenter) "1" else "0"
        )
        return true
    }

    /**
     * 把普通模式的布局按当前展开状态写回系统。
     *
     * 供 [exitCreativeMode] 与「恢复备份需要关闭创造模式」共用。
     *
     * @return 是否写入成功
     */
    suspend fun applyNormalLayout(context: Context): Boolean {
        val config = getConfig(context)
        val tiles = if (config.isExpanded) config.expandedTiles else config.collapsedTiles
        return SecureSettingsHelper.setCurrentTiles(context, tiles)
    }

    /** 开启创造模式的结果，供设置页给出精确提示 */
    sealed class CreativeModeResult {
        /** 已切换成功 */
        data object Success : CreativeModeResult()

        /** 读不到系统当前的磁贴配置 */
        data object ReadFailed : CreativeModeResult()

        /** 写入系统布局失败（通常是没给 WRITE_SECURE_SETTINGS） */
        data object WriteFailed : CreativeModeResult()
    }

    /**
     * 开启创造模式。
     *
     * 顺序很重要：**先写系统、成功后才落 flag**。反序会出现「flag 说开着、系统没变、
     * 普通配置也再也回不去」的坏状态。
     *
     * 首次开启（两层都还没有数据）时：
     * 1. 层 1 = 当前系统 `sysui_qs_tiles` 原样（保证开启瞬间面板外观零变化）；
     * 2. 层 2 = 层 1 副本（用户在此基础上改差异，第一次切层不会看到空面板）；
     * 3. 两层的「显示磁贴名 / 融合设备中心」按系统当前值反推种子化——否则第一次
     *    切回层 1 时会把用户原本的无字模式/设备中心状态覆盖掉。
     *
     * 非首次开启：保留用户已编辑的两层，直接应用上次的生效层。
     *
     * @return [CreativeModeResult]
     */
    suspend fun enterCreativeMode(context: Context): CreativeModeResult {
        // 读当前系统布局（读 secure settings 不需要权限）
        val current = SecureSettingsHelper.getCurrentTiles(context)
        if (current.isEmpty()) return CreativeModeResult.ReadFailed
        val seededTiles = ensureExTile(current)

        val existing = getCreativeConfig(context)
        // 两层都没数据 → 视为首次开启（也覆盖「只有一层被脏数据写空」的异常情况）
        val firstTime = existing.layers.all { it.tiles.isEmpty() }
        val targetLayer: Int
        if (firstTime) {
            val showLabels = SecureSettingsHelper.getSecureSetting(context, "wordless_mode") != "1"
            val showDeviceCenter =
                SecureSettingsHelper.getSecureSetting(context, "smart_device_control") == "1"
            val seed = CreativeLayer(
                tiles = seededTiles,
                showTileLabels = showLabels,
                showDeviceCenter = showDeviceCenter,
            )
            saveCreativeLayer(context, 1, seed)
            saveCreativeLayer(context, 2, seed)
            targetLayer = 1
        } else {
            targetLayer = existing.activeLayer.coerceIn(1, CREATIVE_LAYER_COUNT)
        }

        // 应用目标层（首次开启时就是系统原列表，写进去等于无变化）
        val targetTiles = ensureExTile(getCreativeConfig(context).layer(targetLayer).tiles)
        if (!SecureSettingsHelper.setCurrentTiles(context, targetTiles)) {
            return CreativeModeResult.WriteFailed
        }
        saveCreativeActiveLayer(context, targetLayer)
        saveCreativeEnabled(context, true)
        return CreativeModeResult.Success
    }

    /**
     * 关闭创造模式：把普通配置按当前展开状态写回系统，成功后关闭开关。
     *
     * 两层的编辑数据与生效层**保留**，下次开启可直接接着用。
     * 写入失败时返回 false 且开关保持开启（避免「界面说关了、系统还是层布局」）。
     *
     * @return 是否成功关闭
     */
    suspend fun exitCreativeMode(context: Context): Boolean {
        if (!applyNormalLayout(context)) return false
        saveCreativeEnabled(context, false)
        return true
    }
}
