package com.hrsthrt74.qstile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hrsthrt74.qstile.DebugToolsActivity
import com.hrsthrt74.qstile.DocsActivity
import com.hrsthrt74.qstile.ExTileApplication
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.BackupRepository
import com.hrsthrt74.qstile.data.ChangelogEntry
import com.hrsthrt74.qstile.data.ChangelogProvider
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.DeviceProfile
import com.hrsthrt74.qstile.data.ThemeRepository
import com.hrsthrt74.qstile.data.UpdateCheckState
import com.hrsthrt74.qstile.data.UpdateChecker
import com.hrsthrt74.qstile.ui.AppTopBar
import com.hrsthrt74.qstile.ui.LocalIsWideScreen
import com.hrsthrt74.qstile.ui.TopBarIconButton
import com.hrsthrt74.qstile.ui.awaitStartupFrames
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.AppCard
import com.hrsthrt74.qstile.ui.components.AppDialog
import com.hrsthrt74.qstile.ui.components.AppStyleSpinnerPreference
import com.hrsthrt74.qstile.ui.components.AppTextField
import com.hrsthrt74.qstile.ui.components.ContainedIcon
import com.hrsthrt74.qstile.ui.components.QQ_GROUP_NUMBER
import com.hrsthrt74.qstile.ui.components.QQ_GROUP_URL
import com.hrsthrt74.qstile.ui.components.rememberDialogState
import com.hrsthrt74.qstile.ui.components.rememberSheetState
import com.hrsthrt74.qstile.ui.contentBottomPadding
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import com.kyant.shapes.Capsule
import com.microsoft.clarity.modifiers.clarityMask
import com.microsoft.clarity.modifiers.clarityUnmask
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults
import top.yukonga.miuix.kmp.glass.glassShadow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.Remove
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Unpin
import top.yukonga.miuix.kmp.icon.glass.AiScan
import top.yukonga.miuix.kmp.icon.glass.AiWriting
import top.yukonga.miuix.kmp.icon.glass.AllSdCardData
import top.yukonga.miuix.kmp.icon.glass.DecorationMaintenance
import top.yukonga.miuix.kmp.icon.glass.HighlighterPen
import top.yukonga.miuix.kmp.icon.glass.Maintenance
import top.yukonga.miuix.kmp.icon.glass.MiHome
import top.yukonga.miuix.kmp.icon.glass.MotionPhoto
import top.yukonga.miuix.kmp.icon.glass.OpenWithOtherApp
import top.yukonga.miuix.kmp.icon.glass.PasteEditPoint
import top.yukonga.miuix.kmp.icon.glass.PhoneIcon
import top.yukonga.miuix.kmp.icon.glass.PptNotes
import top.yukonga.miuix.kmp.icon.glass.RepairFill
import top.yukonga.miuix.kmp.icon.glass.SoundWave
import top.yukonga.miuix.kmp.icon.glass.Start
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import android.graphics.Color as AndroidColor

// 配色模式下拉的选项(资源 id;stringResource 无法在文件级常量与 remember 内调用,使用处在 Composable 上下文解析)
private val dayNightModeLabels = listOf(
    R.string.settings_theme_follow_system,
    R.string.settings_theme_light,
    R.string.settings_theme_dark
)
// 设置项行首图标（或空白占位）与标题文字之间的间距，统一由此变量控制，方便整体调整
private val PreferenceIconEndPadding = 8.dp

