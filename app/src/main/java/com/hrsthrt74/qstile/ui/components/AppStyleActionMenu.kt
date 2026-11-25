package com.hrsthrt74.qstile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「动作菜单」的单个条目（点击即执行，不表示选中状态）。
 *
 * 与 miuix 的 [top.yukonga.miuix.kmp.basic.DropdownItem] 的区别：
 * - 图标用 [ImageVector] 直接表达，而不是 `@Composable (Modifier) -> Unit` 插槽。
 *   玻璃路径的 [GlassPopupItem] 只接收 `ImageVector`，两种形态因此可以共用同一份数据源，
 *   由各自的渲染层负责把 [icon] 画出来。
 * - 没有 `selected`：动作菜单不存在「当前项」，玻璃路径若传 `selected` 会被画上勾选标记。
 * - [destructive] 用于「清除配置」这类不可逆操作，渲染成错误色。
 *
 * @param text 主文案
 * @param icon 行首图标
 * @param summary 灰色副标题（可选，玻璃路径与经典路径都会渲染）
 * @param enabled 是否可点击
 * @param destructive 是否为破坏性操作（错误色文案与图标）
 * @param onClick 点击回调；菜单的关闭由宿主组件负责，这里只管业务动作
 */
@Stable
class ActionMenuItem(
    val text: String,
    val icon: ImageVector,
    val summary: String? = null,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * 动作菜单的玻璃形态：从锚点按钮形变展开的一组可点击条目，每条可带图标。
 *
 * 为什么不用官方的 `GlassIconDropdownMenu`：它把 [top.yukonga.miuix.kmp.basic.DropdownEntry]
 * 直接喂给 [GlassPopupItem] 时**只转发了 text / summary / enabled / selected**，
 * `DropdownItem.icon` 被丢弃（见 miuix-glass 的 GlassIconDropdownMenu 实现）。
 * 因此带图标的菜单必须自己展开 `GlassTransformPopup` 的内容，逐项传 `icon`。
 *
 * 动画直接复用官方 [GlassTransformPopup]（也是 `GlassIconDropdownMenu` 的底层实现）：
 * 从锚点矩形形变到面板，同时把 [anchorContent] 作为「收缩端点」的副本参与形变，
 * 观感是「按钮本身长成菜单」，而不是「按钮先消失、再另开一个面板」。
 *
 * 宿主要求：必须放在**全屏的 BoxScope** 里（本组件已自带 `Box(fillMaxSize)` 包裹），
 * 因为 `GlassPopupSurface` 内含全屏的「点击外部关闭」层，若被限制在按钮尺寸内会失效。
 * 调用方通常再用 `TopLayer` 把它提升到应用根部。
 *
 * @param show 是否展开（关闭动画期间仍需保持组合）
 * @param onDismissRequest 点击外部 / 返回键 / 选中任一项后请求关闭
 * @param anchor 玻璃菜单锚点。调用方需用 `Modifier.glassPopupAnchor` 挂在触发按钮上，
 *   用 `Modifier.glassPopupAnchorContent` 挂在按钮内容上（上报形变副本的落点）
 * @param anchorContent 形变起点的副本：应画出与触发按钮一致的图形
 * @param groups 条目分组；组与组之间自动插入分割线（miuix 菜单只在分组之间画线）
 * @param backdrop 玻璃采样背景（页面级 backdrop）
 */
@Composable
fun GlassActionMenu(
    show: Boolean,
    onDismissRequest: () -> Unit,
    anchor: GlassPopupAnchor,
    anchorContent: @Composable () -> Unit,
    groups: List<List<ActionMenuItem>>,
    backdrop: Backdrop?,
) {
    // 全屏宿主：dismiss 层与窗口坐标定位都要求铺满，不能受触发按钮尺寸约束
    Box(modifier = Modifier.fillMaxSize()) {
        GlassTransformPopup(
            show = show,
            onDismissRequest = onDismissRequest,
            anchor = anchor,
            backdrop = backdrop,
            anchorContent = anchorContent,
            cornerRadius = GlassPopupDefaults.CornerRadius,
        ) {
            groups.forEachIndexed { groupIndex, group ->
                group.forEach { item ->
                    GlassPopupItem(
                        text = item.text,
                        summary = item.summary,
                        icon = item.icon,
                        enabled = item.enabled,
                        // 破坏性操作用错误色（文案与图标同色：GlassPopupItem 内部用同一个 color 画 icon）
                        contentColor = if (item.destructive) {
                            MiuixTheme.colorScheme.error
                        } else {
                            MiuixTheme.colorScheme.onSurfaceContainer
                        },
                        // 动作菜单不会传 selected，这里仅防止将来误用时变成主题色
                        selectedContentColor = if (item.destructive) {
                            MiuixTheme.colorScheme.error
                        } else {
                            MiuixTheme.colorScheme.primary
                        },
                        onClick = {
                            // 先执行动作、再请求关闭：与 GlassIconDropdownMenu 的
                            // collapseOnSelection 顺序一致（item.onClick() 之后才 dismiss）。
                            // 动作可能是弹对话框 / 拉起系统面板，对话框是独立 Window，
                            // 始终盖在菜单之上，顺序反过来会出现「菜单先收、对话框后到」的空窗。
                            item.onClick()
                            onDismissRequest()
                        },
                    )
                }
                // 组间分割线：直接用 miuix 官方 GlassMenuDivider 的三个常量
                //（左右 20dp、上下 7.75dp、0.5dp 粗细），保证与 GlassIconDropdownMenu
                // 自带的分割线像素级一致；自己写死 padding 会丢上下留白、线也比官方粗。
                if (groupIndex < groups.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            horizontal = GlassPopupDefaults.DividerPaddingHorizontal,
                            vertical = GlassPopupDefaults.DividerPaddingVertical,
                        ),
                        thickness = GlassPopupDefaults.DividerThickness,
                    )
                }
            }
        }
    }
}
