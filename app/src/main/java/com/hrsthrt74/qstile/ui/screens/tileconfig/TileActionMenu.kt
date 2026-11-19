package com.hrsthrt74.qstile.ui.screens.tileconfig

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.asymmetricDropdownPositionProvider
import com.hrsthrt74.qstile.ui.components.GlassTileOperationMenu
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

/**
 * 磁贴操作菜单：移动到顶端 / 移动到底端 / 删除。
 *
 * 由 [`TileConfigScreen`] 的页内局部 `TileActionMenu` 拆分而来，行为完全保持：
 * - **玻璃风格**走 [GlassTileOperationMenu]（窗口坐标定位 + 根层渲染，不被网格裁剪）；
 * - **经典风格**走 [WindowListPopup] + [DropdownImpl]。
 *
 * 两者共用同一份动作定义与可用性判断，只是外壳不同。
 *
 * 注意：外层调用点不要用 `show` 参与「是否组合」的条件——那样关闭时弹窗会直接从组合树移除，
 * 退场动画来不及播放。调用方应只在「目标磁贴匹配」时组合本组件，显隐交给 [show]。
 *
 * @param show 是否展开（关闭动画期间仍需保持组合）
 * @param anchorBounds 锚点（磁贴圆形底）在窗口坐标系中的矩形，玻璃形态定位用
 * @param backdrop 玻璃形态的模糊采样背景
 * @param tileName 菜单顶部展示的磁贴名（不可点击的标题行）
 * @param isGlassStyle 是否玻璃风格
 * @param canMoveTop 「移动到顶端」是否可用（已经是第一个时置灰）
 * @param canMoveBottom 「移动到底端」是否可用（已经是最后一个时置灰）
 * @param onMoveTop / onMoveBottom / onDelete 三个操作回调（由调用方负责关闭菜单）
 * @param onDismissRequest 菜单被点击外部 / 返回键关闭时回调
 */
@Composable
fun TileActionMenu(
    show: Boolean,
    anchorBounds: Rect,
    backdrop: Backdrop?,
    tileName: String,
    isGlassStyle: Boolean,
    canMoveTop: Boolean,
    canMoveBottom: Boolean,
    onMoveTop: () -> Unit,
    onMoveBottom: () -> Unit,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (isGlassStyle) {
        // 玻璃风格：官方 GlassDropdownPopup（窗口坐标定位 + PopupLayout 根层渲染）
        GlassTileOperationMenu(
            show = show,
            anchorBounds = anchorBounds,
            backdrop = backdrop,
            tileName = tileName,
            moveTopLabel = stringResource(R.string.config_move_top),
            moveBottomLabel = stringResource(R.string.config_move_bottom),
            deleteLabel = stringResource(R.string.common_delete),
            moveTopEnabled = canMoveTop,
            moveBottomEnabled = canMoveBottom,
            onMoveTop = onMoveTop,
            onMoveBottom = onMoveBottom,
            onDelete = onDelete,
            onDismissRequest = onDismissRequest,
        )
    } else {
        // 经典风格：原版 WindowListPopup（吸附在锚点附近，按屏幕边缘对齐）
        val errorColors = DropdownDefaults.dropdownColors(
            contentColor = MiuixTheme.colorScheme.error,
            selectedContentColor = MiuixTheme.colorScheme.error
        )
        WindowListPopup(
            show = show,
            popupPositionProvider = asymmetricDropdownPositionProvider(
                verticalMargin = 8.dp,
                startMargin = 0.dp,
                endMargin = 8.dp
            ),
            onDismissRequest = onDismissRequest
        ) {
            ListPopupColumn {
                // 标题行：当前磁贴名（灰色小字，不可点击）
                Text(
                    text = tileName,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .padding(start = 20.dp)
                        .padding(bottom = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                DropdownImpl(
                    text = stringResource(R.string.config_move_top),
                    optionSize = 3,
                    isSelected = false,
                    index = 0,
                    enabled = canMoveTop,
                    onSelectedIndexChange = { onMoveTop() }
                )
                DropdownImpl(
                    text = stringResource(R.string.config_move_bottom),
                    optionSize = 3,
                    isSelected = false,
                    index = 1,
                    enabled = canMoveBottom,
                    onSelectedIndexChange = { onMoveBottom() }
                )
                DropdownImpl(
                    text = stringResource(R.string.common_delete),
                    optionSize = 3,
                    isSelected = false,
                    index = 2,
                    dropdownColors = errorColors,
                    onSelectedIndexChange = { onDelete() }
                )
            }
        }
    }
}