// 群号与加群链接已抽取到 ui/components/FeedbackLinks.kt，与主页提示卡片共用
// 设置项行首图标（或空白占位）左侧的间距，用于与卡片内边距拉开一点距离
private val PreferenceIconStartPadding = 4.dp
// 无图标设置项的行首空白占位宽度，与图标宽度（24dp）保持一致以对齐
private val PreferenceIconSize = 24.dp
// 打赏收款码的最大宽度：Sheet 在平板 / 横屏下很宽，直接 fillMaxWidth 会把二维码撑成
// 一整行宽的大图。封顶后窄屏仍按可用宽度铺满，宽屏则保持这个上限（见 DonateSheet 里的修饰符顺序）
private val DonateQrMaxWidth = 300.dp
// 调色板风格/颜色规范开关暂未启用，相关标签定义一并注释
// private val paletteStyleLabels = listOf("TonalSpot", "Neutral", "Vibrant", "Expressive")
// private val colorSpecLabels = listOf("Spec2021", "Spec2025")
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

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    // 获取 ViewModel
    val viewModel: SettingsViewModel = viewModel()

    // 主题设置：由 ExTileTheme 通过 LocalThemeSettings 同步提供（数据就绪后才组合到这里，不会闪烁）
    val themeSettings = LocalThemeSettings.current

    // 应用风格开关（复用「渐进模糊」开关）：true=Glass 全套组件，false=经典组件
    val glassStyle = themeSettings.progressiveBlur
    // 顶部栏模糊：创建 backdrop 捕获滚动内容，模糊开关关闭或 RuntimeShader 不支持时退回纯色
    val backdrop = rememberBlurBackdrop(enabled = themeSettings.enableBlur)

    // 自定义应用选择器 Sheet 的显示状态（统一由 AppBottomSheet 管理）
    val appPickerSheetState = rememberSheetState()
    // 自定义磁贴名输入 Sheet 的显示状态（统一由 AppBottomSheet 管理）
    val customTileLabelSheetState = rememberSheetState()

    // 系统磁贴查看 / 备份结果 / 导入备份 三个 Sheet 的显示状态（统一由 AppBottomSheet 管理）
    val systemTilesSheetState = rememberSheetState()
    val backupSheetState = rememberSheetState()
    val importBackupSheetState = rememberSheetState()
    // 支持作者（微信收款码）Sheet 的显示状态（统一由 AppBottomSheet 管理）
    val donateSheetState = rememberSheetState()
    // 作者信息（hrsthrt74 各社交平台入口）Sheet 的显示状态（统一由 AppBottomSheet 管理）
    val authorSheetState = rememberSheetState()
    // 检查更新 Sheet（点击「版本」条目弹出：自动检查开关 + 检查状态 + 手动检查）的显示状态
    val updateSheetState = rememberSheetState()
    // 从系统导入磁贴的确认对话框显示状态（统一由 AppDialog 管理）
    val importDialogState = rememberDialogState()
    // 备份列表相关状态
    val backupListSheetState = rememberSheetState()
    val deleteBackupDialogState = rememberDialogState()

    // Toast 文案在 Composable 上下文预解析,避免 launcher 回调/协程/onClick 内
    // context.getString 触发「非配置感知」lint 错误(LocalContextGetResourceValueCall)。
    // 注意:必须声明在下方 launchers 之前(Kotlin 先声明后使用)
    val savedToast = stringResource(R.string.settings_saved_toast)
    val saveFailedToast = stringResource(R.string.settings_save_failed_toast)
    val savedLocalToast = stringResource(R.string.settings_saved_local_toast)
    val copiedToast = stringResource(R.string.settings_copied_toast)
    val restoredToast = stringResource(R.string.settings_restored_toast)
    val invalidFormatToast = stringResource(R.string.settings_invalid_format_toast)
    val readErrorToast = stringResource(R.string.settings_read_error_toast)
    val loadFailedToast = stringResource(R.string.settings_load_failed_toast)
    val deletedToast = stringResource(R.string.settings_deleted_toast)
    val importedToast = stringResource(R.string.settings_imported_toast)
    val readFailedToast = stringResource(R.string.settings_read_failed_toast)
    val noneSelectedText = stringResource(R.string.settings_none_selected)
    val unknownText = stringResource(R.string.common_unknown)
    // 创造模式开关的结果提示（开启 / 关闭都有系统写入，失败必须让用户知道）
    val creativeReadFailedToast = stringResource(R.string.settings_creative_read_failed)
    val creativeWriteFailedToast = stringResource(R.string.settings_creative_write_failed)
    val creativeExitFailedToast = stringResource(R.string.settings_creative_exit_failed)
    // 恢复备份时「配置已写入、但创造模式布局没进系统」的提示
    val creativeRestoreFailedToast = stringResource(R.string.settings_restore_creative_failed)
    // 旧备份导入成功、但当前开着创造模式（面板上看不到变化）的提示
    val creativeRestoreActiveToast = stringResource(R.string.settings_restore_creative_active)

    /**
     * 统一的恢复结果提示。
     *
     * 三条恢复入口（本地备份 / 文件导入 / 粘贴 JSON）共用，避免各写一套 if-else 后文案走岔。
     * 特别是 [SettingsViewModel.RestoreResult.CreativeApplyFailed]：层数据其实已经恢复，
     * 只是没能切系统布局，必须给出与「格式错误」不同的提示。
     */
    fun showRestoreResult(result: SettingsViewModel.RestoreResult) {
        val message = when (result) {
            SettingsViewModel.RestoreResult.Success -> restoredToast
            SettingsViewModel.RestoreResult.InvalidFormat -> invalidFormatToast
            SettingsViewModel.RestoreResult.LoadFailed -> loadFailedToast
            SettingsViewModel.RestoreResult.RestoredCreativeActive -> creativeRestoreActiveToast
            SettingsViewModel.RestoreResult.CreativeApplyFailed -> creativeRestoreFailedToast
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // 「另存为」系统文件保存的 launcher：用户选择保存位置后将 JSON 写入
    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(viewModel.backupText.toByteArray())
                }
                Toast.makeText(context, savedToast, Toast.LENGTH_SHORT).show()
                backupSheetState.dismiss()
            } catch (_: Exception) {
                Toast.makeText(context, saveFailedToast, Toast.LENGTH_SHORT).show()
            }
        }
    }
    // 「从文件导入」的 launcher：用户选择 JSON 文件后读取并恢复配置
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val json = context.contentResolver.openInputStream(it)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: return@let
                scope.launch {
                    // 文件导入与粘贴 JSON 走同一套解析 / 恢复逻辑（含创造模式）
                    val result = viewModel.restoreFromJson(context, json)
                    withContext(Dispatchers.Main) { showRestoreResult(result) }
                }
            } catch (_: Exception) {
                Toast.makeText(context, readErrorToast, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

    // 首次进入页面：一次性读回全部设置项后再放行渲染（未就绪就空白，与主题/OOBE 门控策略一致）
    // 先让出启动首帧后的几帧：本页在冷启动首帧就会随 Pager 一起被组合，若此处立即开跑，
    // 加载完成后的「整页首次组合」（1300 多行 + 一堆卡片）会正好压住玻璃底栏指示器所等的
    // 那次重组，表现为底栏先出现、高亮块过半秒才冒出来且期间点击无响应。详见 awaitStartupFrames 注释。
    LaunchedEffect(Unit) {
        awaitStartupFrames()
        viewModel.loadSettings(context)
        viewModel.loadSystemTiles(context)
    }

    if (!viewModel.settingsLoaded) return

    // 打开应用选择器时，在后台线程加载应用列表（查询所有应用 + 加载图标较耗时）
    LaunchedEffect(appPickerSheetState.show) {
        if (appPickerSheetState.show) {
            viewModel.loadLaunchableApps(context)
        }
    }

    // ==================== 局部 UI 函数（闭包捕获，不显式传参） ====================
    // 拆分仅出于可读性 / IDE 导航考虑，不改变重组范围与任何业务逻辑。

    /** 通用分类卡片：磁贴行为设置 */
    @Composable
    fun GeneralCategoryCard() {
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

            // ===== 高级功能：创造模式（两套互相独立的层布局）=====
            // 放在「通用」分类最后。开启后 exTile 磁贴不再是「展开 / 收起」，而是切换层 1 / 层 2，
            // 因此上面的「磁贴联动」两个开关会被接管（置灰），改由每层的「当切换到此层时…」决定。
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SmallTitle(
                text = stringResource(R.string.settings_section_advanced),
                insideMargin = PaddingValues(start = 20.dp, top = 12.dp)
            )

            SwitchPreference(
                title = stringResource(R.string.settings_creative_title),
                summary = stringResource(R.string.settings_creative_summary),
                checked = viewModel.creativeModeEnabled,
                // 开关状态由「写系统成功」驱动，写入期间禁用避免连续点击并发写 sysui_qs_tiles
                enabled = !viewModel.creativeModeBusy,
                onCheckedChange = { enabled ->
                    viewModel.setCreativeMode(context, enabled) { result ->
                        val message = when (result) {
                            ConfigRepository.CreativeModeResult.Success -> null
                            ConfigRepository.CreativeModeResult.ReadFailed -> creativeReadFailedToast
                            ConfigRepository.CreativeModeResult.WriteFailed ->
                                if (enabled) creativeWriteFailedToast else creativeExitFailedToast
                        }
                        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    }
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.AiWriting) }
            )
        }
    }

    /** 外观分类卡片：主题设置 */
    @Composable
    fun AppearanceCategoryCard() {
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

    /** 数据分类卡片：备份 / 恢复 / 系统导入（合并原两个板块） */
    @Composable
    fun DataCategoryCard() {
        ExpandableSettingsCard(
            title = stringResource(R.string.settings_cat_data),
            icon = MiuixIcons.Glass.AllSdCardData,
            // 数据分类品牌色：红色；Monet 动态取色下会被自动压平为主题 primary
            containerColor = Color(0xFFFA382E),
            expanded = viewModel.expandedCategory == SettingsViewModel.SettingsCategory.DATA,
            onToggle = { viewModel.toggleCategory(SettingsViewModel.SettingsCategory.DATA) }
        ) {
            ArrowPreference(
                title = stringResource(R.string.settings_backup_title),
//                summary = "将当前配置导出为 JSON 格式",
                onClick = {
                    // 先刷新一次配置快照再生成 JSON：设置页是 Pager 里的常驻页，
                    // loadSettings 只在首次组合时跑过，编辑页之后的改动不会自动同步过来。
                    // 备份必须反映「此刻」的配置（含创造模式的两层）。
                    scope.launch {
                        viewModel.refreshBackupSource(context)
                        viewModel.backupText = viewModel.generateBackupJson()
                        backupSheetState.show()
                    }
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Remove) }
            )

            ArrowPreference(
                title = stringResource(R.string.settings_restore_title),
//                summary = "从 JSON 文本恢复磁贴配置",
                onClick = {
                    viewModel.clearImportBackupJson()
                    importBackupSheetState.show()
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Import) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SmallTitle(
                text = stringResource(R.string.settings_section_tools),
                insideMargin = PaddingValues(start = 20.dp, top = 12.dp)
            )

            ArrowPreference(
                title = stringResource(R.string.settings_view_sysui_title),
                summary = stringResource(R.string.settings_view_sysui_summary),
                onClick = { systemTilesSheetState.show() },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.PhoneIcon) }
            )

            ArrowPreference(
                title = stringResource(R.string.settings_import_system_title),
                // 创造模式下系统里显示的是某一层的布局，导进来只会把普通配置污染成该层内容；
                // 层内导入请走磁贴编辑页的「从系统导入」（只写当前编辑层）
                summary = if (viewModel.creativeModeEnabled) {
                    stringResource(R.string.settings_import_system_summary_creative)
                } else {
                    stringResource(R.string.settings_import_system_summary)
                },
                enabled = !viewModel.creativeModeEnabled,
                onClick = { importDialogState.show() },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.PasteEditPoint) }
            )
        }
    }

    /** 关于分类卡片：版本 / GitHub / 开源许可 */
    @Composable
    fun AboutCategoryCard() {
        // MS Clarity 匿名统计同意状态
        val clarityConsent by ConfigRepository.getClarityConsentFlow(context)
            .collectAsState(initial = false)

        ExpandableSettingsCard(
            title = stringResource(R.string.settings_cat_about),
            // 关于分类使用应用自身的 ExTile 图标（vector drawable），
            // 通过 vectorResource 转成 ImageVector，交由通用组件「有底图标」统一染色
            icon = ImageVector.vectorResource(R.drawable.ic_settings_extile),
            // 该图标内容（4~44 / 48 视口）比 miuix 图标更「满」，同尺寸渲染反而显大，
            // 因此比其它分类图标小一档（24dp vs 26dp）做视觉对齐
            iconSize = 24.dp,
            // 关于分类品牌色：灰色；Monet 动态取色下会被自动压平为主题 primary
            containerColor = Color(0xFF8C9DAF),
            expanded = viewModel.expandedCategory == SettingsViewModel.SettingsCategory.ABOUT,
            onToggle = { viewModel.toggleCategory(SettingsViewModel.SettingsCategory.ABOUT) }
        ) {
            // 作者：头像 + 昵称 + 身份，点击弹出作者信息 Sheet 展示各社交平台入口
            ArrowPreference(
                title = "hrsthrt74",
                summary = stringResource(R.string.settings_author_role),
                onClick = { authorSheetState.show() },
                // 头像为彩色图片，需用 Color.Unspecified 保留原图颜色，不能走 PreferenceLeadingIcon 的染色逻辑
                startAction = {
                    Icon(
                        painter = painterResource(R.drawable.avatar),
                        contentDescription = stringResource(R.string.settings_author_avatar),
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .padding(start = PreferenceIconStartPadding - 4.dp, end = PreferenceIconEndPadding - 4.dp)
                            .size(PreferenceIconSize + 8.dp)
                            .squircleClip(8.dp)
                    )
                }
            )

            // 版本号：点击弹出「检查更新」Sheet（自动检查开关 / 检查状态 / 手动检查）
            ArrowPreference(
                title = stringResource(R.string.settings_version_title),
                summary = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: unknownText
                } catch (e: Exception) {
                    unknownText
                },
                onClick = { updateSheetState.show() },
                startAction = { PreferenceLeadingIcon(R.drawable.ic_settings_extile) },
                // 版本号 -> Clarity
                modifier = Modifier.clarityUnmask()
            )

            // Github Repo
            ArrowPreference(
                title = "GitHub",
                summary = "hrsthrt74/exTile",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/hrsthrt74/exTile")))
                },
                startAction = { PreferenceLeadingIcon(R.drawable.ic_github) }
            )

            // 交流群（QQ 群）：仅当系统语言为简体中文时显示（群聊仅面向简中用户）
            // 简体中文判断：语言为 zh 且文字为 Hans（或地区为 CN/SG），可覆盖 zh-CN/zh-Hans 等变体，
            // 排除繁体环境（zh-TW/zh-HK 的 script 为 Hant）。
            // 使用 LocalConfiguration 而非 context.resources.configuration：前者是配置感知的，
            // 语言变化时会触发重组，避免读到过期的旧配置
            val locale = LocalConfiguration.current.locales[0]
            val isSimplifiedChinese = locale?.language == "zh" &&
                (locale.script == "Hans" || locale.country == "CN" || locale.country == "SG")
            if (isSimplifiedChinese) {
                ArrowPreference(
                    title = stringResource(R.string.settings_qq_group_title),
                    summary = stringResource(R.string.settings_qq_group_summary),
                    onClick = {
                        // 群号复制到剪贴板，方便直接在 QQ 里搜索加群
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("QQ群号", QQ_GROUP_NUMBER)
                        )
                        // 提示已复制；用 applicationContext 避免 Android 12+ 的 Toast 限制问题
                        Toast.makeText(
                            context.applicationContext,
                            context.getString(R.string.settings_qq_group_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                        // runCatching 兜底：设备无浏览器应用时避免崩溃
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(QQ_GROUP_URL))
                            )
                        }
                    },
                    startAction = { PreferenceLeadingIcon(MiuixIcons.Community) }
                )
            }

            // 支持作者
            ArrowPreference(
                title = stringResource(R.string.settings_donate_title),
                summary = stringResource(R.string.settings_donate_summary),
                onClick = { donateSheetState.show() },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Favorites) }
            )

             // 文档：隐私政策 / 用户协议 / 开源许可 统一入口，进入 Docs 页用 Tab 切换
            ArrowPreference(
                title = stringResource(R.string.settings_docs_title),
                summary = stringResource(R.string.settings_docs_summary),
                onClick = {
                    context.startActivity(Intent(context, DocsActivity::class.java))
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.File) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SmallTitle(
                text = stringResource(R.string.settings_section_other),
                insideMargin = PaddingValues(start = 20.dp, top = 12.dp)
            )

            // 允许匿名统计开关：开启时初始化 Clarity，关闭时标记停止状态
            // Clarity SDK 不支持运行时停止，关闭后需重启应用才能完全停止数据收集
            SwitchPreference(
                title = stringResource(R.string.settings_clarity_title),
                summary = stringResource(R.string.settings_clarity_summary),
                checked = clarityConsent,
                onCheckedChange = { newValue ->
                    scope.launch {
                        ConfigRepository.saveClarityConsent(context, newValue)
                        val application = context.applicationContext as? ExTileApplication
                        if (newValue) {
                            // 同意：初始化 Clarity
                            application?.initClarity()
                        } else {
                            // 撤销同意：标记停止状态
                            application?.shutdownClarity()
                        }
                    }
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.SoundWave) }
            )

            // 重新运行首次配置引导：仅重置 OOBE 标志（配置数据保留，可再次导入覆盖），
            // 壳层监听到标志变 false 后自动切回 OOBE 页面
            ArrowPreference(
                title = stringResource(R.string.settings_rerun_oobe_title),
                onClick = {
                    scope.launch {
                        ConfigRepository.saveOobeCompleted(context, false)
                    }
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.Start) }
            )
        }
    }

    /** 检查更新 Sheet：自动检查开关 / 检查状态 / 手动检查 / 当前版本更新日志（点击「版本」条目弹出） */
    @Composable
    fun UpdateSheet() {
        // 自动检查更新开关：直连 DataStore，与 OOBE 隐私页的开关共享同一状态
        val autoCheckUpdate by ConfigRepository.getAutoCheckUpdateFlow(context)
            .collectAsState(initial = false)
        // 检查状态：来自 UpdateChecker 的进程级共享状态（冷启动自动检查的结果也会体现在这里）
        val checkState by UpdateChecker.state.collectAsState()
        // 是否正在检查：检查中要禁用手动按钮，避免并发重复请求
        val checking = checkState is UpdateCheckState.Checking
        // 当前安装版本号：用于在内置更新日志里匹配对应版本小节；
        // PackageManager 读取放 remember 里，避免每次重组都查一遍
        val currentVersionName = remember(context) {
            UpdateChecker.getCurrentVersionName(context).orEmpty()
        }
        // 内置更新日志：从随应用打包的 assets/CHANGELOG（按应用语言选中文/英文）里切出当前安装版本那一节。
        // 与检查更新完全解耦——打开 Sheet 就能看到，无需联网；文件很小，仍在 IO 线程读取
        val builtinChangelog by produceState<ChangelogEntry?>(initialValue = null, context, currentVersionName) {
            value = withContext(Dispatchers.IO) {
                ChangelogProvider.loadForVersion(context, currentVersionName)
            }
        }
        // 内置日志卡片的标题：「版本号 · 发布日期」（如 1.0.2 · 2026-10-10）。
        // 日期取自 CHANGELOG 节标题的括号，没写日期就只显示版本号；
        // 整节都读不到时退化为通用标题「更新日志」，避免标题空着
        val fallbackChangelogTitle = stringResource(R.string.update_changelog_title)
        val builtinChangelogTitle = builtinChangelog
            ?.let { entry -> entry.date?.let { date -> "${entry.version} · $date" } ?: entry.version }
            ?: fallbackChangelogTitle

        AppBottomSheet(
            state = updateSheetState,
            title = stringResource(R.string.settings_update_sheet_title)
        ) {
            // 整页滚动：日志不再单独限高内滚，长日志跟随整个 Sheet 内容一起滑动
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 自动检查更新开关：开启后应用每次冷启动时会静默检查一次
                // 浮层内的卡片走 AppCard：玻璃风格自动换成 GlassCard（半透明，露出 Sheet 材质）
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = stringResource(R.string.settings_update_auto_title),
                        checked = autoCheckUpdate,
                        onCheckedChange = { newValue ->
                            scope.launch {
                                ConfigRepository.saveAutoCheckUpdate(context, newValue)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 手动检查按钮：检查中禁用并用文案提示，防止并发重复请求。
                // 检查结果不再占用卡片，直接用 Toast 反馈（已是最新 / 发现新版本 / 暂无发布 / 失败）
                Button(
                    onClick = {
                        scope.launch {
                            val result = UpdateChecker.checkForUpdate(context)
                            // Idle / Checking 是过程态，没有可提示的结论；其余终态各给一句
                            val message = when (result) {
                                is UpdateCheckState.UpToDate ->
                                    context.getString(R.string.update_status_up_to_date)
                                is UpdateCheckState.Available ->
                                    context.getString(R.string.update_status_available, result.latestVersion)
                                is UpdateCheckState.NoRelease ->
                                    context.getString(R.string.update_status_no_release)
                                is UpdateCheckState.Failed ->
                                    context.getString(R.string.update_status_failed)
                                else -> null
                            }
                            message?.let {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !checking,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 检查中把按钮文案换成「正在检查更新…」，让按钮变灰有明确原因
                    Text(
                        stringResource(
                            if (checking) R.string.update_status_checking else R.string.update_check_now
                        )
                    )
                }

                // 当前版本更新日志：来自打包进 assets 的 CHANGELOG（按应用语言选择），
                // 离线渲染、与检查结果无关，始终显示
                val changelogEntry = builtinChangelog
                if (changelogEntry != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = builtinChangelogTitle,
                                style = MiuixTheme.textStyles.body1,
                                fontWeight = FontWeight.Bold
                            )
                            // Markdown 渲染（标题/列表/加粗/链接等）：不设限高，跟随 Sheet 整体滚动
                            ChangelogMarkdownText(changelog = changelogEntry.body)
                        }
                    }
                }

                // 底部避让导航栏（与其他 Sheet 保持一致）
                Spacer(modifier = Modifier.padding(bottom = navBarBottomPadding))
            }
        }
    }

    /** 系统磁贴查看 Sheet */
    @Composable
    fun SystemTilesSheet() {
        AppBottomSheet(
            state = systemTilesSheetState,
            title = stringResource(R.string.settings_sheet_sysui_title),
        ) {
            Text(
                text = viewModel.currentSysuiTiles.ifEmpty { stringResource(R.string.common_no_data) },
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.padding(bottom = navBarBottomPadding)
            )
        }
    }

    /** 备份结果 Sheet：显示 JSON 预览，支持「保存」「另存为」「复制」三种操作 */
    @Composable
    fun BackupSheet() {
        AppBottomSheet(
            state = backupSheetState,
            title = stringResource(R.string.settings_sheet_backup_title),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = navBarBottomPadding)
            ) {
                Text(
                    text = viewModel.backupText,
                    // 别问为什么是脚注2
                    style = MiuixTheme.textStyles.footnote2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(horizontal = 8.dp)
                        .verticalScroll(rememberScrollState())
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 「保存」按钮：将配置保存到 app 内部存储
                Button(
                    onClick = {
                        scope.launch {
                            val fileName = viewModel.saveBackup(context)
                            if (fileName != null) {
                                Toast.makeText(context, savedLocalToast, Toast.LENGTH_SHORT).show()
                                backupSheetState.dismiss()
                            } else {
                                Toast.makeText(context, saveFailedToast, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.common_save))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 「另存为」按钮：拉起系统文件选择器，用户选择保存位置
                Button(
                    onClick = {
                        // 生成默认文件名：exTile_backup_yyyyMMdd_HHmmss.json
                        val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                        val defaultName = "exTile_backup_${sdf.format(java.util.Date())}"
                        saveAsLauncher.launch(defaultName)
                        backupSheetState.dismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_save_as))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 「复制」按钮：将 JSON 复制到剪贴板
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("exTile backup", viewModel.backupText))
                        Toast.makeText(context, copiedToast, Toast.LENGTH_SHORT).show()
                        backupSheetState.dismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.common_copy))
                }
            }
        }
    }

    /**
     * 支持作者 Sheet：微信 / 支付宝收款码。
     *
     * 头部用分段 TabRow 切换打赏渠道（0 = 微信 / 1 = 支付宝），两张收款码用 [AnimatedContent] 的
     * 默认动画（淡入淡出 + 轻微缩放）切换。TabRow 的写法照搬「添加磁贴」Sheet
     * （`ui/screens/tileconfig/AddTileSheet.kt`）：
     * - 玻璃风格：`GlassSegmentedTabRow`（胶囊轨道 + 滑块指示器），阴影画在胶囊裁剪之前
     *   （阴影会溢出节点边界，塞进 clip 之内只会被裁成一圈硬边）；
     * - 经典风格：`TabRowWithContour`；
     * - 玻璃控件的采样源必须在 Sheet 内部单独录制，**不能**传页面级 backdrop——那样采样到的是
     *   Sheet 后面的设置页，看起来像「穿透了 Sheet」。
     *
     * 采样层还必须覆盖到 TabRow 所在的区域，玻璃控件才能取到背景，故沿用添加磁贴的
     * 「头部浮层 + 内容层」结构：头部（感谢文案 + TabRow）作为浮层叠在采样层之上，采样层用
     * 顶部内边距让出头部高度——既是采样源，又不会被自身采样（本层不含 TabRow）。
     *
     * 收款码宽度由 [DonateQrMaxWidth] 封顶，避免在平板 / 横屏下撑满整个 Sheet。
     */
    @Composable
    fun DonateSheet() {
        val haptic = LocalHapticFeedback.current
        // 当前展示的打赏渠道：0 = 微信，1 = 支付宝
        var donateTab by remember { mutableIntStateOf(0) }

        // 两个渠道的 Tab 文案
        val donateTabLabels = listOf(
            stringResource(R.string.settings_donate_tab_wx),
            stringResource(R.string.settings_donate_tab_alipay),
        )

        AppBottomSheet(
            state = donateSheetState,
            title = stringResource(R.string.settings_donate_title),
            // 关闭动画完成后复位，下次打开总是从微信开始
            onDismissed = { donateTab = 0 },
        ) {
            // 头部（感谢文案 + TabRow）的实测高度（px）：测量后回填成内容层的顶部内边距，
            // 让采样层从 Sheet 内容区顶部开始，从而把 TabRow 所在的区域也包含进去
            var headerHeightPx by remember { mutableIntStateOf(0) }
            val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }

            // 采样源：在 Sheet 内单独录制（模糊关闭 / 不支持时返回 null，TabRow 回退经典样式）
            val headerBackdrop = rememberBlurBackdrop(enabled = glassStyle && themeSettings.enableBlur)

            Box(modifier = Modifier.fillMaxWidth()) {
                // 内容层 = 采样层：收款码 + 打赏声明，顶部让出头部高度（头部浮层在其之外，不被自身采样）
                Box(
                    modifier = if (headerBackdrop != null) {
                        Modifier.fillMaxWidth().layerBackdrop(headerBackdrop)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = headerHeight, bottom = navBarBottomPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 两张收款码之间切换：直接用 AnimatedContent 的默认动画
                        AnimatedContent(
                            targetState = donateTab,
                            contentAlignment = Alignment.Center,
                            label = "donateTabContent",
                            modifier = Modifier.padding(top = 16.dp)
                        ) { tab ->
                            Image(
                                painter = painterResource(
                                    if (tab == 0) R.drawable.pic_donate_wx
                                    else R.drawable.pic_donate_alipay
                                ),
                                contentDescription = stringResource(
                                    if (tab == 0) R.string.settings_donate_qr_desc_wx
                                    else R.string.settings_donate_qr_desc_alipay
                                ),
                                // 宽度 = min(可用宽度, DonateQrMaxWidth)。
                                // **必须先 widthIn 再 fillMaxWidth**：顺序反过来的话，fillMaxWidth
                                // 会先把约束钉成父级宽度，widthIn 的 max 会被强制拉回去（限宽失效）；
                                // 这样写窄屏仍是「铺满可用宽度」，宽屏则停在 DonateQrMaxWidth 上。
                                modifier = Modifier
                                    .widthIn(max = DonateQrMaxWidth)
                                    .fillMaxWidth()
                                    .squircleClip(16.dp)
                            )
                        }
                        // 打赏声明：说明继续打赏即视为同意《用户协议》第 3 节（费用与打赏），且不支持退款。
                        // 「用户协议」为可点击链接，点击后打开 Docs 页并定位到「用户协议」Tab。
                        Text(
                            text = buildAnnotatedString {
                                append(stringResource(R.string.settings_donate_agree_prefix))
                                // 链接样式：主题色 + 下划线，与 OOBE 页的法律文档链接保持一致
                                withLink(
                                    LinkAnnotation.Clickable(
                                        tag = DocsTab.TERMS_OF_SERVICE.name,
                                        styles = TextLinkStyles(
                                            style = SpanStyle(
                                                color = MiuixTheme.colorScheme.primary,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        ),
                                        linkInteractionListener = { link ->
                                            if (link is LinkAnnotation.Clickable) {
                                                context.startActivity(
                                                    Intent(context, DocsActivity::class.java).putExtra(
                                                        // EXTRA 以 String 形式传递（枚举序列化后 getStringExtra 取不到），
                                                        // 与 OOBE 页的写法保持一致，避免点击被兜底为隐私政策。
                                                        DocsActivity.EXTRA_TAB,
                                                        DocsTab.TERMS_OF_SERVICE.name
                                                    )
                                                )
                                            }
                                        }
                                    )
                                ) {
                                    append(stringResource(R.string.settings_donate_agree_link_terms))
                                }
                                append(stringResource(R.string.settings_donate_agree_suffix))
                            },
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(), start = 12.dp, end = 12.dp)
                        )
                    }
                }

                // 头部浮层：感谢文案 + 渠道 TabRow（高度回填给上面内容层的顶部内边距）
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .onSizeChanged { headerHeightPx = it.height },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.settings_donate_thanks),
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    if (glassStyle && headerBackdrop != null) {
                        // 玻璃风格：官方 GlassSegmentedTabRow（胶囊轨道 + 滑动指示器），
                        // 采样的是 Sheet 内录制的 headerBackdrop，不会被自身内容污染。
                        // 阴影（关键）：玻璃阴影是画在节点边界之外的，塞进 clip 之内只会被裁成一圈硬边，
                        // 故把 glassShadow 挂在外层 Box、clip 在其之后，组件自身传 shadow = null。
                        // 颜色用库自带的 GlassShadows.Float（纯黑低透明），不要贴主题色 / onSurface，
                        // 浅色模式下那会变成一圈亮色、看着像「反色的阴影」。
                        val tabTrackShape = remember {
                            GlassShape(GlassSegmentedTabRowDefaults.Height / 2, smoothing = 0f)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassShadow(tabTrackShape, GlassShadows.Float)
                                .clip(Capsule())
                        ) {
                            GlassSegmentedTabRow(
                                tabs = donateTabLabels,
                                selectedIndex = donateTab,
                                onSelect = { index ->
                                    // 只在真正切换时给触感反馈
                                    if (index != donateTab) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        donateTab = index
                                    }
                                },
                                backdrop = headerBackdrop,
                                // 阴影已在外层画好（见上）
                                shadow = null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        // 经典风格：Miuix 描边分段控件
                        TabRowWithContour(
                            tabs = donateTabLabels,
                            selectedTabIndex = donateTab,
                            onTabSelected = { index ->
                                if (index != donateTab) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    donateTab = index
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    /** 作者信息 Sheet：展示作者各社交平台入口，点击对应平台打开其主页链接 */
    @Composable
    fun AuthorSheet() {
        AppBottomSheet(
            state = authorSheetState,
            title = "hrsthrt74",
        ) {
            // 平台入口 Row：每个平台一个 Column（上方图标 + 下方平台名），点击打开对应链接
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = navBarBottomPadding + 16.dp),
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

    /** 导入备份 Sheet：支持「粘贴 JSON」恢复和「从本地备份」恢复两种方式 */
    @Composable
    fun ImportBackupSheet() {
        AppBottomSheet(
            state = importBackupSheetState,
            title = stringResource(R.string.settings_restore_title),
            // 关闭动画完成后清空输入的 JSON，避免下次打开残留
            onDismissed = { viewModel.clearImportBackupJson() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = navBarBottomPadding)
            ) {
                // 「从本地备份恢复」按钮：打开备份列表选择已有备份
                Button(
                    onClick = {
                        // 关闭当前 Sheet 后打开备份列表 Sheet，避免多层 Sheet 堆叠
                        importBackupSheetState.dismiss()
                        viewModel.loadBackupList(context)
                        backupListSheetState.show()
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_restore_local))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 「从文件导入」按钮：拉起系统文件选择器，读取外部 JSON 文件
                Button(
                    onClick = {
                        importBackupSheetState.dismiss()
                        openFileLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_restore_file))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // 以下为原有的「粘贴 JSON」恢复方式
                AppTextField(
                    value = viewModel.importBackupJson,
                    onValueChange = { viewModel.importBackupJson = it },
                    label = stringResource(R.string.settings_restore_paste_label),
                    useLabelAsPlaceholder = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val result = viewModel.restoreFromJson(context, viewModel.importBackupJson)
                            showRestoreResult(result)
                            // 恢复成功（含「层数据已恢复、系统布局没写进去」）都关闭 Sheet；
                            // 只有格式错误保持打开，方便用户就地修正粘贴内容
                            if (result != SettingsViewModel.RestoreResult.InvalidFormat) {
                                importBackupSheetState.dismiss()
                                viewModel.clearImportBackupJson()
                            }
                        }
                    },
//                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.common_restore))
                }
            }
        }
    }

    /** 自定义磁贴名输入 Sheet */
    @Composable
    fun CustomTileLabelSheet() {
        AppBottomSheet(
            state = customTileLabelSheetState,
            title = stringResource(R.string.settings_sheet_custom_label_title),
            // 关闭动画完成后清空临时输入，避免下次打开残留
            onDismissed = { viewModel.clearCustomTileLabelInput() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = navBarBottomPadding)
            ) {
                AppTextField(
                    value = viewModel.customTileLabelInput,
                    onValueChange = { viewModel.customTileLabelInput = it },
                    label = stringResource(R.string.settings_custom_label_hint),
                    useLabelAsPlaceholder = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // 去掉首尾空白后保存；空内容则视为未设置（显示时会回退到「更多磁贴」）
                        val name = viewModel.customTileLabelInput.trim()
                        viewModel.saveTileLabelCustom(context, name)
                        customTileLabelSheetState.dismiss()
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.common_save))
                }
            }
        }
    }

    /** 本地备份列表 Sheet：展示已保存的备份，支持恢复和删除 */
    @Composable
    fun BackupListSheet() {
        AppBottomSheet(
            state = backupListSheetState,
            title = stringResource(R.string.settings_sheet_backup_list_title),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = navBarBottomPadding)
            ) {
                if (viewModel.backupList.isEmpty()) {
                    // 空状态提示
                    Text(
                        text = stringResource(R.string.settings_no_backups),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    // 备份列表：每条显示日期时间、磁贴数量，右侧有删除按钮
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = navBarBottomPadding)
                    ) {
                        items(viewModel.backupList, key = { it.fileName }) { backup ->
                            // 浮层内卡片：玻璃风格用 GlassCard，经典风格沿用 secondaryContainer 实色
                            AppCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                insideMargin = PaddingValues(16.dp),
                                color = MiuixTheme.colorScheme.secondaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 左侧：点击恢复
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                scope.launch {
                                                    val result = viewModel.restoreFromBackup(context, backup)
                                                    withContext(Dispatchers.Main) {
                                                        showRestoreResult(result)
                                                        // 读文件失败时保持列表打开，方便换一个备份再试
                                                        if (result != SettingsViewModel.RestoreResult.LoadFailed) {
                                                            backupListSheetState.dismiss()
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = BackupRepository.formatDate(backup.createdAt),
                                            style = MiuixTheme.textStyles.body1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            // 含创造模式数据时多标一层：用户能一眼分辨
                                            // 「老备份只恢复普通布局」和「新备份连两层一起恢复」
                                            text = if (backup.creativeLayerCount > 0) {
                                                stringResource(
                                                    R.string.settings_backup_counts_creative,
                                                    backup.expandedCount,
                                                    backup.collapsedCount,
                                                    backup.creativeLayerCount
                                                )
                                            } else {
                                                stringResource(
                                                    R.string.settings_backup_counts,
                                                    backup.expandedCount,
                                                    backup.collapsedCount
                                                )
                                            },
                                            style = MiuixTheme.textStyles.footnote2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    }

                                    // 右侧：删除按钮
                                    IconButton(onClick = {
                                        viewModel.deleteTargetBackup = backup
                                        deleteBackupDialogState.show()
                                    }) {
                                        Icon(
                                            imageVector = MiuixIcons.Delete,
                                            contentDescription = stringResource(R.string.common_delete),
                                            tint = MiuixTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** 删除备份确认 Dialog */
    @Composable
    fun DeleteBackupDialog() {
        AppDialog(
            state = deleteBackupDialogState,
            title = stringResource(R.string.settings_delete_backup_title),
            summary = stringResource(
                R.string.settings_delete_backup_summary,
                viewModel.deleteTargetBackup?.let { BackupRepository.formatDate(it.createdAt) } ?: ""
            ),
            confirmText = stringResource(R.string.common_delete),
            destructive = true,
            onConfirm = {
                val target = viewModel.deleteTargetBackup ?: return@AppDialog
                scope.launch {
                    val success = viewModel.deleteBackup(context, target)
                    if (success) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, deletedToast, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                viewModel.clearDeleteTargetBackup()
            },
            onDismissed = { viewModel.clearDeleteTargetBackup() }
        )
    }

    /** 系统导入确认 Dialog */
    @Composable
    fun ImportSystemTilesDialog() {
        AppDialog(
            state = importDialogState,
            title = stringResource(R.string.settings_import_system_title),
            summary = stringResource(R.string.settings_import_system_dialog_summary),
            confirmText = stringResource(R.string.common_confirm),
            onConfirm = {
                scope.launch {
                    val success = viewModel.importFromSystem(context)
                    if (success) {
                        Toast.makeText(context, importedToast, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, readFailedToast, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    /** 自定义应用选择器 Sheet */
    @Composable
    fun AppPickerSheet() {
        AppBottomSheet(
            state = appPickerSheetState,
            title = stringResource(R.string.settings_sheet_app_picker_title),
            // 关闭动画完成后清空搜索关键字，避免下次打开残留
            onDismissed = { viewModel.clearAppSearchQuery() },
        ) {
            val haptic = LocalHapticFeedback.current
            // 列表为空时是加载中，需要区分"加载中"和"确实没有应用"
            val apps = viewModel.launchableApps
            val isLoading = apps == null

            Column(modifier = Modifier.clarityMask()) {
                when {
                    isLoading -> {
                        // 加载状态：使用 Miuix 无限进度指示器 + 加载文本，居中显示
                        // 用 fillMaxHeight 撑满，与列表加载完成后的 sheet 高度一致，避免高度突变
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            InfiniteProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.common_loading),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                    apps.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.settings_no_apps),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    }
                    else -> {
                        // 根据搜索关键字过滤：同时匹配应用名和包名（不区分大小写）
                        val filteredApps = remember(apps, viewModel.appSearchQuery) {
                            val query = viewModel.appSearchQuery.trim()
                            if (query.isEmpty()) {
                                apps
                            } else {
                                apps.filter {
                                    it.label.contains(query, ignoreCase = true) ||
                                        it.packageName.contains(query, ignoreCase = true)
                                }
                            }
                        }
                        // 搜索框（支持按应用名或包名搜索）
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SearchBar(
                                expanded = false,
                                onExpandedChange = { expanded ->
                                    if (!expanded) {
                                        appPickerSheetState.dismiss()
                                    }
                                },
                                insideMargin = DpSize(0.dp, 4.dp),
                                inputField = {
                                    InputField(
                                        query = viewModel.appSearchQuery,
                                        onQueryChange = { viewModel.appSearchQuery = it },
                                        onSearch = {},
                                        // 避免打开 sheet 时自动聚焦弹键盘
                                        expanded = false,
                                        onExpandedChange = {},
                                        label = stringResource(R.string.common_search),
                                        color = MiuixTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { }

                            // 搜索框与结果列表之间的间距（用 Spacer 实现，确保有效）
//                    Spacer(modifier = Modifier.height(4.dp))

                            if (filteredApps.isEmpty()) {
                                // 搜索无结果提示
                                Text(
                                    text = stringResource(R.string.settings_no_matching_apps),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
                                    contentPadding = PaddingValues(bottom = navBarBottomPadding)
                                ) {
                                    items(filteredApps, key = { it.packageName }) { app ->
                                // 每个应用项用卡片包裹，提供背景（无阴影）+ 圆角；
                                // 浮层内走 AppCard：玻璃风格换成 GlassCard（半透明），经典风格保持 secondaryContainer
                                AppCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.saveCustomAppPackage(context, app.packageName)
                                            appPickerSheetState.dismiss()
                                        },
                                    insideMargin = PaddingValues(16.dp),
                                    color = MiuixTheme.colorScheme.secondaryContainer,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 应用图标
                                        val icon = app.icon
                                        val bitmap = remember(icon) { icon.toBitmap() }
                                        Icon(
                                            painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) },
                                            contentDescription = app.label,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(48.dp)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = app.label,
                                                style = MiuixTheme.textStyles.body1
                                            )
                                            Text(
                                                text = app.packageName,
                                                style = MiuixTheme.textStyles.footnote2,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

    Scaffold(
        topBar = {
            // 统一自适应顶栏：渐进模糊开→官方 GlassTopAppBar，关→原版顶栏（普通模糊）
            // 窄屏保留大标题形态，宽屏（侧边导航栏）用无大标题形态
            AppTopBar(
                title = stringResource(R.string.nav_settings),
                largeTitle = stringResource(R.string.nav_settings),
                useLargeTitle = !LocalIsWideScreen.current,
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                progressive = themeSettings.progressiveBlur,
                actions = {
                    // 调试工具入口：右上角设置图标按钮，点击直接跳转 DebugToolsActivity
                    // 用 TopBarIconButton 跟随顶栏形态：玻璃态→GlassIconButton，原版→IconButton
                    TopBarIconButton(onClick = {
                        context.startActivity(Intent(context, DebugToolsActivity::class.java))
                    }) {
                        Icon(MiuixIcons.Glass.Maintenance, contentDescription = stringResource(R.string.debug_title))
                    }
                }
            )
        }
    ) { paddingValues ->
        // 滚动内容挂载 backdrop，供顶部栏模糊捕获
        Box(
            modifier = if (backdrop != null) Modifier.fillMaxSize().layerBackdrop(backdrop) else Modifier.fillMaxSize()
        ) {
            // 保持垂直居中布局（内容不足一屏时卡片居中显示，此时顶栏下方是空白，
            // 顶栏模糊采样不到内容、看不到模糊——这是居中布局与「内容不足一屏就有模糊」
            // 不可兼得的取舍，折叠状态下无模糊属预期）。
            //
            // 关键：把「避让顶栏」从 Column 的 padding 改到 LazyColumn 的 contentPadding.top。
            // Scaffold 内容区从 (0,0) 开始、topBar 叠在上方，若用 Column padding(top=顶栏高度)
            // 会把内容推到顶栏下方，backdrop 图层的 0~顶栏 区域始终是空白，顶栏模糊采不到内容；
            // 改为 contentPadding.top 后，展开分类/滚动时内容会进入顶栏采样区，顶栏模糊即可见。
            // （居中基准因 contentPadding.top 同时增大 LazyColumn 高度与卡片起始偏移而相互抵消，
            //   折叠状态下卡片位置相对此前仅 ~2dp 变化，布局基本不变）
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .scrollEndHaptic(
                            hapticFeedbackType = HapticFeedbackType.TextHandleMove
                        ),
                    contentPadding = PaddingValues(
                        // 顶部避让顶栏（与编辑页一致）：展开/滚动时内容进入顶栏采样区供模糊捕获
                        top = paddingValues.calculateTopPadding() + 4.dp,
                        // 水平边距 = Scaffold 已算好的挖孔/导航条 insets（卡片自身另有 16dp 边距）：
                        // 竖屏水平为 0 保持原样，横屏/反向横屏自动避让左右摄像头挖孔；
                        // 宽屏起始侧已消费，自动为 0，不与 NavigationRail 重复避让
                        start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                        end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                        bottom = contentBottomPadding()
                    )
                ) {

                    // 写 0 是因为 item 之间有间距
            item { Spacer(modifier = Modifier.height(0.dp)) }

            // ===== 通用分类：磁贴行为设置 =====
            item { GeneralCategoryCard() }

            // ===== 外观分类：主题设置 =====
            item { AppearanceCategoryCard() }

            // ===== 数据分类：备份 / 恢复 / 系统导入（合并原两个板块） =====
            item { DataCategoryCard() }

            // ===== 关于分类 =====
            item { AboutCategoryCard() }

                    // 同上面那个 Spacer
            item { Spacer(modifier = Modifier.height(0.dp)) }
                }
            }
        }
    }

    // ---- 系统磁贴查看 Sheet ----
    SystemTilesSheet()

    // ---- 备份结果 Sheet ----
    BackupSheet()

    // ---- 支持作者 Sheet ----
    DonateSheet()

    // ---- 作者信息 Sheet ----
    AuthorSheet()

    // ---- 检查更新 Sheet ----
    UpdateSheet()

    // ---- 导入备份 Sheet ----
    ImportBackupSheet()

    // ---- 自定义磁贴名 Sheet ----
    CustomTileLabelSheet()

    // ---- 本地备份列表 Sheet ----
    BackupListSheet()

    // ---- 系统导入确认 Dialog ----
    ImportSystemTilesDialog()

    // ---- 删除备份确认 Dialog ----
    DeleteBackupDialog()

    // ---- 自定义应用选择器 Sheet ----
    AppPickerSheet()
}

/**
 * 作者信息 Sheet 中的单个平台入口项。
 * 纵向布局：上方为平台图标，下方为平台名，整体可点击打开对应链接。
 * @param iconRes 平台图标 drawable 资源 id
 * @param label 平台名
 * @param url 点击后跳转的链接
 * @param keepOriginalColor 是否为彩色图片（如 webp 图标），true 时保留原图颜色不染色；纯色矢量图标传 false（默认）用主题前景色染色
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

    Card(colors = CardDefaults.defaultColors(
        color = MiuixTheme.colorScheme.secondaryContainerVariant
    )) {
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

/**
 * 设置项行首图标。
 * 放在各 Preference 的 startAction 中，使用图标自身配色 + 图标宽度（[PreferenceIconSize]），
 * 左右留出间距（[PreferenceIconStartPadding] / [PreferenceIconEndPadding]）。
 * @param icon 图标
 */
@Composable
private fun PreferenceLeadingIcon(icon: ImageVector) {
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
 * @param iconRes drawable 资源 id（R.drawable.xxx）
 */
@Composable
private fun PreferenceLeadingIcon(iconRes: Int) {
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
 */
@Composable
private fun PreferenceLeadingPlaceholder() {
    Spacer(
        modifier = Modifier
            .padding(start = PreferenceIconStartPadding, end = PreferenceIconEndPadding)
            .size(PreferenceIconSize)
    )
}

/**
 * 可展开的设置分类卡片。
 * 点击标题行在展开/收起之间切换，内容使用 AnimatedVisibility 平滑展开/收起。
 * 标题行左侧为分类图标（通用组件 [ContainedIcon]「有底图标」），右侧箭头图标随展开状态旋转
 * （收起时指向右，展开时指向下）。
 * @param title 分类标题
 * @param icon 分类图标
 * @param iconSize 分类图标绘制尺寸（默认 26dp，可按图标观感单独微调）
 * @param containerColor 分类图标底色；null 表示跟随主题 primary（Monet 下始终为 primary）
 * @param expanded 是否处于展开状态
 * @param onToggle 点击标题行时的回调（切换展开状态）
 * @param content 展开后展示的设置项内容
 */
@Composable
private fun ExpandableSettingsCard(
    title: String,
    icon: ImageVector,
    iconSize: Dp = 26.dp,
    containerColor: Color? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    // 箭头旋转动画：展开时旋转 90°（指向下），收起时回到 0°（指向右）
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "expandArrowRotation"
    )
    // 卡片背景不透明度动画：收起时 0.45，展开时 1（颜色 token 不变，仅不透明度变化）
    val cardAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.45f,
        animationSpec = tween(durationMillis = 200),
        label = "expandCardAlpha"
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = cardAlpha)
        )
    ) {
        // 标题行：布局样式对齐 Miuix Preference（headline1 字号 + Medium 字重 + 16dp 内边距）
        // 点击时触发触觉反馈（LongPress 震动）并切换展开状态
        val haptic = LocalHapticFeedback.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标（通用组件「有底图标」）
            ContainedIcon(icon = icon, containerColor = containerColor, iconSize = iconSize)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            // 展开/收起箭头图标，与 Preference 的箭头样式保持一致（10x16dp）
            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = if (expanded) stringResource(R.string.settings_collapse)
                                    else stringResource(R.string.settings_expand),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier = Modifier
                    .size(width = 10.dp, height = 16.dp)
                    .rotate(arrowRotation)
            )
        }
        // 展开内容：与标题行之间用分隔线隔开，动画复用 AnimatedVisibility 默认展开/收起动画
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                content()
            }
        }
    }
}

/**
 * 更新日志 Markdown 渲染组件：用 Markwon 把 GitHub Release 的 Markdown 源文本
 * 解析为富文本（标题/列表/加粗/行内代码/链接等）显示在 TextView 上。
 *
 * 与法律文档页（MarkdownDocumentContent）共用同一套 Markwon 解析方案，差异点：
 * - 字号更小（跟随设置页 footnote1 字号）、正文用摘要色，视觉上融入 Sheet 卡片；
 * - 高度与滚动由调用方控制（本组件只撑开内容完整高度），便于限高内滚动。
 *
 * @param changelog Markdown 源文本（GitHub Release body）。
 * @param modifier 应用到 AndroidView 容器上的修饰符（调用方传入限高 + 滚动）。
 */
@Composable
private fun ChangelogMarkdownText(changelog: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Markwon 实例按 Context 缓存：解析器构建有开销，重组时不重复创建
    val markwon = remember(context) { Markwon.builder(context).build() }
    // 正文/链接颜色随主题切换：正文用摘要色弱化层级，链接用主题色提示可点击
    val textColor = MiuixTheme.colorScheme.onSurfaceVariantSummary.toArgb()
    val linkColor = MiuixTheme.colorScheme.primary.toArgb()
    // 渲染基准字号：跟随 footnote1 的 sp 值（Markwon 的标题按此基准相对缩放），
    // 取不到有效 sp 值时退回 13sp，避免 TextView 字号异常
    val footnoteFontSize = MiuixTheme.textStyles.footnote1.fontSize
    val baseTextSize = if (footnoteFontSize.type == TextUnitType.Sp && footnoteFontSize.value > 0f) {
        footnoteFontSize.value
    } else {
        13f
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            TextView(viewContext).apply {
                setTextColor(textColor)
                setLinkTextColor(linkColor)
                // 透明背景：融入 Sheet 卡片底色
                setBackgroundColor(AndroidColor.TRANSPARENT)
                textSize = baseTextSize
                // 让 Markdown 里的链接可以点击打开
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            // 主题切换 / 字号变化时同步 View 属性，再重新应用 Markdown
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            textView.textSize = baseTextSize
            markwon.setMarkdown(textView, changelog)
        }
    )
}
