package com.hrsthrt74.qstile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SearchBarDefaults
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults
import top.yukonga.miuix.kmp.glass.glass
import top.yukonga.miuix.kmp.glass.glassShadow
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 玻璃风格的搜索输入框：胶囊底下垫一层**真实的背景模糊**（采样 [backdrop]），
 * 观感与 `GlassSegmentedTabRow` 的玻璃轨道一致，用于 Sheet 浮层头部里的搜索框。
 *
 * ### 为什么不用 GlassTextField
 *
 * miuix 的 `GlassTextField` 只是把 `TextField` 的配色换成「半透明填充 + 高对比文字」，
 * 并**不采样 backdrop**（没有模糊）。要让搜索框真的吃到底下的模糊，只能自己在背后画玻璃材质：
 * 官方 `InputField` 的 `color` 参数注释里就写明了这种做法——「传 [Color.Transparent]，
 * 当你自己在其背后画背景时（例如 backdrop blur）」。本组件即按这个约定封装。
 *
 * ### 画法与 GlassSegmentedTabRow 一致
 *
 * - `glassShadow` 必须挂在 `clip` **之前**（外层）：玻璃阴影会溢出节点边界，
 *   塞进裁剪之内会被裁成一圈硬边；
 * - 玻璃材质用 `GlassSegmentedTabRowDefaults.material()` / `stroke()`（`puredThinGlass` +
 *   同尺寸辉光描边），保证和同屏的分段 TabRow 是同一套材质，不会一个偏白一个偏灰；
 * - `shading = false`：搜索框是「薄玻璃」层，不做折射，避免文字被折射扭曲。
 *
 * ### 胶囊半径要按实测高度算
 *
 * `InputField` 的胶囊是 `CircleShape`（半径 = 半高），且高度是 `heightIn(min = 45dp)`——
 * 用户放大系统字体时会高于 45dp。若把半径写死成 22.5dp，字体一大就会变成圆角矩形；
 * 因此这里用 [onSizeChanged] 量出实际高度再取一半（首帧用 45dp 兜底，形状不会跳）。
 *
 * @param query 当前搜索词
 * @param onQueryChange 搜索词变化回调
 * @param onSearch 键盘搜索键回调
 * @param expanded 输入框是否处于展开态（为 true 时进入即自动聚焦弹键盘）
 * @param onExpandedChange 展开态变化回调
 * @param label 占位文案
 * @param backdrop 玻璃要采样的背景；为 null（模糊关闭 / 运行时 shader 不支持 / 经典风格）时
 *   回退成普通 [InputField]（用官方下沉底色，否则透明底会让文字悬空）
 * @param modifier 应用到输入框容器上的修饰符
 */
@Composable
fun GlassSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    label: String,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
) {
    // 没有可采样的背景：回退成普通输入框（保持和改造前完全一样的观感）
    if (backdrop == null) {
        InputField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            label = label,
            color = MiuixTheme.colorScheme.surfaceContainerHighest,
            modifier = modifier.fillMaxWidth(),
        )
        return
    }

    val density = LocalDensity.current
    // 输入框实测高度（px）：用来算胶囊半径，见上面的说明
    var heightPx by remember { mutableIntStateOf(0) }
    val radius = if (heightPx > 0) {
        with(density) { (heightPx / 2f).toDp() }
    } else {
        // 首帧还没测量：用官方最小高度的一半兜底，之后的实测值一般与它相同
        SearchBarDefaults.InputFieldMinHeight / 2
    }
    val shape = remember(radius) { GlassShape(radius, smoothing = 0f) }
    // 与同屏的 GlassSegmentedTabRow 共用同一套材质 / 描边
    val material = GlassSegmentedTabRowDefaults.material()
    val stroke = GlassSegmentedTabRowDefaults.stroke()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { heightPx = it.height }
    ) {
        // 玻璃层：铺满输入框胶囊（matchParentSize 不参与测量，尺寸由下面的 InputField 决定）
        Box(
            modifier = Modifier
                .matchParentSize()
                // 阴影画在裁剪之前：否则会被下面的 clip 裁成一圈硬边
                .glassShadow(shape, GlassShadows.Float)
                .clip(Capsule())
                .glass(
                    backdrop = backdrop,
                    shape = shape,
                    material = material,
                    stroke = stroke,
                    // 薄玻璃层不做折射，避免输入文字被扭曲
                    shading = false,
                )
        )
        // 输入框本体：底色透明，让下面这层玻璃材质透出来
        InputField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            label = label,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
