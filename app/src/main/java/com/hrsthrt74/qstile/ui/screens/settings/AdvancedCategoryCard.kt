package com.hrsthrt74.qstile.ui.screens.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.DebugToolsActivity
import com.hrsthrt74.qstile.ExTileApplication
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.ui.components.DialogState
import com.hrsthrt74.qstile.ui.components.ExpandableSettingsCard
import com.hrsthrt74.qstile.ui.components.PreferenceLeadingIcon
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.AiWriting
import top.yukonga.miuix.kmp.icon.glass.Maintenance
import top.yukonga.miuix.kmp.icon.glass.More
import top.yukonga.miuix.kmp.icon.glass.PasteEditPoint
import top.yukonga.miuix.kmp.icon.glass.SoundWave
import top.yukonga.miuix.kmp.icon.glass.Start
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * 高级分类卡片：创造模式 / 从系统导入磁贴 / 允许匿名统计 / 重新运行引导 / 调试工具。
 *
 * 前四项原先分散在「通用」「数据」「关于」三个分类里，统一收拢到独立的「高级」分类，
 * 使常用设置保持简洁。各项的行为与迁移前完全一致：
 * - 创造模式的开关状态仍由「写系统成功」驱动，写入期间禁用避免连续点击并发写 sysui_qs_tiles；
 * - 创造模式下「从系统导入」仍禁用，并引导去磁贴编辑页按层导入；
 * - 匿名统计开关仍同步 Clarity SDK（开启初始化、关闭标记停止，需重启才完全生效）；
 * - 重新运行引导仍只重置 OOBE 标志，不触碰配置数据。
 *
 * 调试工具入口是从设置页顶栏挪过来的「第二入口」：顶栏按钮只在 debug 变体显示，
 * 这里作为更靠里的常驻入口，release 包也能由此进入 DebugToolsActivity。
 *
 * @param viewModel 设置页 ViewModel（创造模式状态、分类展开状态等）
 * @param scope 页面级协程作用域（Clarity 同意状态与「重新运行引导」都要写 DataStore / SDK，
 *        且卡片会随分类收起离开组合，用组件内作用域会取消正在进行的写入）
 * @param importDialogState 从系统导入确认 Dialog 状态（浮层本体挂在页面级）
 */
@Composable
fun AdvancedCategoryCard(
    viewModel: SettingsViewModel,
    scope: CoroutineScope,
    importDialogState: DialogState,
) {
    val context = LocalContext.current

    // 创造模式开关的结果提示（开启 / 关闭都有系统写入，失败必须让用户知道）
    val creativeReadFailedToast = stringResource(R.string.settings_creative_read_failed)
    val creativeWriteFailedToast = stringResource(R.string.settings_creative_write_failed)
    val creativeExitFailedToast = stringResource(R.string.settings_creative_exit_failed)

    // MS Clarity 匿名统计同意状态（仍和 OOBE 的开关共用同一个 DataStore key）
    val clarityConsent by ConfigRepository.getClarityConsentFlow(context)
        .collectAsState(initial = false)

    ExpandableSettingsCard(
        title = stringResource(R.string.settings_cat_advanced),
        icon = MiuixIcons.Glass.Demibold.More,
        containerColor = Color(0xFF8C9DAF),
        expanded = viewModel.expandedCategory == SettingsViewModel.SettingsCategory.ADVANCED,
        onToggle = { viewModel.toggleCategory(SettingsViewModel.SettingsCategory.ADVANCED) }
    ) {
        // 第一组：与磁贴布局直接相关的两个入口
        SmallTitle(
            text = stringResource(R.string.settings_section_tiles),
            insideMargin = PaddingValues(start = 20.dp, top = 12.dp)
        )

        // ===== 创造模式：两套互相独立的层布局 =====
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

        // ===== 从系统导入磁贴：创造模式下系统里显示的是某一层，导进来只会污染普通配置 =====
        ArrowPreference(
            title = stringResource(R.string.settings_import_system_title),
            summary = if (viewModel.creativeModeEnabled) {
                stringResource(R.string.settings_import_system_summary_creative)
            } else {
                stringResource(R.string.settings_import_system_summary)
            },
            enabled = !viewModel.creativeModeEnabled,
            onClick = { importDialogState.show() },
            startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.PasteEditPoint) }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // 第二组：应用级开关与入口（隐私统计 / 重跑引导 / 调试工具）
        SmallTitle(
            text = stringResource(R.string.settings_section_app),
            insideMargin = PaddingValues(start = 20.dp, top = 12.dp)
        )

        // ===== 允许匿名统计 =====
        // SDK 不支持运行时停止，关闭后需重启应用才能完全停止数据收集
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

        // ===== 调试工具：从顶栏下沉到这里的常驻入口 =====
        // 顶栏那个入口由 SettingsScreen 用 BuildConfig.DEBUG 限定仅 debug 包显示，
        // 这里不设限，release 包也能进调试页排查问题
        ArrowPreference(
            title = stringResource(R.string.debug_title),
            onClick = {
                context.startActivity(Intent(context, DebugToolsActivity::class.java))
            },
            startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.Maintenance) }
        )

        // ===== 重新运行首次配置引导 =====
        // 仅重置 OOBE 标志（配置数据保留，可再次导入覆盖），壳层监听到标志变 false 后自动切回 OOBE 页面
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
