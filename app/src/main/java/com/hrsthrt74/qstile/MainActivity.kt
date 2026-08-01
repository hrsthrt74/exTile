package com.hrsthrt74.qstile

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.hrsthrt74.qstile.ui.theme.ExTileTheme
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用唯一的主 Activity。
 * 参考官方示例：Activity 只负责系统栏处理（enableEdgeToEdge）与 Shizuku 权限监听，
 * 不含任何 UI，UI 骨架全部交由 [MainApp]（见 App.kt）。
 */
class MainActivity : ComponentActivity() {
    companion object {
        /** Shizuku 权限请求码，用于回调中识别请求 */
        private const val REQUEST_CODE_SHIZUKU = 1001
    }

    /** Shizuku 权限是否已授予（Compose 可观察状态） */
    private var shizukuPermissionGranted by mutableStateOf(false)
    /** 权限请求完成后的回调，由 HomeScreen 注册 */
    private var onPermissionResult: ((Boolean) -> Unit)? = null

    /**
     * Shizuku 权限请求结果监听器。
     * 当用户同意或拒绝权限后由 Shizuku 框架回调。
     * 更新本地权限状态，通知 HomeScreen，并弹出 Toast 提示。
     */
    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        shizukuPermissionGranted = granted
        onPermissionResult?.invoke(granted)
        if (granted) {
            Toast.makeText(this, "Shizuku 权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Shizuku 权限被拒绝", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启用边到边显示，让内容延伸到系统栏区域
        enableEdgeToEdge()

        // 注册 Shizuku 权限回调监听
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        // 设置 Compose 内容，外层包裹自定义主题
        setContent {
            MiuixTheme {
                ExTileTheme {
                    MainApp(
                        // 将 Shizuku 权限请求逻辑下传给 HomeScreen
                        onRequestShizukuPermission = { callback ->
                            onPermissionResult = callback
                            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 移除监听器，防止内存泄漏
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }
}
