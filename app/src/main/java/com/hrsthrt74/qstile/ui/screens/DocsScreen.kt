package com.hrsthrt74.qstile.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.DocsActivity
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.AppTopBar
import com.hrsthrt74.qstile.ui.LocalAppTopBarGlass
import com.hrsthrt74.qstile.ui.TopBarIconButton
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults
import top.yukonga.miuix.kmp.glass.glassShadow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassShadows

/** Docs 页面的三个 Tab。标题持有 @StringRes 资源 id,支持多语言,使用处在 Composable 上下文解析。 */
enum class DocsTab(@StringRes val titleRes: Int, val assetName: String?) {
    /** 隐私政策：渲染 docs/PRIVACY_POLICY.md。 */
    PRIVACY_POLICY(R.string.docs_tab_privacy, "PRIVACY_POLICY.md"),

    /** 用户协议：渲染 docs/TERMS_OF_SERVICE.md。 */
    TERMS_OF_SERVICE(R.string.docs_tab_terms, "TERMS_OF_SERVICE.md"),

    /** 开源许可：展示开源库列表，无对应 Markdown 文档。 */
    LICENSES(R.string.docs_tab_licenses, null);

    companion object {
        /** 按 Intent extra 解析 Tab，未传入或非法时兜底为隐私政策，避免页面为空。 */
        fun fromValue(value: String?): DocsTab {
            return entries.firstOrNull { it.name == value } ?: PRIVACY_POLICY
        }
    }
}

/**
 * 文档聚合页：在「隐私政策 / 用户协议 / 开源许可」间切换。
 *
 * 样式对齐 miuix 官方 demo 的子页面（LicensePage / IconPage）：
 * - 顶栏统一走 [AppTopBar]，跟随应用的 Glass / 经典风格开关，并随滚动折叠大标题；
 * - Tab 从内容区挪进顶栏的 `bottomContent` 槽，常驻可见（与「创造模式层 Tab」同一套写法）；
 * - 内容区挂 [layerBackdrop] 供顶栏模糊采样，Tab 位于顶栏内、处在采样子树之外，不会自采样成环；
 * - 三个 Tab 的内容用横向淡入淡出 + 小幅平移切换，避免生硬整块替换。
 *
 * [rememberSaveable] 保存选中 Tab，旋转屏幕不会丢失当前页。
 */
