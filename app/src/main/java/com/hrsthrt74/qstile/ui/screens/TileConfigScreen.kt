package com.hrsthrt74.qstile.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.withoutEventHandling
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.CustomTileUtils
import com.hrsthrt74.qstile.data.DeviceProfile
import com.hrsthrt74.qstile.data.TileCapabilityFlags
import com.hrsthrt74.qstile.data.TileCatalog
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.data.ThemeRepository
import com.hrsthrt74.qstile.data.ThemeSettings
import com.hrsthrt74.qstile.ui.asymmetricDropdownPositionProvider
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.AppDialog
import com.hrsthrt74.qstile.ui.components.rememberDialogState
import com.hrsthrt74.qstile.ui.components.rememberSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TooltipDefaults
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.PlainTooltip
import top.yukonga.miuix.kmp.basic.rememberTooltipState
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.WindowIconDropdownMenu
import top.yukonga.miuix.kmp.window.WindowListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun TileConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    // 设备能力快照（isXiaomi/isTablet/SDK 等一次性采集并缓存，见 DeviceProfile）
    val profile = remember { DeviceProfile.from(context) }
    val isXiaomi = profile.isXiaomi

    // 主题设置（用于判断动态取色、模糊开关是否启用）
    val themeSettings by ThemeRepository.getThemeSettingsFlow(context)
        .collectAsState(initial = ThemeSettings())

    // 顶部栏模糊：效果不佳，故禁用（模糊开关关闭或 RuntimeShader 不支持时退回纯色）
    val isDynamicColor = themeSettings.isDynamicColorMode

    val haptic = LocalHapticFeedback.current

    // 本地可变状态：拖动排序时同步更新，保证 reorderable 库数据源即时一致（避免抽搐）
    var config by remember { mutableStateOf(TileConfig()) }
    // 添加磁贴 / 添加第三方磁贴 Sheet 的显示状态（统一由 AppBottomSheet 管理）
    val addSheetState = rememberSheetState()
    val customSheetState = rememberSheetState()
    var customTileValue by remember { mutableStateOf("") }
    // 两个确认操作对话框的显示状态（统一由 AppDialog 管理）
    val clearConfirmDialogState = rememberDialogState()
    val resetConfirmDialogState = rememberDialogState()

    // 第三方磁贴服务列表（含图标）：null 表示尚未加载，在后台线程批量查询避免阻塞主线程
    var allTileServices by remember { mutableStateOf<List<CustomTileUtils.QSTileServiceInfo>?>(null) }

    // 磁贴菜单状态
    var showTileMenu by remember { mutableStateOf(false) }
    var selectedTileForMenu by remember { mutableStateOf<String?>(null) }

    // 小米设备特化：固定卡片
    val configuration = LocalConfiguration.current
    val isTablet = remember {
        val screenSize = configuration.screenLayout
        val sizeMask = screenSize and Configuration.SCREENLAYOUT_SIZE_MASK
        sizeMask >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }
    // 需要从网格中抽出、放在上方固定卡片的磁贴
    val fixedTileValues = remember(isXiaomi, isTablet) {
        if (isXiaomi) {
            if (isTablet) listOf("wifi", "bt") else listOf("wifi", "cell")
        } else emptyList()
    }

    // 打开「添加第三方磁贴」sheet 时，在后台线程批量查询所有 QS Tile 服务及其图标（扫描应用较耗时）
    LaunchedEffect(customSheetState.show) {
        if (customSheetState.show && allTileServices == null) {
            allTileServices = withContext(Dispatchers.IO) {
                CustomTileUtils.getAllQSTileServicesWithIcon(context)
            }
        }
    }

    // 监听 DataStore 外部变更（如备份导入/恢复），同步到本地 config。
    // 注意：本页自身的写入也会触发此监听，但值相同，重复赋值无副作用，不影响拖拽。
    LaunchedEffect(Unit) {
        ConfigRepository.getConfigFlow(context).collect { newConfig ->
            config = newConfig
        }
    }

    // 通用保存：直接写入给定的完整配置（恢复默认设置使用）
    fun updateConfig(newConfig: TileConfig) {
        // 同步更新本地状态：拖动排序时 reorderable 数据源必须立即一致，否则位置会抽搐
        config = newConfig
        scope.launch {
            ConfigRepository.saveExpandedTiles(context, newConfig.expandedTiles)
            ConfigRepository.saveCollapsedTiles(context, newConfig.collapsedTiles)
        }
    }

    /**
     * 保存磁贴列表（一页式单一数据源，含 exTile），并派生保存 expanded/collapsed 两个列表：
     * - expanded = 固定卡片 + 全部磁贴 + edit
     * - collapsed = 固定卡片 + 框外磁贴（exTile 及其之前）+ edit
     *
     * 收起列表由「框外」自动派生，不再独立编辑；独立列表结构保留，供后续「切层」高级功能使用。
     */
    fun updateGridTiles(newGridTiles: List<String>) {
        val exTileIndex = newGridTiles.indexOf(TileCatalog.EXTILE_CUSTOM)
        // exTile 及其之前的磁贴 = 收起时可见（框外）；之后的 = 仅展开时可见（框内）
        val outerTiles = if (exTileIndex >= 0) newGridTiles.take(exTileIndex + 1) else newGridTiles
        val expanded = if (isXiaomi) fixedTileValues + newGridTiles + listOf("edit") else newGridTiles
        val collapsed = if (isXiaomi) fixedTileValues + outerTiles + listOf("edit") else outerTiles
        updateConfig(config.copy(expandedTiles = expanded, collapsedTiles = collapsed))
    }

    // 获取磁贴图标的辅助函数
    @Composable
    fun rememberTileIcon(tile: String): Painter? {
        val iconRes = TileCatalog.iconRes(tile)
        if (iconRes != null) {
            return painterResource(iconRes)
        }
        // 尝试获取 custom 磁贴的图标
        if (tile.startsWith("custom(")) {
            val drawable = remember(tile) { CustomTileUtils.getCustomTileIcon(context, tile) }
            if (drawable != null) {
                val bitmap = remember(drawable) { drawable.toBitmap() }
                return remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
            }
        }
        return null
    }

    // 获取 custom 磁贴的显示名与应用名（缓存避免重复查询）
    @Composable
    fun rememberCustomTileNames(tile: String): Pair<String, String>? {
        if (!tile.startsWith("custom(")) return null
        return remember(tile) { CustomTileUtils.getCustomTileNames(context, tile) }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                    title = "磁贴配置",
                    largeTitle = "磁贴配置",
                    scrollBehavior = scrollBehavior,
                    actions = {
                            val entry = DropdownEntry(
                                items = listOf(
                                    DropdownItem(
                                        text = "清除当前配置的磁贴",
                                        onClick = { clearConfirmDialogState.show() }
                                    ),
                                    DropdownItem(
                                        text = "恢复默认设置",
                                        onClick = { resetConfirmDialogState.show() }
                                    )
                                )
                            )
                            WindowIconDropdownMenu(entry = entry) {
                                Icon(MiuixIcons.More, contentDescription = "更多操作")
                            }
                        }
                    )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                // 拖拽排序网格：按住任意磁贴直接拖动，4 列布局。
                // 一页式：exTile 磁贴为「分界锚点」——它及其之前的磁贴 = 收起时可见（框外），
                // 之后的磁贴 = 仅展开时可见（框内，渲染在虚线框内）。拖动磁贴跨过 exTile 即自动进出框。
                key(isXiaomi, fixedTileValues) {
                    // 单一数据源（过滤固定卡片和 edit），含 exTile
                    val gridTiles = config.expandedTiles.filter { it !in fixedTileValues && it != "edit" }
                    val exTileIndex = gridTiles.indexOf(TileCatalog.EXTILE_CUSTOM)
                    // 框外磁贴 = exTile 及其之前；框内磁贴 = exTile 之后
                    val outerTiles = if (exTileIndex >= 0) gridTiles.take(exTileIndex + 1) else gridTiles
                    val innerTiles = if (exTileIndex >= 0) gridTiles.drop(exTileIndex + 1) else emptyList()
                    // 只要存在 exTile 就显示「展开后」区域（标题行 + 虚线框）。
                    // 框内磁贴为空时也保留空框，提示新添加的磁贴会进这里；exTile 被删除才整体隐藏。
                    val hasInner = exTileIndex >= 0
                    val outerCount = outerTiles.size
                    // 固定卡片 item 数（网格中占 1 个不可拖拽 item）
                    val fixedItemCount = if (isXiaomi && fixedTileValues.isNotEmpty()) 1 else 0

                    // 标题行 item 的 key（overlay 绘制框顶线时用它定位）
                    val headerKey = "inner-section-header"
                    // 标记对象用 remember 保持稳定，供 renderList 与 itemKeyOf 用 === 匹配
                    val headerMarker = remember { Any() }
                    // 统一渲染列表：框外磁贴 + 标题行 + 框内磁贴。
                    // 所有磁贴在同一 items 块内渲染，跨框拖拽时 item 不跨组合块、key 不变，
                    // 避免 ReorderableItem 拖拽句柄丢失导致拖动被打断。
                    // 注意：不在框内添加「占位 ReorderableItem」——无对应数据的幽灵 item 会干扰
                    // 库的落点/索引计算，导致拖拽抽搐。空框拖入改由「标题行在空框时作为落点」实现。
                    val renderList = remember(gridTiles, hasInner) {
                        buildList {
                            addAll(outerTiles)
                            if (hasInner) add(headerMarker)
                            addAll(innerTiles)
                        }
                    }
                    // 框内磁贴 key 集合（overlay 定位虚线框用）
                    val innerKeys = remember(innerTiles) { innerTiles.toSet() }
                    // gridItems 的 key 生成：磁贴用 spec 值，标题行用固定 key
                    fun itemKeyOf(item: Any): Any = if (item is String) item else headerKey

                    val lazyGridState = rememberLazyGridState()
                    val reorderableState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
                        // 库的索引是 LazyGrid 全部 item 的绝对位置，需映射回数据索引。
                        // 渲染列表索引 = gi - 固定卡片数；数据索引需跳过标题行（标题位于渲染索引 outerCount 处）。
                        fun gridToData(gi: Int): Int {
                            val ri = gi - fixedItemCount
                            if (ri < 0) return -1
                            if (!hasInner) return ri
                            // 标题行：空框时作为落点（= 框内末尾追加位），非空时不注册、不会被选为目标
                            if (ri == outerCount) return if (innerTiles.isEmpty()) gridTiles.size else -1
                            // 框内磁贴（跳过标题行）
                            return if (ri > outerCount) ri - 1 else ri
                        }
                        val fromData = gridToData(from.index)
                        val toData = gridToData(to.index)
                        // 目标落在标题/固定卡片/添加按钮等非数据 item 上时忽略本次移动。
                        // toData == gridTiles.size 表示「框内末尾」（拖入占位行），为合法目标
                        if (fromData < 0 || toData < 0 ||
                            fromData >= gridTiles.size || toData > gridTiles.size ||
                            fromData == toData
                        ) {
                            return@rememberReorderableLazyGridState
                        }
                        val newList = gridTiles.toMutableList().apply {
                            // toData == gridTiles.size 表示「框内末尾」（拖入占位行）。
                            // toData 是 removeAt 前的索引，removeAt 后 size 减 1，
                            // 追加到末尾应使用 gridTiles.size - 1（= removeAt 后的 size），避免越界
                            val insertAt = if (toData == gridTiles.size) gridTiles.size - 1 else toData
                            add(insertAt, removeAt(fromData))
                        }
                        updateGridTiles(newList)
                        // 每次调换位置触发震动
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }

                    // 单个磁贴格子的渲染（含拖拽、点击菜单）。虚线框不再画在磁贴上（会随拖拽移动），
                    // 改由网格 overlay 统一绘制。
                    // 使用 LazyGridItemScope receiver：ReorderableItem 是其扩展函数，只能在网格 item 作用域内调用。
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
                                                selectedTileForMenu = tile
                                                showTileMenu = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val icon = rememberTileIcon(tile)
                                        if (icon != null) {
                                            Icon(
                                                painter = icon,
                                                contentDescription = TileCatalog.getDisplayName(tile, profile),
                                                tint = if (tile == "cell" && !isDynamicColor) Color(0xFF1FCD39) else MiuixTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }

                                        // 磁贴操作弹出菜单（锚定到 icon 外的圆形 Box，而非整个 grid item）。
                                        // 注意：这里不能用 `showTileMenu` 参与条件，否则关闭时整个 WindowListPopup
                                        // 会直接从组合树移除，ListPopupLayout 的退场动画（缩放/透明渐出）来不及播放。
                                        // 因此只在目标磁贴匹配时组合，show 参数单独控制显隐，让组件内部走完退场动画。
                                        if (selectedTileForMenu == tile) {
                                            val gridTilesForMenu = gridTiles
                                            val tileIndexForMenu = gridTilesForMenu.indexOf(tile)

                                            // 删除按钮的错误颜色
                                            val errorColors = DropdownDefaults.dropdownColors(
                                                contentColor = MiuixTheme.colorScheme.error,
                                                selectedContentColor = MiuixTheme.colorScheme.error
                                            )

                                            WindowListPopup(
                                                show = showTileMenu,
                                                // 垂直间距 8dp：popup 与图标保持间距；左右边距独立设置左 0 右 8
                                                popupPositionProvider = asymmetricDropdownPositionProvider(
                                                    verticalMargin = 8.dp,
                                                    startMargin = 0.dp,
                                                    endMargin = 8.dp
                                                ),
                                                onDismissRequest = { showTileMenu = false }
                                            ) {
                                                ListPopupColumn {
                                                    // 磁贴名称（不可点击）
                                                    DropdownImpl(
                                                        text = TileCatalog.getDisplayName(tile, profile),
                                                        optionSize = 1,
                                                        isSelected = false,
                                                        index = 0,
                                                        enabled = false,
                                                        onSelectedIndexChange = {}
                                                    )
                                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                                    // 移动到顶端（框外第一个，收起时最先显示）
                                                    DropdownImpl(
                                                        text = "移动到顶端",
                                                        optionSize = 3,
                                                        isSelected = false,
                                                        index = 0,
                                                        enabled = tileIndexForMenu > 0,
                                                        onSelectedIndexChange = {
                                                            val newList = gridTilesForMenu.toMutableList().apply {
                                                                removeAt(tileIndexForMenu)
                                                                add(0, tile)
                                                            }
                                                            updateGridTiles(newList)
                                                            showTileMenu = false
                                                        }
                                                    )
                                                    // 移动到底端（框内最后一个，仅展开时显示）
                                                    DropdownImpl(
                                                        text = "移动到底端",
                                                        optionSize = 3,
                                                        isSelected = false,
                                                        index = 1,
                                                        enabled = tileIndexForMenu < gridTilesForMenu.size - 1,
                                                        onSelectedIndexChange = {
                                                            val newList = gridTilesForMenu.toMutableList().apply {
                                                                removeAt(tileIndexForMenu)
                                                                add(gridTilesForMenu.size - 1, tile)
                                                            }
                                                            updateGridTiles(newList)
                                                            showTileMenu = false
                                                        }
                                                    )
                                                    // 删除（错误颜色）
                                                    DropdownImpl(
                                                        text = "删除",
                                                        optionSize = 3,
                                                        isSelected = false,
                                                        index = 2,
                                                        dropdownColors = errorColors,
                                                        onSelectedIndexChange = {
                                                            val newList = gridTilesForMenu.toMutableList().apply {
                                                                remove(tile)
                                                            }
                                                            updateGridTiles(newList)
                                                            showTileMenu = false
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
                                            ?: TileCatalog.getDisplayName(tile, profile),
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

                    // 虚线框颜色（@Composable 属性，需在 Composable 上下文取值后供 drawBehind 捕获）
                    val frameColor = MiuixTheme.colorScheme.primary
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
                                // 存在 exTile 时显示「展开后」区域
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
                                    val top: Float
                                    val bottom: Float
                                    if (innerVisible.isEmpty()) {
                                        // 框内无磁贴：空框只包住标题行
                                        if (headerItem == null) return@drawBehind
                                        top = headerItem.offset.y.toFloat()
                                        bottom = (headerItem.offset.y + headerItem.size.height).toFloat()
                                    } else {
                                        // 有框内磁贴：框顶用标题行，框底用末行实际行底
                                        val lastRowStart = ((innerTiles.size - 1) / 4) * 4
                                        val lastRowKeys = innerTiles.subList(lastRowStart, innerTiles.size).toSet()
                                        val lastRowVisible = innerVisible.filter { it.key in lastRowKeys }
                                        val lastRowComplete = lastRowVisible.size == lastRowKeys.size
                                        top = if (headerItem != null) headerItem.offset.y.toFloat()
                                              else innerVisible.minOf { it.offset.y }.toFloat()
                                        bottom = if (lastRowComplete) lastRowVisible.maxOf { it.offset.y + it.size.height }.toFloat()
                                                 else innerVisible.maxOf { it.offset.y + it.size.height }.toFloat()
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
                            // 使框贴边撑满时磁贴距框线正好 10dp
                            start = 10.dp,
                            end = 10.dp,
                            bottom = NavigationBarDefaults.ItemHeight +
                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                            )
                        ) {
                        // 固定卡片（小米特化，不参与拖拽）
                        if (isXiaomi && fixedTileValues.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        // 俩卡片的外边距
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    // 俩卡片中间的间距
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val scope = rememberCoroutineScope()
                                    fixedTileValues.forEach { tile ->
                                        val tooltipState = rememberTooltipState(isPersistent = true)

                                        TooltipBox(
                                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                                            tooltip = {
                                                PlainTooltip {
                                                    Text(
                                                        text = "系统限制此磁贴无法移动",
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
                                                    if (icon != null) {
                                                        Icon(
                                                            painter = icon,
                                                            contentDescription = TileCatalog.getDisplayName(tile, profile),
                                                            tint = if (tile == "cell" && !isDynamicColor) Color(0xFF1FCD39) else MiuixTheme.colorScheme.primary,
                                                            modifier = Modifier.size(36.dp)
                                                        )
                                                    }

                                                    // 图标与文字之间的间距
                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    // 磁贴名 和 “固定磁贴”
                                                    Column {
                                                        Text(
                                                            text = TileCatalog.getDisplayName(tile, profile),
                                                            style = MiuixTheme.textStyles.body1
                                                        )
                                                        Text(
                                                            text = "固定磁贴",
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
                                // 标题行「展开后显示的磁贴」（span 全行）。
                                // 用 ReorderableItem 包裹并仅在空框时 enabled：
                                // 空框时标题行作为拖拽落点（拖磁贴到这里 = 追加到框内末尾，库的落点只能是
                                // ReorderableItem，见 findTargetItem 的 reorderableKeys 过滤）；
                                // 非空框时不注册 reorderableKeys、不作为落点，不影响正常排序拖拽。
                                // 不挂拖拽句柄，标题行自身不可拖动。框顶线由网格 overlay 绘制。
                                ReorderableItem(
                                    reorderableState,
                                    key = headerKey,
                                    enabled = innerTiles.isEmpty(),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "展开后显示的磁贴",
                                            style = MiuixTheme.textStyles.subtitle,
                                            color = frameColor,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 添加按钮区域，占满整行
                        item(span = { GridItemSpan(maxLineSpan) }) {
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
                                        Icon(MiuixIcons.AddCircle, contentDescription = "添加")
                                        Text("添加磁贴", modifier = Modifier.padding(start = 8.dp))
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
                                        Icon(MiuixIcons.AddCircle, contentDescription = "添加自定义")
                                        Text("添加第三方磁贴", modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AppBottomSheet(
            state = addSheetState,
            title = "添加磁贴",
        ) {
            // 当前已添加的磁贴（含固定卡片、edit、exTile，用于过滤重复项）
            val currentTiles = config.expandedTiles

            // 读取调试 flag（订阅变化），切换后刷新可用磁贴列表
            val capabilityKey = listOf(
                TileCapabilityFlags.satelliteOverride,
                TileCapabilityFlags.coolingFanOverride,
                TileCapabilityFlags.propOverrides.toMap(),
                TileCapabilityFlags.featureOverrides.toMap(),
            )
            val availableTiles = remember(currentTiles, profile, capabilityKey) {
                TileCatalog.getAvailableTiles(profile).filter { it.value !in currentTiles }
            }

            // 按分类分组
            val tilesByCategory = remember(availableTiles) {
                availableTiles.groupBy { it.category }
            }

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
                        Column(
                            modifier = Modifier
                                // 竖向要比横向大一点
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
                                        // 添加到网格末尾（框内末尾），拖出框外即可让收起时也显示
                                        val gridTiles = config.expandedTiles.filter { it !in fixedTileValues && it != "edit" }
                                        updateGridTiles(gridTiles + tile.value)
                                        addSheetState.dismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = rememberTileIcon(tile.value)
                                if (icon != null) {
                                    Icon(
                                        painter = icon,
                                        contentDescription = tile.displayName,
                                        tint = MiuixTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = tile.displayName,
                                style = MiuixTheme.textStyles.footnote1,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
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

        AppBottomSheet(
            state = customSheetState,
            title = "添加第三方磁贴",
            // 关闭动画完成后清空上次输入的磁贴值，避免下次打开残留
            onDismissed = { customTileValue = "" },
        ) {
            // 检查是否有 QUERY_ALL_PACKAGES 权限
            val hasQueryPermission = remember {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.QUERY_ALL_PACKAGES
                ) == PackageManager.PERMISSION_GRANTED
            }

            // 获取所有 QS Tile 服务（后台线程已加载，见顶部 LaunchedEffect）
            val services = allTileServices

            // 获取当前已添加的磁贴
            val currentTiles = config.expandedTiles

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
                            text = "需要读取应用列表权限才能获取其他应用的磁贴",
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
                            Text("前往授权")
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
                            text = "加载中...",
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
                            text = "没有可用的自定义磁贴",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                // 显示磁贴网格
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 64.dp)
                    ) {
                        gridItems(availableTileServices, key = { it.packageName + "/" + it.className }) { service ->
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
                                            val gridTiles = config.expandedTiles.filter { it !in fixedTileValues && it != "edit" }
                                            updateGridTiles(gridTiles + tileValue)
                                            customSheetState.dismiss()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // 图标已在后台批量预取，渲染时不再触发 IPC
                                    val drawable = service.icon
                                    if (drawable != null) {
                                        val bitmap = remember(drawable) { drawable.toBitmap() }
                                        Icon(
                                            painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) },
                                            contentDescription = service.label,
                                            tint = MiuixTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
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

        AppDialog(
            state = clearConfirmDialogState,
            title = "清除当前配置的磁贴",
            summary = "将清空全部磁贴配置，此操作不可撤销。",
            confirmText = "确认清除",
            destructive = true,
            onConfirm = {
                updateGridTiles(emptyList())
            }
        )

        AppDialog(
            state = resetConfirmDialogState,
            title = "恢复默认设置",
            summary = "将展开和收起磁贴配置恢复为默认值，当前配置将丢失。",
            confirmText = "确认恢复",
            destructive = true,
            onConfirm = {
                updateConfig(TileConfig())
            }
        )
}
