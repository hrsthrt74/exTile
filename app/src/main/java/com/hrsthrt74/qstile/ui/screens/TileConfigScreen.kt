package com.hrsthrt74.qstile.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.data.TileMapping
import com.hrsthrt74.qstile.data.ThemeRepository
import com.hrsthrt74.qstile.data.ThemeSettings
import com.hjq.device.compat.DeviceOs
import com.hrsthrt74.qstile.ui.asymmetricDropdownPositionProvider
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
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TooltipDefaults
import top.yukonga.miuix.kmp.basic.TooltipState
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
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun TileConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    // 设备类型检测
    val isXiaomi = remember { DeviceOs.isMiui() || DeviceOs.isHyperOs() }

    // 主题设置（用于判断动态取色、模糊开关是否启用）
    val themeSettings by ThemeRepository.getThemeSettingsFlow(context)
        .collectAsState(initial = ThemeSettings())

    // 顶部栏模糊：效果不佳，故禁用（模糊开关关闭或 RuntimeShader 不支持时退回纯色）
    // val backdrop = rememberBlurBackdrop(enabled = themeSettings.enableBlur)
    // val blurActive = backdrop != null
    // val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val isDynamicColor = themeSettings.isDynamicColorMode

    val haptic = LocalHapticFeedback.current

    val tabs = listOf("展开时", "收起时")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    // 本地可变状态：拖动排序时同步更新，保证 reorderable 库数据源即时一致（避免抽搐）
    var config by remember { mutableStateOf(TileConfig()) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showCustomSheet by remember { mutableStateOf(false) }
    var customTileValue by remember { mutableStateOf("") }
    var showCopyConfirmDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // 第三方磁贴服务列表（含图标）：null 表示尚未加载，在后台线程批量查询避免阻塞主线程
    var allTileServices by remember { mutableStateOf<List<TileMapping.QSTileServiceInfo>?>(null) }

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
    LaunchedEffect(showCustomSheet) {
        if (showCustomSheet && allTileServices == null) {
            allTileServices = withContext(Dispatchers.IO) {
                TileMapping.getAllQSTileServicesWithIcon(context)
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

    fun updateConfig(newConfig: TileConfig) {
        // 同步更新本地状态：拖动排序时 reorderable 数据源必须立即一致，否则位置会抽搐
        config = newConfig
        scope.launch {
            ConfigRepository.saveExpandedTiles(context, newConfig.expandedTiles)
            ConfigRepository.saveCollapsedTiles(context, newConfig.collapsedTiles)
        }
    }

    // 获取磁贴图标的辅助函数
    @Composable
    fun rememberTileIcon(tile: String): Painter? {
        val iconRes = TileMapping.iconRes(tile)
        if (iconRes != null) {
            return painterResource(iconRes)
        }
        // 尝试获取 custom 磁贴的图标
        if (tile.startsWith("custom(")) {
            val drawable = remember(tile) { TileMapping.getCustomTileIcon(context, tile) }
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
        return remember(tile) { TileMapping.getCustomTileNames(context, tile) }
    }

    Scaffold(
            topBar = {
                // 顶部栏模糊：效果不佳，故禁用（原 BlurredBar(backdrop, blurActive) 包裹）
                TopAppBar(
                    title = "磁贴配置",
                    largeTitle = "磁贴配置",
                    scrollBehavior = scrollBehavior,
                    actions = {
                            val entry = DropdownEntry(
                                items = listOf(
                                    DropdownItem(
                                        text = "展开配置 → 收起",
                                        onClick = { showCopyConfirmDialog = true }
                                    ),
                                    DropdownItem(
                                        text = "清除当前配置的磁贴",
                                        onClick = { showClearConfirmDialog = true }
                                    ),
                                    DropdownItem(
                                        text = "恢复默认设置",
                                        onClick = { showResetConfirmDialog = true }
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
                    // 顶部栏模糊已禁用，不再挂载 backdrop
            ) {
                TabRow(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTabIndex = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )

                // 拖拽排序网格：按住任意磁贴直接拖动，4 列布局
                key(selectedTabIndex, isXiaomi, fixedTileValues) {
                    val currentTiles = if (selectedTabIndex == 0) config.expandedTiles else config.collapsedTiles
                    val gridTiles = currentTiles.filter { it !in fixedTileValues && it != "edit" }
                    val lazyGridState = rememberLazyGridState()
                    val reorderableState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
                        // 库的索引是 LazyGrid 绝对位置，需减去前面固定卡片的偏移
                        val indexOffset = if (isXiaomi && fixedTileValues.isNotEmpty()) 1 else 0
                        val newList = gridTiles.toMutableList().apply {
                            this[to.index - indexOffset] = this[from.index - indexOffset].also {
                                this[from.index - indexOffset] = this[to.index - indexOffset]
                            }
                        }
                        val fullList = if (isXiaomi) {
                            fixedTileValues + newList + listOf("edit")
                        } else {
                            newList
                        }
                        val newConfig = if (selectedTabIndex == 0) {
                            config.copy(expandedTiles = fullList)
                        } else {
                            config.copy(collapsedTiles = fullList)
                        }
                        updateConfig(newConfig)
                        // 每次调换位置触发震动
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        state = lazyGridState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
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
                                                            contentDescription = TileMapping.getDisplayName(tile),
                                                            tint = if (tile == "cell" && !isDynamicColor) Color(0xFF1FCD39) else MiuixTheme.colorScheme.primary,
                                                            modifier = Modifier.size(36.dp)
                                                        )
                                                    }

                                                    // 图标与文字之间的间距
                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    // 磁贴名 和 “固定磁贴”
                                                    Column {
                                                        Text(
                                                            text = TileMapping.getDisplayName(tile),
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

                        gridItems(gridTiles, key = { it }) { tile ->
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
                                                contentDescription = TileMapping.getDisplayName(tile),
                                                tint = if (tile == "cell" && !isDynamicColor) Color(0xFF1FCD39) else MiuixTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }

                                        // 磁贴操作弹出菜单（锚定到 icon 外的圆形 Box，而非整个 grid item）。
                                        // 注意：这里不能用 `showTileMenu` 参与条件，否则关闭时整个 WindowListPopup
                                        // 会直接从组合树移除，ListPopupLayout 的退场动画（缩放/透明渐出）来不及播放。
                                        // 因此只在目标磁贴匹配时组合，show 参数单独控制显隐，让组件内部走完退场动画。
                                        if (selectedTileForMenu == tile) {
                                            val currentTilesForMenu = if (selectedTabIndex == 0) config.expandedTiles else config.collapsedTiles
                                            val gridTilesForMenu = currentTilesForMenu.filter { it !in fixedTileValues && it != "edit" }
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
                                                BackHandler { showTileMenu = false }
                                                ListPopupColumn {
                                                    // 磁贴名称（不可点击）
                                                    DropdownImpl(
                                                        text = TileMapping.getDisplayName(tile),
                                                        optionSize = 1,
                                                        isSelected = false,
                                                        index = 0,
                                                        enabled = false,
                                                        onSelectedIndexChange = {}
                                                    )
                                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                                    // 移动到顶端
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
                                                            val fullList = if (isXiaomi) {
                                                                fixedTileValues + newList + listOf("edit")
                                                            } else {
                                                                newList
                                                            }
                                                            val newConfig = if (selectedTabIndex == 0) {
                                                                config.copy(expandedTiles = fullList)
                                                            } else {
                                                                config.copy(collapsedTiles = fullList)
                                                            }
                                                            updateConfig(newConfig)
                                                            showTileMenu = false
                                                        }
                                                    )
                                                    // 移动到底端
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
                                                            val fullList = if (isXiaomi) {
                                                                fixedTileValues + newList + listOf("edit")
                                                            } else {
                                                                newList
                                                            }
                                                            val newConfig = if (selectedTabIndex == 0) {
                                                                config.copy(expandedTiles = fullList)
                                                            } else {
                                                                config.copy(collapsedTiles = fullList)
                                                            }
                                                            updateConfig(newConfig)
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
                                                            val fullList = if (isXiaomi) {
                                                                fixedTileValues + newList + listOf("edit")
                                                            } else {
                                                                newList
                                                            }
                                                            val newConfig = if (selectedTabIndex == 0) {
                                                                config.copy(expandedTiles = fullList)
                                                            } else {
                                                                config.copy(collapsedTiles = fullList)
                                                            }
                                                            updateConfig(newConfig)
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
                                            ?: TileMapping.getDisplayName(tile),
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
                                            showAddSheet = true
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
                                            showCustomSheet = true
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

        WindowBottomSheet(
            show = showAddSheet,
            title = if (selectedTabIndex == 0) "添加展开磁贴" else "添加收起磁贴",
            onDismissRequest = { showAddSheet = false }
        ) {
            BackHandler { showAddSheet = false }

            val currentTiles = if (selectedTabIndex == 0) {
                config.expandedTiles
            } else {
                config.collapsedTiles
            }

            val availableTiles = remember(currentTiles, isXiaomi) {
                TileMapping.getAvailableTiles(isXiaomi).filter { it.value !in currentTiles }
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
                                        // 添加到列表末尾，但如果存在 edit 则插入到 edit 前面
                                        val hasEdit = currentTiles.contains("edit")
                                        val newTiles = if (hasEdit) {
                                            val editIndex = currentTiles.indexOf("edit")
                                            currentTiles.toMutableList().apply {
                                                add(editIndex, tile.value)
                                            }
                                        } else {
                                            currentTiles + tile.value
                                        }
                                        val newConfig = if (selectedTabIndex == 0) {
                                            config.copy(expandedTiles = newTiles)
                                        } else {
                                            config.copy(collapsedTiles = newTiles)
                                        }
                                        updateConfig(newConfig)
                                        showAddSheet = false
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

        WindowBottomSheet(
            show = showCustomSheet,
            title = "添加第三方磁贴",
            onDismissRequest = {
                showCustomSheet = false
                customTileValue = ""
            }
        ) {
            BackHandler {
                showCustomSheet = false
                customTileValue = ""
            }

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
            val currentTiles = if (selectedTabIndex == 0) {
                config.expandedTiles
            } else {
                config.collapsedTiles
            }

            // 获取系统预定义的磁贴 ComponentName 集合（用于过滤）
            val systemTileComponents = remember {
                TileMapping.getAllValues()
                    .filter { it.startsWith("custom(") }
                    .mapNotNull { TileMapping.parseCustomComponent(it) }
                    .toSet()
            }

            // 获取已添加的磁贴 ComponentName 集合（用于过滤）
            val currentTileComponents = remember(currentTiles) {
                currentTiles
                    .filter { it.startsWith("custom(") }
                    .mapNotNull { TileMapping.parseCustomComponent(it) }
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
                                            val newTiles = currentTiles + tileValue
                                            val newConfig = if (selectedTabIndex == 0) {
                                                config.copy(expandedTiles = newTiles)
                                            } else {
                                                config.copy(collapsedTiles = newTiles)
                                            }
                                            updateConfig(newConfig)
                                            showCustomSheet = false
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

        ConfigConfirmDialog(
            title = "展开配置 → 收起",
            summary = "将展开状态的磁贴配置复制到收起状态，现有的收起配置将被覆盖。",
            show = showCopyConfirmDialog,
            confirmText = "确认",
            onDismissRequest = { showCopyConfirmDialog = false },
            onConfirm = {
                updateConfig(config.copy(collapsedTiles = config.expandedTiles))
                showCopyConfirmDialog = false
            }
        )

        ConfigConfirmDialog(
            title = "清除当前配置的磁贴",
            summary = "将清空当前展开/收起状态的所有磁贴配置，此操作不可撤销。",
            show = showClearConfirmDialog,
            confirmText = "确认清除",
            destructive = true,
            onDismissRequest = { showClearConfirmDialog = false },
            onConfirm = {
                val newConfig = if (selectedTabIndex == 0) {
                    config.copy(expandedTiles = emptyList())
                } else {
                    config.copy(collapsedTiles = emptyList())
                }
                updateConfig(newConfig)
                showClearConfirmDialog = false
            }
        )

        ConfigConfirmDialog(
            title = "恢复默认设置",
            summary = "将展开和收起磁贴配置恢复为默认值，当前配置将丢失。",
            show = showResetConfirmDialog,
            confirmText = "确认恢复",
            destructive = true,
            onDismissRequest = { showResetConfirmDialog = false },
            onConfirm = {
                updateConfig(TileConfig())
                showResetConfirmDialog = false
            }
        )
}

/**
 * 确认对话框（可复用）
 *
 * 磁贴配置页的三个确认操作（复制 / 清除 / 恢复默认）结构完全相同，
 * 仅标题、说明、确认按钮文案和按钮样式不同，故统一封装为一个组件，
 * 避免三份几乎一样的 WindowDialog 代码。
 *
 * @param title 对话框标题
 * @param summary 对话框说明文字
 * @param show 是否显示
 * @param confirmText 确认按钮文案
 * @param destructive 是否为破坏性操作；为 true 时确认按钮使用错误色（红色）
 * @param onDismissRequest 取消 / 点击外部关闭回调
 * @param onConfirm 点击确认按钮时的回调
 */
@Composable
private fun ConfigConfirmDialog(
    title: String,
    summary: String,
    show: Boolean,
    confirmText: String,
    destructive: Boolean = false,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    WindowDialog(
        title = title,
        summary = summary,
        show = show,
        onDismissRequest = onDismissRequest
    ) {
        // miuix 居然没这个间距，没了看起来很奇怪哎
        Spacer(modifier = Modifier.height(8.dp))

        // 返回键视为取消
        BackHandler { onDismissRequest() }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧固定为「取消」按钮
            TextButton(
                text = "取消",
                onClick = onDismissRequest,
                modifier = Modifier.weight(1f)
            )

            if (destructive) {
                // 破坏性操作：确认按钮用错误色填充，突出「不可撤销」的警告
                Button(
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.error,
                        contentColor = MiuixTheme.colorScheme.onError,
                    ),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(confirmText)
                }
            } else {
                // 普通操作：确认按钮用主题主色
                TextButton(
                    text = confirmText,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
