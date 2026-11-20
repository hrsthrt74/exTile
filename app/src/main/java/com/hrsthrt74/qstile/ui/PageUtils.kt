package com.hrsthrt74.qstile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏当前是否为玻璃形态（progressive 开且模糊可用）。
 * 由 [AppTopBar] 在组合时写入，供顶栏内的按钮读取，从而跟随切换
 * [GlassIconButton] / 普通 [IconButton]（照搬官方 demo 的 BackNavigationIcon 写法）。
 */
val LocalAppTopBarGlass = staticCompositionLocalOf { false }

/** 全局统一的模糊半径（dp），原版顶栏普通模糊时共用 */
const val AppBlurRadius = 80f

/**
 * 统一的模糊色彩配置（参考官方示例参数）。
 * 在模糊层上叠加表面色 —— 这是让栏看起来半透明的关键（alpha 越低越透），
 * 不额外做亮度/对比度/饱和度调整，保持与官方一致的纯净观感。
 */
@Composable
fun blurBarColors(): BlurColors = BlurDefaults.blurColors(
    blendColors = listOf(
        BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = 0.7f))
    )
)

/**
 * 让出冷启动首帧之后的若干帧，把「首帧之后才开始的重活」挪到首屏真正画出来之后。
 *
 * 背景（不要删，也别随手改小）：
 * `GlassNavigationBar` 的指示器几何要等条幅**第一次布局结束**（`onSizeChanged` 回填
 * `barWidth`）之后、在下一次重组里才 snap 到位，所以首帧里指示器必然是空的。
 * 如果首帧之后主线程立刻被别的活占住（例如 Pager 非当前页的首次加载 / 整页首次组合），
 * 那次重组会被推迟到主线程空出来为止 —— 表现为「胶囊先出现、高亮块过一会儿才突然冒出来」，
 * 而且这段时间点击、滑动全部无响应（debug 构建尤其明显，release 下这点开销看不出来）。
 *
 * 因此：凡是「Pager 里所有页都会在冷启动首帧被组合」而只有当前页才需要立刻干的活，
 * 请在这个 `LaunchedEffect` 的开头先 `awaitStartupFrames()` 再执行。
 *
 * @param frames 让出的帧数。默认 3 帧，刚好覆盖这条链：
 *   第 1 帧布局回填 `barWidth` → 第 2 帧重组并 snap 指示器 → 第 3 帧把指示器画出来。
 *   让出的帧要落在第 3 帧**之后**，否则仍会挡住指示器。
 */
suspend fun awaitStartupFrames(frames: Int = 3) {
    repeat(frames) { withFrameNanos { } }
}

/**
 * 创建并记住一个可供玻璃组件（GlassTopAppBar / GlassNavigationBar / GlassNavigationRail）
 * 采样背景的 [LayerBackdrop]。写法与 miuix 官方 demo 的 `rememberBlurBackdrop()` 完全一致：
 * - 未开启模糊开关或 RuntimeShader 不支持时直接返回 null，组件退回纯色降级
 * - 捕获内容前先绘制表面色作为底色，保证滚动内容不满屏时背景一致
 *
 * @param enabled 模糊总开关，关闭时返回 null，组件退回纯色
 */
