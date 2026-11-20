package com.hrsthrt74.qstile.ui.components

import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.TextField

/**
 * 文本输入框（对 miuix `TextField` 的 state 版重载做一层「字符串 ↔ TextFieldState」桥接）。
 *
 * ## 为什么需要这层封装
 *
 * miuix 0.9.5-beta02 起把 `TextField(value: String, onValueChange: ...)` 与
 * `TextField(value: TextFieldValue, ...)` 两个旧重载标记为 `@Deprecated`，
 * 官方唯一推荐写法是持有 `TextFieldState`：
 * 文本从 `state.text` 读，改文本用 `state.edit { }` / `setTextAndPlaceCursorAtEnd()`。
 *
 * 但本项目的输入框状态都放在 ViewModel / `remember { mutableStateOf("") }` 里（普通 String），
 * 且存在「外部清空」的场景（Sheet 关闭后复位、提交成功后清空）。直接把状态换成 `TextFieldState`
 * 会把这些状态全部改写并牵动多处调用点；因此这里统一做桥接：
 *
 * - 调用点继续用 `value` / `onValueChange` 的直觉写法（与旧重载签名一致）；
 * - 组件内部持有唯一的 [TextFieldState]，双向同步到外部字符串状态。
 *
 * ## 同步方向说明（避免循环/光标跳动）
 *
 * 1. **输入 → 外部**：`snapshotFlow { state.text }` 观察内部状态，变化后回调 `onValueChange`。
 * 2. **外部 → 输入**：`LaunchedEffect(value)` 在外部值变化时回填内部状态。
 * 两步都用「字符串不相等才写」的判断兜住：用户正常输入时（外部值由第 1 步写入）
 * 第 2 步比较结果相等，因此不会重置光标位置，也不会和输入法打架。
 *
 * @param value 当前文本（外部受控值）
 * @param onValueChange 文本变化回调
 * @param modifier 外部修饰符
 * @param label 标签文案；为空时不显示标签
 * @param useLabelAsPlaceholder 标签是否兼作占位符（有内容时淡出）
 * @param enabled 是否可编辑
 * @param lineLimits 行数限制；默认 [TextFieldLineLimits.Default]（等同于旧重载 `singleLine = false`）
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    useLabelAsPlaceholder: Boolean = false,
    enabled: Boolean = true,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
) {
    // 内部状态只在首次组合时按外部初始值创建，之后由用户输入驱动
    val state = remember { TextFieldState(value) }

    // 用 rememberUpdatedState 拿最新的回调与外部值：effect 只随 state/onValueChange 重建，
    // 直接捕获参数会读到首次组合时的旧值
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentValue by rememberUpdatedState(value)

    // 输入 → 外部：把内部文本变化同步出去（首帧也会发一次初始值，外部写入同值是无副作用的）。
    // effect 只随 state 启动一次；回调与外部值走 rememberUpdatedState，避免每次都重建协程。
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .collect { text -> if (text != currentValue) currentOnValueChange(text) }
    }

    // 外部 → 输入：外部清空/回填（Sheet 复位、恢复配置后清空等）时同步到内部状态
    LaunchedEffect(value) {
        if (state.text.toString() != value) {
            // 换文本后把光标挪到末尾，避免光标停在越界的旧位置
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }

    TextField(
        state = state,
        modifier = modifier,
        label = label,
        useLabelAsPlaceholder = useLabelAsPlaceholder,
        enabled = enabled,
        lineLimits = lineLimits,
    )
}
