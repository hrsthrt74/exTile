package com.hrsthrt74.qstile.ui.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
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
 * 当前生效的主题设置。
 * 由 [ExTileTheme] 在 DataStore 数据就绪后通过 [CompositionLocalProvider] 提供给整个 UI 树，
 * 供 App 壳层及各页面同步读取（如模糊开关、动态取色等），
 * 避免各自重复订阅 DataStore 导致「初始渲染默认值、次帧才切到真实值」的闪烁。
 */
val LocalThemeSettings = staticCompositionLocalOf<ThemeSettings> {
    error("LocalThemeSettings 尚未提供：请在 ExTileTheme 作用域内读取")
}

/**
 * 应用的自定义主题包装器。
 * 从 DataStore 读取用户主题偏好，构建 MIUIX ThemeController，然后包裹内容。
 *
 * 防闪烁策略：主题数据是异步读取的，若像原来那样用 `initial = ThemeSettings()`
 * 渲染，首屏会先按默认主题画一帧、再切到用户保存的主题（闪一下默认设置）。
 * 因此这里初始值用 null，数据就绪前直接 return 不渲染 content；
 * DataStore 读盘极快（毫秒级），信息到达后即以正确主题渲染，用户感知不到空白。
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

    // 主题设置数据流：initial=null 表示「尚未读取到」，就绪前不渲染任何 UI
    val settings by ThemeRepository.getThemeSettingsFlow(context)
        .collectAsState(initial = null)
    // 委托属性无法智能转换，取局部值用于后续分支判断
    val currentSettings = settings
    if (currentSettings == null) return

    val isSystemDark = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val effectiveDark = when (currentSettings.dayNightMode) {
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
    val controller = remember(
        currentSettings.dayNightMode,
        currentSettings.isDynamicColorMode,
        currentSettings.paletteStyle,
        currentSettings.colorSpec
    ) {
        ThemeController(
            colorSchemeMode = mapColorSchemeMode(currentSettings.dayNightMode, currentSettings.isDynamicColorMode),
            isDark = dayNightModeToIsDark(currentSettings.dayNightMode),
            paletteStyle = currentSettings.paletteStyle.toPaletteStyle(),
            colorSpec = currentSettings.colorSpec.toColorSpec()
        )
    }

    // 通过 CompositionLocal 向下提供主题设置，各页面改为同步读取
    CompositionLocalProvider(LocalThemeSettings provides currentSettings) {
        MiuixTheme(
            controller = controller,
            content = content
        )
    }
}
