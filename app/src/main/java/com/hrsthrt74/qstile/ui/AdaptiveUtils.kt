package com.hrsthrt74.qstile.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults

/** 全树可读的宽屏标志：窄屏=底部 NavigationBar，宽屏=侧边 NavigationRail */
val LocalIsWideScreen = staticCompositionLocalOf { false }

/**
 * 宽屏内容区的最大内容宽度。
 * 平板横屏/大屏下内容区很宽，超过该宽度后内容限宽并水平居中，
 * 两侧留白显示背景色，避免内容摊得过宽影响阅读。
 */
val MaxContentWidth = 740.dp

/**
 * 宽屏判定（参照 miuix 官方示例 AdaptiveUtils）：
 * width >= 840dp，或 width >= 600dp 且 高/宽 < 1.2（接近横屏/方形）即视为宽屏。
 * 竖屏手机不满足，走底部导航。
 */
@Composable
fun shouldShowSplitPane(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) {
        val widthDp = windowInfo.containerSize.width.toDp()
        val heightDp = windowInfo.containerSize.height.toDp()
        val ratio = heightDp / widthDp
        widthDp >= 840.dp || (widthDp >= 600.dp && ratio < 1.2f)
    }
}

/**
 * 页面内容的底部预留高度：
 * - 窄屏：底部导航栏高度 + 系统手势条 + 16dp（保持现有观感）
 * - 宽屏：无底部导航栏，只留系统手势条 + 16dp
 */
@Composable
fun contentBottomPadding(): Dp {
    val isWideScreen = LocalIsWideScreen.current
    val navBars = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return if (isWideScreen) navBars + 16.dp
    else NavigationBarDefaults.ItemHeight + navBars + 16.dp
}
