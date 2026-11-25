package com.hrsthrt74.qstile.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.AppTextField
import com.hrsthrt74.qstile.ui.components.SheetState
import com.hrsthrt74.qstile.ui.sheetBottomPadding
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text

/**
 * 导入备份 Sheet：支持「本地备份列表」「从文件导入」与「粘贴 JSON」三种恢复方式。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `ImportBackupSheet` 拆分而来，行为保持不变：
 * 关闭动画结束后清空粘贴内容；恢复结果非 `InvalidFormat` 才关闭（格式错误时保持打开，方便就地修正）。
 *
 * @param state Sheet 显示状态（由页面 `rememberSheetState()` 创建）
 * @param viewModel 设置页 ViewModel（粘贴内容与恢复动作都在它上面）
 * @param scope 页面级协程作用域（`restoreFromJson` 是 suspend；本 Sheet 关闭后内容离开组合，
 *        用组件内作用域会把恢复协程取消）
 * @param onImportFile 「从文件导入」回调（页面级 OpenDocument launcher + 读取文件）
 * @param onShowBackupList 「从本地备份恢复」回调：页面负责 `dismiss() → loadBackupList() → 打开备份列表`
 *        这三步顺序（避免多层 Sheet 堆叠）
 * @param onRestoreResult 统一的恢复结果提示（页面级 showRestoreResult，三条恢复入口共用一套文案映射）
 */
@Composable
fun ImportBackupSheet(
    state: SheetState,
    viewModel: SettingsViewModel,
    scope: CoroutineScope,
    onImportFile: () -> Unit,
    onShowBackupList: () -> Unit,
    onRestoreResult: (SettingsViewModel.RestoreResult) -> Unit,
) {
    val context = LocalContext.current

    AppBottomSheet(
        state = state,
        title = stringResource(R.string.settings_restore_title),
        // 关闭动画完成后清空输入的 JSON，避免下次打开残留
        onDismissed = { viewModel.clearImportBackupJson() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = sheetBottomPadding())
        ) {
            // 「从本地备份恢复」按钮：打开备份列表选择已有备份
            Button(
                onClick = {
                    // 关闭当前 Sheet 后打开备份列表 Sheet，避免多层 Sheet 堆叠
                    onShowBackupList()
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
                    state.dismiss()
                    onImportFile()
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
                        onRestoreResult(result)
                        // 恢复成功（含「层数据已恢复、系统布局没写进去」）都关闭 Sheet；
                        // 只有格式错误保持打开，方便用户就地修正粘贴内容
                        if (result != SettingsViewModel.RestoreResult.InvalidFormat) {
                            state.dismiss()
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
