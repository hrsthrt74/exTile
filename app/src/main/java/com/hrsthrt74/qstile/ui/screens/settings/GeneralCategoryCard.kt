package com.hrsthrt74.qstile.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.DeviceProfile
import com.hrsthrt74.qstile.ui.components.AppStyleSpinnerPreference
import com.hrsthrt74.qstile.ui.components.ExpandableSettingsCard
import com.hrsthrt74.qstile.ui.components.PreferenceLeadingIcon
import com.hrsthrt74.qstile.ui.components.PreferenceLeadingPlaceholder
import com.hrsthrt74.qstile.ui.components.SheetState
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import com.microsoft.clarity.modifiers.clarityMask
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.icon.extended.Unpin
import top.yukonga.miuix.kmp.icon.glass.MiHome
import top.yukonga.miuix.kmp.icon.glass.OpenWithOtherApp
import top.yukonga.miuix.kmp.icon.glass.PptNotes
import top.yukonga.miuix.kmp.icon.glass.RepairFill
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 长按行为下拉的选项(资源 id,使用处在 Composable 上下文解析;顺序与 ConfigRepository.LongPressBehavior 一致)
private val longPressBehaviorLabels = listOf(
    R.string.settings_longpress_extile,
    R.string.settings_longpress_settings,
    R.string.settings_device_center_title,
    R.string.settings_longpress_custom
)

// 磁贴名预设下拉的选项(资源 id,使用处在 Composable 上下文解析;
// 显示顺序与 ConfigRepository.TileLabelPreset.labels 一致,但文案随语言,
// 数据层 labels 仅供写入系统 QS 标签使用,两者不共享资源)
private val tileLabelPresetLabels = listOf(
    R.string.settings_preset_more_tiles,
    R.string.settings_preset_expand_collapse,
    R.string.settings_preset_switch,
    R.string.settings_preset_custom
)

/**
 * 通用分类卡片：磁贴联动开关 / 磁贴图标 / 磁贴名 / 长按行为。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `GeneralCategoryCard` 拆分而来，行为保持不变：
 * 非小米机型隐藏「无字模式」与长按下拉里的「融合设备中心」项（下拉索引与存储值解耦，
 * 通过 displayedBehaviorValues 双向映射）；创造模式开启后整块「磁贴联动」折叠隐藏
 * （不是置灰——三个开关在切层模式下都没有意义）。创造模式开关本身已迁到「高级」分类。
 *
 * @param viewModel 设置页 ViewModel（所有开关与下拉的存储值、分类展开状态都在它上面）
 * @param backdrop 页面 backdrop（仅供三个下拉的玻璃弹窗采样；null 时弹窗回退纯色）
 * @param customTileLabelSheetState 自定义磁贴名输入 Sheet 状态
 * @param appPickerSheetState 自定义应用选择器 Sheet 状态
 */
