package com.hrsthrt74.qstile.ui

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangularShape
import top.yukonga.miuix.kmp.material.GlassShape

/**
 * kyant shapes → miuix [GlassShape] 的桥接工具。
 *
 * ## 为什么需要这层桥接
 * miuix 的玻璃材质（`Modifier.glassPanel` / `GlassNavigationBar` 等）只接受 [GlassShape]，
 * 而 [GlassShape] 是 `CornerBasedShape` 的子类，从结构上只认识「四个角的圆角尺寸」，
 * 无法直接吃一个 `androidx.compose.ui.graphics.Shape`（kyant 的 `Capsule` 就是后者）。
 * 官方 `glassSurface()` 里的 `resolveGlassSurfaceShape()` 也印证了这一点：
 * 只有 `RoundedCornerShape` 能被转换，**其它 Shape 一律退化成固定圆角**，
 * 也就是说 kyant 的连续曲率轮廓会被丢掉。
 *
 * ## 这里的做法
 * 不去裁剪（clip）组件，而是把 kyant 形状的**几何**翻译成 [GlassShape]：
 * - 四个角：用 [CornerSize] 适配器，按 kyant 形状自己算出来的像素半径回报
 *   （`Capsule` 即 `min(宽, 高) / 2`，等价于 `CornerSize(percent = 50)`，
 *   与 `RoundedCornerShape(percent = 50)` 的胶囊轮廓完全一致）；
 * - 曲率风格：kyant 的 `Continuous` → [GlassShape] 的 `smoothing = 1f`（连续曲率 squircle），
 *   `Circular` → `smoothing = 0f`（普通圆弧）。
 *
 * 这样做的好处是轮廓能进入玻璃 shader / 描边 / 阴影三条渲染路径，
 * 与组件内部真正画出来的形状严格一致。
 *
 * ## 为什么不直接 `Modifier.clip(Capsule())`
 * `GlassNavigationBar` 的 modifier 作用在**整组**节点上（胶囊 + 胶囊外的 trailingAction
 * 由 `GlassNavigationBarLayout` 排版在同一个 Layout 里），外层 clip 会：
 * 1. 把胶囊右侧的「添加磁贴」跟随按钮（位于胶囊轮廓之外）整块裁掉；
 * 2. 把玻璃阴影（`drawBehind` 绘制、会溢出节点边界）一起裁掉，胶囊失去悬浮感。
 * 所以底栏这类带附属元素的玻璃组件，走「形状适配」而不是「外层裁剪」。
 */

/**
 * 把 kyant 的圆角矩形族形状（`Capsule` / `RoundedRectangle` / `UnevenRoundedRectangle` 等）
 * 转换成 miuix 可用的 [GlassShape]。
 *
 * 注意：kyant 形状的 `corners()` 需要实际尺寸才能算出像素半径，所以这里用 [CornerSize]
 * 适配器把「求值」推迟到布局阶段，尺寸变化（字体放大导致的底栏变高、平板拉宽等）时依然准确。
 *
 * @return 与 kyant 形状轮廓、曲率风格一致的 [GlassShape]
 */
fun RoundedRectangularShape.toGlassShape(): GlassShape = GlassShape(
    // 与 GlassShape 构造参数顺序一致：topStart, topEnd, bottomEnd, bottomStart
    // （kyant 的 Corners 用 topLeft/topRight/bottomRight/bottomLeft 命名，一一对应）
    topStart = KyantCornerSize(this, KyantCorner.TopStart),
    topEnd = KyantCornerSize(this, KyantCorner.TopEnd),
    bottomEnd = KyantCornerSize(this, KyantCorner.BottomEnd),
    bottomStart = KyantCornerSize(this, KyantCorner.BottomStart),
    // 连续曲率 → smoothing 1f；普通圆弧 → 0f（与 miuix 的 GlassDefaults.Smoothing 语义一致）
    smoothing = if (style == RoundedCornerStyle.Continuous) 1f else 0f,
)

/** 四个角的标识，避免为每个角写一个匿名 lambda（便于 equals/hashCode 稳定比较）。 */
private enum class KyantCorner { TopStart, TopEnd, BottomEnd, BottomStart }

/**
 * 把 kyant 形状某个角的半径适配成 Compose 的 [CornerSize]。
 *
 * kyant 的 `corners()` 需要 `(size, layoutDirection, density)` 三个参数，
 * 而 [CornerSize.toPx] 只给 `(shapeSize, density)`，因此这里固定传 [LayoutDirection.Ltr]：
 * 从 kyant 形状里只取「几何半径」这一个标量，镜像/方向翻转交给 [GlassShape.createOutline]
 * 按当前布局方向处理（与 Compose 自带 `RoundedCornerShape` 的行为一致）。
 *
 * @param shape 需要取半径的 kyant 形状
 * @param corner 取哪一个角
 */
private class KyantCornerSize(
    private val shape: RoundedRectangularShape,
    private val corner: KyantCorner,
) : CornerSize {

    override fun toPx(shapeSize: Size, density: Density): Float {
        val corners = shape.corners(shapeSize, LayoutDirection.Ltr, density)
        return when (corner) {
            KyantCorner.TopStart -> corners.topLeft
            KyantCorner.TopEnd -> corners.topRight
            KyantCorner.BottomEnd -> corners.bottomRight
            KyantCorner.BottomStart -> corners.bottomLeft
        }
    }

    // GlassShape.equals 会逐角比较 CornerSize，实现值语义可避免重组时产生「形状不相等」
    // 从而让玻璃材质被无谓地重建。
    override fun equals(other: Any?): Boolean =
        other is KyantCornerSize && other.shape == shape && other.corner == corner

    override fun hashCode(): Int = 31 * shape.hashCode() + corner.hashCode()
}
