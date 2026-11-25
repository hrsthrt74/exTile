package com.hrsthrt74.qstile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.glass.GlassBottomSheetDefaults
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 玻璃风格 Sheet 内容左右额外补回的外边距。
 *
 * miuix 0.9.5-beta04 把 [GlassBottomSheet] 的默认内边距由经典 Sheet 的 24dp
 * （`BottomSheetDefaults.insideMargin`）换成了自己的 `GlassBottomSheetDefaults.InsideMargin` = 12dp，
 * 于是玻璃风格下所有 Sheet 内容突然比之前贴边一倍，而经典 [WindowBottomSheet] 仍是 24dp，
 * 两种风格的左右留白就此对不上。
 *
 * 这里在**内容侧**补回这 12dp 的差额（而不是给 Sheet 传 `insideMargin`）：
 * - 给 Sheet 加内边距会连带把标题栏、拖动条、以及「列表铺满 Sheet、头部浮在上层」这类
 *   贴边布局（添加磁贴 Sheet）一起推开，且两种风格还得各写一套数值；
 * - 加在内容上则只影响「内容的左右外边距」，Sheet 自身的几何（圆角、宽度、拖动条）完全不变。
 *
 * 需要「铺满」的 Sheet（如添加磁贴：列表要一直铺到面板边缘，顶部淡出遮罩、玻璃头部采样的
 * backdrop、Tab 切换的横向滑动都以整幅宽度为前提）用 `fullBleedContent = true` 关掉这层
 * 容器级外边距，改在内容内部对此常量自行留白——否则列表视口被整体缩进，滑动时内容会在
 * 面板内侧被硬切出一条边。
 */
internal val GlassSheetContentExtraInset = 12.dp

/**
 * 「铺满型」Sheet（`fullBleedContent = true`）由内容自己负责的左右留白，取经典 Sheet 的默认内边距 24dp。
 *
 * 这类 Sheet 会把 Sheet 的 `insideMargin` 置 0（见 [AppBottomSheet]）：只有内边距为 0，列表视口才真正
 * 铺到面板边缘，Tab 切换的横向滑动/淡出才只在面板边缘被裁，而不会在面板内侧露出一条「内容突然消失」
 * 的硬边。留白改由列表 `contentPadding`、头部浮层的 `padding`、以及标题行动作区的 padding 提供。
 */
internal val FullBleedSheetSideInset = 24.dp

/**
 * 底部 Sheet 的显示状态。
 *
 * 统一管理一个底部 Sheet 的显示/隐藏，并封装「请求关闭」逻辑（点击遮罩 / 按下返回键 / 下拉拖拽）。
 * 页面中不再需要为每个 Sheet 单独声明 `var showXxx by remember { mutableStateOf(false) }` 并重复编写
 * `onDismissRequest = { showXxx = false }`，改用本状态后只需：
 *
 * ```
 * val addSheetState = rememberSheetState()
 * // 打开
 * addSheetState.show()
 * // 关闭（由 AppBottomSheet 内部自动调用，页面无需手动处理）
 * addSheetState.dismiss()
 * ```
 *
 * @param initialShow 初始是否显示
 */
@Stable
class SheetState(initialShow: Boolean = false) {

    /** 当前是否显示（只读，外部通过 [show]/[dismiss] 控制） */
    var show by mutableStateOf(initialShow)
        private set

    /** 打开 Sheet */
    fun show() {
        show = true
    }

    /** 关闭 Sheet（等价于点击遮罩/返回键时的行为，供 Sheet 内容里的按钮调用） */
    fun dismiss() {
        show = false
    }
}

/**
 * 创建并记住一个 [SheetState]。
 *
 * @param initialShow 初始是否显示
 */
@Composable
fun rememberSheetState(initialShow: Boolean = false): SheetState =
    remember { SheetState(initialShow) }

