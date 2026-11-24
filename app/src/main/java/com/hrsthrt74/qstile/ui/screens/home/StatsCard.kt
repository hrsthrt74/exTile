package com.hrsthrt74.qstile.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.TileStats
import com.hrsthrt74.qstile.ui.components.ContainedIcon
import com.microsoft.clarity.modifiers.clarityUnmask
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.SoundWave
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.text.NumberFormat
import java.time.LocalDate

/** 热力图展示的天数（含今天） */
private const val HeatmapDayCount = 10

/** 小方格之间的间距 */
private val HeatmapCellGap = 4.dp

/** 小方格圆角 */
private val HeatmapCellCorner = 4.dp

/** 无使用记录的小方格底色透明度（几乎不可见的一层灰，用来占位而非留空） */
private const val HeatmapEmptyAlpha = 0.07f

/**
 * 使用统计卡片组件。
 *
 * 展示「累计使用次数」+ 最近 10 天的热力图（一天一个渐深的小方格）。
 * 统计由 [com.hrsthrt74.qstile.data.StatsRepository] 在磁贴成功切换布局（含创造模式切层）时记录。
 *
 * 热力图不使用任何第三方图表库：10 个方格就是 10 个按透明度着色的 [Box]，
 * 数据量这么小时自绘比引入依赖划算得多。
 *
 * @param stats 统计数据（累计次数 + 按天分桶）
 */
@Composable
internal fun StatsCard(stats: TileStats) {
    val colorScheme = MiuixTheme.colorScheme

    // 大数字加千分位，避免长期使用后出现一长串看不出量级的数字
    val totalText = remember(stats.totalUsageCount) {
        NumberFormat.getIntegerInstance().format(stats.totalUsageCount)
    }

    Card(
        pressFeedbackType = PressFeedbackType.Sink,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 统计数据 -> Clarity（数字本身不是敏感信息，允许在录屏回放中展示）
        Column(
            modifier = Modifier
                .padding(16.dp)
                .clarityUnmask()
        ) {
            // 标题行：左「有底图标」+ 右标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContainedIcon(MiuixIcons.Glass.SoundWave)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.home_stats_title),
                    // 卡片标题统一 headline1（与设置页 Preference 标题同级）
                    style = MiuixTheme.textStyles.headline1
                )
            }

            // 「最近 10 天」与累计次数同一行：左侧说明、右侧数值，三者按基线对齐
            // 注意：这一行紧贴标题行，中间不插间距（用户要求去掉）。
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.home_stats_recent_days, HeatmapDayCount),
                    style = MiuixTheme.textStyles.footnote2,
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.alignByBaseline()
                )
                // 把右侧「数值 + 单位」顶到行的末端
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = totalText,
                    // 比标题小一号（title2 = 24sp），不再用撑满视线的大号数字
                    style = MiuixTheme.textStyles.title2,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = stringResource(R.string.home_stats_unit_times),
                    style = MiuixTheme.textStyles.footnote1,
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.alignByBaseline()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (stats.totalUsageCount == 0L) {
                // 空白态：还没用过磁贴时给一句话，比画一排空格子更清楚
                Text(
                    text = stringResource(R.string.home_stats_empty),
                    style = MiuixTheme.textStyles.footnote1,
                    color = colorScheme.onSurfaceVariantSummary
                )
            } else {
                UsageHeatmap(stats = stats, today = LocalDate.now())
            }
        }
    }
}

/**
 * 最近 [HeatmapDayCount] 天（含今天）的热力图：从左到右一天一个小方格，颜色越深表示当天用得越多。
 *
 * 方格尺寸完全由一行可用宽度决定：每格宽度 = (可用宽度 - 总间距) / 天数，再用 1:1 锁成正方形，
 * 所以这排方格永远铺满整行（不做边长上限，避免右侧留出一条空白）。
 *
 * 着色用「相对 10 天内最大次数」的档位，而不是绝对次数：
 * 有人每天 1 次、有人每天 30 次，只有相对档位才能让两种人都看出冷热。
 *
 * @param stats 统计数据
 * @param today 今天（由调用方传入，避免同一次绘制里跨天）
 */
@Composable
private fun UsageHeatmap(stats: TileStats, today: LocalDate) {
    val colorScheme = MiuixTheme.colorScheme

    // 从今天往前数 10 天，最后一项是今天（时间从左往右，最新在右侧）
    val days = remember(today) {
        (HeatmapDayCount - 1 downTo 0).map { offset -> today.minusDays(offset.toLong()) }
    }
    val counts = days.map { stats.usageOn(it) }
    // 最大次数决定配色基准；全为 0 时用 1 避免除零（实际只在 totalUsageCount > 0 时渲染）
    val maxCount = counts.max().coerceAtLeast(1L)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HeatmapCellGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 只关心每天的次数，日期本身不参与绘制
        counts.forEach { count ->
            Box(
                modifier = Modifier
                    // weight 平分一行的可用宽度，aspectRatio(1f) 把方格锁成正方形
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(HeatmapCellCorner))
                    .background(cellColor(count, maxCount, colorScheme))
            )
        }
    }
}

/**
 * 按「当天次数 / 10 天内最大值」算出方格颜色。
 *
 * 共 4 档：0（几乎透明的灰，占位）/ ≤33% / ≤66% / 更高（满色）。
 *
 * @param count 当天次数
 * @param maxCount 10 天内的最大次数（保证 > 0）
 * @param colorScheme 当前配色（取主题色 primary 与 onSurface）
 */
private fun cellColor(count: Long, maxCount: Long, colorScheme: Colors): Color {
    if (count <= 0L) return colorScheme.onSurface.copy(alpha = HeatmapEmptyAlpha)

    val ratio = count.toFloat() / maxCount.toFloat()
    val alpha = when {
        ratio <= 1f / 3f -> 0.30f
        ratio <= 2f / 3f -> 0.55f
        ratio < 1f -> 0.78f
        else -> 1f
    }
    return colorScheme.primary.copy(alpha = alpha)
}
