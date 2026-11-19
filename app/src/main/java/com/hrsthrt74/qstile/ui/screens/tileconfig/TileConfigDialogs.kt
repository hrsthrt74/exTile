package com.hrsthrt74.qstile.ui.screens.tileconfig

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.AppDialog
import com.hrsthrt74.qstile.ui.components.DialogState

/**
 * 磁贴编辑页的两个确认对话框：清除当前配置 / 从系统导入磁贴。
 *
 * 由原 `TileConfigScreen` 的页内局部函数 `ConfirmationDialogs` 拆分而来，行为保持不变。
 * 对话框只负责「确认」这一步的 UI，真正的数据操作由调用方通过回调注入，
 * 因此本组件不依赖 ViewModel，可独立复用与测试。
 *
 * 说明：原「恢复默认设置」入口已下架，对应的确认对话框一并移除（见 git 历史）。
 *
 * @param clearState 「清除配置」对话框状态
 * @param importState 「从系统导入磁贴」对话框状态
 * @param onClearConfirm 确认清除配置（调用方负责清空并持久化）
 * @param onImportConfirm 确认从系统导入（调用方负责读取系统列表并给出 Toast 提示）
 */
@Composable
fun TileConfigDialogs(
    clearState: DialogState,
    importState: DialogState,
    onClearConfirm: () -> Unit,
    onImportConfirm: () -> Unit,
) {
    AppDialog(
        state = clearState,
        title = stringResource(R.string.config_menu_clear),
        summary = stringResource(R.string.config_clear_summary),
        confirmText = stringResource(R.string.config_clear_confirm),
        destructive = true,
        onConfirm = onClearConfirm
    )

    // 从系统导入磁贴确认对话框：与设置页共用同一组文案资源
    AppDialog(
        state = importState,
        title = stringResource(R.string.settings_import_system_title),
        summary = stringResource(R.string.settings_import_system_dialog_summary),
        confirmText = stringResource(R.string.common_confirm),
        onConfirm = onImportConfirm
    )
}