@Composable
fun rememberBlurBackdrop(enabled: Boolean = true): LayerBackdrop? {
    if (!enabled || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * 创建一个 popup 定位器，左右边距可独立设置，让 popup 距屏幕边缘两侧等距。
 *
 * 背景：官方 [ListPopupDefaults.dropdownPositionProvider] 只支持对称的 horizontalMargin，
 * 而它的 clamp 逻辑下限是 `windowBounds.left`（不强制留左 margin）、上限减右 margin。
 * 若两侧用同一个 margin，最左磁贴的 popup 靠 anchor 定位（anchor.left + margin）间距偏大，
 * 最右磁贴被 clamp 到只留 margin 间距偏小，看起来不等距。
 *
 * 解决：左 margin 设 0、右 margin 设期望值即可让两侧视觉等距
 * （最左 = anchor.left(网格 padding 8) + 0 = 8，最右 = clamp 留 8）。
 *
 * @param verticalMargin popup 与锚点（磁贴）的垂直间距
 * @param startMargin popup 距屏幕左（或 RTL 的右）边缘的间距
 * @param endMargin popup 距屏幕右（或 RTL 的左）边缘的间距
 */
fun asymmetricDropdownPositionProvider(
    verticalMargin: Dp = 0.dp,
    startMargin: Dp = 0.dp,
    endMargin: Dp = 8.dp,
): PopupPositionProvider = object : PopupPositionProvider {
    private val margins = PaddingValues(
        start = startMargin,
        end = endMargin,
        top = verticalMargin,
        bottom = verticalMargin,
    )

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
        popupMargin: IntRect,
        alignment: PopupPositionProvider.Align,
    ): IntOffset {
        val offsetX = if (alignment == PopupPositionProvider.Align.End) {
            anchorBounds.right - popupContentSize.width - popupMargin.right
        } else {
            anchorBounds.left + popupMargin.left
        }
        val offsetY = if (windowBounds.bottom - anchorBounds.bottom > popupContentSize.height) {
            // 下方空间足够，显示在锚点下方
            anchorBounds.bottom + popupMargin.bottom
        } else if (anchorBounds.top - windowBounds.top > popupContentSize.height) {
            // 上方空间足够，显示在锚点上方
            anchorBounds.top - popupContentSize.height - popupMargin.top
        } else {
            // 上下都不够，垂直居中
            anchorBounds.top + anchorBounds.height / 2 - popupContentSize.height / 2
        }
        return IntOffset(
            // 水平 clamp：下限不强制留左 margin（对齐官方逻辑），上限保留右 margin
            x = offsetX.coerceIn(
                windowBounds.left,
                (windowBounds.right - popupContentSize.width - popupMargin.right).coerceAtLeast(windowBounds.left),
            ),
            y = offsetY.coerceIn(
                (windowBounds.top + popupMargin.top)
                    .coerceAtMost(windowBounds.bottom - popupContentSize.height - popupMargin.bottom),
                windowBounds.bottom - popupContentSize.height - popupMargin.bottom,
            ),
        )
    }

    override fun getMargins(): PaddingValues = margins
}

/**
 * 应用统一的自适应顶栏，完全参照 miuix 官方 demo 的 `AdaptiveTopAppBar` + `BlurredBar` 写法。
 *
 * 两种形态由「设置-外观-渐进模糊」开关 [progressive] 决定：
 * - [progressive] = true：使用官方 [GlassTopAppBar]，渐进模糊由组件内部
 *   `progressiveTextureBlur` 实现（我们不再手写渐进模糊），并随滚动量渐变浮现；
 * - [progressive] = false：回退到原版顶栏（统一 [TopAppBar]，窄屏大标题 / 宽屏由 expandable 收紧），
 *   保留普通 [textureBlur] 毛玻璃。
 *
 * 两种形态共用同一个 [backdrop]（对内容区 `layerBackdrop` 捕获），确保模糊采样一致。
 * 关闭模糊（[backdrop] 为 null）时两种形态都退回纯色。
 *
 * @param title 收起态标题（宽屏紧凑顶栏 / GlassTopAppBar 的紧凑标题）
 * @param largeTitle 展开态大标题（仅原版窄屏 TopAppBar / GlassTopAppBar 使用）
 * @param useLargeTitle 是否使用大标题形态：窄屏 true，宽屏 false
 * @param scrollBehavior 顶栏滚动折叠联动
 * @param backdrop 由 [rememberBlurBackdrop] 创建的背景捕获层，null 表示模糊不可用
 * @param progressive 是否启用官方玻璃渐进模糊顶栏
 * @param color 原版顶栏在模糊不可用时的回退底色
 * @param navigationIcon leading 图标槽
 * @param actions trailing 操作槽
 * @param bottomContent 顶栏底部附加内容槽
 */
@Composable
fun AppTopBar(
    title: String,
    largeTitle: String,
    useLargeTitle: Boolean,
    scrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop?,
    progressive: Boolean,
    color: Color = MiuixTheme.colorScheme.surface,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    val blurActive = backdrop != null
    // 应用风格开关单独决定组件形态（与模糊开关解耦）：
    // 风格=Glass 时用 GlassTopAppBar（backdrop 为 null 时组件内部自动回退纯色填充）；
    // 风格=经典时用 basic TopAppBar。按钮形态也随之切换。
    val glassStyle = progressive
    // 用 CompositionLocal 把形态下发给按钮槽：GlassTopAppBar / 原版 TopAppBar
    // 内部调用这些槽时都会经过此 Provider，按钮据此切换外观。
    val wrappedNavigationIcon: @Composable () -> Unit = {
        androidx.compose.runtime.CompositionLocalProvider(LocalAppTopBarGlass provides glassStyle) {
            navigationIcon()
        }
    }
    val wrappedActions: @Composable RowScope.() -> Unit = {
        androidx.compose.runtime.CompositionLocalProvider(LocalAppTopBarGlass provides glassStyle) {
            actions()
        }
    }
    if (glassStyle) {
        // 官方 demo 写法：GlassTopAppBar 内置渐进模糊（progressiveBlur=true），
        // bandBrush 用表面色生成顶部渐变带，按钮浮在模糊带之上。
        GlassTopAppBar(
            title = title,
            largeTitle = largeTitle,
            expandable = useLargeTitle,
            subtitle = "",
            backdrop = backdrop,
            // 渐进模糊本身仍受模糊总开关约束：backdrop 为空则 GlassTopAppBar 回退纯色带
            progressiveBlur = blurActive,
            scrollBehavior = scrollBehavior,
            defaultWindowInsetsPadding = true,
            navigationIcon = wrappedNavigationIcon,
            actions = wrappedActions,
            bottomContent = bottomContent,
        )
    } else {
        // 原版顶栏：普通纹理模糊层铺在内容下层（官方 demo 的 BlurredBar 非玻璃分支），
        // 顶层放 basic TopAppBar，颜色设为透明让下层模糊透出。
        Box {
            if (blurActive) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .textureBlur(
                            backdrop = backdrop,
                            shape = RectangleShape,
                            blurRadius = AppBlurRadius,
                            colors = blurBarColors(),
                        ),
                )
            }
            val barColor = if (blurActive) Color.Transparent else color
            // 统一用 TopAppBar，通过 expandable 控制大/小标题（窄屏大标题、宽屏紧凑）。
            // 不用 SmallTopAppBar：它专为「只有小标题」的场景设计，本项目窄屏要的是可折叠大标题，
            // 语义与 TopAppBar(expandable=true) 一致；且官方 demo 的经典顶栏同样只用 TopAppBar。
            //（历史背景：miuix 0.9.5-beta01 的 SmallTopAppBarLayout 只 measure actionIcons 却从不
            //  placeRelative，会让 actions 槽（顶栏按钮）完全不可见；0.9.5-beta02 已修复该 bug，
            //  但仍保持统一用法，避免两套顶栏行为分叉。）
            TopAppBar(
                title = title,
                largeTitle = largeTitle,
                expandable = useLargeTitle,
                color = barColor,
                scrollBehavior = scrollBehavior,
                navigationIcon = wrappedNavigationIcon,
                actions = wrappedActions,
                bottomContent = bottomContent,
            )
        }
    }
}

/**
 * 顶栏图标按钮：跟随 [AppTopBar] 当前形态自动切换外观。
 * - 玻璃形态 → [GlassIconButton]（自动继承 GlassTopAppBar 的材质/大小/描边）
 * - 原版形态 → 普通 [IconButton]
 *
 * 按钮放置于 [AppTopBar] 的 `actions` / `navigationIcon` 槽内即可生效。
 * 参照官方 demo 的 `BackNavigationIcon` 写法。
 */
@Composable
fun TopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (LocalAppTopBarGlass.current) {
        GlassIconButton(onClick = onClick, modifier = modifier, content = content)
    } else {
        IconButton(onClick = onClick, modifier = modifier, content = content)
    }
}
