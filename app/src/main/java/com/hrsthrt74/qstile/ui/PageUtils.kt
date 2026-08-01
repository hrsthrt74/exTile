package com.hrsthrt74.qstile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
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
