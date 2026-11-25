package com.hrsthrt74.qstile.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.LocalContentColor

// 设置项行首图标（或空白占位）左侧的间距，用于与卡片内边距拉开一点距离
internal val PreferenceIconStartPadding = 4.dp

// 设置项行首图标（或空白占位）与标题文字之间的间距，统一由此变量控制，方便整体调整
internal val PreferenceIconEndPadding = 8.dp

// 无图标设置项的行首空白占位宽度，与图标宽度（24dp）保持一致以对齐
internal val PreferenceIconSize = 24.dp

/**
 * 设置项行首图标。
 * 放在各 Preference 的 startAction 中，使用图标自身配色 + 图标宽度（[PreferenceIconSize]），
 * 左右留出间距（[PreferenceIconStartPadding] / [PreferenceIconEndPadding]）。
 *
 * 由原 `SettingsScreen.kt` 的文件级私有组件搬迁而来，行为保持不变：
 * 不指定 tint（使用图标自身的默认配色），整体 0.8 透明度弱化。
 *
 * @param icon 图标
 */
@Composable
fun PreferenceLeadingIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        // 不指定 tint，使用图标自身的默认配色
        // 注意 Modifier 顺序：padding 在前、size 在后，size 约束的是内容区域，
        // 图标始终完整占据 24dp，左右的 padding 是在图标外侧额外留出空隙，不会把图标挤小
        modifier = Modifier
            .padding(start = PreferenceIconStartPadding, end = PreferenceIconEndPadding)
            .size(PreferenceIconSize)
            .alpha(0.8f)
    )
}

/**
 * 设置项行首图标（drawable 版本）。
 * 用于加载 res/drawable 下的自定义图标（如磁贴图标 tile_*.xml），
 * 用法与 [PreferenceLeadingIcon]（ImageVector 版本）一致，调用 `PreferenceLeadingIcon(R.drawable.xxx)` 即可。
 * 通过 painter 加载并使用 [LocalContentColor] 染色，使图标跟随主题前景色
 * （适用于纯色 drawable，如黑色填充的图标在深色模式下也能正常显示）；
 * 若想保留 drawable 的彩色原图，请移除 tint 参数（改用 Color.Unspecified）。
 *
 * 由原 `SettingsScreen.kt` 的文件级私有组件搬迁而来，行为保持不变。
 *
 * @param iconRes drawable 资源 id（R.drawable.xxx）
 */
@Composable
fun PreferenceLeadingIcon(iconRes: Int) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        // 使用主题前景色染色，适配深浅色模式；miuix Icon 的默认 tint 即 LocalContentColor.current
        tint = LocalContentColor.current,
        modifier = Modifier
            .padding(start = PreferenceIconStartPadding, end = PreferenceIconEndPadding)
            .size(PreferenceIconSize)
            .alpha(0.8f)
    )
}

/**
 * 设置项行首空白占位。
 * 没有配图标的设置项用它占位，保证所有设置项图标列对齐；
 * 宽度与图标一致（[PreferenceIconSize]），左右两侧同样留出与图标相同的间距。
 *
 * 由原 `SettingsScreen.kt` 的文件级私有组件搬迁而来，行为保持不变。
 */
@Composable
fun PreferenceLeadingPlaceholder() {
    Spacer(
        modifier = Modifier
            .padding(start = PreferenceIconStartPadding, end = PreferenceIconEndPadding)
            .size(PreferenceIconSize)
    )
}
