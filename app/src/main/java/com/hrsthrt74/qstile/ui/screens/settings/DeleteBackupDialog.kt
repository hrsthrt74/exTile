package com.hrsthrt74.qstile.ui.screens.settings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.BackupRepository
import com.hrsthrt74.qstile.ui.components.AppDialog
import com.hrsthrt74.qstile.ui.components.DialogState
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 删除备份确认 Dialog（破坏性动作，确认按钮用 Destructive 配色）。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `DeleteBackupDialog` 拆分而来，行为保持不变：
 * summary 里带该备份的日期；确认后删除文件、成功弹 Toast，并清理 deleteTargetBackup；
 * 关闭（含取消）时也清理 deleteTargetBackup。
 *
 * @param state Dialog 显示状态（由页面 `rememberDialogState()` 创建）
 * @param viewModel 设置页 ViewModel（待删备份与删除动作都在它上面）
 * @param scope 页面级协程作用域。**不要**改用本组件内的 rememberCoroutineScope：
 *        `AppDialog` 在 onConfirm 返回后会立刻关闭，Dialog 内容随之离开组合，
 *        组件内作用域会把 deleteBackup 这个 suspend 调用当场取消（表现为「点了删除但文件还在」）。
 */
@Composable
fun DeleteBackupDialog(
    state: DialogState,
    viewModel: SettingsViewModel,
    scope: CoroutineScope,
) {
    val context = LocalContext.current
    // 文案在 Composable 上下文预解析，避免在 onConfirm lambda 里读资源（lint: 非配置感知）
    val deletedToast = stringResource(R.string.settings_deleted_toast)

    AppDialog(
        state = state,
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
