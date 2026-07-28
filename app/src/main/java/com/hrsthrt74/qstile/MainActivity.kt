package com.hrsthrt74.qstile

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.hrsthrt74.qstile.ui.screens.HomeScreen
import com.hrsthrt74.qstile.ui.screens.SettingsScreen
import com.hrsthrt74.qstile.ui.screens.TileConfigScreen
import com.hrsthrt74.qstile.ui.theme.ExTileTheme
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 应用唯一的主 Activity。
 * 负责初始化 Shizuku 权限监听、设置 Compose 内容、组装页面导航骨架。
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

/**
 * 导航路由封装类。
 * 每个 Screen 定义路由字符串（route）、展示文本（title）及底部导航图标（icon）。
 */
sealed class Screen(val route: String, val title: String) {
    @Composable abstract fun icon(): ImageVector
    /** 主页 Tab — drawable/home_outlined */
    data object Home : Screen("home", "主页") {
        @Composable override fun icon() = ImageVector.vectorResource(R.drawable.home_outlined)
    }
    /** 磁贴编辑 Tab — MIUIX Edit 图标 */
    data object Config : Screen("config", "编辑") {
        @Composable override fun icon() = MiuixIcons.Edit
    }
    /** 设置 Tab — MIUIX Settings 图标 */
    data object SettingPage : Screen("settings", "设置") {
        @Composable override fun icon() = MiuixIcons.Settings
    }
}

/**
 * 应用的 Compose 根组件。
 * 使用 MIUIX 风格的 Scaffold + 底部导航栏 + NavHost 构建三页导航结构。
 *
 * @param onRequestShizukuPermission Shizuku 权限请求的入口方法，接收一个结果回调
 */
@Composable
fun MainApp(
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit
) {
    // 导航控制器，管理页面栈
    val navController = rememberNavController()
    // 观察当前返回栈的顶部路由，用于高亮底部导航项
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = listOf(Screen.Home, Screen.Config, Screen.SettingPage)

    val navigationEventDispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)

    // MIUIX 风格 Scaffold，提供 MIUIX 弹窗宿主（MiuixPopupHost）支持
    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner
    ) {
        MiuixScaffold(
            bottomBar = {
                // 底部导航栏：根据 currentRoute 高亮对应项
                NavigationBar {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        // 返回栈弹出到 Home，避免栈无限增长
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        // 同一目标只保留一个实例
                                        launchSingleTop = true
                                        // 返回时恢复之前的状态
                                        restoreState = true
                                    }
                                }
                            },
                            icon = screen.icon(),
                            label = screen.title
                        )
                    }
                }
            }
        ) { paddingValues ->
            // 导航主体，承载三个页面的路由
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
            ) {
                // 主页：展示 Shizuku 状态 + 当前磁贴配置概览
                composable(Screen.Home.route) {
                    HomeScreen(
                        onRequestShizukuPermission = onRequestShizukuPermission
                    )
                }
                // 编辑页：配置展开/收起时的磁贴列表
                composable(Screen.Config.route) {
                    TileConfigScreen()
                }
                // 设置页：主题配色 + 备份恢复 + 系统导入
                composable(Screen.SettingPage.route) {
                    SettingsScreen()
                }
            }
        }
    }
}