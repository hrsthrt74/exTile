package com.hrsthrt74.qstile.ui.screens

import androidx.activity.compose.BackHandler
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.data.TileMapping
import com.hjq.device.compat.DeviceOs
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.WindowIconDropdownMenu
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

    val tabs = listOf("展开时", "收起时")
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var config by remember { mutableStateOf(TileConfig()) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showCustomSheet by remember { mutableStateOf(false) }
    var customTileValue by remember { mutableStateOf("") }
    var showCopyConfirmDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        config = ConfigRepository.getConfig(context)
    }

    fun updateConfig(newConfig: TileConfig) {
        config = newConfig
        scope.launch {
            ConfigRepository.saveExpandedTiles(context, newConfig.expandedTiles)
            ConfigRepository.saveCollapsedTiles(context, newConfig.collapsedTiles)
        }
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
                                    text = "展开配置 → 收起",
                                    onClick = { showCopyConfirmDialog = true }
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
            ) {
                TabRow(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )

                // 小米设备特化：固定卡片 + 编辑 Chip
                val isXiaomi = remember { DeviceOs.isMiui() || DeviceOs.isHyperOs() }
                val isTablet = remember {
                    val screenSize = context.resources.configuration.screenLayout
                    val sizeMask = screenSize and Configuration.SCREENLAYOUT_SIZE_MASK
                    sizeMask >= Configuration.SCREENLAYOUT_SIZE_LARGE
                }
                // 需要从网格中抽出、放在上方固定卡片的磁贴
                val fixedTileValues = remember(isXiaomi, isTablet) {
                    if (isXiaomi) {
                        if (isTablet) listOf("wifi", "bt") else listOf("wifi", "cell")
                    } else emptyList()
                }

                // 拖拽排序网格：按住任意磁贴直接拖动，4 列布局
                key(selectedTabIndex) {
                    val currentTiles = if (selectedTabIndex == 0) config.expandedTiles else config.collapsedTiles
                    // edit 在小米设备上不可拖动（固定在末尾），其他设备保留在网格内
                    val gridTiles = if (isXiaomi) {
                        currentTiles.filter { it !in fixedTileValues && it != "edit" }
                    } else {
                        currentTiles.filter { it !in fixedTileValues }
                    }
                    val lazyGridState = rememberLazyGridState()
                    val reorderableState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
                        val newList = gridTiles.toMutableList().apply {
                            this[to.index] = this[from.index].also {
                                this[from.index] = this[to.index]
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
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    fixedTileValues.forEach { tile ->
                                        Card(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(MiuixTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (tile == "wifi") {
                                                        Icon(
                                                            painter = painterResource(R.drawable.tile_wifi),
                                                            contentDescription = "WLAN",
                                                            tint = MiuixTheme.colorScheme.primary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                    }
                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = TileMapping.getDisplayName(tile),
                                                        style = MiuixTheme.textStyles.body1
                                                    )
                                                    Text(
                                                        text = "固定磁贴",
                                                        style = MiuixTheme.textStyles.body2,
                                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                                    )
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
                                    if (isDragging) 1.03f else 1f,
                                    label = "dragScale"
                                )
                                Column(
                                    modifier = Modifier
                                        .scale(scale)
                                        .longPressDraggableHandle()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(MiuixTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (tile == "wifi") {
                                            Icon(
                                                painter = painterResource(R.drawable.tile_wifi),
                                                contentDescription = "WLAN",
                                                tint = MiuixTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = TileMapping.getDisplayName(tile),
                                        style = MiuixTheme.textStyles.body2,
                                        maxLines = 2
                                    )

                                    Text(
                                        text = tile,
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        maxLines = 2
                                    )
                                }
                            }
                        }

                        // 编辑磁贴（小米设备：不可拖动，badge 样式）
                        if (isXiaomi) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card {
                                        Text(
                                            text = "编辑磁贴",
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                            style = MiuixTheme.textStyles.body2
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
                                        onClick = { showAddSheet = true },
                                        colors = ButtonDefaults.buttonColorsPrimary(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pressable(
                                                interactionSource = null,
                                                indication = SinkFeedback()
                                            )
                                    ) {
                                        Icon(MiuixIcons.AddCircle, contentDescription = "添加")
                                        Text("添加磁贴", modifier = Modifier.padding(start = 8.dp))
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = { showCustomSheet = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(MiuixIcons.AddCircle, contentDescription = "添加自定义")
                                        Text("添加自定义磁贴", modifier = Modifier.padding(start = 8.dp))
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

            val availableTiles = remember(currentTiles) {
                TileMapping.systemTiles.filter { it.value !in currentTiles }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)
            ) {
                items(availableTiles.size) { index ->
                    val tile = availableTiles[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newTiles = currentTiles + tile.value
                                val newConfig = if (selectedTabIndex == 0) {
                                    config.copy(expandedTiles = newTiles)
                                } else {
                                    config.copy(collapsedTiles = newTiles)
                                }
                                updateConfig(newConfig)
                                showAddSheet = false
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tile.displayName,
                                style = MiuixTheme.textStyles.body1
                            )
                            Text(
                                text = tile.value,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }
            }
        }

        WindowBottomSheet(
            show = showCustomSheet,
            title = "添加自定义磁贴",
            onDismissRequest = {
                showCustomSheet = false
                customTileValue = ""
            }
        ) {
            BackHandler {
                showCustomSheet = false
                customTileValue = ""
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "请输入磁贴标识值，如 custom(包名/类名)",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = customTileValue,
                    onValueChange = { customTileValue = it },
                    label = "磁贴值",
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            showCustomSheet = false
                            customTileValue = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            if (customTileValue.isNotBlank()) {
                                val currentTiles = if (selectedTabIndex == 0) {
                                    config.expandedTiles
                                } else {
                                    config.collapsedTiles
                                }
                                val newConfig = if (selectedTabIndex == 0) {
                                    config.copy(expandedTiles = currentTiles + customTileValue.trim())
                                } else {
                                    config.copy(collapsedTiles = currentTiles + customTileValue.trim())
                                }
                                updateConfig(newConfig)
                                showCustomSheet = false
                                customTileValue = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = customTileValue.isNotBlank()
                    ) {
                        Text("添加")
                    }
                }
            }
        }

        WindowDialog(
            title = "展开配置 → 收起",
            summary = "将展开状态的磁贴配置复制到收起状态，现有的收起配置将被覆盖。",
            show = showCopyConfirmDialog,
            onDismissRequest = { showCopyConfirmDialog = false }
        ) {
            // miuix 居然没这个间距，没了看起来很奇怪哎
            Spacer(modifier = Modifier.height(8.dp))

            BackHandler { showCopyConfirmDialog = false }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    text = "取消",
                    onClick = { showCopyConfirmDialog = false },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = "确认",
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        updateConfig(config.copy(collapsedTiles = config.expandedTiles))
                        showCopyConfirmDialog = false
                    },
                    modifier = Modifier.weight(1f)
                )
            }
    }

        WindowDialog(
            title = "恢复默认设置",
            summary = "将展开和收起磁贴配置恢复为默认值，当前配置将丢失。",
            show = showResetConfirmDialog,
            onDismissRequest = { showResetConfirmDialog = false }
        ) {
            // 同 Line 375
            Spacer(modifier = Modifier.height(8.dp))

            BackHandler { showResetConfirmDialog = false }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    text = "取消",
                    onClick = { showResetConfirmDialog = false },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    colors = ButtonDefaults.buttonColors(
                        color = MiuixTheme.colorScheme.error,
                        contentColor = MiuixTheme.colorScheme.onError,
                    ),
                    onClick = {
                        updateConfig(TileConfig())
                        showResetConfirmDialog = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("确认恢复")
                }
            }
    }
}
