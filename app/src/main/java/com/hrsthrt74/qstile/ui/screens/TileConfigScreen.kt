package com.hrsthrt74.qstile.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.withoutEventHandling
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.shapes.Capsule
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.CustomTileUtils
import com.hrsthrt74.qstile.data.TileCapabilityFlags
import com.hrsthrt74.qstile.data.TileCatalog
import com.hrsthrt74.qstile.ui.AppTopBar
import com.hrsthrt74.qstile.ui.TopBarIconButton
import com.hrsthrt74.qstile.ui.TopLayer
import com.hrsthrt74.qstile.ui.asymmetricDropdownPositionProvider
import com.hrsthrt74.qstile.ui.awaitStartupFrames
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.AppChip
import com.hrsthrt74.qstile.ui.components.AppDialog
import com.hrsthrt74.qstile.ui.components.GlassTileOperationMenu
import com.hrsthrt74.qstile.ui.components.TileGridCell
import com.hrsthrt74.qstile.ui.components.rememberDialogState
import com.hrsthrt74.qstile.ui.components.rememberSheetState
import com.hrsthrt74.qstile.ui.components.rememberTileIcon
import com.hrsthrt74.qstile.ui.LocalIsWideScreen
import com.hrsthrt74.qstile.ui.LocalNavPageAction
import com.hrsthrt74.qstile.ui.contentBottomPadding
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.hrsthrt74.qstile.viewmodel.TileConfigViewModel
import com.microsoft.clarity.modifiers.clarityMask
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PlainTooltip
import top.yukonga.miuix.kmp.basic.RichTooltip
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TooltipDefaults
import top.yukonga.miuix.kmp.basic.rememberTooltipState
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassIconDropdownMenu
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.glassShadow
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.menu.WindowIconDropdownMenu
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowListPopup
import androidx.compose.foundation.lazy.grid.items as gridItems

