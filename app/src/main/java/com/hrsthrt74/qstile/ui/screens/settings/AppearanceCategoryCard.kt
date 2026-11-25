package com.hrsthrt74.qstile.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.ThemeRepository
import com.hrsthrt74.qstile.ui.components.AppStyleSpinnerPreference
import com.hrsthrt74.qstile.ui.components.ExpandableSettingsCard
import com.hrsthrt74.qstile.ui.components.PreferenceLeadingIcon
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.glass.AiScan
import top.yukonga.miuix.kmp.icon.glass.DecorationMaintenance
import top.yukonga.miuix.kmp.icon.glass.HighlighterPen
import top.yukonga.miuix.kmp.icon.glass.MotionPhoto
import top.yukonga.miuix.kmp.preference.SwitchPreference

// 配色模式下拉的选项(资源 id;stringResource 无法在文件级常量与 remember 内调用,使用处在 Composable 上下文解析)
private val dayNightModeLabels = listOf(
    R.string.settings_theme_follow_system,
    R.string.settings_theme_light,
    R.string.settings_theme_dark
)

// 调色板风格/颜色规范开关暂未启用，相关标签定义一并注释
// private val paletteStyleLabels = listOf("TonalSpot", "Neutral", "Vibrant", "Expressive")
// private val colorSpecLabels = listOf("Spec2021", "Spec2025")

/**
 * 外观分类卡片：主题设置（配色模式 / 动态取色 / 模糊效果 / 应用风格）。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `AppearanceCategoryCard` 拆分而来，行为保持不变。
 * 主题相关状态全部由 `LocalThemeSettings` 直接读取（`ExTileTheme` 在根部下发），
 * 与拆分前从页面透传的是同一个值，无需逐层传参。
 *
 * @param viewModel 设置页 ViewModel（分类卡片的展开/收起状态在它上面）
 * @param backdrop 页面 backdrop（仅供配色模式下拉的玻璃弹窗采样；null 时弹窗回退纯色）
 * @param scope 页面级协程作用域（`ThemeRepository` 的保存方法都是 suspend，
 *        且卡片会随分类收起/列表滚动离开组合，用组件内作用域会取消正在写的 DataStore）
 */
@Composable
fun AppearanceCategoryCard(
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop?,
    scope: CoroutineScope,
) {
    val context = LocalContext.current
    val themeSettings = LocalThemeSettings.current
    // 应用风格开关（true=Glass 全套组件 / false=经典组件），与页面共用同一开关
    val glassStyle = themeSettings.progressiveBlur
    val dayNightModeOptions = dayNightModeLabels.map { stringResource(it) }

    ExpandableSettingsCard(
        title = stringResource(R.string.settings_cat_appearance),
        icon = MiuixIcons.Glass.DecorationMaintenance,
        // 外观分类品牌色：紫色；Monet 动态取色下会被自动压平为主题 primary
        containerColor = Color(0xFF7667F8),
        expanded = viewModel.expandedCategory == SettingsViewModel.SettingsCategory.APPEARANCE,
        onToggle = { viewModel.toggleCategory(SettingsViewModel.SettingsCategory.APPEARANCE) }
    ) {
        AppStyleSpinnerPreference(
            title = stringResource(R.string.settings_color_mode_title),
            items = dayNightModeOptions,
            selectedIndex = themeSettings.dayNightMode,
            glassStyle = glassStyle,
            backdrop = backdrop,
            onSelectedIndexChange = { mode ->
                scope.launch {
                    ThemeRepository.saveDayNightMode(context, mode)
                }
            },
            startAction = { PreferenceLeadingIcon(MiuixIcons.Theme) }
        )

        SwitchPreference(
            title = stringResource(R.string.settings_dynamic_color_title),
            summary = stringResource(R.string.settings_dynamic_color_summary),
            checked = themeSettings.isDynamicColorMode,
            onCheckedChange = { enabled ->
                scope.launch {
                    ThemeRepository.saveIsDynamicColorMode(context, enabled)
                }
            },
            startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.HighlighterPen) }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        SwitchPreference(
            title = stringResource(R.string.settings_blur_title),
            summary = stringResource(R.string.settings_blur_summary),
            checked = themeSettings.enableBlur,
            onCheckedChange = { enabled ->
                scope.launch {
                    ThemeRepository.saveEnableBlur(context, enabled)
                }
            },
            startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.MotionPhoto) }
        )

        // 应用风格切换（由原「渐进模糊」开关升级而来，复用同一开关与设置项）：
        // 开=Glass（顶栏/按钮/底栏/侧栏/Spinner/Dropdown 全部玻璃风格），
        // 关=经典风格（全部回退 basic 组件）。独立于「模糊效果」开关，始终显示。
        SwitchPreference(
            title = stringResource(R.string.settings_progressive_blur_title),
            summary = stringResource(R.string.settings_progressive_blur_summary),
            checked = themeSettings.progressiveBlur,
            onCheckedChange = { enabled ->
                scope.launch {
                    ThemeRepository.saveProgressiveBlur(context, enabled)
                }
            },
            startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.AiScan) }
        )

        // 这两个开关仅在指定颜色时可用，暂不提供指定颜色功能，故禁用
        // if (themeSettings.isDynamicColorMode) {
        //     WindowSpinnerPreference(
        //         title = "调色板风格",
        //         items = paletteStyleOptions,
        //         selectedIndex = themeSettings.paletteStyle,
        //         onSelectedIndexChange = { style ->
        //             scope.launch {
        //                 ThemeRepository.savePaletteStyle(context, style)
        //             }
        //         }
        //     )
        //     WindowSpinnerPreference(
        //         title = "颜色规范",
        //         items = colorSpecOptions,
        //         selectedIndex = themeSettings.colorSpec,
        //         onSelectedIndexChange = { spec ->
        //             scope.launch {
        //                 ThemeRepository.saveColorSpec(context, spec)
        //             }
        //         }
        //     )
        // }
    }
}
