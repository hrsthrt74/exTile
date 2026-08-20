package com.hrsthrt74.qstile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hrsthrt74.qstile.ui.screens.DocsScreen
import com.hrsthrt74.qstile.ui.screens.DocsTab
import com.hrsthrt74.qstile.ui.theme.ExTileTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 文档聚合页：隐私政策 / 用户协议 / 开源许可 三个 Tab 集合在同一页面。
 *
 * 通过 Intent extra 传入 `EXTRA_TAB`（[DocsTab] 的枚举名）指定初始展示的 Tab；
 * 未传入或值非法时兜底为隐私政策，避免页面为空。
 */
class DocsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialTab = DocsTab.fromValue(intent.getStringExtra(EXTRA_TAB))

        setContent {
            MiuixTheme {
                ExTileTheme {
                    DocsScreen(initialTab = initialTab)
                }
            }
        }
    }

    companion object {
        /** 指定初始 Tab 的 Intent extra，值为 [DocsTab] 的 name。 */
        const val EXTRA_TAB = "extra_tab"
    }
}
