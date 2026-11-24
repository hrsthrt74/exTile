package com.hrsthrt74.qstile.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.ContainedIcon
import com.microsoft.clarity.modifiers.clarityUnmask
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.SoundWave
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 使用统计卡片组件。
 * 展示累计展开/收起次数（磁贴布局切换成功时由 StatsRepository 记录）。
 *
 * 布局与首页其它卡片保持一致：左侧「有底图标」（竖直居中）+ 右侧内容（标题 + 统计行）。
 *
 * @param totalExpandCount 累计展开次数
 * @param totalCollapseCount 累计收起次数
 */
@Composable
internal fun StatsCard(
    totalExpandCount: Long,
    totalCollapseCount: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 统一卡片布局：左「有底图标」（竖直居中）+ 右内容（标题 + 统计行）
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 使用统计图标（记录/历史语义）
            ContainedIcon(MiuixIcons.Glass.SoundWave)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_stats_title),
                    // 卡片标题统一 headline1（与设置页 Preference 标题同级）
                    style = MiuixTheme.textStyles.headline1
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 两行键值对：累计展开/收起次数
                StatusRow(stringResource(R.string.home_stats_expand_total), totalExpandCount.toString())
                StatusRow(stringResource(R.string.home_stats_collapse_total), totalCollapseCount.toString())
            }
        }
    }
}

/**
 * 单行「标签-值」展示组件。
 * 左对齐显示标签文字，右对齐显示对应的数值。
 * 仅本文件内的统计卡片使用，故为 private。
 */
@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            // 统计数据 -> Clarity
            .clarityUnmask(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface
        )
    }
}
