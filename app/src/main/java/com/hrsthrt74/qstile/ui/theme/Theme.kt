package com.hrsthrt74.qstile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.hrsthrt74.qstile.data.ThemeRepository
import com.hrsthrt74.qstile.data.ThemeSettings
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/**
 * 将存储的整数映射为 MIUIX 的 ColorSchemeMode 枚举。
 * 0=Monet取色跟随系统, 1=跟随系统, 2=浅色Monet, 3=深色Monet, 4=浅色, 5=深色
 */
private fun Int.toColorSchemeMode(): ColorSchemeMode = when (this) {
    0 -> ColorSchemeMode.MonetSystem
    1 -> ColorSchemeMode.System
    2 -> ColorSchemeMode.MonetLight
    3 -> ColorSchemeMode.MonetDark
    4 -> ColorSchemeMode.Light
    5 -> ColorSchemeMode.Dark
    else -> ColorSchemeMode.MonetSystem
}

/**
 * 将存储的整数映射为动态取色调色板风格。
 * 0=TonalSpot, 1=Neutral, 2=Vibrant, 3=Expressive
 */
private fun Int.toPaletteStyle(): ThemePaletteStyle = when (this) {
    0 -> ThemePaletteStyle.TonalSpot
    1 -> ThemePaletteStyle.Neutral
    2 -> ThemePaletteStyle.Vibrant
    3 -> ThemePaletteStyle.Expressive
    else -> ThemePaletteStyle.TonalSpot
}

/**
 * 将存储的整数映射为颜色规范版本。
 * 0=Spec2021(Android 12 风格), 1=Spec2025(Material 3 新规范)
 */
private fun Int.toColorSpec(): ThemeColorSpec = when (this) {
    0 -> ThemeColorSpec.Spec2021
    1 -> ThemeColorSpec.Spec2025
    else -> ThemeColorSpec.Spec2021
}

/**
 * 应用的自定义主题包装器。
 * 从 DataStore 读取用户主题偏好，构建 MIUIX ThemeController，然后包裹内容。
 *
 * @param content 需要被主题包裹的 Compose 内容
 */
@Composable
fun ExTileTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // 以 Flow 形式收集 DataStore 中的主题设置，自动响应变化
    val settings by ThemeRepository.getThemeSettingsFlow(context)
        .collectAsState(initial = ThemeSettings())

    // 当设置变化时重新创建 ThemeController，驱动 MIUIX 主题切换
    val controller = remember(settings.colorSchemeMode, settings.isDark, settings.paletteStyle, settings.colorSpec) {
        ThemeController(
            colorSchemeMode = settings.colorSchemeMode.toColorSchemeMode(),
            isDark = settings.isDark,
            paletteStyle = settings.paletteStyle.toPaletteStyle(),
            colorSpec = settings.colorSpec.toColorSpec()
        )
    }

    MiuixTheme(
        controller = controller,
        content = content
    )
}
