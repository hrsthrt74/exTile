package com.hrsthrt74.qstile.ui.screens

import android.graphics.Color as AndroidColor
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hrsthrt74.qstile.LegalDocumentsActivity
import io.noties.markwon.Markwon
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 可由外部 Intent 选择的法律文档类型。 */
enum class LegalDocumentType(
    val assetName: String,
    val title: String
) {
    PRIVACY_POLICY("PRIVACY_POLICY.md", "隐私政策"),
    TERMS_OF_SERVICE("TERMS_OF_SERVICE.md", "用户协议");

    companion object {
        /** 未传入或传入未知值时默认展示隐私政策，避免页面为空。 */
        fun fromIntentValue(value: String?): LegalDocumentType {
            return entries.firstOrNull { it.name == value } ?: PRIVACY_POLICY
        }
    }
}

/**
 * 使用 Markwon 渲染仓库 Markdown 文档。
 *
 * Markwon 负责完整解析 Markdown 语法，AndroidView 只承载离线 TextView；
 * 文档内容仍来自 App assets，不会加载远程页面或网络资源。
 */
@Composable
fun LegalDocumentsScreen(documentType: LegalDocumentType) {
    val context = LocalContext.current
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)
    val markdown = remember(documentType) {
        context.assets.open(documentType.assetName).bufferedReader().use { it.readText() }
    }
    val markwon = remember(context) {
        Markwon.builder(context).build()
    }
    val textColor = MiuixTheme.colorScheme.onSurface.toArgb()
    val linkColor = MiuixTheme.colorScheme.primary.toArgb()

    Scaffold(
        topBar = {
            TopAppBar(
                title = documentType.title,
                largeTitle = documentType.title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { (context as? LegalDocumentsActivity)?.finish() }) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(
                    PaddingValues(
                        top = paddingValues.calculateTopPadding() + 24.dp,
                        start = 24.dp,
                        end = 24.dp,
                        bottom = paddingValues.calculateBottomPadding() + 24.dp
                    )
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
}
