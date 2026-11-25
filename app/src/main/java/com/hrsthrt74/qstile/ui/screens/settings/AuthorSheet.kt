package com.hrsthrt74.qstile.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.SheetState
import com.hrsthrt74.qstile.ui.sheetBottomPadding
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 作者信息 Sheet：展示作者各社交平台入口，点击对应平台打开其主页链接。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `AuthorSheet` 拆分而来，行为保持不变：
 * 标题仍硬编码为 `hrsthrt74`，三个平台项横向等距排列，底部留出导航栏 + 16dp。
 *
 * @param state Sheet 显示状态（由页面 `rememberSheetState()` 创建）
 */
@Composable
fun AuthorSheet(state: SheetState) {
    AppBottomSheet(
        state = state,
        title = "hrsthrt74",
    ) {
        // 平台入口 Row：每个平台一个 Column（上方图标 + 下方平台名），点击打开对应链接
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = sheetBottomPadding() + 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AuthorPlatformItem(
                iconRes = R.drawable.ic_github,
                label = "GitHub",
                url = "https://github.com/hrsthrt74"
            )
            AuthorPlatformItem(
                iconRes = R.drawable.coolapk,
                label = stringResource(R.string.settings_platform_coolapk),
                url = "https://www.coolapk.com/u/972147",
                // 酷安图标是 webp 彩色图片，保留原图颜色不染色
                keepOriginalColor = true
            )
            AuthorPlatformItem(
                iconRes = R.drawable.ic_bilibili,
                label = "Bilibili",
                url = "https://space.bilibili.com/12090372"
            )
        }
    }
}

/**
 * 作者信息 Sheet 中的单个平台入口项。
 * 纵向布局：上方为平台图标，下方为平台名，整体可点击打开对应链接。
 *
 * 随 `AuthorSheet` 一并从 `SettingsScreen` 搬出，仍为文件内私有组件（全仓仅作者 Sheet 使用）。
 *
 * @param iconRes 平台图标 drawable 资源 id
 * @param label 平台名
 * @param url 点击后跳转的链接
 * @param keepOriginalColor 是否为彩色图片（如 webp 图标），true 时保留原图颜色不染色；
 *        纯色矢量图标传 false（默认）用主题前景色染色
 */
@Composable
private fun AuthorPlatformItem(
    iconRes: Int,
    label: String,
    url: String,
    keepOriginalColor: Boolean = false
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Card(
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.secondaryContainerVariant
        )
    ) {
        Column(
            // 点击触发触觉反馈并打开平台主页链接（与设置页其他可点击项一致）
            modifier = Modifier
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 平台图标：纯色矢量图标（如 GitHub）用主题前景色染色以适配深浅色；
            // 彩色图片（如 webp 酷安图标）保留原图颜色，避免被染成单色
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = if (keepOriginalColor) Color.Unspecified else LocalContentColor.current,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MiuixTheme.textStyles.body2
            )
        }
    }
}
