package com.hrsthrt74.qstile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 供玻璃弹窗（Dialog / BottomSheet）采样页面背景的 backdrop。
 *
 * 由 App.kt 在最外层导航内容（AppPager，已挂载 [top.yukonga.miuix.kmp.blur.layerBackdrop]）
 * 处下发：它覆盖整页含顶栏，但不含提升到根 Scaffold 的弹窗宿主与遮罩，
 * 符合官方 demo 对 glass 弹窗 backdrop 的要求。Oobe / DebugTools 等独立作用域
 * 未提供时为 null，玻璃组件自动回退纯色容器。
 */
val LocalOverlayBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * 对话框的显示状态。
 *
 * 与 [SheetState] 同构：统一管理一个对话框的显示/隐藏，并封装「请求关闭」逻辑
 * （点击遮罩 / 按下返回键）。页面中不再需要为每个对话框单独声明
 * `var showXxx by remember { mutableStateOf(false) }` 并重复编写
 * `onDismissRequest = { showXxx = false }`，改用本状态后只需：
 *
 * ```
 * val confirmDialogState = rememberDialogState()
 * // 打开
 * confirmDialogState.show()
 * // 关闭（由 AppDialog 内部自动调用，页面无需手动处理）
 * confirmDialogState.dismiss()
 * ```
 *
 * @param initialShow 初始是否显示
 */
@Stable
class DialogState(initialShow: Boolean = false) {

    /** 当前是否显示（只读，外部通过 [show]/[dismiss] 控制） */
    var show by mutableStateOf(initialShow)
        private set

    /** 打开对话框 */
    fun show() {
        show = true
    }

    /** 关闭对话框（等价于点击遮罩/返回键时的行为，供对话框内容里的按钮调用） */
    fun dismiss() {
        show = false
    }
}

/**
 * 创建并记住一个 [DialogState]。
 *
 * @param initialShow 初始是否显示
 */
@Composable
fun rememberDialogState(initialShow: Boolean = false): DialogState =
    remember { DialogState(initialShow) }

/**
 * 统一的对话框组件。
 *
 * 封装 [WindowDialog]，将项目内所有 Dialog 的重复逻辑集中到一处，默认样式为
 * 「取消 + 确认」按钮（本项目所有对话框都用这种交互）：
 * - **请求关闭统一处理**：点击遮罩 / 按下返回键 / 取消按钮时自动调用 [DialogState.dismiss]，
 *   页面不再需要重复编写 `onDismissRequest = { showXxx = false }`
 * - **确认按钮统一行为**：点击确认后先执行 [onConfirm] 再自动关闭对话框
 * - **关闭后清理统一入口**：通过 [onDismissed] 在关闭动画完成后集中执行状态清理
 *
 * @param state Dialog 的显示状态（由 [rememberDialogState] 创建）
 * @param modifier 应用到 Dialog 内容上的修饰符
 * @param title 标题
 * @param summary 说明文字
 * @param cancelText 取消按钮文案（传 null 时使用资源默认「取消」，支持多语言）
 * @param confirmText 确认按钮文案
 * @param confirmEnabled 确认按钮是否可点击（默认 true）；为 false 时置灰禁用，
 *   常用于需要等待一段时间（如倒计时）后才允许确认的场景
 * @param destructive 是否为破坏性操作；为 true 时确认按钮使用错误色（红色）
 * @param onConfirm 点击确认按钮时的回调；确认后对话框由本组件自动关闭
 * @param onDismissed 关闭动画完成后的回调（用于清理临时状态）；注意 show 为 false 但从未显示时不会触发
 * @param content 按钮区上方的额外内容（可省略）
 */
@Composable
fun AppDialog(
    state: DialogState,
    modifier: Modifier = Modifier,
    title: String? = null,
    summary: String? = null,
    // 取消按钮文案：默认参数不能调用 stringResource（非 Composable 上下文），
    // 因此改为可空 + 函数体内按需解析，随系统语言自动重组
    cancelText: String? = null,
    confirmText: String,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismissed: (() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    // 解析取消按钮文案：外部未显式传参时回落到资源默认值
    val resolvedCancelText = cancelText ?: stringResource(R.string.common_cancel)

    // 随应用风格开关切换：true=玻璃风格 GlassDialog，false=经典 WindowDialog。
    // 与顶栏/底栏/Dropdown 使用同一个开关，保持全局一致。
    val glassStyle = LocalThemeSettings.current.progressiveBlur
    val backdrop = LocalOverlayBackdrop.current

    // 额外内容 + 按钮区：两种风格共用结构，仅按钮实现随风格切换，
    // 避免各屏幕调用点重复（它们只声明文案与回调）。
    val dialogContent: @Composable () -> Unit = {
        content()
        // 经典 dialog 缺这个间距，留一点呼吸感（玻璃态同样适用）
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (glassStyle) {
                // 玻璃态：官方 GlassDialogButton（跟随玻璃 dialog 材质）
                GlassDialogButton(
                    onClick = { state.dismiss() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(resolvedCancelText, style = MiuixTheme.textStyles.button)
                }
                GlassDialogButton(
                    onClick = {
                        onConfirm()
                        state.dismiss()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = confirmEnabled,
                    role = if (destructive) {
                        GlassDialogButtonRole.Destructive
                    } else {
                        GlassDialogButtonRole.Primary
                    },
                ) {
                    Text(confirmText, style = MiuixTheme.textStyles.button)
                }
            } else {
                // 经典态：原 TextButton / Button 组合
                TextButton(
                    text = resolvedCancelText,
                    onClick = { state.dismiss() },
                    modifier = Modifier.weight(1f)
                )
                if (destructive) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.error,
                            contentColor = MiuixTheme.colorScheme.onError,
                        ),
                        enabled = confirmEnabled,
                        onClick = {
                            onConfirm()
                            state.dismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(confirmText)
                    }
                } else {
                    TextButton(
                        text = confirmText,
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        enabled = confirmEnabled,
                        onClick = {
                            onConfirm()
                            state.dismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (glassStyle) {
        GlassDialog(
            show = state.show,
            backdrop = backdrop,
            title = title,
            summary = summary,
            modifier = modifier,
            // 请求关闭（遮罩/返回）统一交给状态管理
            onDismissRequest = { state.dismiss() },
            // 官方组件在退场动画完成后回调，天然避开「遮罩常驻吞触摸」卡死问题
            onDismissFinished = { onDismissed?.invoke() },
            content = dialogContent,
        )
    } else {
        WindowDialog(
            show = state.show,
            title = title,
            summary = summary,
            modifier = modifier,
            onDismissRequest = { state.dismiss() },
            onDismissFinished = { onDismissed?.invoke() },
            content = dialogContent,
        )
    }
}
