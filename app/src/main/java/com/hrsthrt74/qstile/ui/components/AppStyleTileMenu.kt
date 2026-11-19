package com.hrsthrt74.qstile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

// 标题行图标暂时停用（见 Row 里的说明）。恢复时把下面两个常量与那段 Icon 一起取消注释：
// 图标 20dp（比 GlassPopupItem 的 24dp 略小，与 footnote1 的文字比例更协调）、与文字间距 6dp。
// private val MenuTitleIconSize = 20.dp
// private val MenuTitleIconGap = 6.dp

/**
 * 磁贴操作菜单的玻璃形态。
 *
 * 动画直接用官方 [GlassTransformPopup]，它也是 `GlassIconDropdownMenu`（玻璃图标下拉菜单）的底层实现：
 * 从锚点矩形（[GlassPopupAnchor.containerBounds]）形变到面板，同时把 [anchorContent] 作为「收缩端点」
 * 的副本参与形变——副本随进度放大并淡出、面板内容随之淡入。锚点在动画期间由
 * `GlassPopupAnchor.contentHidden` 隐藏（瞬时），观感是「磁贴本身变成菜单」，
 * 而不是「磁贴先消失、再另开一个菜单」。
 *
 * 为什么不用 `GlassDropdownPopup`：它的入场走 `arcFrame`，起始矩形是「最终宽度的 69% × 该宽度的
 * 0.2 倍」的一条扁带，内容按 0.69 缩放后被 clip 裁掉，只露得出内容最顶上一行——于是会先看到一个
 * 孤立的小图标（正是标题行那个）、面板再长成菜单。`GlassTransformPopup` 的起点就是锚点本身，
 * 没有这个中间态。
 *
 * 宿主要求：必须放在**全屏的 BoxScope** 里（本组件已自带 `Box(fillMaxSize)` 包裹），
 * 因为 `GlassPopupSurface` 内含全屏的「点击外部关闭」层，若被限制在磁贴格子（68dp）里会失效。
 * 调用方（`TileConfigGrid`）因此把菜单层提到页面的全屏 Box 中，而不是塞进磁贴内部。
 *
 * @param show 是否展开（关闭动画期间仍需保持组合）
 * @param anchor 玻璃菜单锚点。调用方需用 `Modifier.glassPopupAnchor` 挂在磁贴格子上
 *   （上报 containerBounds，并在展开期间隐藏磁贴）、用 `Modifier.glassPopupAnchorContent`
 *   挂在圆形底上（上报形变副本的落点）
 * @param backdrop 玻璃采样背景（页面级 backdrop）
 * @param tileName 菜单顶部展示的磁贴名（不可点击的标题行）
 * @param anchorContent 形变起点的副本：应画出与磁贴一致的「圆形底 + 图标」，
 *   尺寸由 `glassPopupAnchorContent` 上报的区域决定
 * @param moveTopLabel / moveBottomLabel / deleteLabel 三个操作项文案
 * @param moveTopEnabled / moveBottomEnabled 移动项可用性
 * @param onMoveTop / onMoveBottom / onDelete 三个操作回调（由调用方负责关闭菜单）
 * @param onDismissRequest 点击外部 / 返回键请求关闭
 */
@Composable
fun GlassTileOperationMenu(
    show: Boolean,
    anchor: GlassPopupAnchor,
    backdrop: Backdrop?,
    tileName: String,
    anchorContent: @Composable () -> Unit,
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
    // 全屏宿主：dismiss 层与窗口坐标定位都要求铺满，不能受磁贴格子约束
    Box(modifier = Modifier.fillMaxSize()) {
        GlassTransformPopup(
            show = show,
            onDismissRequest = onDismissRequest,
            anchor = anchor,
            backdrop = backdrop,
            anchorContent = anchorContent,
            cornerRadius = GlassPopupDefaults.CornerRadius,
        ) {
            // 标题行：磁贴图标 + 磁贴名（灰色小字，不可点击）。
            // 行高与垂直留白对齐 GlassPopupItem（最小高度 44dp、内容垂直居中），
            // 保证「信息行 → 分割线 → 选项」的行高节奏一致。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = GlassPopupDefaults.ItemMinHeight)
                    // 左边距与选项文字对齐（ItemPressInset 7dp + 内容内边距 13dp = 20dp）
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 磁贴图标：暂时不显示（位图图标在菜单的形变副本 / 离屏层里仍有偶发的尺寸异常，
                // 先只保留磁贴名）。恢复时把这段与文件顶部两个常量一起取消注释。
                // Icon(
                //     painter = tileIcon,
                //     contentDescription = null,
                //     tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                //     modifier = Modifier.size(MenuTitleIconSize),
                // )
                Text(
                    text = tileName,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // fill = false：文本短时只占自身宽度，长时收缩并省略，不会把其它内容挤走
                    modifier = Modifier
                        // .padding(start = MenuTitleIconGap)   // 图标恢复时一并取消
                        .weight(1f, fill = false),
                )
            }
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
                // 删除不可逆，文案用错误色（与经典风格的 DropdownImpl 一致）
                contentColor = MiuixTheme.colorScheme.error,
                selectedContentColor = MiuixTheme.colorScheme.error,
                onClick = onDelete,
            )
        }
    }
}
