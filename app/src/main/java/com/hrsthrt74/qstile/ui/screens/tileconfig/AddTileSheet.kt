package com.hrsthrt74.qstile.ui.screens.tileconfig

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.AppChip
import com.hrsthrt74.qstile.ui.components.FullBleedSheetSideInset
import com.hrsthrt74.qstile.ui.components.SheetState
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.hrsthrt74.qstile.viewmodel.TileConfigViewModel
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults
import top.yukonga.miuix.kmp.glass.glassShadow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「添加磁贴」底部 Sheet：系统磁贴与第三方磁贴合并为同一个 Sheet。
 *
 * 由原 `TileConfigScreen` 的页内局部函数 `AddTileSheet` 拆分而来，行为保持不变：
 * - 头部默认显示 TabRow 切换来源（0 = 系统磁贴 / 1 = 第三方磁贴）；
 * - 点击标题栏左侧的搜索按钮后，TabRow 在同一位置变换为搜索框（[AnimatedContent]），
 *   退出搜索再变回来；
 * - 标题栏右侧是「连续放置」开关（开启后点磁贴只添加、不收起 Sheet）；
 * - 关闭动画完成后清空两个来源各自的搜索词与连续放置状态，并把 Tab 复位到「系统磁贴」。
 *
 * 两个容易踩坑的实现细节（详见正文注释，勿轻易改动）：
 * 1. **头部玻璃控件的 backdrop 必须在 Sheet 内部单独录制**（只含列表层），不能传页面级
 *    backdrop，否则采样到的是 Sheet 后面的编辑页，看起来像「穿透了 Sheet」；
 * 2. **顶部淡出用列表层的 alpha 遮罩（BlendMode.DstIn）而不是叠一层底色渐变**，
 *    叠色在玻璃态 Sheet 上会露出一块带直角的浅灰色块。
 *
 * 第三方磁贴服务的加载也在这里触发（首次切到第三方 Tab 才扫描），因为 Tab 状态是本组件的内部状态。
 *
 * @param state Sheet 显示状态（由页面的 `rememberSheetState()` 创建，底栏跟随按钮也会调用它）
 * @param viewModel 磁贴配置 ViewModel（提供候选列表、搜索词与添加动作）
 */
