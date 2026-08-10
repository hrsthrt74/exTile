package com.hrsthrt74.qstile.ui.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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
private fun mapColorSchemeMode(dayNightMode: Int, isDynamicColorMode: Boolean): ColorSchemeMode = when {
    isDynamicColorMode && dayNightMode == 0 -> ColorSchemeMode.MonetSystem
    !isDynamicColorMode && dayNightMode == 0 -> ColorSchemeMode.System
    isDynamicColorMode && dayNightMode == 1 -> ColorSchemeMode.MonetLight
    !isDynamicColorMode && dayNightMode == 1 -> ColorSchemeMode.Light
    isDynamicColorMode && dayNightMode == 2 -> ColorSchemeMode.MonetDark
    !isDynamicColorMode && dayNightMode == 2 -> ColorSchemeMode.Dark
    else -> ColorSchemeMode.System
}

private fun dayNightModeToIsDark(mode: Int): Boolean? = when (mode) {
    0 -> null
    1 -> false
    2 -> true
    else -> null
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
    else -> ThemeColorSpec.Spec2025
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
    val view = LocalView.current
    val configuration = LocalConfiguration.current

    val settings by ThemeRepository.getThemeSettingsFlow(context)
        .collectAsState(initial = ThemeSettings())

    val isSystemDark = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val effectiveDark = when (settings.dayNightMode) {
        1 -> false
        2 -> true
        else -> isSystemDark
    }

    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !effectiveDark
            // 同步设置导航栏图标亮暗，让三键导航的按钮颜色与主题一致
            isAppearanceLightNavigationBars = !effectiveDark
        }
        // 关闭「导航栏对比度增强」：
        // enableEdgeToEdge() 默认会开启它，导致三键导航模式下系统强制在导航栏
        // 后面画一层黑色/白色的半透明 scrim（保证按钮对比度），看起来导航栏
        // 就是实色不透明；手势导航无此需求所以正常透明。
        // 这里显式关闭，让三键导航与手势导航一样保持透明，交由 Miuix NavigationBar 绘制背景。
        window.isNavigationBarContrastEnforced = false
    }

    // 当设置变化时重新创建 ThemeController，驱动 MIUIX 主题切换
    val controller = remember(settings.dayNightMode, settings.isDynamicColorMode, settings.paletteStyle, settings.colorSpec) {
        ThemeController(
            colorSchemeMode = mapColorSchemeMode(settings.dayNightMode, settings.isDynamicColorMode),
            isDark = dayNightModeToIsDark(settings.dayNightMode),
            paletteStyle = settings.paletteStyle.toPaletteStyle(),
            colorSpec = settings.colorSpec.toColorSpec()
        )
    }

    MiuixTheme(
        controller = controller,
        content = content
    )
}
