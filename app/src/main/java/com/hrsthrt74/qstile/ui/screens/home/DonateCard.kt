package com.hrsthrt74.qstile.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.ContainedIcon
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.glass.Gift
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 打赏卡片的图标底色：暖橙（打赏/心意语义）。动态取色（Monet）开启时会被压平为主题色 */
private val DonateIconColor = Color(0xFFFF8A00)

/**
 * 主页打赏卡片。
 *
 * 与设置页的「支持作者」共用同一个打赏 Sheet（[com.hrsthrt74.qstile.ui.components.DonateSheet]），
 * 这里只做「一眼能看到入口」的卡片形态。
 *
 * 布局照搬主页的 [StatsCard]：单行标题行 =「有底图标 + 12dp 间距 + headline1 标题」，
 * 不加副标题，也不加右侧箭头（整卡可点，图标和标题已经足够表达「点开看详情」）。
 *
 * 点击反馈走 miuix 自己的两套机制（[Card] 默认都关着，必须显式打开）：
 * - `showIndication = true`：按下时由 `MiuixIndication` 叠加主题色蒙版（跟随主题、自动淡入淡出）；
 * - `pressFeedbackType = Sink`：按下内凹 + 回弹，与磁贴格子（`TileGridCell`）的按压手感一致。
 *
 * @param onClick 点击卡片（由调用方打开打赏 Sheet）
 */
@Composable
internal fun DonateCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContainedIcon(
                    icon = MiuixIcons.FavoritesFill,
                    containerColor = DonateIconColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.settings_donate_title),
                    // 卡片标题统一 headline1（与统计卡片标题同级）
                    style = MiuixTheme.textStyles.headline1,
                    // 占满剩余宽度，把箭头推到行尾
                    modifier = Modifier.weight(1f)
                )
                // 右侧箭头：卡片外观上看不出可点，靠这个箭头表达「点开有内容」
                // 尺寸与设置页可折叠卡片的箭头一致（10x16dp）
                Icon(
                    imageVector = MiuixIcons.Basic.ArrowRight,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier.size(width = 10.dp, height = 16.dp)
                )
            }
        }
    }
}
