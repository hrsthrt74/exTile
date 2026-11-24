package com.hrsthrt74.qstile.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.DocsActivity
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.screens.DocsTab
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults
import top.yukonga.miuix.kmp.glass.glassShadow
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 打赏收款码的最大宽度：Sheet 在平板 / 横屏下很宽，直接 fillMaxWidth 会把二维码撑成一整行宽的大图 */
private val DonateQrMaxWidth = 300.dp

/**
 * 打赏 Sheet（微信 / 支付宝收款码）。
 *
 * 设置页的「支持作者」与主页的打赏卡片共用这一个实现，避免两处各写一套收款码界面后走岔。
 * 状态由调用方通过 [state] 控制（[rememberSheetState] 创建，`show()` / `dismiss()`）。
 *
 * 头部用分段 TabRow 切换打赏渠道（0 = 微信 / 1 = 支付宝），两张收款码用 [AnimatedContent] 的
 * 默认动画（淡入淡出 + 轻微缩放）切换。TabRow 的写法照搬「添加磁贴」Sheet
 * （`ui/screens/tileconfig/AddTileSheet.kt`）：
 * - 玻璃风格：`GlassSegmentedTabRow`（胶囊轨道 + 滑块指示器），阴影画在胶囊裁剪之前
 *   （阴影会溢出节点边界，塞进 clip 之内只会被裁成一圈硬边）；
 * - 经典风格：`TabRowWithContour`；
 * - 玻璃控件的采样源必须在 Sheet 内部单独录制，**不能**传页面级 backdrop——那样采样到的是
 *   Sheet 后面的页面内容，看起来像「穿透了 Sheet」。
 *
 * 采样层还必须覆盖到 TabRow 所在的区域，玻璃控件才能取到背景，故沿用添加磁贴的
 * 「头部浮层 + 内容层」结构：头部（感谢文案 + TabRow）作为浮层叠在采样层之上，采样层用
 * 顶部内边距让出头部高度——既是采样源，又不会被自身采样（本层不含 TabRow）。
 *
 * @param state Sheet 的显示状态
 */
