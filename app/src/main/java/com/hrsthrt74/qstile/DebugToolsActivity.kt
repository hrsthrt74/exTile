package com.hrsthrt74.qstile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hrsthrt74.qstile.ui.screens.DebugToolsScreen
import com.hrsthrt74.qstile.ui.theme.ExTileTheme
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.theme.MiuixTheme

class DebugToolsActivity : ComponentActivity() {
    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 Shizuku
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        setContent {
            // 导航事件 dispatcher 由 ComponentActivity 自动提供（activity 1.13+），
            // 无需手动注入；弹窗组件的预测式返回（NavigationBackHandler）依赖它
            MiuixTheme {
                ExTileTheme {
                    DebugToolsScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }
}