/**
 * 统一的底部 Sheet 组件。
 *
 * 封装 [WindowBottomSheet]，将项目内所有 Sheet 的重复逻辑集中到一处：
 * - **请求关闭统一处理**：点击遮罩 / 按下返回键 / 下拉拖拽时自动调用 [SheetState.dismiss]，
 *   页面不再需要重复编写 `onDismissRequest = { showXxx = false }`
 * - **返回键行为统一控制**：通过 [allowDismiss] 参数统一控制是否允许返回/拖拽关闭（miuix 0.9.3
 *   内置 NavigationBackHandler 已自动生效，无需页面手动注册 BackHandler）
 * - **关闭后清理统一入口**：通过 [onDismissed] 在关闭动画完成后集中执行状态清理（如清空输入框内容）
 *
 * 玻璃风格下改用官方 [GlassBottomSheet]（0.9.5 miuix-glass 方案），经典风格保留 [WindowBottomSheet]，
 * 两者随「应用风格」开关（LocalThemeSettings.progressiveBlur）统一切换，调用方无需感知。
 *
 * **头部浮层（miuix 0.9.5-rc01 起）**：玻璃 [GlassBottomSheet] 的标题栏不再独占一段高度，而是**浮在内容之上**，
 * 内容区顶部让出的高度由组件通过 `content` 的 [PaddingValues] 下发（`progressiveBlur` 默认开：滚动时
 * 头部绘制渐进模糊带 + 底色遮罩，内容可以从头部下方滑过）。
 * - 普通 Sheet：本封装默认把这段内边距施加在内容容器上，**调用点无需改动**——内容仍然从标题栏下方开始，
 *   只是滚动时会在标题栏下缘被模糊带盖一下（内容本身不会滑到头部下方）；
 * - 可滚动内容想真正「延伸到顶部」（条目滑到头部下方、被模糊带盖住）时传 [contentExtendsUnderHeader] = true：
 *   顶部内边距**原样交给内容**，由滚动容器自己消费（`LazyColumn(contentPadding = innerPadding)`,
 *   或 `verticalScroll(...).padding(innerPadding)`）——`fullBleedContent = true` 隐含此行为。
 *
 * 头部下方追加内容（如分段 TabRow / 搜索框）用 [headerBottomContent]：玻璃风格它会被放进 Sheet 的
 * 头部槽位（属于头部、随头部一起浮在内容之上、不会被模糊带模糊），经典风格则退化为内容顶部的普通一行
 * （经典头部是堆叠式，没有浮层可放），两种风格调用点都不用再自己测量头部高度。
 *
 * @param state Sheet 的显示状态（由 [rememberSheetState] 创建）
 * @param modifier 应用到 Sheet 内容上的修饰符
 * @param title 标题（显示在 Sheet 顶部居中）
 * @param startAction 标题行左侧的动作区；为 null 时不显示
 * @param endAction 标题行右侧的动作区（如搜索按钮）；为 null 时不显示
 * @param headerBottomContent 标题下方、属于头部的内容（如分段 TabRow / 搜索框）；为 null 时不显示。
 *   玻璃风格走官方头部槽位（`GlassBottomSheet(headerBottomContent = ...)`），
 *   经典风格退化为内容顶部的一行——两种风格的横向补边由本组件统一处理，调用点不要再自己加左右 padding
 * @param allowDismiss 是否允许通过拖拽 / 返回手势 / 点击遮罩关闭；为 false 时这些操作不生效
 * @param enableNestedScroll 是否启用内容的嵌套滚动（用于下拉关闭联动）
 * @param onDismissed 关闭动画完成后的回调（用于清理临时状态）；注意 show 为 false 但从未显示时不会触发
 * @param maxHeightFraction 内容区最大高度占窗口高度的比例。
 *   超高的内容（如可滚动列表）会在受限高度内滚动，避免 sheet 几乎占满整屏；
 *   注意约束作用于内容区，面板顶部的拖动条/标题会在此基础上叠加数十 dp
 * @param fullBleedContent 内容是否「铺满」面板左右边缘（Sheet 的 `insideMargin` 置 0、不加容器级外边距）。
 *   默认 false：内容左右各留 12dp（玻璃风格）以对齐经典风格的 24dp。
 *   设为 true 时由内容自己在内部留白（[FullBleedSheetSideInset]）——用于「列表要铺满、头部浮在上层」
 *   的 Sheet（添加磁贴），那里顶部淡出遮罩、玻璃头部采样的 backdrop、Tab 切换的横向滑动都需要
 *   整幅宽度；只要 Sheet 还有内边距，列表视口就被缩在里面，滑动时内容会在面板内侧被硬切出一条边。
 *   同时这类 Sheet 的顶部内边距不会被容器消费，而是交给内容自己（见 [contentExtendsUnderHeader]）。
 * @param contentExtendsUnderHeader 内容是否「延伸到头部下方」——可滚动内容专用。
 *   为 true 时容器**不**加顶部内边距，把 [PaddingValues] 交给内容，让滚动容器自己让位：
 *   `LazyColumn(contentPadding = innerPadding)`（或 `verticalScroll(...).padding(innerPadding)`）。
 *   这样滚动视口从面板顶端开始，条目能滑到头部（标题栏 / [headerBottomContent]）下方并被模糊带盖住；
 *   若由容器让位（默认），视口就已经在头部下方，条目永远滑不进头部区域。
 *   注意：非滚动内容不要开——它会把内容顶到头部下面去。`fullBleedContent = true` 隐含开启。
 * @param content Sheet 内容。拿到的是「Sheet 头部占用的顶部高度」：
 *   普通 Sheet（未开 [contentExtendsUnderHeader]）恒为 0（已由容器消费）；
 *   开启后（含铺满型）为头部实测高度（含 [headerBottomContent]），应作为滚动容器的 `contentPadding.top`
 */
