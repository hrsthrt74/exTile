package com.hrsthrt74.qstile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 全局统一的模糊半径（dp），顶部栏与底部导航栏共用，保证视觉一致 */
const val AppBlurRadius = 80f

/**
 * 统一的模糊色彩配置（参考官方示例参数）。
 * 在模糊层上叠加 80% 的表面色 —— 这是让栏看起来半透明的关键（alpha 越低越透），
 * 不额外做亮度/对比度/饱和度调整，保持与官方一致的纯净观感。
 */
@Composable
fun blurBarColors(): BlurColors = BlurDefaults.blurColors(
    blendColors = listOf(
        BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = 0.7f))
    )
)

/**
 * 创建并记住一个可用于顶部栏模糊的 [LayerBackdrop]。
 *
 * 参考官方示例的做法：
 * - 未开启模糊开关或 RuntimeShader 不支持时直接返回 null，栏退回纯色背景（降级保护）
 * - 捕获内容前先绘制表面色作为底色，保证滚动内容不满屏时背景一致
 *
 * @param enabled 模糊总开关，关闭时返回 null，栏退回纯色
 */
@Composable
fun rememberBlurBackdrop(enabled: Boolean = true): LayerBackdrop? {
    if (!enabled || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * 创建一个 popup 定位器，左右边距可独立设置，让 popup 距屏幕边缘两侧等距。
 *
 * 背景：官方 [ListPopupDefaults.dropdownPositionProvider] 只支持对称的 horizontalMargin，
 * 而它的 clamp 逻辑下限是 `windowBounds.left`（不强制留左 margin）、上限减右 margin。
 * 若两侧用同一个 margin，最左磁贴的 popup 靠 anchor 定位（anchor.left + margin）间距偏大，
 * 最右磁贴被 clamp 到只留 margin 间距偏小，看起来不等距。
 *
 * 解决：左 margin 设 0、右 margin 设期望值即可让两侧视觉等距
 * （最左 = anchor.left(网格 padding 8) + 0 = 8，最右 = clamp 留 8）。
 *
 * @param verticalMargin popup 与锚点（磁贴）的垂直间距
 * @param startMargin popup 距屏幕左（或 RTL 的右）边缘的间距
 * @param endMargin popup 距屏幕右（或 RTL 的左）边缘的间距
 */
fun asymmetricDropdownPositionProvider(
    verticalMargin: Dp = 0.dp,
    startMargin: Dp = 0.dp,
    endMargin: Dp = 8.dp,
): PopupPositionProvider = object : PopupPositionProvider {
    private val margins = PaddingValues(
        start = startMargin,
        end = endMargin,
        top = verticalMargin,
        bottom = verticalMargin,
    )

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
        popupMargin: IntRect,
        alignment: PopupPositionProvider.Align,
    ): IntOffset {
        val offsetX = if (alignment == PopupPositionProvider.Align.End) {
            anchorBounds.right - popupContentSize.width - popupMargin.right
        } else {
            anchorBounds.left + popupMargin.left
        }
        val offsetY = if (windowBounds.bottom - anchorBounds.bottom > popupContentSize.height) {
            // 下方空间足够，显示在锚点下方
            anchorBounds.bottom + popupMargin.bottom
        } else if (anchorBounds.top - windowBounds.top > popupContentSize.height) {
            // 上方空间足够，显示在锚点上方
            anchorBounds.top - popupContentSize.height - popupMargin.top
        } else {
            // 上下都不够，垂直居中
            anchorBounds.top + anchorBounds.height / 2 - popupContentSize.height / 2
        }
        return IntOffset(
            // 水平 clamp：下限不强制留左 margin（对齐官方逻辑），上限保留右 margin
            x = offsetX.coerceIn(
                windowBounds.left,
                (windowBounds.right - popupContentSize.width - popupMargin.right).coerceAtLeast(windowBounds.left),
            ),
            y = offsetY.coerceIn(
                (windowBounds.top + popupMargin.top)
                    .coerceAtMost(windowBounds.bottom - popupContentSize.height - popupMargin.bottom),
                windowBounds.bottom - popupContentSize.height - popupMargin.bottom,
            ),
        )
    }

    override fun getMargins(): PaddingValues = margins
}

/**
 * 包裹顶部栏的模糊容器。
 * 当 [blurEnabled] 且 [backdrop] 不为空时，对整个栏应用纹理模糊；否则栏原样渲染。
 *
 * @param backdrop 由 [rememberBlurBackdrop] 创建的背景捕获层
 * @param blurEnabled 是否启用模糊
 * @param content 实际的顶部栏内容（如 [top.yukonga.miuix.kmp.basic.TopAppBar]）
 */
@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    val blurActive = blurEnabled && backdrop != null
    Box(
        modifier = if (blurActive) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = AppBlurRadius,
                colors = blurBarColors(),
            )
        } else {
            Modifier
        },
    ) {
        content()
    }
}
