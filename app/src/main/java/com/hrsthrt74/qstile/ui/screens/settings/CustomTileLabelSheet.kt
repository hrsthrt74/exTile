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
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text

/**
 * 自定义磁贴名输入 Sheet。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `CustomTileLabelSheet` 拆分而来，行为保持不变：
 * 关闭动画结束后清空临时输入；保存时去掉首尾空白，空内容视为未设置（显示时回退到预设文案）。
 *
 * @param state Sheet 显示状态（由页面 `rememberSheetState()` 创建）
 * @param viewModel 设置页 ViewModel（临时输入与保存都走它）
 */
@Composable
fun CustomTileLabelSheet(
    state: SheetState,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current

    AppBottomSheet(
        state = state,
        title = stringResource(R.string.settings_sheet_custom_label_title),
        // 关闭动画完成后清空临时输入，避免下次打开残留
        onDismissed = { viewModel.clearCustomTileLabelInput() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = sheetBottomPadding())
        ) {
            AppTextField(
                value = viewModel.customTileLabelInput,
                onValueChange = { viewModel.customTileLabelInput = it },
                label = stringResource(R.string.settings_custom_label_hint),
                useLabelAsPlaceholder = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // 去掉首尾空白后保存；空内容则视为未设置（显示时会回退到「更多磁贴」）
                    val name = viewModel.customTileLabelInput.trim()
                    viewModel.saveTileLabelCustom(context, name)
                    state.dismiss()
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.common_save))
            }
        }
    }
}
