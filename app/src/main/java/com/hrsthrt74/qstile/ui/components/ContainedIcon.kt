package com.hrsthrt74.qstile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 有底图标（图标 + 底色块）。
 *
 * 视觉形态：一个「圆角矩形（超椭圆）纯色底」+ 居中的线性图标，
 * 常用在分类标题行、列表项左侧等需要「一眼区分颜色」的位置。
 *
 * 配色策略（关键，决定组件是否跟随主题）：
 * - **动态取色（Monet）开启时**：底与图标强制使用主题的 `primary` / `onPrimary`。
 *   Monet 的主题色本来就跟壁纸走，此时再叠一层写死的品牌色会与整体界面打架，
 *   所以一律压平为主题色，保证全 App 配色由壁纸统一驱动。
 * - **动态取色关闭时**：使用调用方传入的 [containerColor]；
 *   图标前景色按底色**亮度自动取**（亮的底配深色图标，暗的底配白色图标），
 *   这样调用方只需要关心「底是什么颜色」。
 * - 未传 [containerColor] 时同样回退到 `primary` / `onPrimary`。
 *
 * 用法示例：
 * ```
 * ContainedIcon(icon = MiuixIcons.Glass.DecorationMaintenance, containerColor = Color(0xFF7667F8))
 * ```
 *
 * @param icon 图标（ImageVector）
 * @param modifier 外部修饰符（尺寸/间距等由调用方覆盖时用）
 * @param containerColor 底色；为 null 表示跟随主题 primary。Monet 下该参数会被忽略
 * @param contentDescription 无障碍描述；装饰性图标传 null
 * @param containerSize 底块边长，默认 32dp
 * @param iconSize 图标绘制尺寸，默认 26dp（不同图标源的视觉大小不一致，可按需微调）
 * @param cornerRadius 底块圆角半径，默认 8dp
 */
@Composable
fun ContainedIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentDescription: String? = null,
    containerSize: Dp = 32.dp,
    iconSize: Dp = 26.dp,
    cornerRadius: Dp = 8.dp
) {
    val colorScheme = MiuixTheme.colorScheme
    // Monet 状态：动态取色开启时忽略调用方传入的自定义色，统一压到主题色
    val isMonet = LocalThemeSettings.current.isDynamicColorMode

    // 底色：Monet 或未指定颜色 → primary；否则用调用方给的颜色
    val background = if (isMonet || containerColor == null) colorScheme.primary else containerColor
    // 前景色：跟随主题时用 onPrimary（保证对比度）；
    // 自定义色时按底色亮度决定——浅底用主题的 onBackground（近黑，比纯黑柔和），深底用纯白
    val foreground = if (isMonet || containerColor == null) {
        colorScheme.onPrimary
    } else {
        if (background.luminance() > 0.5f) colorScheme.onBackground else Color.White
    }

    Box(
        modifier = modifier
            .size(containerSize)
            .squircleClip(cornerRadius = cornerRadius)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = foreground,
            modifier = Modifier.size(iconSize)
        )
    }
}
