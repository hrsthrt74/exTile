package com.hrsthrt74.qstile

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.ui.blurBarColors
import com.hrsthrt74.qstile.ui.navigation.rememberMainPagerState
import com.hrsthrt74.qstile.ui.screens.HomeScreen
import com.hrsthrt74.qstile.ui.screens.OobeScreen
import com.hrsthrt74.qstile.ui.screens.SettingsScreen
import com.hrsthrt74.qstile.ui.screens.TileConfigScreen
import kotlinx.coroutines.launch
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
 * 应用的 Compose 根组件（壳层）。
 * 参考 Miuix 官方示例的层级结构：
 * - 底部导航栏 [NavigationBar] 写在壳层，所有页面共享一份
 * - 每个页面（[HomeScreen]/[TileConfigScreen]/[SettingsScreen]）自己再包一层 Scaffold + TopAppBar
 * - 壳层创建 backdrop 捕获内容，导航栏通过 textureBlur 实现毛玻璃
 *
 * @param onRequestShizukuPermission Shizuku 权限请求的入口方法，接收一个结果回调
 */
@Composable
fun MainApp(
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 预热 window insets：Compose 的 WindowInsets 是系统异步派发的，
    // 首次组合读取常为 0（官方文档 "Insets and Jetpack Compose phases"：insets
    // 在组合阶段之后、布局阶段之前更新）。这里在 OOBE 门控之前强制读取一次，
    // 会向系统订阅 insets 变化：真实 insets 派发后触发重组（此时仍被门控 return，
    // 不渲染界面），第二次读取即可拿到真实值并缓存，避免主界面首次组合时读到 0，
    // 导致顶部栏/底部导航栏首帧不避让状态栏和导航栏（启动瞬间闪跳）。
    val density = LocalDensity.current
    WindowInsets.statusBars.getTop(density)
    WindowInsets.navigationBars.getBottom(density)

    // OOBE 完成标志流：initial 用 null 表示「尚未读取到」，
    // 避免已完成的用户首次渲染时闪一下引导页
    val oobeCompleted by ConfigRepository.getOobeCompletedFlow(context)
        .collectAsState(initial = null)
    // 委托属性无法智能转换，取局部值用于后续分支判断
    val oobeDone = oobeCompleted
    // 数据未就绪前不渲染任何 UI，防止闪烁
    if (oobeDone == null) return

    // ===== OOBE 未完成：渲染引导页（壳层不渲染底部导航栏） =====
    // 完成后 saveOobeCompleted(true)，数据流变化触发重组自动切回下方主页 UI
    if (!oobeDone) {
        MiuixScaffold {
            OobeScreen(
                onRequestShizukuPermission = onRequestShizukuPermission,
                onCompleted = {
                    scope.launch {
                        ConfigRepository.saveOobeCompleted(context, true)
                    }
                }
            )
        }
        return
    }

    // ===== OOBE 已完成：现有 UI 原样 =====
    // 读取主题设置，获取模糊开关状态
    val themeSettings by com.hrsthrt74.qstile.data.ThemeRepository.getThemeSettingsFlow(
        androidx.compose.ui.platform.LocalContext.current
    ).collectAsState(initial = com.hrsthrt74.qstile.data.ThemeSettings())
    val blurEnabled = themeSettings.enableBlur

    // 创建模糊背景捕获器，用于抓取导航栏后方的内容像素
    val backdrop = rememberLayerBackdrop()

    // 模糊色彩配置（与顶部栏统一，见 PageUtils）
    val navBarBlurColors = blurBarColors()

    // 导航栏颜色：模糊开启时半透明（配合 textureBlur 的 enabled），关闭时回退纯色
    val navBarColor = if (blurEnabled) {
        MiuixTheme.colorScheme.surface.copy(alpha = 0.1f)
    } else {
        MiuixTheme.colorScheme.surface
    }

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

    // 导航事件 dispatcher 由 ComponentActivity 自动提供（activity 1.13+），
    // 无需手动注入；miuix 弹窗组件内部已注册 NavigationBackHandler（预测式返回）
    val haptic = LocalHapticFeedback.current
    MiuixScaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.textureBlur(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(0.dp),
                    blurRadius = com.hrsthrt74.qstile.ui.AppBlurRadius,
                    colors = navBarBlurColors,
                    enabled = blurEnabled
                ),
                color = navBarColor,
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