@Composable
fun DocsScreen(initialTab: DocsTab) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    // 主题设置由 ExTileTheme 通过 LocalThemeSettings 同步提供（数据就绪后才组合到这里）
    val themeSettings = LocalThemeSettings.current
    // 应用风格开关（复用「渐进模糊」开关）：true=Glass 全套组件，false=经典组件
    val glassStyle = themeSettings.progressiveBlur
    // 顶栏模糊采样源：模糊关闭或 RuntimeShader 不支持时返回 null，组件回退纯色
    val backdrop = rememberBlurBackdrop(enabled = themeSettings.enableBlur)

    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)
    val tabs = DocsTab.entries.map { stringResource(it.titleRes) }

    // 记住初始 Tab；rememberSaveable 保证配置变更（旋转）后仍停留在原 Tab
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialTab.ordinal) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.docs_screen_title),
                largeTitle = stringResource(R.string.docs_screen_title),
                // 独立子页面没有宽屏 LocalIsWideScreen，一律保留可折叠大标题
                useLargeTitle = true,
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                progressive = glassStyle,
                navigationIcon = {
                    // 返回按钮跟随顶栏形态切换图标（照搬官方 demo 的 BackNavigationIcon）：
                    // 玻璃态用 28dp 的 Glass.ChevronBackward，经典态用 24dp 的普通 Back。
                    // TopBarIconButton 自身还会把按钮容器切成 GlassIconButton / IconButton。
                    TopBarIconButton(onClick = { (context as? DocsActivity)?.finish() }) {
                        if (LocalAppTopBarGlass.current) {
                            Icon(
                                imageVector = MiuixIcons.Glass.ChevronBackward,
                                contentDescription = stringResource(R.string.common_back),
                                modifier = Modifier.padding(2.dp),
                            )
                        } else {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.common_back),
                            )
                        }
                    }
                },
                bottomContent = {
                    DocsTabBar(
                        tabs = tabs,
                        selectedIndex = selectedTabIndex,
                        onSelect = { index ->
                            // 点击 Tab 给一次轻触感反馈，与项目内其它触觉反馈风格一致
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTabIndex = index
                        },
                        backdrop = backdrop,
                        isGlassStyle = glassStyle,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 12.dp,
                        ),
                    )
                }
            )
        }
    ) { paddingValues ->
        // 顶栏高度：交给各内容组件作为滚动内容的 contentPadding.top。
        //
        // 关键（与设置页 / 编辑页一致）：内容区从 (0,0) 铺满整屏、topBar 叠在上方，
        // 绝不能用 Box 的 padding(top=顶栏高度) 把内容推到顶栏下方——那样内容永远
        // 进不了顶栏背面，既看不到文字从顶栏下穿过的效果，backdrop 在 0~顶栏 区域
        // 也始终采不到内容、模糊不可见。把避让放进滚动内容后，滚动时文字会进入
        // 顶栏采样区，折叠大标题与玻璃模糊才会真正联动。
        val topContentPadding = paddingValues.calculateTopPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                // 内容区作为顶栏模糊的采样源；Tab 在顶栏内、不在此子树中，不会自采样成环
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
        ) {
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    // 切到靠后的 Tab 从右侧滑入，反之从左侧滑入；位移仅取 1/8 页宽
                    val forward = targetState > initialState
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { width ->
                        if (forward) width / 8 else -width / 8
                    }) togetherWith (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { width ->
                        if (forward) -width / 8 else width / 8
                    })
                },
                label = "DocsTabContent",
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                when (val currentTab = DocsTab.entries[index]) {
                    DocsTab.PRIVACY_POLICY,
                    DocsTab.TERMS_OF_SERVICE,
                    -> MarkdownDocumentContent(
                        assetName = checkNotNull(currentTab.assetName),
                        scrollBehavior = scrollBehavior,
                        topContentPadding = topContentPadding,
                    )

                    DocsTab.LICENSES -> LicensesContent(
                        scrollBehavior = scrollBehavior,
                        topContentPadding = topContentPadding,
                    )
                }
            }
        }
    }
}

/**
 * 文档页的分段 Tab：玻璃风格用官方 [GlassSegmentedTabRow]，经典风格用 [TabRowWithContour]。
 *
 * 写法与「创造模式层 Tab」保持一致：GlassSegmentedTabRow 内部写死正圆胶囊且没有 shape 参数，
 * 外层先画玻璃阴影再裁剪，组件自身传 `shadow = null`，否则阴影会被裁成一圈硬边。
 *
 * @param tabs 各 Tab 的标题
 * @param selectedIndex 当前选中的 Tab 下标
 * @param onSelect 选中某一 Tab 的回调（参数为下标）
 * @param backdrop 内容区录制的模糊采样源，仅供玻璃态使用
 * @param isGlassStyle 应用风格开关：true = Glass，false = 经典
 */
@Composable
private fun DocsTabBar(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: LayerBackdrop?,
    isGlassStyle: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isGlassStyle && backdrop != null) {
        // 轨道形状 = 高度一半的正圆胶囊（与 miuix 内部一致），只为给外层阴影用
        val tabTrackShape = remember {
            GlassShape(GlassSegmentedTabRowDefaults.Height / 2, smoothing = 0f)
        }
        Box(
            modifier = modifier
                .fillMaxWidth()
                // 阴影先画（会溢出节点边界），再裁剪，保证阴影完整铺在胶囊外侧
                .glassShadow(tabTrackShape, GlassShadows.Float)
                .clip(Capsule())
        ) {
            GlassSegmentedTabRow(
                tabs = tabs,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                backdrop = backdrop,
                // 阴影已在外层画好，组件内部再画会被外层 clip 裁成硬边
                shadow = null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        // 经典风格：Miuix 描边分段控件
        TabRowWithContour(
            tabs = tabs,
            selectedTabIndex = selectedIndex,
            onTabSelected = onSelect,
            modifier = modifier.fillMaxWidth(),
        )
    }
}