@Composable
fun DonateSheet(state: SheetState) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val themeSettings = LocalThemeSettings.current
    // 应用风格开关：true = Glass 全套组件，false = 经典组件
    val glassStyle = themeSettings.progressiveBlur
    // 底部系统导航条留白（与设置页其它 Sheet 的底部间距保持一致）
    val navBarBottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

    // 当前展示的打赏渠道：0 = 微信，1 = 支付宝
    var donateTab by remember { mutableIntStateOf(0) }

    // 两个渠道的 Tab 文案
    val donateTabLabels = listOf(
        stringResource(R.string.settings_donate_tab_wx),
        stringResource(R.string.settings_donate_tab_alipay),
    )

    AppBottomSheet(
        state = state,
        title = stringResource(R.string.settings_donate_title),
        // 关闭动画完成后复位，下次打开总是从微信开始
        onDismissed = { donateTab = 0 },
    ) {
        // 头部（感谢文案 + TabRow）的实测高度（px）：测量后回填成内容层的顶部内边距，
        // 让采样层从 Sheet 内容区顶部开始，从而把 TabRow 所在的区域也包含进去
        var headerHeightPx by remember { mutableIntStateOf(0) }
        val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }

        // 采样源：在 Sheet 内单独录制（模糊关闭 / 不支持时返回 null，TabRow 回退经典样式）
        val headerBackdrop = rememberBlurBackdrop(enabled = glassStyle && themeSettings.enableBlur)

        Box(modifier = Modifier.fillMaxWidth()) {
            // 内容层 = 采样层：收款码 + 打赏声明，顶部让出头部高度（头部浮层在其之外，不被自身采样）
            Box(
                modifier = if (headerBackdrop != null) {
                    Modifier.fillMaxWidth().layerBackdrop(headerBackdrop)
                } else {
                    Modifier.fillMaxWidth()
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = headerHeight, bottom = navBarBottomPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 两张收款码之间切换：直接用 AnimatedContent 的默认动画
                    AnimatedContent(
                        targetState = donateTab,
                        contentAlignment = Alignment.Center,
                        label = "donateTabContent",
                        modifier = Modifier.padding(top = 16.dp)
                    ) { tab ->
                        Image(
                            painter = painterResource(
                                if (tab == 0) R.drawable.pic_donate_wx
                                else R.drawable.pic_donate_alipay
                            ),
                            contentDescription = stringResource(
                                if (tab == 0) R.string.settings_donate_qr_desc_wx
                                else R.string.settings_donate_qr_desc_alipay
                            ),
                            // 宽度 = min(可用宽度, DonateQrMaxWidth)。
                            // **必须先 widthIn 再 fillMaxWidth**：顺序反过来的话，fillMaxWidth
                            // 会先把约束钉成父级宽度，widthIn 的 max 会被强制拉回去（限宽失效）；
                            // 这样写窄屏仍是「铺满可用宽度」，宽屏则停在 DonateQrMaxWidth 上。
                            modifier = Modifier
                                .widthIn(max = DonateQrMaxWidth)
                                .fillMaxWidth()
                                .squircleClip(16.dp)
                        )
                    }
                    // 打赏声明：说明继续打赏即视为同意《用户协议》第 3 节（费用与打赏），且不支持退款。
                    // 「用户协议」为可点击链接，点击后打开 Docs 页并定位到「用户协议」Tab。
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.settings_donate_agree_prefix))
                            // 链接样式：主题色 + 下划线，与 OOBE 页的法律文档链接保持一致
                            withLink(
                                LinkAnnotation.Clickable(
                                    tag = DocsTab.TERMS_OF_SERVICE.name,
                                    styles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = MiuixTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    ),
                                    linkInteractionListener = { link ->
                                        if (link is LinkAnnotation.Clickable) {
                                            context.startActivity(
                                                Intent(context, DocsActivity::class.java).putExtra(
                                                    // EXTRA 以 String 形式传递（枚举序列化后 getStringExtra 取不到），
                                                    // 与 OOBE 页的写法保持一致，避免点击被兜底为隐私政策。
                                                    DocsActivity.EXTRA_TAB,
                                                    DocsTab.TERMS_OF_SERVICE.name
                                                )
                                            )
                                        }
                                    }
                                )
                            ) {
                                append(stringResource(R.string.settings_donate_agree_link_terms))
                            }
                            append(stringResource(R.string.settings_donate_agree_suffix))
                        },
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 12.dp,
                                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                                start = 12.dp,
                                end = 12.dp
                            )
                    )
                }
            }

            // 头部浮层：感谢文案 + 渠道 TabRow（高度回填给上面内容层的顶部内边距）
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { headerHeightPx = it.height },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.settings_donate_thanks),
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (glassStyle && headerBackdrop != null) {
                    // 玻璃风格：官方 GlassSegmentedTabRow（胶囊轨道 + 滑动指示器），
                    // 采样的是 Sheet 内录制的 headerBackdrop，不会被自身内容污染。
                    // 阴影（关键）：玻璃阴影是画在节点边界之外的，塞进 clip 之内只会被裁成一圈硬边，
                    // 故把 glassShadow 挂在外层 Box、clip 在其之后，组件自身传 shadow = null。
                    // 颜色用库自带的 GlassShadows.Float（纯黑低透明），不要贴主题色 / onSurface，
                    // 浅色模式下那会变成一圈亮色、看着像「反色的阴影」。
                    val tabTrackShape = remember {
                        GlassShape(GlassSegmentedTabRowDefaults.Height / 2, smoothing = 0f)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassShadow(tabTrackShape, GlassShadows.Float)
                            .clip(Capsule())
                    ) {
                        GlassSegmentedTabRow(
                            tabs = donateTabLabels,
                            selectedIndex = donateTab,
                            onSelect = { index ->
                                // 只在真正切换时给触感反馈
                                if (index != donateTab) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    donateTab = index
                                }
                            },
                            backdrop = headerBackdrop,
                            // 阴影已在外层画好（见上）
                            shadow = null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    // 经典风格：Miuix 描边分段控件
                    TabRowWithContour(
                        tabs = donateTabLabels,
                        selectedTabIndex = donateTab,
                        onTabSelected = { index ->
                            if (index != donateTab) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                donateTab = index
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