@Composable
fun GeneralCategoryCard(
    viewModel: SettingsViewModel,
    backdrop: LayerBackdrop?,
    customTileLabelSheetState: SheetState,
    appPickerSheetState: SheetState,
) {
    val context = LocalContext.current
    // 应用风格开关（true=Glass 全套组件 / false=经典组件），与页面共用同一开关
    val glassStyle = LocalThemeSettings.current.progressiveBlur
    // 文案在 Composable 上下文预解析，避免在回调 lambda 里读资源（lint: 非配置感知）
    val noneSelectedText = stringResource(R.string.settings_none_selected)
    // 设备能力快照：用于判断是否为小米机型，非小米机型隐藏「融合设备中心」选项
    val profile = remember { DeviceProfile.from(context) }
    // 非小米机型移除「融合设备中心」；显示顺序与存储值解耦，
    // 通过 displayedBehaviorValues 在「下拉索引 ↔ 存储值」之间双向映射。
    // remember 内只做资源 id 与存储值的配对/过滤(纯计算),文案解析在外层 stringResource 完成
    val (displayedBehaviorEntries, displayedBehaviorValues) = remember(profile.isXiaomi) {
        // 显示顺序：exTile → 系统设置 → 融合设备中心 → 自定义
        val all = longPressBehaviorLabels.zip(
            listOf(
                ConfigRepository.LongPressBehavior.OPEN_EXTILE,
                ConfigRepository.LongPressBehavior.OPEN_SETTINGS,
                ConfigRepository.LongPressBehavior.OPEN_DEVICE_CENTER,
                ConfigRepository.LongPressBehavior.OPEN_CUSTOM_APP
            )
        )
        val visible = if (profile.isXiaomi) all else all.filter { it.second != ConfigRepository.LongPressBehavior.OPEN_DEVICE_CENTER }
        Pair(visible, visible.map { it.second })
    }
    val longPressBehaviorOptions = displayedBehaviorEntries.map {
        DropdownItem(text = stringResource(it.first))
    }
    // 已选自定义应用的显示名（包名 → 应用名）
    val customAppLabel = remember(viewModel.customAppPackage, noneSelectedText) {
        if (viewModel.customAppPackage.isBlank()) {
            noneSelectedText
        } else {
            try {
                context.packageManager.getApplicationInfo(viewModel.customAppPackage, 0)
                    .loadLabel(context.packageManager).toString()
            } catch (e: Exception) {
                viewModel.customAppPackage
            }
        }
    }

    ExpandableSettingsCard(
        title = stringResource(R.string.settings_cat_general),
        icon = MiuixIcons.Glass.RepairFill,
        expanded = viewModel.expandedCategory == SettingsViewModel.SettingsCategory.GENERAL,
        onToggle = { viewModel.toggleCategory(SettingsViewModel.SettingsCategory.GENERAL) }
    ) {
        // 创造模式下整块「磁贴联动」都没有意义：
        // - 无字模式 / 融合设备中心改由**每一层**自己接管（编辑页每层底部的卡片）；
        // - 「面板关闭后自动收起」的语义也变成了「回到层 1」。
        // 所以直接折叠整块（含分节标题与下方的分隔线），而不是把开关置灰——
        // 三个都点不动的开关留在原地只会让人以为「这里还能调，只是暂时锁住」。
        AnimatedVisibility(
            visible = !viewModel.creativeModeEnabled,
        ) {
            // AnimatedVisibility 的内容只有一个子节点槽（内部按单子节点测量），
            // 多个条目必须自己包 Column，否则会叠在一起
            Column(modifier = Modifier.fillMaxWidth()) {
                SmallTitle(
                    text = stringResource(R.string.settings_section_tile_sync),
                    insideMargin = PaddingValues(start = 20.dp, top = 12.dp)
                )
                // 无字模式写入的 wordless_mode 是 HyperOS 专属 Secure Setting，仅小米设备显示
                if (profile.isXiaomi) {
                    SwitchPreference(
                        title = stringResource(R.string.settings_wordless_title),
                        summary = stringResource(R.string.settings_wordless_summary),
                        checked = viewModel.wordlessModeSync,
                        onCheckedChange = { enabled ->
                            viewModel.saveWordlessModeSync(context, enabled)
                        },
                        startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.PptNotes) }
                    )
                }

                SwitchPreference(
                    title = stringResource(R.string.settings_device_center_title),
                    summary = stringResource(R.string.settings_device_center_summary),
                    checked = viewModel.smartDeviceControlSync,
                    onCheckedChange = { enabled ->
                        viewModel.saveSmartDeviceControlSync(context, enabled)
                    },
                    startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.MiHome) }
                )

                SwitchPreference(
                    title = stringResource(R.string.settings_auto_collapse_title),
                    summary = stringResource(R.string.settings_auto_collapse_summary),
                    checked = viewModel.autoCollapseOnClose,
                    onCheckedChange = { enabled ->
                        viewModel.saveAutoCollapseOnClose(context, enabled)
                    },
                    startAction = { PreferenceLeadingIcon(MiuixIcons.Unpin) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        SmallTitle(
            text = stringResource(R.string.settings_section_custom),
            insideMargin = PaddingValues(start = 20.dp, top = 12.dp)
        )

        // ===== 磁贴图标：设置 QS 面板中 exTile 磁贴显示的图标 =====
        // 每项带 icon：弹窗里逐项展示对应图标（默认项用 ic_tile，其余用 tile_icon_* 资源）
        // 图标资源仍来自 ConfigRepository.TileIcon(单一数据源);文案仅首项「默认」随语言,
        // 其余为图标英文名(Add/AddCircle/...)无需翻译
        val tileIconTexts = listOf(stringResource(R.string.settings_tile_icon_default)) +
            ConfigRepository.TileIcon.labels.drop(1)
        // 图标资源均为 vector drawable，可转 ImageVector 供玻璃弹窗项使用；
        // 经典风格下 AppStyleSpinnerPreference 会用 DropdownItem 的 composable icon 槽渲染。
        val tileIconVectors = tileIconTexts.indices.map { index ->
            ImageVector.vectorResource(ConfigRepository.TileIcon.iconRes(index) ?: R.drawable.ic_tile)
        }
        AppStyleSpinnerPreference(
            title = stringResource(R.string.settings_tile_icon_title),
            items = tileIconTexts,
            icons = tileIconVectors,
            selectedIndex = viewModel.tileIcon,
            glassStyle = glassStyle,
            backdrop = backdrop,
            onSelectedIndexChange = { index ->
                viewModel.saveTileIcon(context, index)
            },
            startAction = { PreferenceLeadingIcon(MiuixIcons.Copy) }
        )

        // ===== 磁贴名：设置 QS 面板中 exTile 磁贴显示的标签 =====
        val tileLabelOptions = tileLabelPresetLabels.map { stringResource(it) }
        AppStyleSpinnerPreference(
            title = stringResource(R.string.settings_tile_label_title),
            items = tileLabelOptions,
            selectedIndex = viewModel.tileLabelPreset,
            glassStyle = glassStyle,
            backdrop = backdrop,
            onSelectedIndexChange = { index ->
                viewModel.saveTileLabelPreset(context, index)
            },
            startAction = { PreferenceLeadingIcon(MiuixIcons.Rename) }
        )

        // 仅当选择了「自定义」时显示自定义名字输入入口
        // 使用 AnimatedVisibility 实现平滑的展开/收起动画
        AnimatedVisibility(
            visible = viewModel.tileLabelPreset == ConfigRepository.TileLabelPreset.CUSTOM
        ) {
            ArrowPreference(
                title = stringResource(R.string.settings_display_label_title),
                endActions = {
                    Text(
                        viewModel.tileLabelCustom.ifBlank { stringResource(R.string.settings_not_set) },
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier.clarityMask()
                    )
                },
                onClick = {
                    // 打开输入 Sheet 前先用已保存的自定义名字填充输入框，方便用户直接修改
                    viewModel.customTileLabelInput = viewModel.tileLabelCustom
                    customTileLabelSheetState.show()
                },
                startAction = { PreferenceLeadingPlaceholder() }
            )
        }

        AppStyleSpinnerPreference(
            title = stringResource(R.string.settings_longpress_title),
            // summary = "设置长按快捷设置面板中 exTile 磁贴时执行的操作",
            items = longPressBehaviorOptions.map { it.text },
            // 下拉索引与存储值解耦：通过可见项列表双向映射
            selectedIndex = displayedBehaviorValues.indexOf(viewModel.longPressBehavior).coerceAtLeast(0),
            glassStyle = glassStyle,
            backdrop = backdrop,
            onSelectedIndexChange = { index ->
                viewModel.saveLongPressBehavior(context, displayedBehaviorValues[index])
            },
            startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.OpenWithOtherApp) }
        )

        // 仅当选择了「跳转到自定义应用」时显示应用选择入口
        // 使用 AnimatedVisibility 实现平滑的展开/收起动画
        AnimatedVisibility(
            visible = viewModel.longPressBehavior == ConfigRepository.LongPressBehavior.OPEN_CUSTOM_APP
        ) {
            ArrowPreference(
                title = stringResource(R.string.settings_jump_target_title),
                onClick = { appPickerSheetState.show() },
                startAction = { PreferenceLeadingPlaceholder() },
                endActions = {
                    Text(
                        customAppLabel,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier.clarityMask()
                        )
                }
            )
        }

        // 创造模式开关已迁到「高级」分类；本分类只保留磁贴行为相关设置。
    }
}
