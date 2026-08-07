package com.hrsthrt74.qstile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 底部 Sheet 的显示状态。
 *
 * 统一管理一个底部 Sheet 的显示/隐藏，并封装「请求关闭」逻辑（点击遮罩 / 按下返回键 / 下拉拖拽）。
 * 页面中不再需要为每个 Sheet 单独声明 `var showXxx by remember { mutableStateOf(false) }` 并重复编写
 * `onDismissRequest = { showXxx = false }`，改用本状态后只需：
 *
 * ```
 * val addSheetState = rememberSheetState()
 * // 打开
 * addSheetState.show()
 * // 关闭（由 AppBottomSheet 内部自动调用，页面无需手动处理）
 * addSheetState.dismiss()
 * ```
 *
 * @param initialShow 初始是否显示
 */
@Stable
class SheetState(initialShow: Boolean = false) {

    /** 当前是否显示（只读，外部通过 [show]/[dismiss] 控制） */
    var show by mutableStateOf(initialShow)
        private set

    /** 打开 Sheet */
    fun show() {
        show = true
    }

    /** 关闭 Sheet（等价于点击遮罩/返回键时的行为，供 Sheet 内容里的按钮调用） */
    fun dismiss() {
        show = false
    }
}

/**
 * 创建并记住一个 [SheetState]。
 *
 * @param initialShow 初始是否显示
 */
@Composable
fun rememberSheetState(initialShow: Boolean = false): SheetState =
    remember { SheetState(initialShow) }

/**
 * 统一的底部 Sheet 组件。
 *
 * 封装 [WindowBottomSheet]，将项目内所有 Sheet 的重复逻辑集中到一处：
 * - **请求关闭统一处理**：点击遮罩 / 按下返回键 / 下拉拖拽时自动调用 [SheetState.dismiss]，
 *   页面不再需要重复编写 `onDismissRequest = { showXxx = false }`
 * - **返回键行为统一控制**：通过 [allowDismiss] 参数统一控制是否允许返回/拖拽关闭（miuix 0.9.3
 *   内置 NavigationBackHandler 已自动生效，无需页面手动注册 BackHandler）
 * - **关闭后清理统一入口**：通过 [onDismissed] 在关闭动画完成后集中执行状态清理（如清空输入框内容）
 *
 * 注：曾尝试改用 OverlayBottomSheet 并为面板添加毛玻璃（textureBlur），但 miuix 官方将在 0.9.4
 * 优化相关支持，故暂保持 WindowBottomSheet 纯色实现，待官方方案发布后再跟进。
 *
 * @param state Sheet 的显示状态（由 [rememberSheetState] 创建）
 * @param modifier 应用到 Sheet 内容上的修饰符
 * @param title 标题（显示在 Sheet 顶部居中）
 * @param allowDismiss 是否允许通过拖拽 / 返回手势 / 点击遮罩关闭；为 false 时这些操作不生效
 * @param enableNestedScroll 是否启用内容的嵌套滚动（用于下拉关闭联动）
 * @param onDismissed 关闭动画完成后的回调（用于清理临时状态）；注意 show 为 false 但从未显示时不会触发
 * @param maxHeightFraction 内容区最大高度占窗口高度的比例（默认 0.9，即 90%）。
 *   超高的内容（如可滚动列表）会在受限高度内滚动，避免 sheet 几乎占满整屏；
 *   注意约束作用于内容区，面板顶部的拖动条/标题会在此基础上叠加数十 dp
 * @param content Sheet 内容
 */
@Composable
fun AppBottomSheet(
    state: SheetState,
    modifier: Modifier = Modifier,
    title: String? = null,
    allowDismiss: Boolean = true,
    enableNestedScroll: Boolean = true,
    onDismissed: (() -> Unit)? = null,
    maxHeightFraction: Float = 0.85f,
    content: @Composable () -> Unit,
) {
    // 读取当前窗口高度（与 miuix 内部一致），按比例计算内容区的最大高度
    val windowInfo = LocalWindowInfo.current
    val maxContentHeight = windowInfo.containerDpSize.height * maxHeightFraction

    WindowBottomSheet(
        show = state.show,
        title = title,
        modifier = modifier,
        allowDismiss = allowDismiss,
        enableNestedScroll = enableNestedScroll,
        // 请求关闭（遮罩/返回/拖拽）统一交给状态管理，页面无需重复编写
        onDismissRequest = { state.dismiss() },
        // 关闭动画完成后触发清理回调（如清空输入框内容）
        onDismissFinished = { onDismissed?.invoke() },
    ) {
        // 限制内容区最大高度为窗口高度的 maxHeightFraction：
        // 内容较少时按内容实际高度显示（heightIn 只设上限不设下限），
        // 内容超高时可滚动列表在受限高度内滚动
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxContentHeight)
        ) {
            content()
        }
    }
}
