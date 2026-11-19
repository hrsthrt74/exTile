package com.hrsthrt74.qstile

import androidx.annotation.StringRes
import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.glass.glassPress
import top.yukonga.miuix.kmp.material.GlassColorBlendMode
import top.yukonga.miuix.kmp.material.GlassColorLayer
import top.yukonga.miuix.kmp.material.GlassMaterial
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.drawEdgeStroke
import top.yukonga.miuix.kmp.material.drawGlassMask
import android.net.Uri
// kyant shapes：连续曲率胶囊轮廓（玻璃底栏使用）
import com.kyant.shapes.Capsule
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.UpdateCheckState
import com.hrsthrt74.qstile.data.UpdateChecker
import com.hrsthrt74.qstile.ui.LocalIsWideScreen
import com.hrsthrt74.qstile.ui.LocalNavPageAction
import com.hrsthrt74.qstile.ui.NavPageActionState
import com.hrsthrt74.qstile.ui.AppBlurRadius
import com.hrsthrt74.qstile.ui.LocalTopLayer
import com.hrsthrt74.qstile.ui.MaxContentWidth
import com.hrsthrt74.qstile.ui.TopLayerHost
import com.hrsthrt74.qstile.ui.TopLayerState
import com.hrsthrt74.qstile.ui.blurBarColors
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.toGlassShape
import com.hrsthrt74.qstile.ui.components.AppDialog
import com.hrsthrt74.qstile.ui.components.LocalOverlayBackdrop
import com.hrsthrt74.qstile.ui.components.rememberDialogState
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
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailState
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationBarDefaults
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.glass.GlassNavigationRail
import top.yukonga.miuix.kmp.glass.GlassNavigationRailDefaults
import top.yukonga.miuix.kmp.glass.rememberGlassNavigationRailState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.glass.Add
import top.yukonga.miuix.kmp.icon.glass.All
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.icon.glass.HomeFill
import top.yukonga.miuix.kmp.icon.glass.SettingsFill

/**
 * 导航路由封装类。
 * 每个 Screen 定义标题字符串资源（titleRes）及底部导航图标（icon）。
 * 标题持有 @StringRes 而非字面量，使用处在 Composable 上下文经 stringResource 解析，
 * 以支持多语言（i18n）。
 */