@Composable
fun TileConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    // 获取 ViewModel 实例
    val viewModel: TileConfigViewModel = viewModel()

    // 初始化 ViewModel 的设备信息并开始监听配置变更。
    // 先让出启动首帧后的几帧：本页在冷启动首帧就会随 Pager 一起被组合，若此处立即开跑，
    // 配置流回填后的「整页网格首次组合」（每个磁贴还要 pm.getServiceInfo 取图标）会压住
    // 玻璃底栏指示器所等的那次重组，表现为高亮块晚到、期间点击无响应。详见 awaitStartupFrames。
    LaunchedEffect(Unit) {
        awaitStartupFrames()
        viewModel.initProfile(context)
        viewModel.startConfigObservation(context)
    }

    // 主题设置：由 ExTileTheme 通过 LocalThemeSettings 同步提供（数据就绪后才组合到这里，不会闪烁）
    val themeSettings = LocalThemeSettings.current
    val isDynamicColor = themeSettings.isDynamicColorMode

    // 顶部栏模糊：创建 backdrop 捕获滚动内容，模糊开关关闭或 RuntimeShader 不支持时退回纯色。
    // 顶栏形态（官方玻璃渐进模糊 / 原版顶栏）由 AppTopBar 按 progressiveBlur 开关统一处理。
    val backdrop = rememberBlurBackdrop(enabled = themeSettings.enableBlur)
    // 玻璃弹窗专用 backdrop：官方 demo 为弹窗单独建一个 backdrop 挂在页面根节点，
    // 让弹窗形变动画能采样到含顶栏在内的整页内容（顶栏 backdrop 只挂在滚动内容 Box 上，
    // 覆盖不到顶栏区域，形变时会因采样缺失导致材质跳变）。
    val pageBackdrop = rememberBlurBackdrop(enabled = themeSettings.enableBlur)
    // 屏幕级应用风格标志（复用「渐进模糊」开关，=Glass / 经典）：与 AppTopBar 内部判断一致，
    // 供顶栏槽之外的玻璃菜单层使用——CompositionLocal 只在 AppTopBar 槽内有效。
    // 风格切换与模糊开关解耦：风格=Glass 即用玻璃组件，模糊关闭时组件内部回退纯色。
    val isGlassStyle = themeSettings.progressiveBlur

    val haptic = LocalHapticFeedback.current

    // 「添加磁贴」Sheet 的显示状态（统一由 AppBottomSheet 管理）。
    // 系统磁贴与第三方磁贴已合并到同一个 Sheet，通过顶部的 TabRow 切换来源。
    val addSheetState = rememberSheetState()
    // 当前选中的来源 Tab：0 = 系统磁贴，1 = 第三方磁贴（关闭 Sheet 后复位为 0）
    var addTileTab by remember { mutableIntStateOf(0) }

    // 把「添加磁贴」入口注册到导航栏动作桥：窄屏 + 玻璃风格时，底栏右侧会多出一个跟随按钮
    // （App 层按「当前选中页 == 编辑页」决定是否显示，见 App.kt 的 navTrailingAction）
    val navPageAction = LocalNavPageAction.current
    DisposableEffect(navPageAction, addSheetState) {
        navPageAction?.onAddTile = { addSheetState.show() }
        onDispose { navPageAction?.onAddTile = null }
    }
    // 两个确认操作对话框的显示状态（统一由 AppDialog 管理）
    val clearConfirmDialogState = rememberDialogState()
    // 「恢复默认设置」入口已下架，对应对话框状态一并注释
    // val resetConfirmDialogState = rememberDialogState()
    // 从系统导入磁贴的确认对话框显示状态（统一由 AppDialog 管理）
    val importConfirmDialogState = rememberDialogState()

    // 顶栏「更多」菜单：玻璃形态用 GlassIconDropdownMenu（需自管显示状态与 anchor），
    // 原版形态用 WindowIconDropdownMenu（内部自管状态）。两个状态都提到屏幕级以便顶栏与菜单层共用。
    var showGlassMoreMenu by remember { mutableStateOf(false) }
    val glassMoreAnchor = rememberGlassPopupAnchor()

    // 磁贴操作菜单（玻璃形态）的锚点窗口坐标：每个磁贴图标 Box 都在布局时上报自己的矩形，
    // 打开菜单时按 selectedTileForMenu 取对应值（避免首次点击时坐标尚未上报的问题）。
    val tileMenuAnchorBounds = remember { mutableMapOf<String, androidx.compose.ui.geometry.Rect>() }

    // 菜单项列表：两种形态共用同一份定义（clear / import system）
    val clearMenuText = stringResource(R.string.config_menu_clear)
    val importSystemMenuText = stringResource(R.string.settings_import_system_title)
    val moreMenuEntry = remember(clearMenuText, importSystemMenuText) {
        DropdownEntry(
            items = listOf(
                DropdownItem(text = clearMenuText, onClick = { clearConfirmDialogState.show() }),
                DropdownItem(text = importSystemMenuText, onClick = { importConfirmDialogState.show() }),
            )
        )
    }

    // Toast 文案在 Composable 上下文预解析，避免协程/onClick 内触发非配置感知的资源读取
    val importedToast = stringResource(R.string.settings_imported_toast)
    val importFailedToast = stringResource(R.string.settings_read_failed_toast)

    // 切到「第三方磁贴」Tab 时才加载第三方磁贴服务列表（首次加载较重，避免每次打开 Sheet 都扫描）
    LaunchedEffect(addSheetState.show, addTileTab) {
        if (addSheetState.show && addTileTab == 1) {
            viewModel.loadCustomTileServices(context)
        }
    }

    // 配置尚未从 DataStore 读出前不渲染页面主体（未就绪就空白，与主题/OOBE 的门控策略一致）
    if (!viewModel.isConfigLoaded) return

    // 判断磁贴能否取到真实图标（不含空白占位兜底）：
    // 内置磁贴查静态清单的 iconResId；custom 磁贴运行时查第三方 Service 是否声明了图标；
    // 其余未知磁贴值一律视为无图标。「添加磁贴」Sheet 据此隐藏拿不到图标的磁贴。
    fun hasTileIcon(tile: String): Boolean {
        // 内置磁贴：静态清单有 iconResId 即有图标
        TileCatalog.iconRes(tile)?.let { return true }
        // custom 磁贴：Service 存在且声明了图标才算有
        if (tile.startsWith("custom(")) return CustomTileUtils.hasCustomTileIcon(context, tile)
        // 其他未知磁贴值（如 edit）：无图标
        return false
    }

    // 获取 custom 磁贴的显示名与应用名（缓存避免重复查询）
    @Composable
    fun rememberCustomTileNames(tile: String): Pair<String, String>? {
        if (!tile.startsWith("custom(")) return null
        return remember(tile) { CustomTileUtils.getCustomTileNames(context, tile) }
    }

    /**
     * 顶栏：标题 + 撤销上一步按钮 + More 菜单（清除配置 / 恢复默认）。
     * 撤销按钮仅在存在可撤销快照时显示，出现动画为缩放 0.8→1 + 透明度 0→1 + 模糊 4dp→0dp，
     * 消失动画为反向；不可用时整个图标不显示，故无需置灰，用默认图标色（不强调）。
     * 模糊半径与「设置-外观-模糊效果」联动：enableBlur 关闭时恒定 0dp，总是不模糊。
     */
    @Composable
    fun TileTopAppBar() {
        // 共享的 actions 内容：撤销上一步按钮 + 三点菜单（清除配置 / 恢复默认）。
        // 抽成局部 lambda 变量，供宽/窄屏两个分支复用同一份实现，保证行为完全一致。
        val topBarActions: @Composable RowScope.() -> Unit = {
            // 撤销上一步：仅在存在可撤销快照时显示（一次完整拖拽或一次增删移动算一步）
            AnimatedVisibility(
                visible = viewModel.undoSnapshot != null,
                enter = fadeIn(animationSpec = tween(durationMillis = 200)) +
                    scaleIn(initialScale = 0.5f, animationSpec = tween(durationMillis = 200)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                    scaleOut(targetScale = 0.5f, animationSpec = tween(durationMillis = 150))
            ) {
                // 模糊半径随进入/退出过渡
                // 联动「设置-外观-模糊效果」：enableBlur 关闭时总是不模糊
                val blurRadius by if (LocalThemeSettings.current.enableBlur) {
                    transition.animateFloat(
                        transitionSpec = {
                            if (targetState == EnterExitState.Visible) {
                                tween(durationMillis = 200)
                            } else {
                                tween(durationMillis = 150)
                            }
                        },
                        label = "undoBlur"
                    ) { state ->
                        // 模糊半径 0 <=> 6
                        if (state == EnterExitState.Visible) 0f else 6f
                    }
                } else {
                    remember { mutableFloatStateOf(0f) }
                }
                // 顶栏按钮跟随顶栏形态：玻璃态→GlassIconButton，原版→IconButton
                TopBarIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // 恢复到快照状态（updateGridTiles 会正确派生 expanded/collapsed 并持久化）
                        viewModel.undo(context)
                    },
                    // 与右侧三点菜单保持间距
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        MiuixIcons.Undo,
                        contentDescription = stringResource(R.string.config_undo_desc),
                        modifier = Modifier.blur(blurRadius.dp)
                    )
                }
            }
            if (isGlassStyle) {
                // 玻璃形态：GlassIconButton 作 anchor，GlassIconDropdownMenu 在下方菜单层渲染
                GlassIconButton(
                    onClick = { showGlassMoreMenu = true },
                    modifier = Modifier.glassPopupAnchor(
                        anchor = glassMoreAnchor,
                        cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2,
                        floating = true,
                    ),
                ) {
                    Icon(
                        MiuixIcons.More,
                        contentDescription = stringResource(R.string.config_more_desc),
                        modifier = Modifier.glassPopupAnchorContent(glassMoreAnchor)
                    )
                }
            } else {
                // 原版形态：内部自带普通 IconButton 触发器
                WindowIconDropdownMenu(entry = moreMenuEntry) {
                    Icon(MiuixIcons.More, contentDescription = stringResource(R.string.config_more_desc))
                }
            }
        }
        // 统一自适应顶栏：渐进模糊开→官方 GlassTopAppBar，关→原版顶栏（普通模糊）
        // 宽屏（侧边 NavigationRail）用无大标题形态，窄屏保留大标题形态，跟随 LocalIsWideScreen 联动。
        AppTopBar(
            title = stringResource(R.string.config_title),
            largeTitle = stringResource(R.string.config_title),
            useLargeTitle = !LocalIsWideScreen.current,
            scrollBehavior = scrollBehavior,
            backdrop = backdrop,
            progressive = themeSettings.progressiveBlur,
            actions = topBarActions
        )
    }

    // 小米设备特化：固定卡片（网格中占 1 个不可拖拽 item，不参与排序）
    @Composable
    fun FixedTilesRow() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 俩卡片的外边距
                .padding(horizontal = 6.dp, vertical = 4.dp),
            // 俩卡片中间的间距
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val scope = rememberCoroutineScope()
            viewModel.fixedTileValues.forEach { tile ->
                val tooltipState = rememberTooltipState(isPersistent = true)

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(
                                text = stringResource(R.string.config_fixed_tooltip),
                                style = MiuixTheme.textStyles.footnote1
                            )
                        }
                    },
                    state = tooltipState,
                    modifier = Modifier.weight(1f)
                ) {
                    Card {
                        Row(
                            // 卡片内边距
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch { tooltipState.show() }
                                }
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = rememberTileIcon(tile)
                            Icon(
                                painter = icon,
                                contentDescription = TileCatalog.getDisplayName(tile, viewModel.profile),
                                tint =  if (tile == "cell" && !isDynamicColor) Color(0xFF1FCD39)
                                        else MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )

                            // 磁贴名 和 “固定磁贴”
                            Column(
                                // 图标 <=> 文字 间距
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = TileCatalog.getDisplayName(tile, viewModel.profile),
                                    style = MiuixTheme.textStyles.body1
                                )
                                Text(
                                    text = stringResource(R.string.config_fixed_label),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 网格末尾的添加按钮区域，占满整行。
    // 系统磁贴与第三方磁贴已合并到同一个 Sheet（用 TabRow 切换来源），故只需一个入口按钮。
    @Composable
    fun AddTilesSection() {
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                addSheetState.show()
            },
            colors = ButtonDefaults.buttonColorsPrimary(),
            // 与网格内的磁贴保持同样的左右 8dp 边距，上下留出与列表的间距
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .pressable(
                    interactionSource = null,
                    indication = SinkFeedback(),
                    delay = null
                )
        ) {
            Icon(MiuixIcons.AddCircle, contentDescription = stringResource(R.string.config_add_desc))
            Text(stringResource(R.string.config_add_tiles), modifier = Modifier.padding(start = 8.dp))
        }
    }

    /**
     * 拖拽排序网格主体（一页式布局，含虚线框 overlay）。
     * 按住任意磁贴直接拖动，4 列布局；exTile 磁贴为「分界锚点」——它及其之前的磁贴 = 收起时可见（框外），
     * 之后的磁贴 = 仅展开时可见（框内，渲染在虚线框内）。拖动磁贴跨过 exTile 即自动进出框。
     */
    @Composable
    fun TileGridPane(paddingValues: PaddingValues) {
        // 网格内容顶部避让：Scaffold 内容区从 (0,0) 开始、topBar 叠在上方，
        // 用 contentPadding 让内容从顶栏下方开始（同时内容顶部会进入顶栏采样区供模糊捕获）。
        // 注意：LazyGrid 的 item offset 不包含 contentPadding——contentPadding 是在
        // place 阶段作为 visualOffset 叠加的（见 Compose LazyGridMeasuredItem.place），
        // 虚线框基于 item offset 绘制时必须补回该偏移才能与磁贴/标题行对齐。
        val gridContentTopPadding = paddingValues.calculateTopPadding() + 4.dp
        Column(modifier = Modifier.fillMaxSize()) {
            // 从 ViewModel 获取派生状态
            val gridTiles = viewModel.gridTiles
            val outerTiles = viewModel.outerTiles
            val innerTiles = viewModel.innerTiles
            val hasInner = viewModel.hasInner
            val innerKeys = viewModel.innerKeys
            val renderList = viewModel.renderList
            val headerKey = viewModel.headerKey
            // 底栏「添加磁贴」跟随按钮是否可见：必须与 App.kt 里的判断保持一致
            // （只能挂在本页面提供的动作桥存在时，且窄屏 + 玻璃风格）
            val addTileOnNavBar = LocalNavPageAction.current != null &&
                isGlassStyle && !LocalIsWideScreen.current
            val fixedItemCount = viewModel.fixedItemCount
            val outerCount = outerTiles.size

            // gridItems 的 key 生成：磁贴用 spec 值，标题行用固定 key
            fun itemKeyOf(item: Any): Any = if (item is String) item else headerKey

            val lazyGridState = rememberLazyGridState()
            val reorderableState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
                // 使用 ViewModel 的 gridToData 方法进行索引映射
                val fromData = viewModel.gridToData(from.index)
                val toData = viewModel.gridToData(to.index)
                // 目标落在标题/固定卡片/添加按钮等非数据 item 上时忽略本次移动。
                if (fromData < 0 || toData < 0) return@rememberReorderableLazyGridState
                viewModel.onDragMove(fromData, toData, context)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }

            // 监听拖拽状态变化，把一次完整拖拽（长按→移动跨多格→松手）合并为「一步」可撤销操作。
            LaunchedEffect(reorderableState.isAnyItemDragging) {
                if (reorderableState.isAnyItemDragging) {
                    // 拖拽开始：记录起点
                    viewModel.onDragStart()
                } else {
                    // 拖拽结束：合并为一步
                    viewModel.onDragEnd()
                }
            }

            // 虚线框颜色（@Composable 属性，需在 Composable 上下文取值后供 drawBehind 捕获）
            val frameColor = MiuixTheme.colorScheme.primary

            /**
             * 磁贴操作菜单：移动到顶端 / 移动到底端 / 删除。
             *
             * 玻璃风格走 [GlassTileOperationMenu]（窗口坐标 + 根层渲染），经典风格走 [WindowListPopup]；
             * 两者共用同一份动作定义，锚点来自格子上报的圆形底窗口矩形。
             *
             * @param tile 目标磁贴 value
             * @param gridTiles 当前网格中的磁贴顺序（用于判断顶端/底端是否可点）
             */
            @Composable
            fun TileActionMenu(tile: String, gridTiles: List<String>) {
                // 注意：外层不能用 `showTileMenu` 参与条件，否则关闭时弹窗会直接从组合树移除、
                // 退场动画来不及播放；只在目标磁贴匹配时组合，显隐由 show 参数控制。
                val tileIndex = gridTiles.indexOf(tile)

                val onMoveTop = {
                    viewModel.moveTileToTop(tile, context)
                    viewModel.showTileMenu = false
                }
                val onMoveBottom = {
                    viewModel.moveTileToBottom(tile, context)
                    viewModel.showTileMenu = false
                }
                val onDelete = {
                    viewModel.deleteTile(tile, context)
                    viewModel.showTileMenu = false
                }

                if (isGlassStyle) {
                    // 玻璃风格：官方 GlassDropdownPopup（窗口坐标 + PopupLayout 根层）
                    GlassTileOperationMenu(
                        show = viewModel.showTileMenu,
                        anchorBounds = tileMenuAnchorBounds[tile]
                            ?: androidx.compose.ui.geometry.Rect.Zero,
                        backdrop = pageBackdrop,
                        tileName = TileCatalog.getDisplayName(tile, viewModel.profile),
                        moveTopLabel = stringResource(R.string.config_move_top),
                        moveBottomLabel = stringResource(R.string.config_move_bottom),
                        deleteLabel = stringResource(R.string.common_delete),
                        moveTopEnabled = tileIndex > 0,
                        moveBottomEnabled = tileIndex < gridTiles.size - 1,
                        onMoveTop = onMoveTop,
                        onMoveBottom = onMoveBottom,
                        onDelete = onDelete,
                        onDismissRequest = { viewModel.showTileMenu = false },
                    )
                } else {
                    // 经典风格：原版 WindowListPopup
                    val errorColors = DropdownDefaults.dropdownColors(
                        contentColor = MiuixTheme.colorScheme.error,
                        selectedContentColor = MiuixTheme.colorScheme.error
                    )
                    WindowListPopup(
                        show = viewModel.showTileMenu,
                        popupPositionProvider = asymmetricDropdownPositionProvider(
                            verticalMargin = 8.dp,
                            startMargin = 0.dp,
                            endMargin = 8.dp
                        ),
                        onDismissRequest = { viewModel.showTileMenu = false }
                    ) {
                        ListPopupColumn {
                            Text(
                                text = TileCatalog.getDisplayName(tile, viewModel.profile),
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier
                                    .padding(top = 20.dp)
                                    .padding(start = 20.dp)
                                    .padding(bottom = 16.dp)
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            DropdownImpl(
                                text = stringResource(R.string.config_move_top),
                                optionSize = 3,
                                isSelected = false,
                                index = 0,
                                enabled = tileIndex > 0,
                                onSelectedIndexChange = { onMoveTop() }
                            )
                            DropdownImpl(
                                text = stringResource(R.string.config_move_bottom),
                                optionSize = 3,
                                isSelected = false,
                                index = 1,
                                enabled = tileIndex < gridTiles.size - 1,
                                onSelectedIndexChange = { onMoveBottom() }
                            )
                            DropdownImpl(
                                text = stringResource(R.string.common_delete),
                                optionSize = 3,
                                isSelected = false,
                                index = 2,
                                dropdownColors = errorColors,
                                onSelectedIndexChange = { onDelete() }
                            )
                        }
                    }
                }
            }

            /**
             * 单个磁贴格子的渲染（含拖拽、点击菜单）。虚线框不再画在磁贴上（会随拖拽移动），
             * 改由网格 overlay 统一绘制。
             * 使用 LazyGridItemScope receiver：ReorderableItem 是其扩展函数，只能在网格 item 作用域内调用。
             */
            @Composable
            fun LazyGridItemScope.TileGridItem(
                tile: String,
            ) {
                ReorderableItem(reorderableState, key = tile) { isDragging ->
                    // 拖起时轻微放大并抬高阴影，松手回弹
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.1f else 1f,
                        label = "tileScale"
                    )
                    val elevation by animateDpAsState(
                        targetValue = if (isDragging) 12.dp else 0.dp,
                        label = "tileElevation"
                    )
                    // 标题来源由调用方决定：第三方磁贴用 Service 自己的 label + 应用名，
                    // 系统磁贴用静态清单名 + 原始 value
                    val customNames = rememberCustomTileNames(tile)
                    val displayName = customNames?.first
                        ?: TileCatalog.getDisplayName(tile, viewModel.profile)
                    Box {
                        TileGridCell(
                            icon = rememberTileIcon(tile),
                            title = displayName,
                            subtitle = customNames?.second ?: tile,
                            onClick = {
                                viewModel.selectedTileForMenu = tile
                                viewModel.showTileMenu = true
                            },
                            // 「移动网络」图标用绿色（关闭动态取色时才覆盖，保留品牌色）
                            iconTint = if (tile == "cell" && !isDynamicColor) Color(0xFF1FCD39)
                                       else MiuixTheme.colorScheme.primary,
                            contentDescription = displayName,
                            elevation = elevation,
                            // 玻璃菜单用窗口坐标定位：每个磁贴都上报自己的窗口矩形，
                            // 打开时按 selectedTileForMenu 取用（mutableMap 读取触发不了重组，
                            // 但 showTileMenu 变化会重组，届时 map 已是最新值）
                            onCirclePositioned = { tileMenuAnchorBounds[tile] = it },
                            // 长按拖拽把手 + 拖起缩放挂在最外层；间距与尺寸由组件统一
                            modifier = Modifier
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                                .scale(scale)
                        ) {
                            // 操作菜单锚定在圆形底内部（Popup 自身不受父布局限制）
                            if (viewModel.selectedTileForMenu == tile) {
                                TileActionMenu(tile, gridTiles)
                            }
                        }
                    }
                }
            }

            /**
             * 标题行「展开后显示的磁贴」（span 全行）。
             * 用 ReorderableItem 包裹并仅在空框时 enabled：
             * 空框时标题行作为拖拽落点（拖磁贴到这里 = 追加到框内末尾，库的落点只能是
             * ReorderableItem，见 findTargetItem 的 reorderableKeys 过滤）；
             * 非空框时不注册 reorderableKeys、不作为落点，不影响正常排序拖拽。
             * 不挂拖拽句柄，标题行自身不可拖动。框顶线由网格 overlay 绘制。
             */
            @Composable
            fun LazyGridItemScope.SectionHeaderItem() {
                ReorderableItem(
                    reorderableState,
                    key = headerKey,
                    enabled = innerTiles.isEmpty(),
                ) {
                    // 标题行：文字 + 说明图标。图标点击弹出 RichTooltip。
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 文本距离虚线框的边距
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.config_section_expanded),
                            style = MiuixTheme.textStyles.subtitle,
                            color = frameColor,
                        )
                        // Info 图标，点击显示
                        val infoTooltipState = rememberTooltipState(isPersistent = true)
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                positioning = TooltipAnchorPosition.Below
                            ),
                            tooltip = {
                                RichTooltip(
                                    title = {Text(text = stringResource(R.string.config_tip_title), style = MiuixTheme.textStyles.subtitle)}
                                ) {
                                    Text(
                                        text = stringResource(R.string.config_tip_content),
                                        style = MiuixTheme.textStyles.body2,
                                    )
                                }
                            },
                            state = infoTooltipState,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Icon(
                                MiuixIcons.Info,
                                contentDescription = stringResource(R.string.config_tip_desc),
                                tint = frameColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    // indication = null：去掉点击压暗/涟漪特效
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch { infoTooltipState.show() }
                                    }
                            )
                        }
                    }
                }
            }

            // 系统 overscroll effect（与 LazyVerticalGrid 默认一致，Miuix 定制同样生效）。
            // 通过拆分「渲染/事件」让虚线框进入系统拉伸变换内部：
            // - LazyVerticalGrid 接收 withoutVisualEffect（事件照旧进系统 effect，手感不变，不渲染）
            // - modifier 最外层挂 withoutEventHandling（系统拉伸渲染节点），包住下面的 drawBehind 虚线框，
            //   回弹时框与磁贴被同一个变换拉伸，实现跟随
            val systemOverscrollEffect = rememberOverscrollEffect()

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = lazyGridState,
                overscrollEffect = systemOverscrollEffect?.withoutVisualEffect(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // 系统 overscroll 渲染节点（null 时不渲染），置于虚线框外层使其同被拉伸
                    .overscroll(systemOverscrollEffect?.withoutEventHandling())
                    // 虚线框覆盖层：画在网格内容上层，不随磁贴拖动/缩放移动（修复拖拽带线）；
                    // 底线按末行实际行底绘制（修复磁贴文本行数不同导致底线不齐）。
                    // 框位于 overscroll 渲染变换内部，回弹时与磁贴一起被系统拉伸跟随。
                    .drawBehind {
                        // 「展开后」区域（虚线框）
                        if (hasInner) {
                            val layoutInfo = lazyGridState.layoutInfo
                            val visible = layoutInfo.visibleItemsInfo
                            val strokeWidth = 1.5.dp.toPx()
                            val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                            // 圆角半径 16dp
                            val cornerRadius = CornerRadius(16.dp.toPx())
                            // 框左右边界：撑满宽度（不随磁贴数量收窄），
                            // 距屏幕左右边缘各留 10dp 水平边距，磁贴(contentPadding 10dp)完整在框内
                            val left = 10.dp.toPx()
                            val right = size.width - 10.dp.toPx()
                            // 标题行 item（画顶线的锚点）
                            val headerItem = visible.find { it.key == headerKey }
                            // 可见的框内磁贴 item
                            val innerVisible = visible.filter { it.key in innerKeys }
                            // LazyGrid 的 item offset 不含 contentPadding（contentPadding 在
                            // place 阶段作为 visualOffset 叠加），这里补回 contentPadding.top，
                            // 否则虚线框会比磁贴/标题行整体高出顶栏高度
                            val contentTopOffset = gridContentTopPadding.toPx()
                            val top: Float
                            val bottom: Float
                            if (innerVisible.isEmpty()) {
                                // 框内无磁贴：空框只包住标题行
                                if (headerItem == null) return@drawBehind
                                top = headerItem.offset.y.toFloat() + contentTopOffset
                                bottom = (headerItem.offset.y + headerItem.size.height).toFloat() + contentTopOffset
                            } else {
                                // 有框内磁贴：框顶用标题行，框底用末行实际行底
                                val lastRowStart = ((innerTiles.size - 1) / 4) * 4
                                val lastRowKeys = innerTiles.subList(lastRowStart, innerTiles.size).toSet()
                                val lastRowVisible = innerVisible.filter { it.key in lastRowKeys }
                                val lastRowComplete = lastRowVisible.size == lastRowKeys.size
                                top = if (headerItem != null) headerItem.offset.y.toFloat() + contentTopOffset
                                      else innerVisible.minOf { it.offset.y }.toFloat() + contentTopOffset
                                bottom = if (lastRowComplete) lastRowVisible.maxOf { it.offset.y + it.size.height }.toFloat() + contentTopOffset
                                         else innerVisible.maxOf { it.offset.y + it.size.height }.toFloat() + contentTopOffset
                            }
                            // 绘制裁剪到网格视口内，滚出屏幕的部分被裁掉
                            clipRect {
                                // 圆角虚线框
                                drawRoundRect(
                                    color = frameColor,
                                    topLeft = Offset(left, top),
                                    size = Size(right - left, (bottom - top).coerceAtLeast(0f)),
                                    cornerRadius = cornerRadius,
                                    style = Stroke(width = strokeWidth, pathEffect = dash),
                                )
                            }
                        }
                    }
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
                contentPadding = PaddingValues(
                    // 磁贴与屏幕边缘 10dp：与虚线框的 10dp 水平边距对应，
                    // 使框贴边撑满时磁贴距框线正好 10dp。
                    // 加上 Scaffold 已算好的挖孔/导航条 insets：竖屏水平为 0 保持原间距，
                    // 横屏/反向横屏自动避让左右摄像头挖孔；宽屏起始侧已消费，自动为 0
                    top = gridContentTopPadding,
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current) + 10.dp,
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current) + 10.dp,
                    bottom = contentBottomPadding() + 96.dp
                    )
            ) {
                // 固定卡片（小米特化，不参与拖拽）
                if (viewModel.profile.isXiaomi && viewModel.fixedTileValues.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FixedTilesRow()
                    }
                }

                // 统一渲染列表：框外磁贴 / 标题行 / 框内磁贴。所有磁贴在同一 items 块内渲染，
                // 跨框拖拽时 item 不跨组合块、key 不变，避免 ReorderableItem 拖拽句柄丢失。
                gridItems(
                    renderList,
                    key = { itemKeyOf(it) },
                    // 磁贴占 1 格；标题行「展开后显示的磁贴」占满整行
                    span = { item -> if (item is String) GridItemSpan(1) else GridItemSpan(maxLineSpan) }
                ) { item ->
                    if (item is String) {
                        TileGridItem(tile = item)
                    } else {
                        SectionHeaderItem()
                    }
                }

                // 窄屏 + 玻璃风格时，底栏已有「添加磁贴」跟随按钮，
                // 这里就不再重复给网格末尾的按钮（避免两个入口同时出现）
                if (!addTileOnNavBar) {
                    // 磁贴区域 / 按钮区域之间的间隔
                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    // 添加按钮区域，占满整行
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        AddTilesSection()
                    }
                }
            }
        }
    }

    // 系统磁贴选择区：按分类分组展示可用系统磁贴，点击添加到框内末尾
    @Composable
    // topPadding：列表顶部预留的高度（= 头部浮层高度），让列表从 Sheet 顶部铺满、滚动时滑到头部下方
    fun SystemTilePicker(topPadding: Dp, continuousAdd: Boolean) {
        // 当前已添加的磁贴（含固定卡片、edit、exTile，用于过滤重复项）
        val currentTiles = viewModel.config.expandedTiles

        // 读取调试 flag（订阅变化），切换后刷新可用磁贴列表
        // showUnavailableTiles：调试开关，开启后额外显示预设内因设备能力不满足而不可用的磁贴
        val capabilityKey = listOf(
            TileCapabilityFlags.satelliteOverride,
            TileCapabilityFlags.coolingFanOverride,
            TileCapabilityFlags.propOverrides.toMap(),
            TileCapabilityFlags.featureOverrides.toMap(),
            TileCapabilityFlags.showUnavailableTiles,
            TileCapabilityFlags.showNoIconTiles,
        )
        val availableTiles = remember(currentTiles, viewModel.profile, capabilityKey) {
            // 基础来源：默认仅设备可用磁贴；调试开关开启时放宽为全量静态清单（不可用磁贴降透明度展示）
            val catalog = if (TileCapabilityFlags.showUnavailableTiles) {
                TileCatalog.systemTiles
            } else {
                TileCatalog.getAvailableTiles(viewModel.profile)
            }
            // 过滤已添加磁贴；无图标磁贴默认隐藏（空白占位不展示），调试开关开启时保留（降透明度展示）
            catalog.filter {
                it.value !in currentTiles && (hasTileIcon(it.value) || TileCapabilityFlags.showNoIconTiles)
            }
        }
        // 调试开关开启时，用于区分「不可用」磁贴的能力快照（渲染时降透明度）：
        // 设备能力不满足，或因无图标而本应隐藏（现被调试开关保留展示）的磁贴
        val availableValues = remember(availableTiles, viewModel.profile, capabilityKey) {
            availableTiles.filter {
                it.isAvailable(viewModel.profile) && hasTileIcon(it.value)
            }.map { it.value }.toSet()
        }

        // 根据搜索关键字过滤：同时匹配磁贴显示名和 value（不区分大小写）
        val query = viewModel.addSearchQuery.trim()
        val filteredTiles = remember(availableTiles, query) {
            if (query.isEmpty()) {
                availableTiles
            } else {
                availableTiles.filter { tile ->
                    tile.displayName.contains(query, ignoreCase = true) ||
                        tile.value.contains(query, ignoreCase = true)
                }
            }
        }

        // 按分类分组
        val tilesByCategory = remember(filteredTiles) {
            filteredTiles.groupBy { it.category }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (filteredTiles.isEmpty()) {
                // 无可添加磁贴 / 搜索无结果提示（区分文案）
                Text(
                    text = if (query.isEmpty()) stringResource(R.string.config_no_tiles)
                           else stringResource(R.string.config_no_matching_tiles),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        // 顶部让出头部浮层高度，避免提示文字被 TabRow / 搜索框挡住
                        .padding(top = topPadding, bottom = 24.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
                    contentPadding = PaddingValues(top = topPadding + 8.dp, bottom = 64.dp)
                ) {
                    tilesByCategory.forEach { (category, tiles) ->
                        // 分类标题
                        item(key = "category:$category", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = category,
                                style = MiuixTheme.textStyles.subtitle,
                                modifier = Modifier
                                    // 分类被整组加完时跟着淡出，不要「啪」地消失
                                    // （animateItem 要挂在 item 根节点上，故放在 padding 之前）
                                    .animateItem(fadeInSpec = null, fadeOutSpec = tween(160))
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }

                        // 磁贴网格
                        //
                        // key 必须用磁贴自身的 value（唯一且稳定）：被添加后这一格会从数据源里
                        // 被过滤掉，若沿用 items(tiles.size) 的「按 index 认身份」，animateItem
                        // 会把淡出播到末位的另一格上（内容瞬间变成别人的）。
                        gridItems(tiles, key = { it.value }) { tile ->
                            // 调试开关保留展示的「不可用/无图标」磁贴整体降透明度区分
                            val isUnavailable = tile.value !in availableValues
                            TileGridCell(
                                icon = rememberTileIcon(tile.value),
                                // 标题来源：系统磁贴用静态清单里的显示名，副标题直接显示原始 value
                                title = tile.displayName,
                                subtitle = tile.value,
                                contentDescription = tile.displayName,
                                onClick = {
                                    // 添加到网格末尾（框内末尾），拖出框外即可让收起时也显示
                                    viewModel.addTile(tile.value, context)
                                    // 「连续放置」开启时不收起 Sheet，可以接着点下一个
                                    if (!continuousAdd) addSheetState.dismiss()
                                },
                                modifier = Modifier
                                    // 消失动画：淡出这一格，同时让后面的格子滑过来补位
                                    // （placement 用 animateItem 的默认 spring，不用另配）
                                    .animateItem(fadeInSpec = null, fadeOutSpec = tween(160))
                                    .alpha(if (isUnavailable) 0.4f else 1f),
                            )
                        }

                        // 分类结束分割线
                        item(key = "divider:$category", span = { GridItemSpan(maxLineSpan) }) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .animateItem(fadeInSpec = null, fadeOutSpec = tween(160))
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // 第三方磁贴选择区：展示其他应用的 QS Tile 服务（需 QUERY_ALL_PACKAGES 权限）
    @Composable
    // topPadding：同 SystemTilePicker，为头部浮层预留顶部内边距
    fun ThirdPartyTilePicker(topPadding: Dp, continuousAdd: Boolean) {
        // 检查是否有 QUERY_ALL_PACKAGES 权限
        val hasQueryPermission = remember {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.QUERY_ALL_PACKAGES
            ) == PackageManager.PERMISSION_GRANTED
        }

        // 获取所有 QS Tile 服务（后台线程已加载，见顶部 LaunchedEffect）
        val services = viewModel.allTileServices

        // 获取当前已添加的磁贴
        val currentTiles = viewModel.config.expandedTiles

        // 获取系统预定义的磁贴 ComponentName 集合（用于过滤）
        val systemTileComponents = remember {
            TileCatalog.getAllValues()
                .filter { it.startsWith("custom(") }
                .mapNotNull { CustomTileUtils.parseCustomComponent(it) }
                .toSet()
        }

        // 获取已添加的磁贴 ComponentName 集合（用于过滤）
        val currentTileComponents = remember(currentTiles) {
            currentTiles
                .filter { it.startsWith("custom(") }
                .mapNotNull { CustomTileUtils.parseCustomComponent(it) }
                .toSet()
        }

        // 过滤掉已添加的磁贴和系统预定义的磁贴
        val availableTileServices = remember(services, currentTileComponents, systemTileComponents) {
            services
                ?.filter { service ->
                    val component = ComponentName(service.packageName, service.className)
                    // 过滤掉已添加的磁贴
                    if (component in currentTileComponents) return@filter false
                    // 过滤掉系统预定义的磁贴
                    component !in systemTileComponents
                }
                ?: emptyList()
        }

        when {
            // 无权限时显示引导页面
            !hasQueryPermission -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 顶部让出头部浮层（TabRow / 搜索框）的高度
                        .padding(start = 24.dp, end = 24.dp, top = topPadding + 24.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.config_permission_needed),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // 跳转到应用详情设置页面
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.config_go_grant))
                    }
                }
            }
            // 加载中：显示 Miuix 无限进度指示器
            services == null -> {
                // 用 fillMaxHeight 撑满，与加载完成后的 sheet 高度一致，避免高度突变
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    InfiniteProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.common_loading),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
            // 有权限但过滤后没有可用的磁贴
            availableTileServices.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 顶部让出头部浮层（TabRow / 搜索框）的高度
                        .padding(start = 24.dp, end = 24.dp, top = topPadding + 24.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.config_no_custom_tiles),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            // 显示磁贴网格
            else -> {
                // 根据搜索关键字过滤：同时匹配磁贴 label、应用名和包名（不区分大小写）
                val query = viewModel.customSearchQuery.trim()
                val filteredServices = remember(availableTileServices, query) {
                    if (query.isEmpty()) {
                        availableTileServices
                    } else {
                        availableTileServices.filter { service ->
                            service.label.contains(query, ignoreCase = true) ||
                                service.appName.contains(query, ignoreCase = true) ||
                                service.packageName.contains(query, ignoreCase = true)
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    if (filteredServices.isEmpty()) {
                        // 搜索无结果提示
                        Text(
                            text = stringResource(R.string.config_no_matching_tiles),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier
                                .fillMaxWidth()
                                // 顶部让出头部浮层高度
                                .padding(top = topPadding, bottom = 24.dp)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .scrollEndHaptic(HapticFeedbackType.TextHandleMove)
                                .clarityMask(),
                            contentPadding = PaddingValues(top = topPadding + 8.dp, bottom = 64.dp)
                        ) {
                            gridItems(filteredServices, key = { it.packageName + "/" + it.className }) { service ->
                                val tileValue = "custom(${service.packageName}/${service.className})"
                                // 图标已在后台批量预取（Service 图标或 blank 占位），渲染时不再触发 IPC
                                val bitmap = remember(service.icon) { service.icon.toBitmap() }
                                TileGridCell(
                                    icon = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) },
                                    // 标题来源：第三方磁贴用 Service 自己声明的 label，副标题显示所属应用名
                                    title = service.label,
                                    subtitle = service.appName,
                                    contentDescription = service.label,
                                    onClick = {
                                        viewModel.addTile(tileValue, context)
                                        // 「连续放置」开启时不收起 Sheet
                                        if (!continuousAdd) addSheetState.dismiss()
                                    },
                                    // 消失动画：添加后被过滤出列表时淡出，后面的格子滑过来补位
                                    // （key 已在 gridItems 上按「包名/类名」声明，身份稳定）
                                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = tween(160)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 「添加磁贴」底部 Sheet：系统磁贴与第三方磁贴合并为同一个 Sheet。
    // 头部默认显示 TabRow 切换来源（0=系统磁贴 / 1=第三方磁贴）；点击标题栏右侧的搜索按钮后，
    // TabRow 会在同一位置变换为搜索框（AnimatedContent），退出搜索再变回来。
    @Composable
    fun AddTileSheet() {
        // 头部是否处于搜索态：false = 显示 TabRow，true = 显示搜索框
        var searchActive by remember { mutableStateOf(false) }
        // 「连续放置」开关：开启后点击磁贴不会收起 Sheet，可以连续添加多个
        var continuousAdd by remember { mutableStateOf(false) }

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
            state = addSheetState,
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
            // 标题栏右侧：「连续放置」开关（自绘胶囊 Chip，靠底色区分开关态，
            // 选中 = primaryContainer 底 / onPrimaryContainer 文字）
            // 开启后点磁贴只添加、不收起 Sheet，方便一次放多个
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
                                0 -> SystemTilePicker(topPadding = headerHeight, continuousAdd = continuousAdd)
                                else -> ThirdPartyTilePicker(topPadding = headerHeight, continuousAdd = continuousAdd)
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
                            // 塞在 clip 之内只会被裁成一圈硬边（之前正是因此把 shadow 传成了 null，
                            // 结果胶囊完全没有阴影）。现在改成**画在裁剪之前**：
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

    // 两个确认操作对话框（清除当前配置 / 恢复默认设置）
    @Composable
    fun ConfirmationDialogs() {
        AppDialog(
            state = clearConfirmDialogState,
            title = stringResource(R.string.config_menu_clear),
            summary = stringResource(R.string.config_clear_summary),
            confirmText = stringResource(R.string.config_clear_confirm),
            destructive = true,
            onConfirm = {
                viewModel.updateGridTiles(emptyList(), context)
            }
        )

        // 「恢复默认设置」入口已下架，对应确认对话框一并注释
        // AppDialog(
        //     state = resetConfirmDialogState,
        //     title = stringResource(R.string.config_menu_reset),
        //     summary = stringResource(R.string.config_reset_summary),
        //     confirmText = stringResource(R.string.config_reset_confirm),
        //     destructive = true,
        //     onConfirm = {
        //         viewModel.updateConfig(TileConfig(), context)
        //     }
        // )

        // 从系统导入磁贴确认对话框：与设置页共用同一组文案资源
        AppDialog(
            state = importConfirmDialogState,
            title = stringResource(R.string.settings_import_system_title),
            summary = stringResource(R.string.settings_import_system_dialog_summary),
            confirmText = stringResource(R.string.common_confirm),
            onConfirm = {
                scope.launch {
                    val success = viewModel.importFromSystem(context)
                    Toast.makeText(
                        context,
                        if (success) importedToast else importFailedToast,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    Scaffold(
        // 弹窗专用 backdrop 挂在页面根节点（覆盖含顶栏的整页），供玻璃菜单形变动画采样
        modifier = if (pageBackdrop != null) Modifier.layerBackdrop(pageBackdrop) else Modifier,
        topBar = {
            // TileTopAppBar 内部已通过 AppTopBar 统一处理渐进模糊/原版顶栏切换
            TileTopAppBar()
        }
    ) { paddingValues ->
        // 滚动内容挂载 backdrop，供顶部栏模糊捕获
        Box(
            modifier = if (backdrop != null) Modifier.fillMaxSize().layerBackdrop(backdrop) else Modifier.fillMaxSize()
        ) {
            TileGridPane(paddingValues)
        }
    }

    // 玻璃形态的「更多」菜单：提升到 TopLayer，避免被顶栏/玻璃侧栏遮挡或被 Pager 页面裁剪。
    // 与官方 demo 一致：anchorContent 只画图标，不重复挂 glassPopupAnchorContent（那会回写 anchor 坐标干扰形变）。
    if (isGlassStyle) {
        TopLayer {
            GlassIconDropdownMenu(
                show = showGlassMoreMenu,
                onDismissRequest = { showGlassMoreMenu = false },
                anchor = glassMoreAnchor,
                anchorContent = {
                    Icon(
                        MiuixIcons.More,
                        contentDescription = stringResource(R.string.config_more_desc),
                        modifier = Modifier.size(28.dp),
                    )
                },
                entries = listOf(moreMenuEntry),
                backdrop = pageBackdrop,
                collapseOnSelection = true,
            )
        }
    }

    // 合并后的「添加磁贴」Sheet（内置 TabRow 切换系统 / 第三方来源）
    AddTileSheet()

    ConfirmationDialogs()
}