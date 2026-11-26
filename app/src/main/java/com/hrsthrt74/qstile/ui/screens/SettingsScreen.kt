package com.hrsthrt74.qstile.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hrsthrt74.qstile.BuildConfig
import com.hrsthrt74.qstile.DebugToolsActivity
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.AppTopBar
import com.hrsthrt74.qstile.ui.LocalIsWideScreen
import com.hrsthrt74.qstile.ui.TopBarIconButton
import com.hrsthrt74.qstile.ui.awaitStartupFrames
import com.hrsthrt74.qstile.ui.components.DonateSheet
import com.hrsthrt74.qstile.ui.components.rememberDialogState
import com.hrsthrt74.qstile.ui.components.rememberSheetState
import com.hrsthrt74.qstile.ui.contentBottomPadding
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.screens.settings.AboutCategoryCard
import com.hrsthrt74.qstile.ui.screens.settings.AdvancedCategoryCard
import com.hrsthrt74.qstile.ui.screens.settings.AppPickerSheet
import com.hrsthrt74.qstile.ui.screens.settings.AppearanceCategoryCard
import com.hrsthrt74.qstile.ui.screens.settings.AuthorSheet
import com.hrsthrt74.qstile.ui.screens.settings.BackupListSheet
import com.hrsthrt74.qstile.ui.screens.settings.BackupSheet
import com.hrsthrt74.qstile.ui.screens.settings.CustomTileLabelSheet
import com.hrsthrt74.qstile.ui.screens.settings.DataCategoryCard
import com.hrsthrt74.qstile.ui.screens.settings.DeleteBackupDialog
import com.hrsthrt74.qstile.ui.screens.settings.GeneralCategoryCard
import com.hrsthrt74.qstile.ui.screens.settings.ImportBackupSheet
import com.hrsthrt74.qstile.ui.screens.settings.ImportSystemTilesDialog
import com.hrsthrt74.qstile.ui.screens.settings.SystemTilesSheet
import com.hrsthrt74.qstile.ui.screens.settings.UpdateSheet
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Maintenance
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 设置页（主装配层）。
 *
 * 本文件只保留「页面装配」职责：创建页面级状态（ViewModel、滚动行为、模糊采样源、
 * 11 个 Sheet / Dialog 的显示状态）、预解析 Toast 文案、持有两个系统文件选择器 launcher、
 * 统一恢复结果提示，然后把 UI 分派给 `settings` 子包里的各个独立组件。拆分前这些组件
 * 都是本文件里的页内局部函数，单文件曾达 1995 行。
 *
 * | 组件 | 文件 | 职责 |
 * |------|------|------|
 * | [GeneralCategoryCard] | `settings/GeneralCategoryCard.kt` | 通用：磁贴联动 / 图标 / 磁贴名 / 长按行为 |
 * | [AppearanceCategoryCard] | `settings/AppearanceCategoryCard.kt` | 外观：配色模式 / 动态取色 / 模糊 / 应用风格 |
 * | [DataCategoryCard] | `settings/DataCategoryCard.kt` | 数据：备份 / 恢复 / 查看系统磁贴 |
 * | [AdvancedCategoryCard] | `settings/AdvancedCategoryCard.kt` | 高级：创造模式 / 从系统导入磁贴 / Clarity / 重跑引导 / 调试工具 |
 * | [AboutCategoryCard] | `settings/AboutCategoryCard.kt` | 关于：作者 / 版本 / GitHub / 社区 / 文档 |
 * | [UpdateSheet] | `settings/UpdateSheet.kt` | 检查更新：自动检查开关 + 手动检查 + 内置更新日志 |
 * | [BackupSheet] / [ImportBackupSheet] / [BackupListSheet] / [DeleteBackupDialog] | `settings/` 同名文件 | 备份三种保存、恢复三种入口、本地备份列表与删除确认 |
 * | [AppPickerSheet] / [CustomTileLabelSheet] / [SystemTilesSheet] / [AuthorSheet] / [ImportSystemTilesDialog] | `settings/` 同名文件 | 各分类下的独立浮层 |
 *
 * 两个 **必须留在本文件** 的 ActivityResult launcher：
 * - `saveAsLauncher`（CreateDocument）：备份 Sheet 点「另存为」后会立刻关闭，结果回来时
 *   Sheet 内容已离开组合，launcher 挂在 Sheet 内会丢结果。
 * - `openFileLauncher`（OpenDocument）：回调要用页面 `scope` 与统一的恢复结果提示。
 *
 * 布局结构：
 * ```
 * Scaffold(topBar = AppTopBar)
 *   └─ Box(顶栏 blur backdrop 录制层)
 *        └─ Column(垂直居中)
 *             └─ LazyColumn(五张分类卡片)
 * 11 个 Sheet / Dialog（浮层，组合在页面末尾）
 * ```
 */
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

    // 顶部栏模糊：创建 backdrop 捕获滚动内容，模糊开关关闭或 RuntimeShader 不支持时退回纯色。
    // 同时透传给分类卡片里的下拉（玻璃弹窗采样）。
    val backdrop = rememberBlurBackdrop(enabled = themeSettings.enableBlur)

    // 分类卡片里的两个入口 Sheet（自定义应用选择器 / 自定义磁贴名）的显示状态
    // （其余浮层状态在下面按用途分组声明；全部由 AppBottomSheet / AppDialog 统一管理）
    val appPickerSheetState = rememberSheetState()
    val customTileLabelSheetState = rememberSheetState()

    // 系统磁贴查看 / 备份结果 / 导入备份 三个 Sheet 的显示状态
    val systemTilesSheetState = rememberSheetState()
    val backupSheetState = rememberSheetState()
    val importBackupSheetState = rememberSheetState()
    // 支持作者（微信收款码）Sheet 的显示状态
    val donateSheetState = rememberSheetState()
    // 作者信息（hrsthrt74 各社交平台入口）Sheet 的显示状态
    val authorSheetState = rememberSheetState()
    // 检查更新 Sheet（点击「版本」条目弹出：自动检查开关 + 检查状态 + 手动检查）的显示状态
    val updateSheetState = rememberSheetState()
    // 从系统导入磁贴的确认对话框显示状态
    val importDialogState = rememberDialogState()
    // 备份列表相关状态
    val backupListSheetState = rememberSheetState()
    val deleteBackupDialogState = rememberDialogState()

    // Toast 文案在 Composable 上下文预解析,避免 launcher 回调/协程/onClick 内
    // context.getString 触发「非配置感知」lint 错误(LocalContextGetResourceValueCall)。
    // 注意:必须声明在下方 launchers 之前(Kotlin 先声明后使用)。
    // 各分类卡片 / 浮层用到的文案已随组件迁到各自文件里就近解析，这里只留页面自己用到的 8 条。
    val savedToast = stringResource(R.string.settings_saved_toast)
    val saveFailedToast = stringResource(R.string.settings_save_failed_toast)
    val readErrorToast = stringResource(R.string.settings_read_error_toast)
    // 恢复结果提示（三条恢复入口共用一套映射）
    val restoredToast = stringResource(R.string.settings_restored_toast)
    val invalidFormatToast = stringResource(R.string.settings_invalid_format_toast)
    val loadFailedToast = stringResource(R.string.settings_load_failed_toast)
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
     *
     * 本地备份 / 粘贴 JSON 两条入口在各自的 Sheet 里，通过 `onRestoreResult` 回调调到这里；
     * 文件导入入口由下面的 `openFileLauncher` 直接调用。
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

    // 首次进入页面：一次性读回全部设置项后再放行渲染（未就绪就空白，与主题/OOBE 门控策略一致）
    // 先让出启动首帧后的几帧：本页在冷启动首帧就会随 Pager 一起被组合，若此处立即开跑，
    // 加载完成后的「整页首次组合」（五张分类卡片 + 一堆 Preference）会正好压住玻璃底栏指示器
    // 所等的那次重组，表现为底栏先出现、高亮块过半秒才冒出来且期间点击无响应。
    // 详见 awaitStartupFrames 注释。
    LaunchedEffect(Unit) {
        awaitStartupFrames()
        viewModel.loadSettings(context)
        viewModel.loadSystemTiles(context)
    }

    if (!viewModel.settingsLoaded) return

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
                // 顶栏调试入口仅 debug 变体显示：release 包不应暴露调试能力
                // （强制更新、能力开关模拟等）；debug 包里另外在「设置 → 高级」也加了入口。
                actions = {
                    if (BuildConfig.DEBUG) {
                        // 用 TopBarIconButton 跟随顶栏形态：玻璃态→GlassIconButton，原版→IconButton
                        TopBarIconButton(onClick = {
                            context.startActivity(Intent(context, DebugToolsActivity::class.java))
                        }) {
                            Icon(
                                MiuixIcons.Glass.Maintenance,
                                contentDescription = stringResource(R.string.debug_title)
                            )
                        }
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
                modifier = Modifier.fillMaxSize(),
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
                    item {
                        GeneralCategoryCard(
                            viewModel = viewModel,
                            backdrop = backdrop,
                            customTileLabelSheetState = customTileLabelSheetState,
                            appPickerSheetState = appPickerSheetState,
                        )
                    }

                    // ===== 外观 =====
                    item {
                        AppearanceCategoryCard(
                            viewModel = viewModel,
                            backdrop = backdrop,
                            scope = scope,
                        )
                    }

                    // ===== 数据 =====
                    item {
                        DataCategoryCard(
                            viewModel = viewModel,
                            scope = scope,
                            backupSheetState = backupSheetState,
                            importBackupSheetState = importBackupSheetState,
                            systemTilesSheetState = systemTilesSheetState,
                        )
                    }

                    // ===== 高级 =====
                    item {
                        AdvancedCategoryCard(
                            viewModel = viewModel,
                            scope = scope,
                            importDialogState = importDialogState,
                            backdrop = backdrop,
                        )
                    }

                    // ===== 关于 =====
                    item {
                        AboutCategoryCard(
                            viewModel = viewModel,
                            authorSheetState = authorSheetState,
                            updateSheetState = updateSheetState,
                            donateSheetState = donateSheetState,
                        )
                    }

                    // 同上面那个 Spacer
                    item { Spacer(modifier = Modifier.height(0.dp)) }
                }
            }
        }
    }

    // ---- 系统磁贴查看 Sheet ----
    SystemTilesSheet(state = systemTilesSheetState, viewModel = viewModel)

    // ---- 备份结果 Sheet ----
    BackupSheet(
        state = backupSheetState,
        viewModel = viewModel,
        scope = scope,
        // 「另存为」的系统文件选择器必须留在页面级：Sheet 在点击后立刻关闭，
        // 结果回来时 Sheet 内容已离开组合，挂在 Sheet 内的 launcher 会丢结果
        onSaveAs = { fileName -> saveAsLauncher.launch(fileName) },
    )

    // ---- 支持作者 Sheet（收款码界面与主页打赏卡片共用 ui/components/DonateSheet） ----
    DonateSheet(state = donateSheetState)

    // ---- 作者信息 Sheet ----
    AuthorSheet(state = authorSheetState)

    // ---- 检查更新 Sheet ----
    UpdateSheet(state = updateSheetState, scope = scope)

    // ---- 导入备份 Sheet ----
    ImportBackupSheet(
        state = importBackupSheetState,
        viewModel = viewModel,
        scope = scope,
        // 「从文件导入」的 OpenDocument launcher 留在页面级：它的回调要用 scope + 恢复结果提示
        onImportFile = { openFileLauncher.launch(arrayOf("application/json", "text/plain")) },
        // 跨 Sheet 跳转的三步顺序由页面保证（避免多层 Sheet 堆叠）
        onShowBackupList = {
            importBackupSheetState.dismiss()
            viewModel.loadBackupList(context)
            backupListSheetState.show()
        },
        onRestoreResult = { showRestoreResult(it) },
    )

    // ---- 自定义磁贴名 Sheet ----
    CustomTileLabelSheet(state = customTileLabelSheetState, viewModel = viewModel)

    // ---- 本地备份列表 Sheet ----
    BackupListSheet(
        state = backupListSheetState,
        viewModel = viewModel,
        scope = scope,
        deleteBackupDialogState = deleteBackupDialogState,
        onRestoreResult = { showRestoreResult(it) },
    )

    // ---- 系统导入确认 Dialog ----
    ImportSystemTilesDialog(
        state = importDialogState,
        viewModel = viewModel,
        scope = scope,
    )

    // ---- 删除备份确认 Dialog ----
    DeleteBackupDialog(
        state = deleteBackupDialogState,
        viewModel = viewModel,
        scope = scope,
    )

    // ---- 自定义应用选择器 Sheet ----
    AppPickerSheet(state = appPickerSheetState, viewModel = viewModel)
}
