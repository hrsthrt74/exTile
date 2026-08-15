package com.hrsthrt74.qstile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hrsthrt74.qstile.ui.screens.LegalDocumentType
import com.hrsthrt74.qstile.ui.screens.LegalDocumentsScreen
import com.hrsthrt74.qstile.ui.theme.ExTileTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 离线法律文档页面。
 *
 * 文档由 Gradle 从仓库 docs/ 目录复制到 assets，Activity 不访问网络，
 * 因此即使设备完全断网也能查看隐私政策和用户协议。
 */
class LegalDocumentsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val documentType = LegalDocumentType.fromIntentValue(
            intent.getStringExtra(EXTRA_DOCUMENT_TYPE)
        )

        setContent {
            MiuixTheme {
                ExTileTheme {
                    LegalDocumentsScreen(documentType = documentType)
                }
            }
        }
    }

    companion object {
        const val EXTRA_DOCUMENT_TYPE = "document_type"
    }
}
