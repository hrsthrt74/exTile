package com.hrsthrt74.qstile.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.DropdownArrowEndAction
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassDropdownPopup
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.glassPopupAnchorValue
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

/**
 * 玻璃弹窗退场动画结束后卸载宿主的延迟（毫秒）。
 * 官方 GlassMotion 的 arcFade 退场 200ms、bounds spring（stiffness 0.22）收敛约 300~400ms，
 * 留 420ms 保证动画播完再卸载；过短会截断动画，过长只是多一小段正常的可点击忽略期。
 */
internal const val POPUP_EXIT_UNMOUNT_DELAY_MS = 420L

/**
 * 应用风格下拉选择器：随「应用风格」开关切换外观，统一替代设置页里散落的 Spinner。
 *
 * - [glassStyle] = false：经典风格，直接用 miuix 的 [WindowSpinnerPreference]（原版行为）。
 * - [glassStyle] = true：Glass 风格，自绘 BasicComponent 行 + 玻璃弹窗
 *   （[GlassDropdownPopup] + [GlassPopupItem]），写法与官方 demo 的 GlassDropdownPreference 一致，
 *   但额外支持每项的 [ImageVector] 图标（磁贴图标选择器需要；官方 GlassDropdownPreference 仅纯文本）。
 *
 * @param items 选项文案
 * @param icons 每项可选图标（与 items 等长；空列表表示都不显示图标）
 * @param selectedIndex 当前选中索引
 * @param title 行标题
 * @param glassStyle 应用风格开关（true=Glass / false=经典）
 * @param backdrop 页面 backdrop（玻璃弹窗采样背景；null 时弹窗回退纯色）
 * @param onSelectedIndexChange 选中回调
 * @param startAction 行首图标槽
 */
@Composable
fun AppStyleSpinnerPreference(
    items: List<String>,
    icons: List<ImageVector?> = emptyList(),
    selectedIndex: Int,
    title: String,
    glassStyle: Boolean,
    backdrop: Backdrop?,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    startAction: (@Composable () -> Unit)? = null,
    summary: String? = null,
) {
    if (!glassStyle) {
        // 经典风格：还原为原版 Spinner，图标用 DropdownItem 的 composable icon 槽
        val dropdownItems = items.mapIndexed { index, text ->
            val icon = icons.getOrNull(index)
            DropdownItem(
                text = text,
                icon = if (icon != null) {
                    { iconModifier ->
                        top.yukonga.miuix.kmp.basic.Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = iconModifier,
                        )
                    }
                } else null,
            )
        }
        WindowSpinnerPreference(
            items = dropdownItems,
            selectedIndex = selectedIndex,
            title = title,
            modifier = modifier,
            summary = summary,
            startAction = startAction,
            onSelectedIndexChange = onSelectedIndexChange,
        )
        return
    }

    // ===== Glass 风格：照搬官方 GlassDropdownPreference 结构，扩展图标支持 =====
    var expanded by remember { mutableStateOf(false) }
    // 弹出后挂载弹窗宿主，退场动画结束后必须卸载：
    // PopupLayout 的宿主在 visible=true 期间会渲染一层全屏 pointerInput 吞掉所有触摸，
    // 若 mounted 常驻不复位，关闭弹窗后整个界面将无法点击（表现为「卡死」且无崩溃日志）。
    // public GlassDropdownPopup 无 onDismissFinished，这里按官方退场动画时长延迟卸载。
    val mounted = remember { mutableStateOf(false) }
    val anchor = rememberGlassPopupAnchor()
    var rowBounds by remember { mutableStateOf(Rect.Zero) }
    val actualEnabled = items.isNotEmpty()
    val currentOnSelected by rememberUpdatedState(onSelectedIndexChange)
    val setExpanded: (Boolean) -> Unit = remember {
        { next ->
            if (expanded != next) {
                expanded = next
                if (next) mounted.value = true
            }
        }
    }
    LaunchedEffect(actualEnabled) {
        if (!actualEnabled) setExpanded(false)
    }
    // 关闭后等退场动画播完再卸载宿主，避免全屏遮罩残留吞触摸（详见 mounted 注释）
    LaunchedEffect(expanded) {
        if (!expanded && mounted.value) {
            delay(POPUP_EXIT_UNMOUNT_DELAY_MS)
            mounted.value = false
        }
    }
    val haptic = LocalHapticFeedback.current
    val actionColor = if (actualEnabled) {
        MiuixTheme.colorScheme.onSurfaceVariantActions
    } else {
        MiuixTheme.colorScheme.disabledOnSecondaryVariant
    }

    BasicComponent(
        modifier = modifier.onGloballyPositioned { rowBounds = it.boundsInWindow() },
        title = title,
        summary = summary,
        startAction = startAction,
        endActions = {
            Row(
                modifier = Modifier.glassPopupAnchorValue(anchor),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val value = items.getOrNull(selectedIndex)
                if (!value.isNullOrEmpty()) {
                    Text(
                        text = value,
                        modifier = Modifier.padding(end = 8.dp).weight(1f, fill = false),
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        color = actionColor,
                        textAlign = TextAlign.End,
                    )
                }
                DropdownArrowEndAction(actionColor = actionColor)
            }
        },
        onClick = {
            setExpanded(!expanded)
            if (expanded) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        },
        role = Role.DropdownList,
        holdDownState = mounted.value,
        enabled = actualEnabled,
    )

    if (!mounted.value) return

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
                show = expanded && actualEnabled,
                onDismissRequest = { setExpanded(false) },
                anchorBounds = rowBounds,
                backdrop = backdrop,
                anchor = anchor,
            ) {
                items.forEachIndexed { index, item ->
                    GlassPopupItem(
                        text = item,
                        icon = icons.getOrNull(index),
                        selected = selectedIndex == index,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            setExpanded(false)
                            currentOnSelected(index)
                        },
                    )
                }
            }
        }
    }
}
