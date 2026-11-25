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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.AppChip
import com.hrsthrt74.qstile.ui.components.FullBleedSheetSideInset
import com.hrsthrt74.qstile.ui.components.GlassSearchField
import com.hrsthrt74.qstile.ui.components.SheetState
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.hrsthrt74.qstile.viewmodel.TileConfigViewModel
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
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

/**
 * 「添加磁贴」底部 Sheet：系统磁贴与第三方磁贴合并为同一个 Sheet。
 *
 * - 头部默认显示 TabRow 切换来源（0 = 系统磁贴 / 1 = 第三方磁贴）；
 * - 点击标题栏左侧的搜索按钮后，TabRow 在同一位置变换为搜索框（[AnimatedContent]），
 *   退出搜索再变回来；
 * - 标题栏右侧是「连续放置」开关（开启后点磁贴只添加、不收起 Sheet）；
 * - 关闭动画完成后清空两个来源各自的搜索词与连续放置状态，并把 Tab 复位到「系统磁贴」。
 *
 * ### 头部：交给 Sheet 的浮层头部槽位（miuix 0.9.5-rc01 起）
 *
 * 玻璃风格的 [top.yukonga.miuix.kmp.glass.GlassBottomSheet] 从 rc01 起标题栏是**浮层**：
 * 它覆盖在内容之上，并新增 `headerBottomContent` 槽位（钉在标题下方、**属于头部**）。
 * 于是本 Sheet 的头部（TabRow / 搜索框）不再由内容自己叠一层浮层、也不再自己测量高度，改由
 * [AppBottomSheet] 的 `headerBottomContent` 交给 Sheet：
 * - 头部高度由 Sheet 统一测量，再通过 `content` 的 `PaddingValues` 下发，列表的 `contentPadding.top`
 *   直接用它 —— 不再需要 `onSizeChanged` 回填 + 手写顶部淡出遮罩；
 * - 滚动时 Sheet 会在头部下方画一条渐进模糊带 + 同底色遮罩，列表条目就从头部下方滑过去
 *   （即「列表透到顶部」的效果），取代原来手写的 `BlendMode.DstIn` alpha 遮罩；
 * - 头部自己画在模糊带之上，所以 TabRow / 搜索框始终清晰、不会被模糊掉。
 *
 * 经典风格（[top.yukonga.miuix.kmp.window.WindowBottomSheet]）的头部是堆叠式的，没有浮层槽位：
 * [AppBottomSheet] 会把同一份头部内容渲染成列表上方普通的一行，此时 `PaddingValues` 恒为 0，
 * 列表自然从头部下方开始（两套结构都由 `fullBleedContent = true` + `PaddingValues` 统一，
 * 这里不需要再按风格分支）。
 *
 * ### 列表为什么要「铺满」
 *
 * 列表必须一直铺到面板边缘（[AppBottomSheet] 的 `fullBleedContent = true` 把 Sheet 内边距置 0）：
 * 只要 Sheet 还留着内边距，列表视口就被缩在里面，滚动时内容会在面板内侧被硬切出一条边；
 * 左右留白改由列表的 `contentPadding`、头部浮层的 `padding`、以及标题行动作区（由 [AppBottomSheet]
 * 自动补）统一用 [FullBleedSheetSideInset]，两种风格一致。
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
    // 列表要一直铺到面板边缘，顶部模糊带、玻璃头部采样的 backdrop、Tab 切换的横向滑动
    // 都以整幅宽度为前提。
    //
    // 于是左右留白全部由内容自己给：列表 contentPadding、头部浮层的 padding、
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

    // 列表层的采样源（只含列表，不含头部）：头部玻璃控件的 backdrop **必须在 Sheet 内单独录制**，
    // 不能传页面级 backdrop，否则采样到的是 Sheet 后面的编辑页，看起来像「穿透了 Sheet」。
    //
    // rc01 起头部属于 Sheet 的头部槽位，Sheet 内部已经录了一份「body（= 列表层）」的 scene backdrop
    // 并通过 GlassHeaderContext 下发（优先级高于这里传的），所以正常情况下这份录制只作兜底：
    // 页面没有 backdrop（Oobe / 调试工具）或 Sheet 侧录制不可用时才轮到它。
    // 模糊关闭 / 不支持 / 经典风格时返回 null：经典风格不需要录制（省一次离屏绘制），
    // 玻璃控件在 backdrop 不可用时回退纯色。
    val headerBackdrop = rememberBlurBackdrop(enabled = isGlassStyle && themeSettings.enableBlur)

    // 头部内容（TabRow ⇄ 搜索框，原位交叉淡入淡出 + 轻微缩放）。
    // 同一份内容两用：玻璃风格进 Sheet 的头部槽位，经典风格由 AppBottomSheet 放在列表上方。
    //
    // 左右留白**不要**在这里加：AppBottomSheet 已经把头部内容统一补到
    // `FullBleedSheetSideInset`（与标题栏左右动作区同一套规则，此处 fullBleed = 24dp），
    // 这里再加一份就会变成 48dp——表现为 TabRow / 搜索框明显比列表窄一圈。
    // 底部让出 8dp：列表的 contentPadding.top 会再补 8dp，使首屏条目与头部之间留出呼吸感。
    val headerContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            AnimatedContent(
                targetState = searchActive,
                transitionSpec = {
                    (fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.92f)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.92f))
                },
                contentAlignment = Alignment.Center,
                label = "addTileHeader",
                modifier = Modifier.fillMaxWidth()
            ) { searching ->
                if (searching) {
                    // 搜索框：进入即自动聚焦弹出键盘；失焦不退出搜索态，统一由标题栏按钮退出。
                    // 玻璃风格下垫一层真实 backdrop 模糊（与 GlassSegmentedTabRow 同一套材质），
                    // 采样源就是本 Sheet 的列表层 headerBackdrop；没有 backdrop 时自动回退普通输入框。
                    GlassSearchField(
                        query = if (addTileTab == 0) viewModel.addSearchQuery else viewModel.customSearchQuery,
                        onQueryChange = { updateQuery(it) },
                        onSearch = {},
                        expanded = true,
                        onExpandedChange = {},
                        label = stringResource(R.string.common_search),
                        backdrop = headerBackdrop,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (isGlassStyle && headerBackdrop != null) {
                    // 玻璃风格：官方 GlassSegmentedTabRow（胶囊轨道 + 滑动指示器）
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
        // 头部下方的 TabRow / 搜索框：玻璃风格进 Sheet 的浮层头部（随头部一起浮在列表之上、
        // 不会被模糊带模糊，头部高度也一并被测量），经典风格由 AppBottomSheet 放在列表上方
        headerBottomContent = headerContent,
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
    ) { innerPadding ->
        // 头部让出的高度：玻璃风格 = Sheet 浮层头部的实测高度（拖动条 + 标题行 + TabRow/搜索框，
        // 由 miuix 通过 PaddingValues 下发）；经典风格 = 0（头部是堆叠式，列表已在其下方）。
        // 直接给列表当 contentPadding.top：列表视口仍从面板顶端开始，条目滑上来时会画在
        // padding 区域里，于是「透到头部下方」——这正是官方的做法，不要再改成给外层加 padding
        // （那会把视口一起缩小，条目永远滑不到头部下方）。
        val topPadding = innerPadding.calculateTopPadding()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Tab 切换的横向滑动只在面板边缘被裁：不加这层裁剪，两页在滑动过程中会溢出到面板外
                .clipToBounds()
                // 列表层既是内容层，也是玻璃头部控件的 backdrop 录制层
                // （头部浮层在其之外，不会被自身采样）
                .then(
                    if (headerBackdrop != null) Modifier.layerBackdrop(headerBackdrop)
                    else Modifier
                )
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
                        topPadding = topPadding,
                        sidePadding = sheetSideInset,
                        continuousAdd = continuousAdd,
                        onDismissSheet = { state.dismiss() },
                    )
                    else -> ThirdPartyTilePicker(
                        viewModel = viewModel,
                        topPadding = topPadding,
                        sidePadding = sheetSideInset,
                        continuousAdd = continuousAdd,
                        onDismissSheet = { state.dismiss() },
                    )
                }
            }
        }
    }
}
