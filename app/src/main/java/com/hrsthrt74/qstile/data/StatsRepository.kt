package com.hrsthrt74.qstile.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * 磁贴使用统计数据模型。
 *
 * 「展开 / 收起」拆分统计已被合并为单一的「使用次数」——拆分本身没有信息量
 * （用户点一次磁贴只会切换一次布局），合并后既能直接回答「用了多少次」，
 * 也能按天画出简易趋势图。
 *
 * @property totalUsageCount 累计使用次数（磁贴点击并成功切换布局的次数，含创造模式的切层）
 * @property dailyUsage 按天的使用次数（key = 日期，value = 当天次数），仅保留最近 [StatsRepository.RETENTION_DAYS] 天
 */
data class TileStats(
    val totalUsageCount: Long = 0,
    val dailyUsage: Map<LocalDate, Long> = emptyMap(),
) {
    /**
     * 取某一天的使用次数，没有记录时返回 0。
     * @param date 日期
     */
    fun usageOn(date: LocalDate): Long = dailyUsage[date] ?: 0L
}

/**
 * 磁贴使用统计的持久化读写。
 *
 * 与 [ConfigRepository] 共用同一个 DataStore（`tile_config`），因此无需额外初始化，
 * 直接通过 [android.content.Context.dataStore] 扩展属性访问即可。
 * 计数器使用 Long 防止长期使用后溢出。
 *
 * 存储结构：
 * - `stat_total_usage`（Long）：累计使用次数；
 * - `stat_daily_usage`（String）：按天的次数，编码为 `2026-11-24=3;2026-11-23=5`（按日期升序）。
 *   用单个字符串而不是「一天一个 key」，是为了让 DataStore 里的 key 数量固定，
 *   不会随着使用时间无限增长。
 *
 * 旧版本遗留的 `stat_total_expand` / `stat_total_collapse` 只读不写，仅用于数据迁移。
 */
object StatsRepository {
    private val STAT_TOTAL_USAGE = longPreferencesKey("stat_total_usage")
    private val STAT_DAILY_USAGE = stringPreferencesKey("stat_daily_usage")

    /** 旧版「展开次数」计数器，仅迁移时读取 */
    private val LEGACY_TOTAL_EXPAND = longPreferencesKey("stat_total_expand")

    /** 旧版「收起次数」计数器，仅迁移时读取 */
    private val LEGACY_TOTAL_COLLAPSE = longPreferencesKey("stat_total_collapse")

    /**
     * 按天统计的保留天数。
     *
     * 30 天足够画「最近 7 天」的趋势图，也留出后续做「最近 30 天」汇总的余地；
     * 更早的数据在每次写入时被裁掉，避免字符串无限变长。
     */
    const val RETENTION_DAYS = 30

    /** 按天序列化时的条目分隔符 */
    private const val ENTRY_SEPARATOR = ';'

    /** 按天序列化时的「日期 = 次数」分隔符 */
    private const val VALUE_SEPARATOR = '='

    /**
     * 订阅统计数据流。主页通过 collectAsState 实时刷新。
     * @param context Context
     * @return 当前统计数据流，从未记录过时返回全零的 [TileStats]
     */
    fun getStatsFlow(context: Context): Flow<TileStats> {
        return context.dataStore.data.map { preferences ->
            TileStats(
                totalUsageCount = resolveTotalUsage(preferences),
                dailyUsage = decodeDailyUsage(preferences[STAT_DAILY_USAGE]),
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
     * 记录一次使用（磁贴点击并成功切换布局 / 切层后调用）。
     *
     * 同时累加总次数与当天的分桶次数。解析放在 `edit` 内部而不是外部，
     * 是为了让 DataStore 因并发写而重试时，每次都能拿到最新的原始值，
     * 不会把外部捕获的旧 map 再写回去导致丢计数。
     *
     * @param context Context
     * @param date 归属日期（默认今天，测试或补记时可指定）
     */
    suspend fun recordUsage(context: Context, date: LocalDate = LocalDate.now()) {
        context.dataStore.edit { preferences ->
            // 首次写入时用旧版两个计数器之和作为基线（见 resolveTotalUsage 的说明）
            val base = resolveTotalUsage(preferences)
            preferences[STAT_TOTAL_USAGE] = base + 1

            val daily = decodeDailyUsage(preferences[STAT_DAILY_USAGE]).toMutableMap()
            daily[date] = (daily[date] ?: 0L) + 1L
            preferences[STAT_DAILY_USAGE] = encodeDailyUsage(trimDailyUsage(daily, date))
        }
    }

    /**
     * 计算累计使用次数。
     *
     * 没有新 key 时回退到旧版 `展开次数 + 收起次数`：
     * 旧版本每点一次磁贴只会写其中一个计数器，所以两者之和就是「切换过几次」，
     * 直接求和即可平滑完成迁移（**不要除以 2**，那会把真实次数腰斩）。
     *
     * 唯一的偏差：旧版本连「面板关闭后自动收起」也计数（该开关默认关闭），
     * 开过这个开关的用户迁移后的数字会略高于真实点击数——历史数据无法回溯修正，
     * 这点偏差可以接受，比清零或减半都好。
     */
    private fun resolveTotalUsage(preferences: Preferences): Long {
        preferences[STAT_TOTAL_USAGE]?.let { return it }
        val legacyExpand = preferences[LEGACY_TOTAL_EXPAND] ?: 0L
        val legacyCollapse = preferences[LEGACY_TOTAL_COLLAPSE] ?: 0L
        return legacyExpand + legacyCollapse
    }

    /**
     * 解析按天统计字符串，跳过任何格式不合法的条目。
     * @param raw DataStore 中存的原字符串，可能为 null
     */
    private fun decodeDailyUsage(raw: String?): Map<LocalDate, Long> {
        if (raw.isNullOrEmpty()) return emptyMap()

        val result = LinkedHashMap<LocalDate, Long>()
        raw.split(ENTRY_SEPARATOR).forEach { entry ->
            val separatorIndex = entry.indexOf(VALUE_SEPARATOR)
            if (separatorIndex <= 0) return@forEach
            val date = runCatching {
                LocalDate.parse(entry.substring(0, separatorIndex))
            }.getOrNull() ?: return@forEach
            val count = entry.substring(separatorIndex + 1).toLongOrNull() ?: return@forEach
            if (count > 0) result[date] = count
        }
        return result
    }

    /**
     * 把按天统计序列化成 `日期=次数;日期=次数`（按日期升序，便于人工排查）。
     * @param dailyUsage 按天次数
     */
    private fun encodeDailyUsage(dailyUsage: Map<LocalDate, Long>): String =
        dailyUsage.entries
            .sortedBy { it.key }
            .joinToString(ENTRY_SEPARATOR.toString()) { "${it.key}$VALUE_SEPARATOR${it.value}" }

    /**
     * 丢掉超出保留窗口的旧数据。
     * @param dailyUsage 全部分桶
     * @param today 本次写入的日期（作为「最后一天」）
     */
    private fun trimDailyUsage(dailyUsage: Map<LocalDate, Long>, today: LocalDate): Map<LocalDate, Long> {
        val earliestKept = today.minusDays(RETENTION_DAYS.toLong() - 1)
        return dailyUsage.filterKeys { !it.isBefore(earliestKept) }
    }
}
