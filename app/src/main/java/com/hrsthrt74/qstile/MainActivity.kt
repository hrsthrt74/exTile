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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hrsthrt74.qstile.data.StatsRepository
import com.hrsthrt74.qstile.data.UpdateChecker
import com.hrsthrt74.qstile.ui.theme.ExTileTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    /** Activity 级别的协程作用域，用于统计数据读取 */
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Shizuku 权限是否已授予（Compose 可观察状态） */
    private var shizukuPermissionGranted by mutableStateOf(false)

    /**
     * 界面是否已可显示：SplashScreen 依据它决定是否继续停留在启动画面上。
     * 用 Activity 级字段（而非 Compose state）让 keepOnScreenCondition 直接读取；
     * 不用 by 委托，保留 MutableState 本体传给 MainApp，在首个真实界面渲染处放行。
     */
    private var appReady = mutableStateOf(false)
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
            Toast.makeText(this, getString(R.string.main_shizuku_granted_toast), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.main_shizuku_denied_toast), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // SplashScreen 必须在 super.onCreate 之前安装：
        // 兼容库在此接管系统启动画面，并提供 keepOnScreenCondition 挂屏能力
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // 启用边到边显示，让内容延伸到系统栏区域
        enableEdgeToEdge()

        // 首个真实界面（OOBE 引导页或主页）就绪前一直停留在启动画面，
        // 避免冷启动时在启动画面与界面之间闪纯色窗口背景帧
        splash.setKeepOnScreenCondition { !appReady.value }

        // 前台启动时初始化 Clarity（仅在用户同意隐私政策后生效）
        val application = applicationContext as? ExTileApplication
        application?.initClarityIfNeeded()

        // 前台启动时上传统计数据到 Clarity（只在前台时上传，后台不上传）
        uploadStatsToClarity()

        // 冷启动时按需自动检查更新（仅当用户开启开关且已完成引导）；
        // 发现新版本由壳层 MainApp 弹窗提示，无更新 / 失败一律静默
        checkForUpdateInBackground()

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
                        },
                        appReady = appReady
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

    /**
     * 上传统计数据到 Clarity。
     * 只在前台启动时调用一次，后台不调用。
     * 读取当前的展开/收起计数，发送到 Clarity 作为自定义标签。
     */
    private fun uploadStatsToClarity() {
        val application = applicationContext as? ExTileApplication ?: return

        activityScope.launch {
            try {
                val stats = StatsRepository.getStats(this@MainActivity)
                application.sendStatsToClarity(stats.totalExpandCount, stats.totalCollapseCount)
            } catch (e: Exception) {
                // 静默失败，不影响用户体验
            }
        }
    }

    /**
     * 冷启动时按需自动检查更新。
     * 内部自行判断开关与 OOBE 状态（未开启 / 引导期间直接跳过），每次进程最多检查一次；
     * 结果写入 UpdateChecker.state：发现新版本由 MainApp 弹对话框，
     * 无更新 / 暂无发布 / 检查失败一律静默，不打扰用户。
     */
    private fun checkForUpdateInBackground() {
        activityScope.launch {
            try {
                UpdateChecker.autoCheckIfNeeded(this@MainActivity)
            } catch (e: Exception) {
                // 静默失败：检查更新是尽力而为的能力，任何异常都不影响正常启动
            }
        }
    }
}
