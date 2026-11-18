package com.hrsthrt74.qstile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

/** 选中态配色过渡时长：150ms，切换时底色/文字色平滑补间，不硬切。 */
private const val ChipColorTransitionDurationMs = 150

/**
 * 通用可选中胶囊 Chip（参照 afdian 项目的同名组件裁剪而来）。
 *
 * 只有「选中 / 未选中」两态，状态完全由底色 + 内容色表达：
 * - 选中：`primaryContainer` 底 + `onPrimaryContainer` 文字
 * - 未选中：`secondary` 底 + `onSecondaryVariant` 文字
 *
 * 目前用于「添加磁贴」Sheet 标题栏右侧的「连续放置」开关。
 *
 * @param text Chip 文案
 * @param selected 是否选中
 * @param onClick 点击回调（组件内已补一次长按档触觉）
 * @param modifier 外部修饰符
 */
@Composable
fun AppChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 底色与内容色都用短过渡补间，切换选中态时不「啪」地硬切；主题切换时同样平滑
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MiuixTheme.colorScheme.primaryContainer
        else MiuixTheme.colorScheme.secondary,
        animationSpec = tween(ChipColorTransitionDurationMs),
        label = "appChipBackgroundColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MiuixTheme.colorScheme.onPrimaryContainer
        else MiuixTheme.colorScheme.onSecondaryVariant,
        animationSpec = tween(ChipColorTransitionDurationMs),
        label = "appChipContentColor",
    )
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            // 按下时的吸收反馈（miuix 自带），与水波纹二选一，这里不用 indication
            .pressable(interactionSource = null, indication = SinkFeedback(), delay = null)
            // 50% 圆角 = 完整胶囊
            .clip(RoundedCornerShape(percent = 50))
            .background(backgroundColor)
            .clickable(interactionSource = null, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MiuixTheme.textStyles.body2,
            // 单行不换行，过窄时省略
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
