package com.hrsthrt74.qstile.ui.screens.settings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.AppDialog
import com.hrsthrt74.qstile.ui.components.DialogState
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 「从系统导入磁贴」确认 Dialog。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `ImportSystemTilesDialog` 拆分而来，行为保持不变：
 * 确认后读取系统 `sysui_qs_tiles` 并写入普通配置，成功/失败各弹一条 Toast。
 * 创造模式下设置页的这个入口本身就是禁用状态（见 `DataCategoryCard`），所以这里不需要额外判断。
 *
 * @param state Dialog 显示状态（由页面 `rememberDialogState()` 创建）
 * @param viewModel 设置页 ViewModel（导入动作在它上面）
 * @param scope 页面级协程作用域。**不要**改用本组件内的 rememberCoroutineScope：
 *        `AppDialog` 在 onConfirm 返回后会立刻关闭，Dialog 内容随之离开组合，
 *        组件内作用域会把 importFromSystem 这个 suspend 调用当场取消。
 */
@Composable
fun ImportSystemTilesDialog(
    state: DialogState,
    viewModel: SettingsViewModel,
    scope: CoroutineScope,
) {
    val context = LocalContext.current
    // 文案在 Composable 上下文预解析，避免在 onConfirm lambda 里读资源（lint: 非配置感知）
    val importedToast = stringResource(R.string.settings_imported_toast)
    val readFailedToast = stringResource(R.string.settings_read_failed_toast)

    AppDialog(
        state = state,
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
