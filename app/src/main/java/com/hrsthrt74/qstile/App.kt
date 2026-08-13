package com.hrsthrt74.qstile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.ui.LocalIsWideScreen
import com.hrsthrt74.qstile.ui.MaxContentWidth
import com.hrsthrt74.qstile.ui.blurBarColors
import com.hrsthrt74.qstile.ui.navigation.MainPagerState
import com.hrsthrt74.qstile.ui.navigation.rememberMainPagerState
import com.hrsthrt74.qstile.ui.screens.HomeScreen
import com.hrsthrt74.qstile.ui.screens.OobeScreen
import com.hrsthrt74.qstile.ui.screens.SettingsScreen
import com.hrsthrt74.qstile.ui.screens.TileConfigScreen
import com.hrsthrt74.qstile.ui.shouldShowSplitPane
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailState
import top.yukonga.miuix.kmp.basic.NavigationRailValue
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
 * - 窄屏（竖屏手机）：底部 [NavigationBar] + HorizontalPager，导航栏带毛玻璃模糊
 * - 宽屏（横屏/平板）：侧边 [NavigationRail] + HorizontalPager
 * - 两个布局共享同一份 Pager 状态，切换窗口方向时选中页不丢失
 * - 每个页面（[HomeScreen]/[TileConfigScreen]/[SettingsScreen]）自己再包一层 Scaffold + TopAppBar
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

    // ===== OOBE 已完成 =====
    // Pager 状态管理：在分支之前统一创建，窄屏/宽屏两个布局共享同一份状态，
    // 切换窗口方向（重组切换布局）时选中页不丢失
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { Screen.allPages.size }
    )
    val mainPagerState = rememberMainPagerState(pagerState)

    // 同步页面状态：横滑翻页后把选中页回写到导航高亮
    LaunchedEffect(pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    // 触觉反馈：两个布局的导航项点击都要用，提前取一次共享
    val haptic = LocalHapticFeedback.current

    // 宽屏判定：true=侧边 NavigationRail，false=底部 NavigationBar
    val isWideScreen = shouldShowSplitPane()

    // 注入宽屏标志，供各页面用 contentBottomPadding() 计算底部预留高度
    CompositionLocalProvider(LocalIsWideScreen provides isWideScreen) {
        if (isWideScreen) {
            // 宽屏：侧边导航栏 + Pager（无模糊）
            WideScreenContent(
                pagerState = pagerState,
                mainPagerState = mainPagerState,
                haptic = haptic,
                onRequestShizukuPermission = onRequestShizukuPermission
            )
        } else {
            // 窄屏：底部导航栏 + Pager（带毛玻璃模糊）
            CompactScreenContent(
                blurEnabled = LocalThemeSettings.current.enableBlur,
                pagerState = pagerState,
                mainPagerState = mainPagerState,
                haptic = haptic,
                onRequestShizukuPermission = onRequestShizukuPermission
            )
        }
    }
}

/**
 * 窄屏（竖屏手机）内容布局。
 * 底部 [NavigationBar] + [HorizontalPager]，导航栏带毛玻璃模糊（textureBlur + layerBackdrop）。
 *
 * @param blurEnabled 模糊总开关（来自主题设置）
 * @param pagerState 三个主页共用的 Pager 状态
 * @param mainPagerState 导航与 Pager 的联动状态（选中页高亮）
 * @param haptic 触觉反馈实例，导航项点击时震动
 * @param onRequestShizukuPermission Shizuku 权限请求入口
 */
@Composable
private fun CompactScreenContent(
    blurEnabled: Boolean,
    pagerState: PagerState,
    mainPagerState: MainPagerState,
    haptic: HapticFeedback,
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit
) {
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
        // 内容区域：Pager 挂载 backdrop，滚动内容供导航栏模糊捕获
        AppPager(
            pagerState = pagerState,
            onRequestShizukuPermission = onRequestShizukuPermission,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        )
    }
}

/**
 * 宽屏（横屏/平板）内容布局。
 * 侧边 [NavigationRail] + [HorizontalPager]。state=null 表示固定展开的经典形态
 * （图标+文字、无折叠按钮）；NavigationRail 会自动处理 statusBar/navBar/cutout insets。
 *
 * @param pagerState 三个主页共用的 Pager 状态
 * @param mainPagerState 导航与 Pager 的联动状态（选中页高亮）
 * @param haptic 触觉反馈实例，导航项点击时震动
 * @param onRequestShizukuPermission Shizuku 权限请求入口
 */
@Composable
private fun WideScreenContent(
    pagerState: PagerState,
    mainPagerState: MainPagerState,
    haptic: HapticFeedback,
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 侧边导航栏：state=null → 固定展开的经典形态（图标+文字，无折叠按钮）
        NavigationRail(state = NavigationRailState(NavigationRailValue.Expanded)) {
            Screen.allPages.forEachIndexed { index, screen ->
                NavigationRailItem(
                    selected = mainPagerState.selectedPage == index,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        mainPagerState.animateToPage(index)
                    },
                    icon = screen.icon(),
                    label = screen.title,
                )
            }
        }
        // 剩余区域交给 Pager（宽屏无毛玻璃，内容直铺）。
        // 起始侧（横屏左侧摄像头挖孔/导航条区域）的 insets 已由 NavigationRail 自行避让并铺满背景，
        // 这里在内容区消费掉同样的起始侧 insets，避免各页面自己的 TopAppBar/内容再重复叠加左边距
        // （consumeWindowInsets 会沿 Modifier 树向下传播，windowInsetsPadding 会自动减去已消费部分）。
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .consumeWindowInsets(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                .consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Start))
        ) {
            AppPager(
                pagerState = pagerState,
                onRequestShizukuPermission = onRequestShizukuPermission,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 三个主页的 Pager 内容（窄屏/宽屏两个布局共用）。
 * 用 HorizontalPager 承载横滑切换；beyondViewportPageCount 保留所有页面的状态。
 *
 * @param pagerState Pager 状态
 * @param onRequestShizukuPermission Shizuku 权限请求入口
 * @param modifier 由调用方决定：窄屏挂 backdrop（供导航栏模糊捕获），宽屏直接铺满
 */
@Composable
private fun AppPager(
    pagerState: PagerState,
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = Screen.allPages.size,
        modifier = modifier
    ) { page ->
        // 页面容器：全宽背景铺满（surface，与各 screen 的 Scaffold 背景同色）。
        // Pager 本身保持全宽，翻页动画在整个屏幕上滑动；若把 Pager 直接限宽，
        // 翻页会只在中间一段滑动、超出部分被裁切，因此限宽放在页面容器内部完成。
        Box(modifier = Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)) {
            // 内容限宽并水平居中：平板横屏内容区很宽，限制最大宽度（MaxContentWidth=840dp）
            // 避免摊得过宽影响阅读；小屏/横屏手机宽度未超限则自然铺满，行为不变。
            // 宽度约束由 widthIn 提供，fillMaxHeight 撑满高度，align 负责水平居中
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = MaxContentWidth)
                    .align(Alignment.Center)
            ) {
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
