package com.hrsthrt74.qstile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.data.TileMapping
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TileConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    var config by remember { mutableStateOf(TileConfig()) }
    var showAddExpandedDialog by remember { mutableStateOf(false) }
    var showAddCollapsedDialog by remember { mutableStateOf(false) }

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
                defaultWindowInsetsPadding = false
            )
        },
        contentWindowInsets = WindowInsets()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 16.dp
            )
        ) {
            item {
                SmallTitle(text = "展开时显示的磁贴")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        config.expandedTiles.forEachIndexed { index, tile ->
                            TileItem(
                                tileValue = tile,
                                index = index,
                                isFirst = index == 0,
                                isLast = index == config.expandedTiles.size - 1,
                                onMoveUp = {
                                    if (index > 0) {
                                        val newList = config.expandedTiles.toMutableList()
                                        val tmp = newList[index]
                                        newList[index] = newList[index - 1]
                                        newList[index - 1] = tmp
                                        updateConfig(config.copy(expandedTiles = newList))
                                    }
                                },
                                onMoveDown = {
                                    if (index < config.expandedTiles.size - 1) {
                                        val newList = config.expandedTiles.toMutableList()
                                        val tmp = newList[index]
                                        newList[index] = newList[index + 1]
                                        newList[index + 1] = tmp
                                        updateConfig(config.copy(expandedTiles = newList))
                                    }
                                },
                                onRemove = {
                                    updateConfig(config.copy(expandedTiles = config.expandedTiles.filter { it != tile }))
                                }
                            )
                        }

                        IconButton(
                            onClick = { showAddExpandedDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "添加")
                            Text("添加磁贴", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                SmallTitle(text = "收起时显示的磁贴")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        config.collapsedTiles.forEachIndexed { index, tile ->
                            TileItem(
                                tileValue = tile,
                                index = index,
                                isFirst = index == 0,
                                isLast = index == config.collapsedTiles.size - 1,
                                onMoveUp = {
                                    if (index > 0) {
                                        val newList = config.collapsedTiles.toMutableList()
                                        val tmp = newList[index]
                                        newList[index] = newList[index - 1]
                                        newList[index - 1] = tmp
                                        updateConfig(config.copy(collapsedTiles = newList))
                                    }
                                },
                                onMoveDown = {
                                    if (index < config.collapsedTiles.size - 1) {
                                        val newList = config.collapsedTiles.toMutableList()
                                        val tmp = newList[index]
                                        newList[index] = newList[index + 1]
                                        newList[index + 1] = tmp
                                        updateConfig(config.copy(collapsedTiles = newList))
                                    }
                                },
                                onRemove = {
                                    updateConfig(config.copy(collapsedTiles = config.collapsedTiles.filter { it != tile }))
                                }
                            )
                        }

                        IconButton(
                            onClick = { showAddCollapsedDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "添加")
                            Text("添加磁贴", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showAddExpandedDialog) {
        AddTileDialog(
            title = "添加展开磁贴",
            existingTiles = config.expandedTiles,
            onDismiss = { showAddExpandedDialog = false },
            onAdd = { tile ->
                updateConfig(config.copy(expandedTiles = config.expandedTiles + tile))
                showAddExpandedDialog = false
            }
        )
    }

    if (showAddCollapsedDialog) {
        AddTileDialog(
            title = "添加收起磁贴",
            existingTiles = config.collapsedTiles,
            onDismiss = { showAddCollapsedDialog = false },
            onAdd = { tile ->
                updateConfig(config.copy(collapsedTiles = config.collapsedTiles + tile))
                showAddCollapsedDialog = false
            }
        )
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
            text = "${index + 1}.",
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.width(30.dp)
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

@Composable
private fun AddTileDialog(
    title: String,
    existingTiles: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    val availableTiles = remember(existingTiles) {
        TileMapping.systemTiles.filter { it.value !in existingTiles }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            LazyColumn(contentPadding = PaddingValues(24.dp)) {
                item {
                    Text(text = title, style = MiuixTheme.textStyles.headline2)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(availableTiles.size) { index ->
                    val tile = availableTiles[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAdd(tile.value) }
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
    }
}