sealed class Screen(@StringRes val titleRes: Int) {
    @Composable abstract fun icon(): ImageVector
    /** 主页 Tab — drawable/home_outlined */
    data object Home : Screen(R.string.nav_home) {
        @Composable override fun icon() = MiuixIcons.Glass.HomeFill
    }
    /** 磁贴编辑 Tab — MIUIX Edit 图标 */
    data object Config : Screen(R.string.nav_config) {
//        @Composable override fun icon() = MiuixIcons.Edit
        @Composable override fun icon() = MiuixIcons.Glass.All
    }
    /** 设置 Tab — MIUIX Settings 图标 */
    data object SettingPage : Screen(R.string.nav_settings) {
        @Composable override fun icon() = MiuixIcons.Glass.SettingsFill
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
 * @param appReady 应用界面是否已就绪（由 MainActivity 持有）：OOBE 配置读取完成、
 * 首个真实界面渲染前保持 false，让 SplashScreen 持续停留在启动画面
 */
@Composable
fun MainApp(
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit,
    appReady: MutableState<Boolean>
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

    // 走到这里说明 OOBE 配置已读取完成，引导页/主页即将渲染首个真实界面：
    // 放行 SplashScreen，启动画面无缝过渡到界面，不再闪纯色窗口背景帧
    appReady.value = true

    // ===== OOBE 未完成：渲染引导页（壳层不渲染底部导航栏） =====
    // 完成后 saveOobeCompleted(true)，数据流变化触发重组自动切回下方主页 UI
    if (!oobeDone) {
        MiuixScaffold {
            OobeScreen(
                onRequestShizukuPermission = onRequestShizukuPermission,
                onCompleted = {
                    scope.launch {
                        ConfigRepository.saveOobeCompleted(context, true)
                        // 如果用户同意了隐私政策，初始化 Clarity
                        // 注意：这里在 OOBE 完成后才初始化，而非用户选择开关时
                        // 确保用户有机会完成整个引导流程后再开始数据收集
                        val application = context.applicationContext as? ExTileApplication
                        application?.initClarityIfNeeded()
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

    // 宽屏判定：true=侧边导航，false=底部导航
    val isWideScreen = shouldShowSplitPane()
    val themeSettings = LocalThemeSettings.current
    // 模糊总开关：决定玻璃导航是否对背后内容采样（关闭时玻璃组件回退为纯色容器）
    val blurEnabled = themeSettings.enableBlur
    // 应用风格开关（复用「渐进模糊」开关）：true=Glass 导航，false=经典 NavigationBar/Rail
    val glassStyle = themeSettings.progressiveBlur

    // 顶层浮层状态：玻璃弹窗（GlassIconDropdownMenu）通过 LocalTopLayer 提升到这里，
    // 保证其窗口坐标与层级正确、不被顶栏/玻璃侧栏遮挡或被 Pager 页面裁剪。
    val topLayerState = remember { TopLayerState() }

    // 页面 → 底部导航栏的动作桥：磁贴编辑页把「添加磁贴」入口注册进来，
    // 窄屏玻璃底栏在选中编辑页时把它渲染成 trailingAction（跟随按钮）。
    val navPageAction = remember { NavPageActionState() }

    // 注入宽屏标志（供 contentBottomPadding 计算）与顶层浮层宿主。
    // TopLayerHost 放在导航内容之后绘制，位于所有 chrome 之上。
    CompositionLocalProvider(
        LocalIsWideScreen provides isWideScreen,
        LocalTopLayer provides topLayerState,
        LocalNavPageAction provides navPageAction,
    ) {
        // 全局唯一的 root Scaffold（官方 demo 同款）：任何方向的界面都套在同一层 Scaffold 内，
        // 让 renderInRootScaffold=true 的玻璃 Dialog/Sheet 提升到这里（全屏、z-index 最高，
        // 在玻璃侧栏之上）。此前宽屏分支只有纯 Box，dialog 只能落到各页面自己被 widthIn(840dp)
        // 限宽的 Scaffold 里且被全屏 rail overlay 盖住，表现为「点菜单项后对话框没反应」。
        // containerColor 透明 + contentWindowInsets=0：不改布局/避让，各页面自行处理 insets。
        MiuixScaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isWideScreen) {
                    // 宽屏：侧边导航栏（随风格切换玻璃 / 经典）+ Pager
                    WideScreenContent(
                        glassStyle = glassStyle,
                        blurEnabled = blurEnabled,
                        pagerState = pagerState,
                        mainPagerState = mainPagerState,
                        haptic = haptic,
                        onRequestShizukuPermission = onRequestShizukuPermission
                    )
                } else {
                    // 窄屏：底部导航栏（随风格切换玻璃 / 经典）+ Pager
                    CompactScreenContent(
                        glassStyle = glassStyle,
                        blurEnabled = blurEnabled,
                        pagerState = pagerState,
                        mainPagerState = mainPagerState,
                        haptic = haptic,
                        onRequestShizukuPermission = onRequestShizukuPermission
                    )
                }

                // 顶层玻璃弹窗宿主：绘制在导航 chrome 之上（官方 demo 同款层级）
                TopLayerHost(topLayerState)
            }
        }
    }

    // 自动检查更新的「发现新版本」对话框（Window 级悬浮，与页面布局无关）：
    // 放在 OOBE 完成分支内，引导期间不弹窗
    UpdatePromptHost()
}

/**
 * 自动检查更新的「发现新版本」对话框宿主。
 *
 * 自动检查由 MainActivity 冷启动时执行，结果写入 [UpdateChecker.state]；
 * 这里只负责把结果转成 UI：仅当出现 [UpdateCheckState.Available] 且本进程尚未提示过时弹窗。
 * 无更新 / 仓库暂无发布 / 检查失败等其余状态一律静默，不弹任何提示。
 */
@Composable
private fun UpdatePromptHost() {
    val context = LocalContext.current
    val dialogState = rememberDialogState()
    val checkState by UpdateChecker.state.collectAsState()

    // 仅响应「发现新版本」：updatePromptShown 是进程级标记，
    // 防止「重新运行引导」等导致本组件重建的场景重复弹窗
    LaunchedEffect(checkState) {
        if (checkState !is UpdateCheckState.Available) return@LaunchedEffect
        if (UpdateChecker.updatePromptShown) return@LaunchedEffect
        UpdateChecker.updatePromptShown = true
        dialogState.show()
    }

    // 对话框内容取自当前状态；仅在本组件存在（OOBE 已完成）且 state 为 Available 时有值
    val available = checkState as? UpdateCheckState.Available
    // 更新日志可能为空（Release 没写说明），提前解析兜底文案供 ifBlank 使用（非 Composable lambda 内不能用 stringResource）
    val noChangelogText = stringResource(R.string.update_dialog_no_changelog)

    AppDialog(
        state = dialogState,
        title = stringResource(R.string.update_dialog_title),
        summary = available?.let {
            stringResource(R.string.update_dialog_summary, it.latestVersion)
        },
        confirmText = stringResource(R.string.update_dialog_confirm),
        cancelText = stringResource(R.string.update_dialog_cancel),
        onConfirm = {
            // 跳转 GitHub Release 页面；设备没有浏览器时静默失败
            available?.releaseUrl?.let { url ->
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        }
    ) {
        // 更新日志原文（Markdown 源文本按纯文本展示），限高可滚动，避免日志过长撑爆对话框
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = available?.changelog?.ifBlank { noChangelogText } ?: "",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}

/**
 * 玻璃导航栏材质的动态色补丁，等价官方 miuix 新版的 `dynamicGlassMaterial()`（internal，
 * v0.9.5-beta01 尚未暴露）：Monet 动态色开启时，把材质的 third 层换成
 * `surfaceContainerHigh`（alpha 0.6 + SrcOver），胶囊底色从而跟随主题；关闭时返回原始材质。
 *
 * TODO(miuix): 待 miuix 升级到 0.9.5-beta01 之后的版本（官方已在 GlassNavigationBarDefaults.material()
 * 内部接入 dynamicGlassMaterial），删除本函数，并移除 GlassNavigationBar(...) 处的 `material = ...` 覆盖，
 * 换回官方默认写法。同时清理仅为本补丁引入的 GlassColorLayer / GlassColorBlendMode / GlassMaterial import。
 */
@Composable
private fun dynamicNavBarMaterial(): GlassMaterial {
    val base = GlassNavigationBarDefaults.material()
    val tint = MiuixTheme.colorScheme.surfaceContainerHigh
    return if (MiuixTheme.isDynamicColor) {
        remember(base, tint) {
            base.copy(third = GlassColorLayer(tint.copy(alpha = 0.6f), GlassColorBlendMode.SrcOver))
        }
    } else {
        base
    }
}

/**
 * 窄屏（竖屏手机）内容布局，底部导航随应用风格开关切换：
 * - [glassStyle]=true：[GlassNavigationBar]（悬浮玻璃胶囊，官方 demo 写法）
 * - [glassStyle]=false：经典 [NavigationBar] + textureBlur 毛玻璃
 *
 * @param glassStyle 应用风格（复用「渐进模糊」开关）：true=Glass，false=经典
 * @param blurEnabled 模糊总开关（来自主题设置）
 * @param pagerState 三个主页共用的 Pager 状态
 * @param mainPagerState 导航与 Pager 的联动状态（选中页高亮）
 * @param haptic 触觉反馈实例，导航项点击时震动
 * @param onRequestShizukuPermission Shizuku 权限请求入口
 */
@Composable
private fun CompactScreenContent(
    glassStyle: Boolean,
    blurEnabled: Boolean,
    pagerState: PagerState,
    mainPagerState: MainPagerState,
    haptic: HapticFeedback,
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit
) {
    // 捕获内容像素的 backdrop（内部已判断模糊开关与 RuntimeShader 支持）
    val backdrop = rememberBlurBackdrop(enabled = blurEnabled)

    // 底部导航栏的「跟随按钮」（官方 demo 的 trailingAction 写法）：
    // 仅当处于玻璃风格、且当前选中「磁贴编辑」页、并且该页已把「添加磁贴」动作注册进来时才显示。
    // 经典 NavigationBar 没有 trailingAction 槽，故经典风格不提供（入口仍在网格末尾的按钮）。
    val navPageAction = LocalNavPageAction.current
    val configPageIndex = Screen.allPages.indexOf(Screen.Config)
    val onAddTile = navPageAction?.onAddTile
    val navTrailingAction: (@Composable () -> Unit)? =
        if (glassStyle && mainPagerState.selectedPage == configPageIndex && onAddTile != null) {
            {
                // 「添加磁贴」跟随按钮：主色实心圆 + 反白图标，尺寸沿用官方 example 的
                // 「NavigationBar search button」（直径 = 胶囊高度，图标 28dp）。
                // 按钮改为主色实心后不再需要玻璃材质，故直接用 Box 绘制，并挂 miuix 自带的
                // Modifier.glassPress 做「按压下沉」（缩放量按短边换算，松手回弹，无需自己写动画）。
                val pressSource = remember { MutableInteractionSource() }
                // 按钮轮廓（圆形 squircle）与官方按钮同款描边（TopAppBar 小号 bloom stroke）
                val buttonShape = GlassShape(GlassNavigationBarDefaults.Height / 2)
                val buttonStroke = GlassTopAppBarDefaults.buttonStroke()
                Box(
                    modifier = Modifier
                        .size(GlassNavigationBarDefaults.Height)
                        // 下沉动画作用在背景 + 内容上（graphicsLayer 缩放整层）
                        .glassPress(pressSource)
                        .clickable(
                            interactionSource = pressSource,
                            indication = null,
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddTile()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // 底色层：主色填充 + 玻璃描边高光 + 玻璃遮罩
                    // （照搬 GlassIconButton 在「无 backdrop」时的绘制：drawEdgeStroke + drawGlassMask，
                    //   DstIn 遮罩需要独立离屏层，否则会把整个窗口内容一起裁掉）
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .background(MiuixTheme.colorScheme.primary, buttonShape)
                            .drawWithContent {
                                drawContent()
                                drawEdgeStroke(buttonShape, buttonStroke, 1f)
                                drawGlassMask(buttonShape)
                            }
                    )
                    Icon(
                        imageVector = MiuixIcons.Glass.Add,
                        contentDescription = stringResource(R.string.config_add_desc),
                        modifier = Modifier.size(28.dp),
                        tint = MiuixTheme.colorScheme.onPrimary,
                    )
                }
            }
        } else {
            null
        }

    // 底栏左右留白：
    // - 常态（无跟随按钮）= 64dp，胶囊不会拉得太长；
    // - 出现跟随按钮时 = 24dp，给按钮腾出空间，避免胶囊被压得过短。
    // 用无回弹的 spring 过渡（与官方 navShowHide 同风格的临界阻尼），切页时宽度平滑变化而不是突跳。
    val barSidePadding by animateDpAsState(
        targetValue = if (navTrailingAction != null) 24.dp else 64.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow),
        label = "navBarSidePadding",
    )

    // 玻璃风格 item 模型（icon/label）；经典风格直接用 NavigationBarItem
    val glassItems = Screen.allPages.map { screen ->
        GlassNavigationItem(
            icon = screen.icon(),
            label = stringResource(screen.titleRes),
        )
    }

    // 页面 backdrop 下发给各屏幕内的玻璃 Dialog / BottomSheet 采样。
    // 该 backdrop 挂在 AppPager 上，覆盖整页含顶栏，但不含提升到根 Scaffold 的弹窗宿主与遮罩，
    // 符合官方 demo 对 glass 弹窗 backdrop 的要求（见 AppDialog.kt 的 LocalOverlayBackdrop）。
    CompositionLocalProvider(LocalOverlayBackdrop provides backdrop) {
        MiuixScaffold(
            bottomBar = {
                if (glassStyle) {
                    // 悬浮底栏图层下方的渐变：顶部透明 → 底部 surface（上透下不透），
                    // 让滚动内容在接近底栏时淡出到 surface 色，胶囊浮其上更自然。
                    // 仅 OS4 玻璃风格显示；经典 NavigationBar 为全宽实色，不需要。
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // 渐变绘制区域向上多延伸 8dp（不改布局尺寸，不影响胶囊位置）：
                        // startY 取 -8px、topLeft 上移 8px，高度相应 +8px，底边仍对齐槽底。
                        val surfaceColor = MiuixTheme.colorScheme.surface
                        val extendUpPx = with(LocalDensity.current) { 8.dp.toPx() }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .drawBehind {
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                surfaceColor.copy(alpha = 0f),
                                                surfaceColor.copy(alpha = 0.7f),
                                                surfaceColor.copy(alpha = 0.9f),
                                            ),
                                            startY = -extendUpPx,
                                            endY = size.height,
                                        ),
                                        topLeft = Offset(0f, -extendUpPx),
                                        size = Size(size.width, size.height + extendUpPx),
                                    )
                                }
                        )
                        GlassNavigationBar(
                            items = glassItems,
                            selectedIndex = mainPagerState.selectedPage,
                            onSelect = { index ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                mainPagerState.animateToPage(index)
                            },
                            backdrop = backdrop,
                            // 材质补上官方新版的 dynamicGlassMaterial 逻辑：Monet 开启时把 third 层
                            // 换成 surfaceContainerHigh 染色，胶囊底色才会跟着主题走（v0.9.5-beta01
                            // 的 GlassNavigationBarDefaults.material() 尚无这层，故这里手动补）。
                            material = dynamicNavBarMaterial(),
                            // 胶囊轮廓改用 kyant shapes 的 Capsule()：
                            // miuix 只接受 GlassShape，故用 toGlassShape() 把 Capsule 的几何 + 连续曲率
                            // 翻译过去（Capsule = 最小边长一半的圆角，smoothing=1f），轮廓与官方默认一致，
                            // 但由 kyant 定义来源，后续要换 RoundedRectangle 等形状时只改这一行。
                            // 注意：**不能**在 modifier 上直接 clip(Capsule())——底栏是「胶囊 + 胶囊外
                            // 跟随按钮」一整个节点，外层裁剪会把「添加磁贴」按钮和玻璃阴影一起裁掉。
                            shape = remember { Capsule().toGlassShape() },
                            modifier = Modifier
                                .fillMaxWidth()
                                // 留白随跟随按钮出现与否变化（动画过渡，见 barSidePadding）
                                .padding(horizontal = barSidePadding),
                            // 官方 demo 写法：trailingAction 是胶囊之外、自带材质的独立圆形按钮
                            trailingAction = navTrailingAction,
                        )
                    }
                } else {
                    // 经典风格：全宽 NavigationBar + 普通纹理模糊（模糊关闭时回退纯色）
                    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
                    NavigationBar(
                        modifier = if (backdrop != null) {
                            Modifier.textureBlur(
                                backdrop = backdrop,
                                shape = RoundedCornerShape(0.dp),
                                blurRadius = AppBlurRadius,
                                colors = blurBarColors(),
                            )
                        } else Modifier,
                        color = barColor,
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
                                label = stringResource(screen.titleRes)
                            )
                        }
                    }
                }
            }
        ) {
            // 内容区域：Pager 挂载 backdrop，滚动内容供导航栏采样（玻璃/经典模糊均需要）
            AppPager(
                pagerState = pagerState,
                onRequestShizukuPermission = onRequestShizukuPermission,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
            )
        }
    }
}

