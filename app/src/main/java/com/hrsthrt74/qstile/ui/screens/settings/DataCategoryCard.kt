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
import top.yukonga.miuix.kmp.icon.glass.PhoneIcon
import top.yukonga.miuix.kmp.preference.ArrowPreference

/**
 * 数据分类卡片：备份 / 恢复 / 查看系统磁贴配置。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `DataCategoryCard` 拆分而来，行为保持不变：
 * 「备份」点击先刷新配置快照再生成 JSON（设置页是 Pager 里的常驻页，编辑页之后的改动不会自动同步），
 * 三个入口各自弹出对应的 Sheet；「从系统导入磁贴」已迁到「高级」分类。
 *
 * @param viewModel 设置页 ViewModel（备份内容、分类展开状态都在它上面）
 * @param scope 页面级协程作用域（`refreshBackupSource` 是 suspend；卡片会随分类收起离开组合，
 *        用组件内作用域会取消刷新协程）
 * @param backupSheetState 备份结果 Sheet 状态
 * @param importBackupSheetState 导入备份（恢复）Sheet 状态
 * @param systemTilesSheetState 查看系统磁贴 Sheet 状态
 */
@Composable
fun DataCategoryCard(
    viewModel: SettingsViewModel,
    scope: CoroutineScope,
    backupSheetState: SheetState,
    importBackupSheetState: SheetState,
    systemTilesSheetState: SheetState,
) {
    val context = LocalContext.current

    ExpandableSettingsCard(
        title = stringResource(R.string.settings_cat_data),
        icon = MiuixIcons.Glass.AllSdCardData,
        containerColor = Color(0xFFFA382E),
        expanded = viewModel.expandedCategory == SettingsViewModel.SettingsCategory.DATA,
        onToggle = { viewModel.toggleCategory(SettingsViewModel.SettingsCategory.DATA) }
    ) {
        ArrowPreference(
            title = stringResource(R.string.settings_backup_title),
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
            onClick = {
                viewModel.clearImportBackupJson()
                importBackupSheetState.show()
            },
            startAction = { PreferenceLeadingIcon(MiuixIcons.Import) }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

//        SmallTitle(
//            text = stringResource(R.string.settings_section_tools),
//            insideMargin = PaddingValues(start = 20.dp, top = 12.dp)
//        )

        ArrowPreference(
            title = stringResource(R.string.settings_view_sysui_title),
            onClick = { systemTilesSheetState.show() },
            startAction = { PreferenceLeadingIcon(MiuixIcons.Glass.PhoneIcon) }
        )
    }
}
