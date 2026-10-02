package com.hrsthrt74.qstile.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.withoutEventHandling
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.CustomTileUtils
import com.hrsthrt74.qstile.data.DeviceProfile
import com.hrsthrt74.qstile.data.TileCapabilityFlags
import com.hrsthrt74.qstile.data.TileCatalog
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.ui.BlurredBar
import com.hrsthrt74.qstile.ui.asymmetricDropdownPositionProvider
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.AppDialog
import com.hrsthrt74.qstile.ui.components.rememberDialogState
import com.hrsthrt74.qstile.ui.components.rememberSheetState
import com.hrsthrt74.qstile.ui.LocalIsWideScreen
import com.hrsthrt74.qstile.ui.contentBottomPadding
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.hrsthrt74.qstile.viewmodel.TileConfigViewModel
import com.microsoft.clarity.modifiers.clarityMask
import com.microsoft.clarity.modifiers.clarityUnmask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TooltipDefaults
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTooltipState
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.menu.WindowIconDropdownMenu
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

    // 初始化 ViewModel 的设备信息并开始监听配置变更
    LaunchedEffect(Unit) {
        viewModel.initProfile(context)
        viewModel.startConfigObservation(context)
    }

    // 主题设置：由 ExTileTheme 通过 LocalThemeSettings 同步提供（数据就绪后才组合到这里，不会闪烁）
    val isDynamicColor = LocalThemeSettings.current.isDynamicColorMode

    // 顶部栏模糊：创建 backdrop 捕获滚动内容，模糊开关关闭或 RuntimeShader 不支持时退回纯色。
    // 渐进模糊开启时由 BlurredBar 内部切换为渐变模糊（顶部最强、向下过渡到清晰）。
    val backdrop = rememberBlurBackdrop(enabled = LocalThemeSettings.current.enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    val haptic = LocalHapticFeedback.current

    // 添加磁贴 / 添加第三方磁贴 Sheet 的显示状态（统一由 AppBottomSheet 管理）
    val addSheetState = rememberSheetState()
    val customSheetState = rememberSheetState()
    // 两个确认操作对话框的显示状态（统一由 AppDialog 管理）
    val clearConfirmDialogState = rememberDialogState()
    // 「恢复默认设置」入口已下架，对应对话框状态一并注释
    // val resetConfirmDialogState = rememberDialogState()
    // 从系统导入磁贴的确认对话框显示状态（统一由 AppDialog 管理）
    val importConfirmDialogState = rememberDialogState()

    // Toast 文案在 Composable 上下文预解析，避免协程/onClick 内触发非配置感知的资源读取
    val importedToast = stringResource(R.string.settings_imported_toast)
    val importFailedToast = stringResource(R.string.settings_read_failed_toast)

    // 打开「添加第三方磁贴」sheet 时，加载第三方磁贴服务列表
    LaunchedEffect(customSheetState.show) {
        if (customSheetState.show) {
            viewModel.loadCustomTileServices(context)
        }
    }

    // 配置尚未从 DataStore 读出前不渲染页面主体（未就绪就空白，与主题/OOBE 的门控策略一致）
    if (!viewModel.isConfigLoaded) return

    // 获取磁贴图标的辅助函数：内置磁贴查静态清单；custom 磁贴运行时取第三方 Service 自身图标；
    // 拿不到时一律回退空白占位图标（tile_blank，不回退应用图标——染色后会变成实心圆角矩形），
    // 保证始终有图标可渲染。
    @Composable
    fun rememberTileIcon(tile: String): Painter {
        // 内置磁贴：静态清单直查
        TileCatalog.iconRes(tile)?.let { return painterResource(it) }
        // custom 磁贴：getCustomTileIcon 内部已保证非空（blank 兜底）；其他未知磁贴值直接用 blank 占位
        val drawable = remember(tile) {
            if (tile.startsWith("custom(")) CustomTileUtils.getCustomTileIcon(context, tile)
            else null
        } ?: context.getDrawable(R.drawable.tile_blank)!!
        val bitmap = remember(drawable) { drawable.toBitmap() }
        return remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
    }

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
                IconButton(
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
            val entry = DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = stringResource(R.string.config_menu_clear),
                        onClick = { clearConfirmDialogState.show() }
                    ),
                    // 「恢复默认设置」用处不大，暂时下架（确认对话框与状态同步注释，需要时一并恢复）
                    // DropdownItem(
                    //     text = stringResource(R.string.config_menu_reset),
                    //     onClick = { resetConfirmDialogState.show() }
                    // ),
                    // 从系统导入磁贴：复用设置页的确认对话框文案，确认后读取系统 QS 列表覆盖展开配置
                    DropdownItem(
                        text = stringResource(R.string.settings_import_system_title),
                        onClick = { importConfirmDialogState.show() }
                    )
                )
            )
            WindowIconDropdownMenu(entry = entry) {
                Icon(MiuixIcons.More, contentDescription = stringResource(R.string.config_more_desc))
            }
        }
        // 宽屏（侧边 NavigationRail）用无大标题的 SmallTopAppBar，窄屏保留大标题 TopAppBar。
        // 顶栏形态跟随 LocalIsWideScreen 切换，与底部/侧边导航的布局联动。
        if (LocalIsWideScreen.current) {
            SmallTopAppBar(
                title = stringResource(R.string.config_title),
                color = barColor,
                scrollBehavior = scrollBehavior,
                actions = topBarActions
            )
        } else {
            TopAppBar(
                title = "",
                largeTitle = stringResource(R.string.config_title),
                color = barColor,
                scrollBehavior = scrollBehavior,
                actions = topBarActions
            )
        }
    }

    // 小米设备特化：固定卡片（网格中占 1 个不可拖拽 item，不参与排序）
    @Composable
    fun LazyGridItemScope.FixedTilesRow() {
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

    // 网格末尾的添加按钮区域（添加磁贴 / 添加第三方磁贴），占满整行
    @Composable
    fun LazyGridItemScope.AddTilesSection() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        addSheetState.show()
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressable(
                            interactionSource = null,
                            indication = SinkFeedback(),
                            delay = null
                        )
                ) {
                    Icon(MiuixIcons.AddCircle, contentDescription = stringResource(R.string.config_add_desc))
                    Text(stringResource(R.string.config_add_tiles), modifier = Modifier.padding(start = 8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        customSheetState.show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressable(
                            interactionSource = null,
                            indication = SinkFeedback(),
                            delay = null
                        )
                ) {
                    Icon(MiuixIcons.AddCircle, contentDescription = stringResource(R.string.config_add_custom_desc))
                    Text(stringResource(R.string.config_add_custom_tiles), modifier = Modifier.padding(start = 8.dp))
                }
            }
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
             * 单个磁贴格子的渲染（含拖拽、点击菜单）。虚线框不再画在磁贴上（会随拖拽移动），
             * 改由网格 overlay 统一绘制。
             * 使用 LazyGridItemScope receiver：ReorderableItem 是其扩展函数，只能在网格 item 作用域内调用。
             */
            @Composable
            fun LazyGridItemScope.TileGridItem(
                tile: String,
            ) {
                ReorderableItem(reorderableState, key = tile) { isDragging ->
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.1f else 1f,
                        label = "tileScale"
                    )
                    val elevation by animateDpAsState(
                        targetValue = if (isDragging) 12.dp else 0.dp,
                        label = "tileElevation"
                    )
                    Box {
                        // 一个 Grid（图标+图标背景+名+名）
                        Column(
                            modifier = Modifier
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                                .scale(scale)
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .pressable(
                                        interactionSource = null,
                                        indication = SinkFeedback(sinkAmount = 0.9f, animationSpec = spring(0.8f, 120f)),
                                        delay = null
                                    )
                                    .shadow(elevation, CircleShape)
                                    .clip(CircleShape)
                                    .background(MiuixTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.selectedTileForMenu = tile
                                        viewModel.showTileMenu = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = rememberTileIcon(tile)
                                Icon(
                                    painter = icon,
                                    contentDescription = TileCatalog.getDisplayName(tile, viewModel.profile),
                                    tint = if (tile == "cell" && !isDynamicColor) Color(0xFF1FCD39) else MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )

                                // 磁贴操作弹出菜单（锚定到 icon 外的圆形 Box，而非整个 grid item）。
                                // 注意：这里不能用 `showTileMenu` 参与条件，否则关闭时整个 WindowListPopup
                                // 会直接从组合树移除，ListPopupLayout 的退场动画（缩放/透明渐出）来不及播放。
                                // 因此只在目标磁贴匹配时组合，show 参数单独控制显隐，让组件内部走完退场动画。
                                if (viewModel.selectedTileForMenu == tile) {
                                    val gridTilesForMenu = gridTiles
                                    val tileIndexForMenu = gridTilesForMenu.indexOf(tile)

                                    // 删除按钮的错误颜色
                                    val errorColors = DropdownDefaults.dropdownColors(
                                        contentColor = MiuixTheme.colorScheme.error,
                                        selectedContentColor = MiuixTheme.colorScheme.error
                                    )

                                    WindowListPopup(
                                        show = viewModel.showTileMenu,
                                        // 垂直间距 8dp：popup 与图标保持间距；左右边距独立设置左 0 右 8
                                        popupPositionProvider = asymmetricDropdownPositionProvider(
                                            verticalMargin = 8.dp,
                                            startMargin = 0.dp,
                                            endMargin = 8.dp
                                        ),
                                        onDismissRequest = { viewModel.showTileMenu = false }
                                    ) {
                                        ListPopupColumn {
                                            // 磁贴名称（不可点击）
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
                                            // 移动到顶端（框外第一个，收起时最先显示）
                                            DropdownImpl(
                                                text = stringResource(R.string.config_move_top),
                                                optionSize = 3,
                                                isSelected = false,
                                                index = 0,
                                                enabled = tileIndexForMenu > 0,
                                                onSelectedIndexChange = {
                                                    viewModel.moveTileToTop(tile, context)
                                                    viewModel.showTileMenu = false
                                                }
                                            )
                                            // 移动到底端（框内最后一个，仅展开时显示）
                                            DropdownImpl(
                                                text = stringResource(R.string.config_move_bottom),
                                                optionSize = 3,
                                                isSelected = false,
                                                index = 1,
                                                enabled = tileIndexForMenu < gridTilesForMenu.size - 1,
                                                onSelectedIndexChange = {
                                                    viewModel.moveTileToBottom(tile, context)
                                                    viewModel.showTileMenu = false
                                                }
                                            )
                                            // 删除（错误颜色）
                                            DropdownImpl(
                                                text = stringResource(R.string.common_delete),
                                                optionSize = 3,
                                                isSelected = false,
                                                index = 2,
                                                dropdownColors = errorColors,
                                                onSelectedIndexChange = {
                                                    viewModel.deleteTile(tile, context)
                                                    viewModel.showTileMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 第一行：磁贴显示名（第三方磁贴显示其 label，而非包名后缀）
                            Text(
                                text = rememberCustomTileNames(tile)?.first
                                    ?: TileCatalog.getDisplayName(tile, viewModel.profile),
                                style = MiuixTheme.textStyles.body2,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 第二行：系统磁贴显示 value；第三方磁贴显示应用名
                            Text(
                                text = rememberCustomTileNames(tile)?.second ?: tile,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
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
                    bottom = contentBottomPadding()
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

                // 磁贴区域 / 按钮区域之间的间隔
                item { Spacer(modifier = Modifier.height(24.dp)) }

                // 添加按钮区域，占满整行
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AddTilesSection()
                }
            }
        }
    }

    // 「添加磁贴」底部 Sheet：按分类分组展示可用系统磁贴，点击添加到框内末尾
    @Composable
    fun AddTileSheet() {
        AppBottomSheet(
            state = addSheetState,
            title = stringResource(R.string.config_add_tiles),
            // 关闭动画完成后清空搜索关键字，避免下次打开残留
            onDismissed = { viewModel.clearAddSearchQuery() },
        ) {
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
                // 搜索框（支持按磁贴名或 value 搜索）
                SearchBar(
                    expanded = false,
                    onExpandedChange = { expanded ->
                        if (!expanded) {
                            addSheetState.dismiss()
                        }
                    },
                    insideMargin = DpSize(0.dp, 4.dp),
                    inputField = {
                        InputField(
                            query = viewModel.addSearchQuery,
                            onQueryChange = { viewModel.addSearchQuery = it },
                            onSearch = {},
                            // 避免打开 sheet 时自动聚焦弹键盘
                            expanded = false,
                            onExpandedChange = {},
                            label = stringResource(R.string.common_search),
                            color = MiuixTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { }

                if (filteredTiles.isEmpty()) {
                    // 无可添加磁贴 / 搜索无结果提示（区分文案）
                    Text(
                        text = if (query.isEmpty()) stringResource(R.string.config_no_tiles)
                               else stringResource(R.string.config_no_matching_tiles),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 64.dp)
                    ) {
                        tilesByCategory.forEach { (category, tiles) ->
                            // 分类标题
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = category,
                                    style = MiuixTheme.textStyles.subtitle,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }

                            // 磁贴网格
                            items(tiles.size) { index ->
                                val tile = tiles[index]
                                // 调试开关保留展示的「不可用/无图标」磁贴整体降透明度区分
                                val isUnavailable = tile.value !in availableValues
                                Column(
                                    modifier = Modifier
                                        // 竖向要比横向大一点
                                        .padding(horizontal = 4.dp, vertical = 12.dp)
                                        .alpha(if (isUnavailable) 0.4f else 1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(MiuixTheme.colorScheme.secondaryVariant)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                // 添加到网格末尾（框内末尾），拖出框外即可让收起时也显示
                                                viewModel.addTile(tile.value, context)
                                                addSheetState.dismiss()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val icon = rememberTileIcon(tile.value)
                                        Icon(
                                            painter = icon,
                                            contentDescription = tile.displayName,
                                            tint = MiuixTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Text(
                                        text = tile.displayName,
                                        style = MiuixTheme.textStyles.footnote1,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        //                                 ↓ 图标 <=> 文字的间距
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    )

                                    Text(
                                        text = tile.value,
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // 分类结束分割线
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // 「添加第三方磁贴」底部 Sheet：展示其他应用的 QS Tile 服务（需 QUERY_ALL_PACKAGES 权限）
    @Composable
    fun AddCustomTileSheet() {
        AppBottomSheet(
            state = customSheetState,
            title = stringResource(R.string.config_add_custom_tiles),
            // 关闭动画完成后清空上次输入的磁贴值与搜索关键字，避免下次打开残留
            onDismissed = {
                viewModel.clearCustomTileValue()
                viewModel.clearCustomSearchQuery()
            },
        ) {
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
                            .padding(24.dp),
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
                            .padding(24.dp),
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
                        // 搜索框（支持按磁贴名 / 应用名 / 包名搜索）
                        SearchBar(
                            expanded = false,
                            onExpandedChange = { expanded ->
                                if (!expanded) {
                                    customSheetState.dismiss()
                                }
                            },
                            insideMargin = DpSize(0.dp, 4.dp),
                            inputField = {
                                InputField(
                                    query = viewModel.customSearchQuery,
                                    onQueryChange = { viewModel.customSearchQuery = it },
                                    onSearch = {},
                                    // 避免打开 sheet 时自动聚焦弹键盘
                                    expanded = false,
                                    onExpandedChange = {},
                                    label = stringResource(R.string.common_search),
                                    color = MiuixTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { }

                        if (filteredServices.isEmpty()) {
                            // 搜索无结果提示
                            Text(
                                text = stringResource(R.string.config_no_matching_tiles),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scrollEndHaptic(HapticFeedbackType.TextHandleMove)
                                    .clarityMask(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 64.dp)
                            ) {
                                gridItems(filteredServices, key = { it.packageName + "/" + it.className }) { service ->
                                    val tileValue = "custom(${service.packageName}/${service.className})"
                                    Column(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp, vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(MiuixTheme.colorScheme.secondaryVariant)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.addTile(tileValue, context)
                                                    customSheetState.dismiss()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // 图标已在后台批量预取（Service 图标或 blank 占位），渲染时不再触发 IPC
                                            val bitmap = remember(service.icon) { service.icon.toBitmap() }
                                            Icon(
                                                painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) },
                                                contentDescription = service.label,
                                                tint = MiuixTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = service.label,
                                            style = MiuixTheme.textStyles.footnote1,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Text(
                                            text = service.appName,
                                            style = MiuixTheme.textStyles.footnote2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
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
        topBar = {
            // 渐进模糊开启时使用渐变模糊（顶部最强、向下过渡到清晰），关闭时回退普通模糊
            BlurredBar(backdrop, blurActive, progressive = LocalThemeSettings.current.progressiveBlur) {
                TileTopAppBar()
            }
        }
    ) { paddingValues ->
        // 滚动内容挂载 backdrop，供顶部栏模糊捕获
        Box(
            modifier = if (backdrop != null) Modifier.fillMaxSize().layerBackdrop(backdrop) else Modifier.fillMaxSize()
        ) {
            TileGridPane(paddingValues)
        }
    }

    AddTileSheet()

    AddCustomTileSheet()

    ConfirmationDialogs()
}