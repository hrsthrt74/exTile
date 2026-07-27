package com.hrsthrt74.qstile.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.data.TileMapping
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .scrollEndHaptic(
                            hapticFeedbackType = HapticFeedbackType.TextHandleMove
                        ),
                    contentPadding = PaddingValues(
                        bottom = 16.dp
                    )
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val currentTiles = if (selectedTabIndex == 0) {
                                    config.expandedTiles
                                } else {
                                    config.collapsedTiles
                                }

                                currentTiles.forEachIndexed { index, tile ->
                                    TileItem(
                                        tileValue = tile,
                                        index = index,
                                        isFirst = index == 0,
                                        isLast = index == currentTiles.size - 1,
                                        onMoveUp = {
                                            if (index > 0) {
                                                val newList = currentTiles.toMutableList()
                                                val tmp = newList[index]
                                                newList[index] = newList[index - 1]
                                                newList[index - 1] = tmp
                                                val newConfig = if (selectedTabIndex == 0) {
                                                    config.copy(expandedTiles = newList)
                                                } else {
                                                    config.copy(collapsedTiles = newList)
                                                }
                                                updateConfig(newConfig)
                                            }
                                        },
                                        onMoveDown = {
                                            if (index < currentTiles.size - 1) {
                                                val newList = currentTiles.toMutableList()
                                                val tmp = newList[index]
                                                newList[index] = newList[index + 1]
                                                newList[index + 1] = tmp
                                                val newConfig = if (selectedTabIndex == 0) {
                                                    config.copy(expandedTiles = newList)
                                                } else {
                                                    config.copy(collapsedTiles = newList)
                                                }
                                                updateConfig(newConfig)
                                            }
                                        },
                                        onRemove = {
                                            val newConfig = if (selectedTabIndex == 0) {
                                                config.copy(expandedTiles = currentTiles.filter { it != tile })
                                            } else {
                                                config.copy(collapsedTiles = currentTiles.filter { it != tile })
                                            }
                                            updateConfig(newConfig)
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

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
                                    Text(
                                        "添加自定义磁贴",
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BackHandler { showCopyConfirmDialog = false }
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
            BackHandler { showResetConfirmDialog = false }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
private fun TileItem(
    tileValue: String,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${index + 1}",
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.width(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = TileMapping.getDisplayName(tileValue),
                style = MiuixTheme.textStyles.body1
            )
            Text(
                text = tileValue,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }

        IconButton(onClick = onMoveUp, enabled = !isFirst) {
            Icon(Icons.Default.ExpandLess, contentDescription = "上移")
        }

        IconButton(onClick = onMoveDown, enabled = !isLast) {
            Icon(Icons.Default.ExpandMore, contentDescription = "下移")
        }

        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Remove, contentDescription = "移除")
        }
    }
}
