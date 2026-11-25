@file:OptIn(ExperimentalScrollBarApi::class)

package com.hrsthrt74.qstile.ui.screens

import android.graphics.Color as AndroidColor
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 使用 Markwon 渲染仓库 Markdown 文档的内容区（不含 Scaffold / TopAppBar）。
 *
 * 供 DocsScreen 的「隐私政策」「用户协议」两个 Tab 复用：文档内容仍来自 App assets，
 * 不会加载远程页面或网络资源。正文不套卡片，直接铺在页面底色上；取 MiuixTheme 的
 * paragraph 字号与行高，配色随主题，右侧提供滚动条。视图填满整个屏幕，正文边距
 * 由 TextView 自身的内边距留出（而非缩进视图），这样滚动条贴屏幕边缘、内容左右留白。
 * 滚动通过 [scrollBehavior] 与页面的可收起 TopAppBar 联动。
 *
 * @param assetName assets 中的 Markdown 文件名（如 PRIVACY_POLICY.md）。
 * @param scrollBehavior 页面的 TopAppBar 滚动行为，用于嵌套滚动联动收起。
 * @param topContentPadding 顶栏（含 Tab 行）的高度，作为正文的顶部避让；视图本身仍铺满整屏，
 *   滚动时文字可进入顶栏背后供模糊采样，避免把整个视图推到顶栏下方。
 */
@Composable
fun MarkdownDocumentContent(
    assetName: String,
    scrollBehavior: ScrollBehavior,
    topContentPadding: androidx.compose.ui.unit.Dp,
) {
    val context = LocalContext.current

    // 按文档名缓存 Markdown 文本：切换 Tab 再切回时避免重复读取 assets
    val markdown = remember(assetName) {
        context.assets.open(assetName).bufferedReader().use { it.readText() }
    }
    val markwon = remember(context) {
        Markwon.builder(context).build()
    }
    val scrollState = rememberScrollState()

    // 正文颜色 / 链接颜色跟随当前主题
    val textColor = MiuixTheme.colorScheme.onSurface.toArgb()
    val linkColor = MiuixTheme.colorScheme.primary.toArgb()
    // TextView 的 textSize 单位是 sp，取 miuix paragraph 样式的字号数值（17sp）保持与 demo 一致
    val bodyTextSize = MiuixTheme.textStyles.paragraph.fontSize.value
    // 底部避开手势导航栏，多留一点让最后一行不被贴边
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp

    // TextView 的内边距以 px 计：视图本身填满屏幕，正文左右 16dp、顶部 12dp 由它留白。
    // 用 LocalDensity 把 dp 转成 px，跟随系统字体/密度缩放。
    val density = LocalDensity.current
    val contentPaddingLeft = with(density) { 16.dp.toPx().toInt() }
    val contentPaddingRight = with(density) { 16.dp.toPx().toInt() }
    // 顶部避让 = 顶栏高度 + 12dp 留白；交给 TextView 的 padding 而非视图 padding，
    // 这样视图铺满整屏、滚动内容能进入顶栏采样区
    val contentPaddingTop = with(density) { (topContentPadding + 12.dp).toPx().toInt() }
    val contentPaddingBottom = with(density) { bottomInset.toPx().toInt() }

    Box(modifier = Modifier.fillMaxSize()) {
        // AndroidView 只承载离线 TextView，Markwon 负责完整解析 Markdown 语法。
        // 视图 fillMaxSize 铺满整屏不缩进；正文边距交给 TextView.setPadding，
        // 于是滚动条能贴屏幕右边缘，而文字内容仍有左右留白。
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scrollState)
                .scrollEndHaptic(),
            factory = { viewContext ->
                TextView(viewContext).apply {
                    setTextColor(textColor)
                    setLinkTextColor(linkColor)
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    // 字号取 miuix paragraph，行高 1.2 倍，与官方 demo 正文观感一致
                    textSize = bodyTextSize
                    setLineSpacing(0f, 1.2f)
                    setPadding(
                        contentPaddingLeft,
                        contentPaddingTop,
                        contentPaddingRight,
                        contentPaddingBottom
                    )
                    // 允许用户长按复制文档内容，并让 Markdown 链接正常打开
                    setTextIsSelectable(true)
                    movementMethod = LinkMovementMethod.getInstance()
                }
            },
            update = { textView ->
                textView.setTextColor(textColor)
                textView.setLinkTextColor(linkColor)
                textView.textSize = bodyTextSize
                // 主题/导航栏高度变化时同步刷新 TextView 内边距
                textView.setPadding(
                    contentPaddingLeft,
                    contentPaddingTop,
                    contentPaddingRight,
                    contentPaddingBottom
                )
                markwon.setMarkdown(textView, markdown)
            }
        )

        // 右侧滚动条：与正文滚动状态联动，拖动时同步驱动顶栏折叠
        VerticalScrollBar(
            adapter = rememberScrollBarAdapter(scrollState),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            // 轨道顶部同样避让顶栏，滑块从顶栏下方开始
            trackPadding = PaddingValues(top = topContentPadding + 12.dp, bottom = bottomInset),
            topAppBarScrollBehavior = scrollBehavior,
        )
    }
}
