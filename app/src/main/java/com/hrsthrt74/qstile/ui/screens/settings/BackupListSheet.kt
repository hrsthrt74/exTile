package com.hrsthrt74.qstile.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.BackupRepository
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.AppCard
import com.hrsthrt74.qstile.ui.components.DialogState
import com.hrsthrt74.qstile.ui.components.SheetState
import com.hrsthrt74.qstile.ui.sheetBottomPadding
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 本地备份列表 Sheet：展示已保存的备份，支持恢复和删除。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `BackupListSheet` 拆分而来，行为保持不变：
 * 列表条目滑到标题栏下方（`contentExtendsUnderHeader` + `contentPadding.top`），
 * 点击条目恢复（读文件失败时保持列表打开，方便换一个再试），右侧删除按钮弹确认框。
 *
 * @param state Sheet 显示状态（由页面 `rememberSheetState()` 创建）
 * @param viewModel 设置页 ViewModel（备份列表与恢复动作都在它上面）
 * @param scope 页面级协程作用域（`restoreFromBackup` 是 suspend；本 Sheet 关闭后内容离开组合，
 *        用组件内作用域会把恢复协程取消）
 * @param deleteBackupDialogState 删除确认 Dialog 的状态：点删除按钮时写 deleteTargetBackup 并 show()
 * @param onRestoreResult 统一的恢复结果提示（页面级 showRestoreResult，三条恢复入口共用一套文案映射）
 */
@Composable
fun BackupListSheet(
    state: SheetState,
    viewModel: SettingsViewModel,
    scope: CoroutineScope,
    deleteBackupDialogState: DialogState,
    onRestoreResult: (SettingsViewModel.RestoreResult) -> Unit,
) {
    val context = LocalContext.current

    AppBottomSheet(
        state = state,
        title = stringResource(R.string.settings_sheet_backup_list_title),
        // 列表可滚动：条目滑到标题栏下方（「延伸到顶部」）
        contentExtendsUnderHeader = true,
    ) { innerPadding ->
        // 头部让出的高度交给内容自己消费（见 AppBottomSheet 的 contentExtendsUnderHeader）：
        // 列表的 contentPadding.top、以及空状态文案的 padding.top 都用它
        val headerHeight = innerPadding.calculateTopPadding()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = sheetBottomPadding())
        ) {
            if (viewModel.backupList.isEmpty()) {
                // 空状态提示
                Text(
                    text = stringResource(R.string.settings_no_backups),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = headerHeight + 32.dp, bottom = 32.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                // 备份列表：每条显示日期时间、磁贴数量，右侧有删除按钮
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    // 顶部让位交给 contentPadding：视口铺满到面板顶端，
                    // 条目滚动时会从标题栏下方滑过（被渐进模糊带盖住）
                    contentPadding = PaddingValues(
                        top = headerHeight,
                        bottom = sheetBottomPadding(),
                    )
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
                                                    onRestoreResult(result)
                                                    // 读文件失败时保持列表打开，方便换一个备份再试
                                                    if (result != SettingsViewModel.RestoreResult.LoadFailed) {
                                                        state.dismiss()
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
