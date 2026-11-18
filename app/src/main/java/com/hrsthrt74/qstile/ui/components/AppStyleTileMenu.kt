package com.hrsthrt74.qstile.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

/**
 * 磁贴操作菜单的玻璃形态。锚点是磁贴图标外的圆形 Box，需把它上报的窗口坐标 [anchorBounds] 传入。
 * 菜单内容沿用官方 [GlassDropdownPopup] + [GlassPopupItem]：标题 + 分隔线 + 三个操作项，
 * 「删除」用错误色。弹窗提升到 PopupLayout 的根节点，避免被 Pager 页面裁剪。
 *
 * @param show 是否展开
 * @param anchorBounds 锚点在窗口坐标系的矩形（由触发 Box 的 onGloballyPositioned 上报）
 * @param backdrop 玻璃采样背景
 * @param tileName 菜单标题（磁贴显示名）
 * @param moveTopLabel / moveBottomLabel / deleteLabel 三个操作项文案（由调用方传入以支持多语言）
 * @param moveTopEnabled / moveBottomEnabled 移动项可用性
 * @param onMoveTop / onMoveBottom / onDelete 三个操作回调
 * @param onDismissRequest 关闭回调
 */
@Composable
fun GlassTileOperationMenu(
    show: Boolean,
    anchorBounds: Rect,
    backdrop: Backdrop?,
    tileName: String,
    moveTopLabel: String,
    moveBottomLabel: String,
    deleteLabel: String,
    moveTopEnabled: Boolean,
    moveBottomEnabled: Boolean,
    onMoveTop: () -> Unit,
    onMoveBottom: () -> Unit,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val mounted = remember { mutableStateOf(false) }
    if (show) mounted.value = true
    // 关闭后等退场动画播完再卸载宿主：PopupLayout 宿主在 visible=true 时会渲染全屏
    // pointerInput 吞掉所有触摸，mounted 常驻会导致菜单关闭后整个界面无法点击（卡死）。
    // 复用 AppStyleSpinner 的同一常量（同包的 private const）。
    LaunchedEffect(show) {
        if (!show && mounted.value) {
            delay(POPUP_EXIT_UNMOUNT_DELAY_MS)
            mounted.value = false
        }
    }
    if (!mounted.value) return

    val errorColor = MiuixTheme.colorScheme.error
    PopupLayout(
        visible = mounted,
        enableWindowDim = false,
        enableBackHandler = false,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        renderInRootScaffold = true,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GlassDropdownPopup(
                show = show,
                onDismissRequest = onDismissRequest,
                anchorBounds = anchorBounds,
                backdrop = backdrop,
            ) {
                // 磁贴标题（不可点击）
                Text(
                    text = tileName,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 16.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                GlassPopupItem(
                    text = moveTopLabel,
                    enabled = moveTopEnabled,
                    onClick = onMoveTop,
                )
                GlassPopupItem(
                    text = moveBottomLabel,
                    enabled = moveBottomEnabled,
                    onClick = onMoveBottom,
                )
                GlassPopupItem(
                    text = deleteLabel,
                    contentColor = errorColor,
                    selectedContentColor = errorColor,
                    onClick = onDelete,
                )
            }
        }
    }
}