/**
 * 宽屏（横屏/平板）内容布局，侧边导航随应用风格开关切换：
 * - [glassStyle]=true：[GlassNavigationRail] 悬浮玻璃侧栏（覆盖在页面之上，官方 demo 写法）
 * - [glassStyle]=false：经典 [NavigationRail]（占据一列，图标+文字固定展开）
 *
 * @param glassStyle 应用风格（复用「渐进模糊」开关）：true=Glass，false=经典
 * @param blurEnabled 模糊总开关（来自主题设置）
 * @param pagerState 三个主页共用的 Pager 状态
 * @param mainPagerState 导航与 Pager 的联动状态（选中页高亮）
 * @param haptic 触觉反馈实例，导航项点击时震动
 * @param onRequestShizukuPermission Shizuku 权限请求入口
 */
@Composable
private fun WideScreenContent(
    glassStyle: Boolean,
    blurEnabled: Boolean,
    pagerState: PagerState,
    mainPagerState: MainPagerState,
    haptic: HapticFeedback,
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit
) {
    val backdrop = rememberBlurBackdrop(enabled = blurEnabled)
    // 页面共用的导航点击回调
    val onPageSelect: (Int) -> Unit = { index ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        mainPagerState.animateToPage(index)
    }

    if (!glassStyle) {
        // ===== 经典风格：侧栏占一列，Pager 占剩余空间（原版行为） =====
        CompositionLocalProvider(LocalOverlayBackdrop provides backdrop) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(state = NavigationRailState(NavigationRailValue.Expanded)) {
                    Screen.allPages.forEachIndexed { index, screen ->
                        NavigationRailItem(
                            selected = mainPagerState.selectedPage == index,
                            onClick = { onPageSelect(index) },
                            icon = screen.icon(),
                            label = stringResource(screen.titleRes),
                        )
                    }
                }
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
        return
    }

    // ===== 玻璃风格：侧栏为 overlay，页面按展开进度让出宽度 =====
    // 固定展开的经典形态：显示图标+文字、无折叠按钮
    val railState = rememberGlassNavigationRailState(initialExpanded = true)
    val glassItems = Screen.allPages.map { screen ->
        GlassNavigationItem(
            icon = screen.icon(),
            label = stringResource(screen.titleRes),
        )
    }

    CompositionLocalProvider(LocalOverlayBackdrop provides backdrop) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 对齐官方 demo 的关键点：backdrop 录制层保持全宽，不被让位压缩或平移。
            // 侧栏是覆盖在页面之上的 overlay，采样的是这一整层画面；若把 layerBackdrop 挂在
            // 被 layout 压缩右移后的 AppPager 上，侧栏所在的起始侧区域就不在录制层内，
            // 玻璃采样会错位/发虚（demo 注释原话：pager stays full width for backdrop recording）。
            // 外层 surface 背景：页面让位后露出的起始侧空隙填成与页面一致的表面色（demo 同款）。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                ) {
                    AppPager(
                        pagerState = pagerState,
                        onRequestShizukuPermission = onRequestShizukuPermission,
                        modifier = Modifier
                            .fillMaxSize()
                            .layout { measurable, constraints ->
                                val reserved = ((GlassNavigationRailDefaults.Margin +
                                    GlassNavigationRailDefaults.ExpandedWidth) * railState.progress).roundToPx()
                                val placeable = measurable.measure(
                                    constraints.copy(
                                        minWidth = (constraints.minWidth - reserved).coerceAtLeast(0),
                                        maxWidth = if (constraints.hasBoundedWidth) {
                                            (constraints.maxWidth - reserved).coerceAtLeast(0)
                                        } else {
                                            constraints.maxWidth
                                        },
                                    )
                                )
                                layout(placeable.width + reserved, placeable.height) {
                                    placeable.placeRelative(reserved, 0)
                                }
                            }
                            .consumeWindowInsets(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                            .consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Start))
                    )
                }
            }

            GlassNavigationRail(
                items = glassItems,
                selectedIndex = mainPagerState.selectedPage,
                onSelect = onPageSelect,
                backdrop = backdrop,
                state = railState,
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
