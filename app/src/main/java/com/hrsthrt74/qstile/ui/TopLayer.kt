package com.hrsthrt74.qstile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * 顶层覆盖层状态：收集各页面通过 [TopLayer] 提升上来的浮层内容。
 *
 * 背景：miuix-glass 的玻璃弹窗（GlassIconDropdownMenu / GlassTransformPopup）虽然在页面内
 * 声明，但用「窗口坐标」定位与采样背景，必须绘制在所有 chrome（顶栏、玻璃侧栏）之上，
 * 且不能被 Pager 页面的 widthIn/裁剪影响。官方 demo 正是用这套 TopLayer 机制解决的，
 * 这里照搬其实现。
 */
@Stable
class TopLayerState {
    internal val entries = mutableStateListOf<TopLayerEntry>()
}

/** 单个被提升的浮层条目；content 可变以便浮层重组时同步更新。 */
@Stable
internal class TopLayerEntry {
    var content: @Composable BoxScope.() -> Unit by mutableStateOf({})
}

/** 供页面调用 [TopLayer] 时找到最近的 [TopLayerHost]；未提供时 [TopLayer] 原地绘制。 */
val LocalTopLayer = staticCompositionLocalOf<TopLayerState?> { null }

/**
 * 把 [content] 提升到最近的 [TopLayerHost] 绘制（没有宿主时才原地绘制）。
 * 玻璃弹窗请用本函数包裹，以保证层级与窗口坐标正确。
 */
@Composable
fun TopLayer(content: @Composable BoxScope.() -> Unit) {
    val layer = LocalTopLayer.current
    if (layer == null) {
        // 无宿主时退化为原地全屏 Box，动画层级可能不理想但不至于崩溃
        Box(modifier = Modifier.fillMaxSize(), content = content)
        return
    }
    val entry = remember { TopLayerEntry() }
    SideEffect { entry.content = content }
    DisposableEffect(layer, entry) {
        layer.entries += entry
        onDispose { layer.entries -= entry }
    }
}

/**
 * 顶层浮层宿主。必须放在所有导航 chrome 之后绘制（即覆盖在顶栏/侧栏之上）。
 */
@Composable
fun TopLayerHost(
    state: TopLayerState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        state.entries.forEach { entry ->
            key(entry) { entry.content(this) }
        }
    }
}
