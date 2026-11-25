package com.hrsthrt74.qstile.ui.screens.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.DialogState
import com.hrsthrt74.qstile.ui.components.ExpandableSettingsCard
import com.hrsthrt74.qstile.ui.components.PreferenceLeadingIcon
import com.hrsthrt74.qstile.ui.components.SheetState
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.Remove
import top.yukonga.miuix.kmp.icon.glass.AllSdCardData
import top.yukonga.miuix.kmp.icon.glass.PasteEditPoint
import top.yukonga.miuix.kmp.icon.glass.PhoneIcon
import top.yukonga.miuix.kmp.preference.ArrowPreference

/**
 * 数据分类卡片：备份 / 恢复 / 系统导入（合并原两个板块）。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `DataCategoryCard` 拆分而来，行为保持不变：
 * 「备份」点击先刷新配置快照再生成 JSON（设置页是 Pager 里的常驻页，编辑页之后的改动不会自动同步），
 * 四个入口各自弹出对应的 Sheet / Dialog；创造模式下「从系统导入」置灰并换说明文案。
 *
 * @param viewModel 设置页 ViewModel（备份内容、创造模式状态、分类展开状态都在它上面）
 * @param scope 页面级协程作用域（`refreshBackupSource` 是 suspend；卡片会随分类收起离开组合，
 *        用组件内作用域会取消刷新协程）
 * @param backupSheetState 备份结果 Sheet 状态
 * @param importBackupSheetState 导入备份（恢复）Sheet 状态
 * @param systemTilesSheetState 查看系统磁贴 Sheet 状态
 * @param importDialogState 从系统导入确认 Dialog 状态
 */
@Composable
fun DataCategoryCard(
    viewModel: SettingsViewModel,
    scope: CoroutineScope,
    backupSheetState: SheetState,
    importBackupSheetState: SheetState,
    systemTilesSheetState: SheetState,
    importDialogState: DialogState,
) {
    val context = LocalContext.current

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
