package com.hrsthrt74.qstile.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 页面 → 底部导航栏 的动作桥。
 *
 * 窄屏（底部导航）玻璃风格下，底栏右侧可以额外挂一个「跟随按钮」
 * （官方 demo 的 `GlassNavigationBar(trailingAction = ...)` 写法）。
 *
 * 为什么需要这个桥：
 * - 底栏由 App 层（[com.hrsthrt74.qstile.MainApp] 的 Scaffold bottomBar）渲染，它在页面之上；
 * - 而按钮要触发的状态（「添加磁贴」Sheet 的显示状态）住在磁贴编辑页内部；
 * - 于是：页面通过 [LocalNavPageAction] 注册「点击要做什么」，App 层决定「什么时候显示」
 *   （按当前选中页判断，而不是按注册与否——Pager 会保留所有页面的组合状态）。
 */
@Stable
class NavPageActionState {

    /**
     * 磁贴编辑页注册的「添加磁贴」点击动作；其他页面保持 null（= 底栏不显示该按钮）。
     * 仅允许框架内部（页面自己）写入。
     */
    var onAddTile by mutableStateOf<(() -> Unit)?>(null)
        internal set
}

/**
 * 由 App 根节点提供的动作桥；为 null 表示当前宿主（如 OOBE / 独立 Activity）不支持底栏跟随按钮。
 */
val LocalNavPageAction = staticCompositionLocalOf<NavPageActionState?> { null }
