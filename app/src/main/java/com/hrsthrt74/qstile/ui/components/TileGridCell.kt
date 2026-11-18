package com.hrsthrt74.qstile.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.CustomTileUtils
import com.hrsthrt74.qstile.data.TileCatalog
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

/** 圆形底直径：页面内（非浮层），作为拖拽目标时手感更稳。 */
private val TileCircleSizeOnPage = 68.dp

/** 圆形底直径：浮层内（BottomSheet / Dialog），列表更紧凑。 */
private val TileCircleSizeInOverlay = 64.dp

/** 图标尺寸：页面内与浮层内保持一致。 */
private val TileIconSize = 36.dp

/** 图标与下方文字的间距。 */
private val TileIconTextSpacing = 8.dp

/** 圆形底描边宽度：1dp 细线足够勾出边界，再粗会像选中态。 */
private val TileCircleBorderWidth = 1.dp

/** 格子左右/上下留白：同时充当网格内的间距，保证 4 列排布不贴边。 */
private val TileCellHorizontalPadding = 4.dp
private val TileCellVerticalPadding = 12.dp

/**
 * 「当前磁贴格子是否位于浮层（BottomSheet / Dialog）内」的标记。
 *
 * 只影响圆形底的直径（页面内 68dp / 浮层内 64dp），由 [AppBottomSheet] 与 [AppDialog]
 * 在内容区自动下发，调用方不需要手动传尺寸。
 */
internal val LocalTileCellInOverlay = staticCompositionLocalOf { false }

/**
 * 通用磁贴格子：圆形图标底 + 两行文字（名称 / 说明）。
 *
 * 统一了编辑页网格、添加磁贴 Sheet（系统 / 第三方）与调试工具磁贴网格的三套重复写法：
 * - **尺寸自适应**：页面内 68dp、浮层内 64dp，由 [LocalTileCellInOverlay] 自动决定；
 * - **按下反馈**：按下轻微内凹（[SinkFeedback]）+ 长按档触觉，点击不叠加水波纹；
 * - **拖拽可组合**：长按拖拽把手、拖起缩放等交互由调用方通过 [modifier] 挂载，
 *   组件本身不感知排序逻辑；
 * - **标题由调用方决定**：不同来源的磁贴取名规则不同（系统清单名 / 第三方 Service label /
 *   应用名），组件只接收最终文案，不做任何查表。
 *
 * @param icon 磁贴图标（内置磁贴为矢量图，第三方磁贴为位图）
 * @param title 第一行文字：磁贴显示名
 * @param subtitle 第二行文字：系统磁贴显示 value，第三方磁贴显示应用名
 * @param onClick 点击回调（触觉反馈已由组件内部统一处理）
 * @param modifier 外部修饰符；拖拽把手、缩放、透明度等按需在此挂载
 * @param iconTint 图标着色，默认主题色（「移动网络」等个别磁贴由调用方传专用色）
 * @param contentDescription 无障碍描述；为 null 时回落为 [title]
 * @param elevation 圆形底阴影高度：拖拽抬起时由调用方给非 0 值
 * @param onCirclePositioned 圆形底的窗口坐标回调（玻璃菜单按窗口坐标定位时需要）
 * @param circleContent 圆形底内部的叠加内容（如锚定在磁贴上的操作菜单）
 */
