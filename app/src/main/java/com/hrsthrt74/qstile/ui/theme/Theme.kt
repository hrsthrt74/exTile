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

private fun Int.toColorSchemeMode(): ColorSchemeMode = when (this) {
    0 -> ColorSchemeMode.MonetSystem
    1 -> ColorSchemeMode.System
    2 -> ColorSchemeMode.MonetLight
    3 -> ColorSchemeMode.MonetDark
    4 -> ColorSchemeMode.Light
    5 -> ColorSchemeMode.Dark
    else -> ColorSchemeMode.MonetSystem
}

private fun Int.toPaletteStyle(): ThemePaletteStyle = when (this) {
    0 -> ThemePaletteStyle.TonalSpot
    1 -> ThemePaletteStyle.Neutral
    2 -> ThemePaletteStyle.Vibrant
    3 -> ThemePaletteStyle.Expressive
    else -> ThemePaletteStyle.TonalSpot
}

private fun Int.toColorSpec(): ThemeColorSpec = when (this) {
    0 -> ThemeColorSpec.Spec2021
    1 -> ThemeColorSpec.Spec2025
    else -> ThemeColorSpec.Spec2021
}

@Composable
fun ExTileTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settings by ThemeRepository.getThemeSettingsFlow(context)
        .collectAsState(initial = ThemeSettings())

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
