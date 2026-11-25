package com.hrsthrt74.qstile.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.glass.GlassCard
import top.yukonga.miuix.kmp.glass.GlassCardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Sheet / Dialog 内容里的卡片：**跟随应用风格开关**自动切换实现。
 *
 * - 开（玻璃风格）→ [GlassCard]：半透明填充，露出底下 Sheet 的玻璃材质，卡片读起来是「浮起的一组」
 * - 关（经典风格）→ [Card]：实色填充，与改造前完全一致
 *
 * ### 为什么经典风格不能也用 GlassCard
 *
 * [GlassCard] 的填充色是**半透明**的（浅色取 `background` / `surfaceContainer` 的 55%，
 * 深色取 `onSurfaceContainer` 的 8%），它的存在意义就是「垫在玻璃材质上，让材质透出来」。
 * 而经典 Sheet / Dialog 的底是不透明实色，半透明色块压上去：
 * - 浅色模式下几乎和底同色，卡片边界消失，分组看不出来；
 * - 深色模式下会发灰，比原来的实色卡片更糊。
 * 所以经典风格必须回到实色 [Card]，这两个分支不能合并。
 *
 * ### 玻璃风格下 `color` 参数会被忽略
 *
 * 玻璃风格的卡片一律用 [GlassCardDefaults.colors]（库按深浅色算好的中性半透明填充）：
 * 官方 demo 的玻璃 Sheet 就是这么写的，所有分组卡片外观统一，正是 HyperOS 的观感。
 * 调用点原来传的 `secondaryContainer` / `secondaryVariant` 之类**只对经典风格有意义**
 * （那是实色配色体系里的「分组底色」），在玻璃风格里若照搬会变成一块不透明色块、把玻璃盖住。
 *
 * ### 用法
 *
 * 只在**浮层（Sheet / Dialog）内部**用它。页面上的卡片（首页、设置页、编辑页）垫在不透明
 * 页面背景上，不属于玻璃面，直接用 Miuix 的 [Card]，不要换成这个。
 *
 * @param modifier 应用到卡片上的修饰符
 * @param color 经典风格的卡片底色（玻璃风格忽略，见上文）
 * @param contentColor 经典风格的卡片内容色（玻璃风格忽略）
 * @param cornerRadius 圆角；为 null 时跟随风格默认值（经典 16dp / 玻璃 20dp）
 * @param insideMargin 卡片内边距（两种风格共用同一个值）
 * @param content 卡片内容
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surfaceContainer,
    contentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    cornerRadius: Dp? = null,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 与 AppBottomSheet / AppDialog 同一个开关：progressiveBlur 即「应用风格」
    val glassStyle = LocalThemeSettings.current.progressiveBlur

    if (glassStyle) {
        GlassCard(
            modifier = modifier,
            cornerRadius = cornerRadius ?: GlassCardDefaults.CornerRadius,
            insideMargin = insideMargin,
            // 不给 colors 传参：用库的默认半透明填充，保证浮层内所有卡片观感统一
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            cornerRadius = cornerRadius ?: CardDefaults.CornerRadius,
            insideMargin = insideMargin,
            colors = CardDefaults.defaultColors(color = color, contentColor = contentColor),
            content = content,
        )
    }
}