@Composable
fun TileGridCell(
    icon: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MiuixTheme.colorScheme.primary,
    contentDescription: String? = null,
    elevation: Dp = 0.dp,
    onCirclePositioned: ((Rect) -> Unit)? = null,
    circleContent: @Composable BoxScope.() -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val inOverlay = LocalTileCellInOverlay.current
    // 浮层内改用紧凑尺寸，调用方无需感知（见 LocalTileCellInOverlay 注释）
    val circleSize = if (inOverlay) TileCircleSizeInOverlay else TileCircleSizeOnPage

    // 圆形底颜色必须按所处层级分档（曾因统一成 surfaceVariant 导致 Sheet 内磁贴「消失」）：
    // - 页面内：surfaceVariant（浅色 #FFFFFF / 深色 #242424），与页面 surface 底有层次；
    // - 浮层内：弹层容器底取的是 background（浅色 #FFFFFF / 深色 #242424），与 surfaceVariant
    //   在明暗两种模式下**完全同值**，圆底会与弹层融为一体、边界彻底看不见。
    //   故改用 secondaryContainer（浅色 #F0F0F0 / 深色 #434343）——与 afdian 项目
    //   「浮层内 Card 统一 overlayColors」用的是同一档容器色，明暗下都能与弹层底分离。
    val circleColor = if (inOverlay) {
        MiuixTheme.colorScheme.secondaryContainer
    } else {
        MiuixTheme.colorScheme.surfaceVariant
    }
    // 描边只在浮层内加：弹层底与圆底只差一档灰，用 1dp 细线把边界勾出来；
    // 页面内圆底与页面底本身有层次，不再描边，否则每个格子都像一张小卡片。
    // 颜色取 onSurface 的 10%（afdian 卡片/胶囊描边同款手法：暗色自动提亮、亮色自动压暗），
    // 不写死 outline——深色下 outline #404040 与圆底 #434343 几乎同值，等于没描。
    val circleBorderModifier = if (inOverlay) {
        Modifier.border(
            width = TileCircleBorderWidth,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.10f),
            shape = CircleShape
        )
    } else {
        Modifier
    }

    // 只在需要时挂坐标上报，避免页面内每个格子都做一次窗口坐标计算
    val positionReporter = if (onCirclePositioned != null) {
        Modifier.onGloballyPositioned { coords -> onCirclePositioned(coords.boundsInWindow()) }
    } else {
        Modifier
    }

    Column(
        modifier = modifier.padding(
            horizontal = TileCellHorizontalPadding,
            vertical = TileCellVerticalPadding
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .then(positionReporter)
                // 按下内凹反馈：与水波纹二选一，所以下面的 clickable 不传 indication
                .pressable(
                    interactionSource = null,
                    indication = SinkFeedback(sinkAmount = 0.9f, animationSpec = spring(0.8f, 120f)),
                    delay = null
                )
                .shadow(elevation, CircleShape)
                // 底与描边都按圆形绘制。注意这里不用 clip(CircleShape)：clip 会把它之后的绘制
                // 一并裁到圆内，末尾 1dp 描边会被裁掉外侧一半，只剩半像素灰边，等于白描。
                .background(circleColor, CircleShape)
                .then(circleBorderModifier)
                .clickable(interactionSource = null, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = contentDescription ?: title,
                tint = iconTint,
                modifier = Modifier.size(TileIconSize)
            )

            // 叠加层：编辑页的磁贴操作菜单锚定在这个圆形底上，故必须绘制在其内部
            circleContent()
        }

        Spacer(modifier = Modifier.height(TileIconTextSpacing))

        // 第一行：磁贴显示名
        Text(
            text = title,
            style = MiuixTheme.textStyles.body2,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // 第二行：系统磁贴显示 value；第三方磁贴显示应用名
        Text(
            text = subtitle,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 取磁贴图标的通用实现（编辑页 / 添加磁贴 Sheet / 调试工具共用）。
 *
 * 内置磁贴查静态清单；custom 磁贴运行时取第三方 Service 自身图标；
 * 拿不到时一律回退空白占位图标（tile_blank，不回退应用图标——染色后会变成实心圆角矩形），
 * 保证始终有图标可渲染。
 *
 * @param tile 磁贴 value（如 `wifi`、`custom(包名/类名)`）
 */
@Composable
fun rememberTileIcon(tile: String): Painter {
    val context = LocalContext.current
    // 内置磁贴：静态清单直查
    TileCatalog.iconRes(tile)?.let { return painterResource(it) }
    // custom 磁贴：getCustomTileIcon 内部已保证非空（blank 兜底）；其他未知磁贴值直接用 blank 占位
    val drawable = remember(tile) {
        val custom = if (tile.startsWith("custom(")) {
            CustomTileUtils.getCustomTileIcon(context, tile)
        } else {
            null
        }
        custom ?: context.getDrawable(R.drawable.tile_blank)!!
    }
    val bitmap = remember(drawable) { drawable.toBitmap() }
    return remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
}
