package com.hrsthrt74.qstile

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.hrsthrt74.qstile.ui.navigation.rememberMainPagerState
import com.hrsthrt74.qstile.ui.screens.HomeScreen
import com.hrsthrt74.qstile.ui.screens.SettingsScreen
import com.hrsthrt74.qstile.ui.screens.TileConfigScreen
import com.hrsthrt74.qstile.ui.theme.ExTileTheme
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
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
 * 每个 Screen 定义展示文本（title）及底部导航图标（icon）。
 */
sealed class Screen(val title: String) {
    @Composable abstract fun icon(): ImageVector
    /** 主页 Tab — drawable/home_outlined */
    data object Home : Screen("主页") {
        @Composable override fun icon() = ImageVector.vectorResource(R.drawable.home_outlined)
    }
    /** 磁贴编辑 Tab — MIUIX Edit 图标 */
    data object Config : Screen("编辑") {
        @Composable override fun icon() = MiuixIcons.Edit
    }
    /** 设置 Tab — MIUIX Settings 图标 */
    data object SettingPage : Screen("设置") {
        @Composable override fun icon() = MiuixIcons.Settings
    }

    companion object {
        val allPages = listOf(Home, Config, SettingPage)
    }
}

/**
 * 应用的 Compose 根组件。
 * 使用 MIUIX 风格的 Scaffold + 底部导航栏 + HorizontalPager 构建三页导航结构。
 *
 * @param onRequestShizukuPermission Shizuku 权限请求的入口方法，接收一个结果回调
 */
@Composable
fun MainApp(
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit
) {
    val navigationEventDispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)

    // 创建模糊背景捕获器，用于抓取导航栏后方的内容像素
    val backdrop = rememberLayerBackdrop()

    // 模糊色彩配置
    val navBarBlurColors = BlurDefaults.blurColors(
        blendColors = listOf(
            BlendColorEntry(MiuixTheme.colorScheme.surface.copy(alpha = 0.7f), BlurBlendMode.SrcOver)
        ),
        brightness = 0.05f,
        contrast = 1.1f,
        saturation = 1.2f
    )

    // Pager 状态管理
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { Screen.allPages.size }
    )
    val mainPagerState = rememberMainPagerState(pagerState)

    // 同步页面状态
    LaunchedEffect(pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner
    ) {
        val haptic = LocalHapticFeedback.current
        MiuixScaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = RoundedCornerShape(0.dp),
                        blurRadius = 80f,
                        colors = navBarBlurColors
                    ),
                    color = MiuixTheme.colorScheme.surface.copy(alpha = 0.1f),
                    showDivider = true
                ) {
                    Screen.allPages.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            selected = mainPagerState.selectedPage == index,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                mainPagerState.animateToPage(index)
                            },
                            icon = screen.icon(),
                            label = screen.title
                        )
                    }
                }
            }
        ) {
            // 内容区域使用 HorizontalPager 实现横滑切换
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = Screen.allPages.size,
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        onRequestShizukuPermission = onRequestShizukuPermission
                    )
                    1 -> TileConfigScreen()
                    2 -> SettingsScreen()
                }
            }
        }
    }
}