@Composable
fun AppBottomSheet(
    state: SheetState,
    modifier: Modifier = Modifier,
    title: String? = null,
    startAction: (@Composable () -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
    headerBottomContent: (@Composable () -> Unit)? = null,
    allowDismiss: Boolean = true,
    enableNestedScroll: Boolean = true,
    onDismissed: (() -> Unit)? = null,
    maxHeightFraction: Float = 0.85f,
    fullBleedContent: Boolean = false,
    contentExtendsUnderHeader: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    // 读取当前窗口高度（与 miuix 内部一致），按比例计算内容区的最大高度
    val windowInfo = LocalWindowInfo.current
    val maxContentHeight = windowInfo.containerDpSize.height * maxHeightFraction

    // 随应用风格开关切换：true=玻璃风格 GlassBottomSheet，false=经典 WindowBottomSheet
    val glassStyle = LocalThemeSettings.current.progressiveBlur
    val backdrop = LocalOverlayBackdrop.current

    // 玻璃风格的左右留白比经典风格窄 12dp（见 [GlassSheetContentExtraInset]），在内容侧补回；
    // 经典风格的 Sheet 自带 24dp，无需补偿，保持 0。
    // 铺满型内容（fullBleedContent）不在这里补，由内容自己留白（此时 Sheet 的内边距也是 0）。
    val contentSideInset = if (glassStyle && !fullBleedContent) GlassSheetContentExtraInset else 0.dp

    // 标题行左右动作区的补边：普通 Sheet 只需补玻璃风格的那 12dp；
    // 铺满型 Sheet 的 Sheet 内边距是 0，两种风格都要补满 24dp。
    val headerActionInset = when {
        fullBleedContent -> FullBleedSheetSideInset
        glassStyle -> GlassSheetContentExtraInset
        else -> 0.dp
    }
    val sheetStartAction: (@Composable () -> Unit)? = startAction?.let { action ->
        if (headerActionInset > 0.dp) {
            {
                Box(modifier = Modifier.padding(start = headerActionInset)) {
                    action()
                }
            }
        } else {
            action
        }
    }
    val sheetEndAction: (@Composable () -> Unit)? = endAction?.let { action ->
        if (headerActionInset > 0.dp) {
            {
                Box(modifier = Modifier.padding(end = headerActionInset)) {
                    action()
                }
            }
        } else {
            action
        }
    }

    // Sheet 自身的内边距：铺满型置 0（留白交给内容），否则用各风格默认值。
    // 这里显式传默认值，避免「不传」和「传 0」在两种风格下混淆。
    val sheetInsideMargin = if (fullBleedContent) {
        DpSize(0.dp, 0.dp)
    } else if (glassStyle) {
        GlassBottomSheetDefaults.InsideMargin
    } else {
        BottomSheetDefaults.insideMargin
    }

    // 标题下方的头部内容（如分段 TabRow / 搜索框）：
    // - 玻璃风格交给 Sheet 的头部槽位，属于头部、浮在内容之上，且**不会被头部模糊带模糊**；
    // - 经典风格没有浮层头部槽位，只能退化成内容顶部的一行（在 sheetContent 的经典分支里渲染）。
    // 横向补边统一走 headerActionInset（与标题行动作区同一套规则），调用点不要再自己加左右 padding。
    val sheetHeaderBottomContent: (@Composable () -> Unit)? = headerBottomContent?.let { body ->
        {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = headerActionInset)
            ) {
                body()
            }
        }
    }

    // 内容区共用：限制最大高度为窗口高度的 maxHeightFraction（内容少时按实际高度，
    // 内容超高时可滚动列表在受限高度内滚动）。
    // 同时标记「内容位于浮层内」：磁贴格子会自动改用紧凑尺寸（64dp），调用方无需传参。
    // 左右外边距只加在这里（内容侧），Sheet 本身的宽度 / 圆角 / 拖动条保持不变；
    // 唯一的例外是铺满型 Sheet 把 insideMargin 置 0（见下），留白同样落在内容侧。
    //
    // 顶部内边距（innerPadding）来自玻璃 Sheet 的浮层头部：rc01 起标题栏不再单独占一行，
    // 而是覆盖在内容之上，头部实测高度用 PaddingValues 回传给内容，由内容决定谁来让出来：
    // - 默认由容器让位（内容整体下移），调用点不用改；
    // - 开了 contentExtendsUnderHeader 则原样交给内容（滚动容器自己消费），
    //   这样列表视口铺满到面板顶端、条目能滑到头部下方（「延伸到顶部」）。
    val contentOwnsHeaderPadding = fullBleedContent || contentExtendsUnderHeader
    val sheetContent: @Composable (PaddingValues) -> Unit = { innerPadding ->
        CompositionLocalProvider(LocalTileCellInOverlay provides true) {
            // 高度上限要按「让位方式」换算，否则两种模式会差一整个头部：
            // - 容器让位：padding 在 heightIn **外侧**，它先占掉头部高度、再把剩余空间交给 heightIn，
            //   所以 heightIn(maxContentHeight) 约束的是纯内容，总高 = maxContentHeight + 头部；
            // - 内容自己让位：heightIn 直接约束整块内容（里面已经含了头部让位段），
            //   若仍写 maxContentHeight，Sheet 就比原来矮了一个头部（首屏可见内容少一截）。
            //   因此这里必须把头部高度补回上限，两种模式的总高才一致。
            val headerHeight = if (contentOwnsHeaderPadding) innerPadding.calculateTopPadding() else 0.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (contentOwnsHeaderPadding) Modifier else Modifier.padding(innerPadding))
                    .heightIn(max = maxContentHeight + headerHeight)
                    .padding(horizontal = contentSideInset)
            ) {
                content(if (contentOwnsHeaderPadding) innerPadding else PaddingValues(0.dp))
            }
        }
    }

    if (glassStyle) {
        GlassBottomSheet(
            show = state.show,
            backdrop = backdrop,
            title = title,
            startAction = sheetStartAction,
            endAction = sheetEndAction,
            // 官方头部槽位：钉在标题下方、参与头部高度测量，列表的 innerPadding 会自动包含它
            headerBottomContent = sheetHeaderBottomContent ?: {},
            modifier = modifier,
            allowDismiss = allowDismiss,
            enableNestedScroll = enableNestedScroll,
            insideMargin = sheetInsideMargin,
            onDismissRequest = { state.dismiss() },
            // 官方组件退场动画完成后回调，避免遮罩常驻吞触摸
            onDismissFinished = { onDismissed?.invoke() },
            content = sheetContent,
        )
    } else {
        WindowBottomSheet(
            show = state.show,
            title = title,
            startAction = sheetStartAction,
            endAction = sheetEndAction,
            modifier = modifier,
            allowDismiss = allowDismiss,
            enableNestedScroll = enableNestedScroll,
            insideMargin = sheetInsideMargin,
            // 请求关闭（遮罩/返回/拖拽）统一交给状态管理，页面无需重复编写
            onDismissRequest = { state.dismiss() },
            onDismissFinished = { onDismissed?.invoke() },
            // 经典风格的头部是堆叠式（独占一行，不覆盖内容），没有浮层槽位：
            // 把头部内容渲染成内容顶部的一行，内容从它下方开始
            // （此时 innerPadding 恒为 0，内容不需要让位）
            content = {
                if (sheetHeaderBottomContent != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        sheetHeaderBottomContent()
                        sheetContent(PaddingValues(0.dp))
                    }
                } else {
                    sheetContent(PaddingValues(0.dp))
                }
            },
        )
    }
}
