package com.hrsthrt74.qstile.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 磁贴使用统计数据模型。
 *
 * 目前仅记录累计展开/收起次数，后续如需按日/按时长统计，可在此扩展字段。
 *
 * @property totalExpandCount 累计展开次数（切换为展开布局成功的次数）
 * @property totalCollapseCount 累计收起次数（切换为收起布局成功的次数）
 */
data class TileStats(
    val totalExpandCount: Long = 0,
    val totalCollapseCount: Long = 0,
)

/**
 * 磁贴使用统计的持久化读写。
 *
 * 与 [ConfigRepository] 共用同一个 DataStore（`tile_config`），因此无需额外初始化，
 * 直接通过 [android.content.Context.dataStore] 扩展属性访问即可。
 * 计数器使用 Long 防止长期使用后溢出。
 */
object StatsRepository {
    private val STAT_TOTAL_EXPAND = longPreferencesKey("stat_total_expand")
    private val STAT_TOTAL_COLLAPSE = longPreferencesKey("stat_total_collapse")

    /**
     * 订阅统计数据流。主页通过 collectAsState 实时刷新。
     * @param context Context
     * @return 当前统计数据流，从未记录过时返回全零的 [TileStats]
     */
    fun getStatsFlow(context: Context): Flow<TileStats> {
        return context.dataStore.data.map { preferences ->
            TileStats(
                totalExpandCount = preferences[STAT_TOTAL_EXPAND] ?: 0,
                totalCollapseCount = preferences[STAT_TOTAL_COLLAPSE] ?: 0,
            )
        }
    }

    /**
     * 一次性读取当前统计数据。
     * @param context Context
     * @return 当前统计数据，从未记录过时返回全零的 [TileStats]
     */
    suspend fun getStats(context: Context): TileStats {
        return getStatsFlow(context).first()
    }

    /**
     * 记录一次展开切换（仅在磁贴布局切换成功后调用）。
     * @param context Context
     */
    suspend fun recordExpand(context: Context) {
        context.dataStore.edit { preferences ->
            preferences[STAT_TOTAL_EXPAND] = (preferences[STAT_TOTAL_EXPAND] ?: 0) + 1
        }
    }

    /**
     * 记录一次收起切换（仅在磁贴布局切换成功后调用）。
     * @param context Context
     */
    suspend fun recordCollapse(context: Context) {
        context.dataStore.edit { preferences ->
            preferences[STAT_TOTAL_COLLAPSE] = (preferences[STAT_TOTAL_COLLAPSE] ?: 0) + 1
        }
    }
}