@Composable
fun AddTileSheet(
    state: SheetState,
    viewModel: TileConfigViewModel,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val themeSettings = LocalThemeSettings.current
    // 应用风格：开 = Glass（TabRow / 阴影走玻璃实现），关 = 经典（TabRowWithContour）
    val isGlassStyle = themeSettings.progressiveBlur

    // 本 Sheet 是「列表铺满面板」的（AppBottomSheet 传 fullBleedContent = true）：
    // 列表要一直铺到面板边缘，顶部淡出遮罩、玻璃头部采样的 backdrop、Tab 切换的横向滑动
    // 都以整幅宽度为前提。此时 Sheet 自身的 insideMargin 被置 0，只要还有内边距，
    // 列表视口就被缩在里面，滑动时内容会在面板内侧被硬切出一条边。
    //
    // 于是左右留白全部由内容自己给：列表 contentPadding、头部浮层 padding、
    // 以及标题行动作区（由 AppBottomSheet 自动补）统一用 24dp，两种风格一致。
    val sheetSideInset = FullBleedSheetSideInset

    // 当前选中的来源 Tab：0 = 系统磁贴，1 = 第三方磁贴（关闭 Sheet 后复位为 0）
    var addTileTab by remember { mutableIntStateOf(0) }
    // 头部是否处于搜索态：false = 显示 TabRow，true = 显示搜索框
    var searchActive by remember { mutableStateOf(false) }
    // 「连续放置」开关：开启后点击磁贴不会收起 Sheet，可以连续添加多个
    var continuousAdd by remember { mutableStateOf(false) }

    // 切到「第三方磁贴」Tab 时才加载第三方磁贴服务列表
    // （首次加载较重，避免每次打开 Sheet 都全盘扫描；ViewModel 内部已做幂等保护）
    LaunchedEffect(state.show, addTileTab) {
        if (state.show && addTileTab == 1) {
            viewModel.loadCustomTileServices(context)
        }
    }

    // Tab 文案（两种风格共用）
    val tabLabels = listOf(
        stringResource(R.string.config_add_tab_system),
        stringResource(R.string.config_add_tab_custom),
    )

    // 写入当前 Tab 的搜索词（两种来源各有一份独立搜索状态，切换 Tab 互不干扰）
    fun updateQuery(newQuery: String) {
        if (addTileTab == 0) viewModel.addSearchQuery = newQuery
        else viewModel.customSearchQuery = newQuery
    }

    AppBottomSheet(
        state = state,
        title = stringResource(R.string.config_add_tiles),
        // 标题栏左侧：搜索 / 退出搜索（退出时顺手清空当前 Tab 的搜索词）
        startAction = {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (searchActive) updateQuery("")
                    searchActive = !searchActive
                }
            ) {
                Icon(
                    imageVector = if (searchActive) MiuixIcons.Close else MiuixIcons.Search,
                    contentDescription = stringResource(
                        if (searchActive) R.string.common_close else R.string.common_search
                    ),
                )
            }
        },
        // 标题栏右侧：「连续放置」开关（自绘胶囊 Chip，靠底色区分开关态）
        endAction = {
            AppChip(
                text = stringResource(R.string.config_add_continuous),
                selected = continuousAdd,
                onClick = { continuousAdd = !continuousAdd },
            )
        },
        // 关闭动画完成后清空两个 Tab 各自的搜索关键字、退出搜索态与连续放置，并把 Tab 复位到「系统磁贴」
        onDismissed = {
            viewModel.clearAddSearchQuery()
            viewModel.clearCustomSearchQuery()
            viewModel.clearCustomTileValue()
            addTileTab = 0
            searchActive = false
            continuousAdd = false
        },
        // 列表铺满面板，左右留白由列表 contentPadding 与头部 padding 负责（见 sheetSideInset）
        fullBleedContent = true,
    ) {
        // 头部高度（px）：由头部浮层测量后回填，作为列表顶部的内边距，
        // 使列表内容铺满整个 Sheet 并在滚动时滑到头部下方（而不是被头部挤下去、留出空白）。
        var headerHeightPx by remember { mutableIntStateOf(0) }
        val density = LocalDensity.current
        val headerHeight = with(density) { headerHeightPx.toDp() }

        // 头部玻璃控件的采样源：必须在 Sheet 内单独录制（只含列表层），
        // 不能用页面级 backdrop——那样会采样到 Sheet 后面的编辑页，看起来像「穿透了 Sheet」。
        // 模糊关闭 / 不支持 / 经典风格时返回 null：经典风格不需要录制（省一次离屏绘制），
        // 玻璃控件在 backdrop 为 null 时也回退经典样式。
        val headerBackdrop = rememberBlurBackdrop(enabled = isGlassStyle && themeSettings.enableBlur)

        // 用 Box 叠放：列表在下层铺满，头部浮在上层（TabRow / 搜索框高度变化会自动同步）
        Box(modifier = Modifier.fillMaxWidth()) {
            // 列表层：既是内容层，也是玻璃控件的 backdrop 录制层（头部浮层在其之外，不会被自身采样）
            // 顶部留出头部高度，滚动时内容会进入该区域（contentPadding 在视口内部，
            // 条目滑上来时会画在 padding 区域里），形成「列表延伸到头部下方」的效果
            Box(
                modifier = if (headerBackdrop != null) {
                    Modifier.fillMaxWidth().layerBackdrop(headerBackdrop)
                } else {
                    Modifier.fillMaxWidth()
                }
            ) {
                // 顶部渐隐：让滑到头部下方的列表条目淡出，而不是被生硬裁断。
                //
                // 这里用 **alpha 遮罩**（BlendMode.DstIn），而不是在头部叠一层「Sheet 底色渐变」：
                // 叠色只有在颜色与 Sheet 真实底色完全一致时才隐形，而玻璃态 Sheet 的底色是材质
                // shader 混出来的（浅色模式下明显比 colorScheme.background 更白），
                // 叠一层 background 就会露出一块边缘笔直的浅灰色块——看起来既像「一道被反色的
                // 阴影」，又会在左上 / 右上露出两个直角。
                // alpha 遮罩与任何主题色都无关：它只把内容抹成透明、露出下面的 Sheet，
                // 深色 / 浅色模式下都只有「内容淡出」这一种表现，不留色块也不留硬边。
                //
                // 渐隐终点 = 头部高度 - 底部 8dp 间距（即胶囊下缘），再往下内容完全不透明
                val fadeEndPx = with(density) { (headerHeightPx - 8.dp.toPx()).coerceAtLeast(0f) }
                val topFadeBrush = remember(fadeEndPx) {
                    if (fadeEndPx <= 0f) {
                        null
                    } else {
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black,
                            startY = 0f,
                            endY = fadeEndPx,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // DstIn 只与本层「已经画好的内容」相乘，所以必须离屏隔离：
                        // 没有独立图层时，它会连父层（Sheet 背景）一起擦掉
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            // 内容本身比头部还矮时（极端情况）不遮罩：
                            // 渐隐终点落在内容之外会把整块内容擦成透明
                            if (topFadeBrush != null && size.height > fadeEndPx) {
                                drawRect(brush = topFadeBrush, blendMode = BlendMode.DstIn)
                            }
                        }
                ) {
                    // 两个来源之间切换：淡入淡出 + 小幅平移（1/8 页宽），
                    // 与 afdian 的 OrderScreen / FavoritesScreen 用的是同一套参数——
                    // 位移只走 1/8 而不是整屏，观感是「这一页换了」而不是真的翻页，
                    // 不会让两页的列表在 Sheet 里整块横移（那样很跳）。
                    // 方向：切到更靠后的 Tab 从右侧进来，切回去则从左。
                    // 出场比入场快（150ms vs 220ms），旧页先让位、新页随后就位。
                    AnimatedContent(
                        targetState = addTileTab,
                        transitionSpec = {
                            val forward = targetState > initialState
                            (
                                fadeIn(animationSpec = tween(220)) +
                                    slideInHorizontally(animationSpec = tween(220)) {
                                        if (forward) it / 8 else -it / 8
                                    }
                                ) togetherWith (
                                fadeOut(animationSpec = tween(150)) +
                                    slideOutHorizontally(animationSpec = tween(150)) {
                                        if (forward) -it / 8 else it / 8
                                    }
                                )
                        },
                        label = "addTileTabContent",
                    ) { tab ->
                        when (tab) {
                            0 -> SystemTilePicker(
                                viewModel = viewModel,
                                topPadding = headerHeight,
                                sidePadding = sheetSideInset,
                                continuousAdd = continuousAdd,
                                onDismissSheet = { state.dismiss() },
                            )
                            else -> ThirdPartyTilePicker(
                                viewModel = viewModel,
                                topPadding = headerHeight,
                                sidePadding = sheetSideInset,
                                continuousAdd = continuousAdd,
                                onDismissSheet = { state.dismiss() },
                            )
                        }
                    }
                }
            }

            // 头部浮层：TabRow <-> 搜索框 原位变换（交叉淡入淡出 + 轻微缩放）
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { headerHeightPx = it.height }
                    // 头部自己留出左右边距（外层容器是铺满的，见 sheetSideInset）。
                    // 胶囊外侧的玻璃阴影由 glassShadow 画在 clip 之前，往外溢出到留白里不会被裁掉。
                    .padding(horizontal = sheetSideInset)
            ) {
                // 注意：这里**不要**再叠一层「Sheet 底色渐变」遮罩来做内容淡出。
                // 叠色的颜色没法与玻璃态 Sheet 的真实底色对齐（见列表层顶部的说明），
                // 浅色模式下会变成一块带直角的灰色块；淡出统一由列表层的 alpha 遮罩完成。

                AnimatedContent(
                    targetState = searchActive,
                    transitionSpec = {
                        (fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.92f)) togetherWith
                            (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.92f))
                    },
                    contentAlignment = Alignment.Center,
                    label = "addTileHeader",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) { searching ->
                    if (searching) {
                        // 搜索框：进入即自动聚焦弹出键盘；失焦不退出搜索态，统一由标题栏按钮退出
                        InputField(
                            query = if (addTileTab == 0) viewModel.addSearchQuery else viewModel.customSearchQuery,
                            onQueryChange = { updateQuery(it) },
                            onSearch = {},
                            expanded = true,
                            onExpandedChange = {},
                            label = stringResource(R.string.common_search),
                            color = MiuixTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (isGlassStyle && headerBackdrop != null) {
                        // 玻璃风格：官方 GlassSegmentedTabRow（胶囊轨道 + 滑动指示器）
                        // 采样的是 Sheet 内的 headerBackdrop（列表层），不会被自身内容污染
                        //
                        // 轮廓：miuix 的 GlassSegmentedTabRow 没有开放 shape 参数，内部写死
                        // GlassShape(height / 2, smoothing = 0f)（正圆胶囊），这里仍在最外层再裁一次
                        // ——轨道 / 指示器 / 标签是同一尺寸的嵌套结构，一起裁不会互相裁坏；
                        // 裁剪同时让胶囊外的四个角落不响应点击（对分段控件是预期行为）。
                        //
                        // 阴影（关键）：玻璃阴影是 drawBehind 画的、会溢出节点边界，
                        // 塞在 clip 之内只会被裁成一圈硬边。故改成**画在裁剪之前**：
                        // glassShadow 挂在外层 Box、clip 在其之后，阴影就能完整铺在胶囊外侧。
                        // 颜色用库自带的 GlassShadows.Float（纯黑 + 低透明度，随缩放自动外扩），
                        // 不要贴主题色 / onSurface：浅色模式下那会变成一圈亮色，看着像「反色的阴影」。
                        val tabTrackShape = remember {
                            GlassShape(GlassSegmentedTabRowDefaults.Height / 2, smoothing = 0f)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassShadow(tabTrackShape, GlassShadows.Float)
                                .clip(Capsule())
                        ) {
                            GlassSegmentedTabRow(
                                tabs = tabLabels,
                                selectedIndex = addTileTab,
                                onSelect = { index ->
                                    // 只在真正切换时给触感反馈
                                    if (index != addTileTab) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        addTileTab = index
                                    }
                                },
                                backdrop = headerBackdrop,
                                // 阴影已在外层画好（见上）；组件内部再画会被外层 clip 裁成硬边
                                shadow = null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        // 经典风格：Miuix 描边分段控件
                        TabRowWithContour(
                            tabs = tabLabels,
                            selectedTabIndex = addTileTab,
                            onTabSelected = { index ->
                                if (index != addTileTab) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    addTileTab = index
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
