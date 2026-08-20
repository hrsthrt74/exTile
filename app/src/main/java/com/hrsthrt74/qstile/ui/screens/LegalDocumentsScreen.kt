package com.hrsthrt74.qstile.ui.screens

import android.graphics.Color as AndroidColor
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 使用 Markwon 渲染仓库 Markdown 文档的内容区（不含 Scaffold / TopAppBar）。
 *
 * 供 DocsScreen 的「隐私政策」「用户协议」两个 Tab 复用：文档内容仍来自 App assets，
 * 不会加载远程页面或网络资源。滚动通过 [scrollBehavior] 与页面的可收起 TopAppBar 联动。
 *
 * @param assetName assets 中的 Markdown 文件名（如 PRIVACY_POLICY.md）。
 * @param scrollBehavior 页面的 TopAppBar 滚动行为，用于嵌套滚动联动收起。
 */
@Composable
fun MarkdownDocumentContent(assetName: String, scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current

    // 按文档名缓存 Markdown 文本：切换 Tab 再切回时避免重复读取 assets。
    val markdown = remember(assetName) {
        context.assets.open(assetName).bufferedReader().use { it.readText() }
    }
    val markwon = remember(context) {
        Markwon.builder(context).build()
    }
    val textColor = MiuixTheme.colorScheme.onSurface.toArgb()
    val linkColor = MiuixTheme.colorScheme.primary.toArgb()

    // AndroidView 只承载离线 TextView，Markwon 负责完整解析 Markdown 语法。
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(
                top = 8.dp,
                start = 8.dp,
                end = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 48.dp
            ),
        factory = { viewContext ->
            TextView(viewContext).apply {
                setTextColor(textColor)
                setLinkTextColor(linkColor)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                textSize = 16f
                setLineSpacing(0f, 1.15f)
                setPadding(0, 0, 0, 0)
                // 允许用户长按复制文档内容，并让 Markdown 链接正常打开。
                setTextIsSelectable(true)
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            markwon.setMarkdown(textView, markdown)
        }
    )
}