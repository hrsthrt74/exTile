package com.hrsthrt74.qstile.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.SheetState
import com.hrsthrt74.qstile.ui.sheetBottomPadding
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 系统磁贴查看 Sheet：只读展示系统 `sysui_qs_tiles` 的当前内容。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `SystemTilesSheet` 拆分而来，行为保持不变：
 * 空数据时显示 `common_no_data`，底部避让导航栏的高度与原来一致。
 *
 * @param state Sheet 显示状态（由页面 `rememberSheetState()` 创建）
 * @param viewModel 设置页 ViewModel（与页面共享同一实例；内容读取仍留在 content lambda 内，
 *        不提前到函数体顶层，避免改变失效范围）
 */
@Composable
fun SystemTilesSheet(
    state: SheetState,
    viewModel: SettingsViewModel,
) {
    AppBottomSheet(
        state = state,
        title = stringResource(R.string.settings_sheet_sysui_title),
    ) {
        Text(
            text = viewModel.currentSysuiTiles.ifEmpty { stringResource(R.string.common_no_data) },
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.padding(bottom = sheetBottomPadding())
        )
    }
}
