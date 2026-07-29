package com.hrsthrt74.qstile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hrsthrt74.qstile.ui.screens.DebugToolsScreen
import com.hrsthrt74.qstile.ui.theme.ExTileTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme

class DebugToolsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiuixTheme {
                ExTileTheme {
                    DebugToolsScreen()
                }
            }
        }
    }
}
