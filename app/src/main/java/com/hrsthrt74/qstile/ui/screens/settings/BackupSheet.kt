package com.hrsthrt74.qstile.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.SheetState
import com.hrsthrt74.qstile.ui.sheetBottomPadding
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 备份结果 Sheet：显示 JSON 预览，支持「保存」「另存为」「复制」三种操作。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `BackupSheet` 拆分而来，行为保持不变：
 * JSON 正文可滚动并延伸到标题栏下方（`contentExtendsUnderHeader`），
 * 「保存」写应用内部存储、「复制」写剪贴板、「另存为」交给页面级的系统文件选择器。
 *
 * @param state Sheet 显示状态（由页面 `rememberSheetState()` 创建）
 * @param viewModel 设置页 ViewModel（JSON 正文与保存动作都在它上面）
 * @param scope 页面级协程作用域（`saveBackup` 是 suspend；本组件内容会随 Sheet 关闭离开组合，
 *        用组件内作用域会把保存协程取消）
 * @param onSaveAs 「另存为」回调：参数是默认文件名，真正的 `ActivityResultContracts.CreateDocument`
 *        launcher 必须留在页面级——本 Sheet 在点击后会立刻 dismiss，结果回来时 content 已离开组合。
 */
@Composable
fun BackupSheet(
    state: SheetState,
    viewModel: SettingsViewModel,
    scope: CoroutineScope,
    onSaveAs: (fileName: String) -> Unit,
) {
    val context = LocalContext.current
    // 文案在 Composable 上下文预解析，避免在 onClick lambda 里读资源（lint: 非配置感知）
    val savedLocalToast = stringResource(R.string.settings_saved_local_toast)
    val saveFailedToast = stringResource(R.string.settings_save_failed_toast)
    val copiedToast = stringResource(R.string.settings_copied_toast)

    AppBottomSheet(
        state = state,
        title = stringResource(R.string.settings_sheet_backup_title),
        // JSON 正文可滚动：让它滑到标题栏下方（「延伸到顶部」），而不是被标题栏挤在下面
        contentExtendsUnderHeader = true,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = sheetBottomPadding())
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
                    // 顶部让位放在 verticalScroll **内部**：滚动视口从面板顶端开始，
                    // JSON 首行仍从标题栏下方开始，滚动时正文会滑到标题栏下方被渐进模糊带盖住。
                    .padding(top = innerPadding.calculateTopPadding())
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 「保存」按钮：将配置保存到 app 内部存储
            Button(
                onClick = {
                    scope.launch {
                        val fileName = viewModel.saveBackup(context)
                        if (fileName != null) {
                            Toast.makeText(context, savedLocalToast, Toast.LENGTH_SHORT).show()
                            state.dismiss()
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
                    onSaveAs(defaultName)
                    state.dismiss()
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
                    state.dismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.common_copy))
            }
        }
    }
}
