package com.hrsthrt74.qstile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
}
