package com.hrsthrt74.qstile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.hrsthrt74.qstile.ui.screens.DebugToolsScreen
import com.hrsthrt74.qstile.ui.theme.ExTileTheme
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner

class DebugToolsActivity : ComponentActivity() {
    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 Shizuku
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        setContent {
            val navigationEventDispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)
            MiuixTheme {
                ExTileTheme {
                    CompositionLocalProvider(
                        LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner
                    ) {
                        DebugToolsScreen()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }
}
