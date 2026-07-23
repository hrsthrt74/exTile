package com.hrsthrt74.qstile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.data.TileMapping
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Scaffold
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun TileConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "磁贴配置",
            style = MiuixTheme.textStyles.headline1
        )

        Spacer(modifier = Modifier.height(12.dp))

        TileListSection(
            title = "展开时显示的磁贴",
            tiles = config.expandedTiles,
            onMoveUp = { index ->
                if (index > 0) {
                    val newList = config.expandedTiles.toMutableList()
                    val tmp = newList[index]; newList[index] = newList[index - 1]; newList[index - 1] = tmp
                    updateConfig(config.copy(expandedTiles = newList))
                }
            },
            onMoveDown = { index ->
                if (index < config.expandedTiles.size - 1) {
                    val newList = config.expandedTiles.toMutableList()
                    val tmp = newList[index]; newList[index] = newList[index + 1]; newList[index + 1] = tmp
                    updateConfig(config.copy(expandedTiles = newList))
                }
            },
            onRemove = { tile ->
                updateConfig(config.copy(expandedTiles = config.expandedTiles.filter { it != tile }))
            },
            onAdd = { showAddExpandedDialog = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TileListSection(
            title = "收起时显示的磁贴",
            tiles = config.collapsedTiles,
            onMoveUp = { index ->
                if (index > 0) {
                    val newList = config.collapsedTiles.toMutableList()
                    val tmp = newList[index]; newList[index] = newList[index - 1]; newList[index - 1] = tmp
                    updateConfig(config.copy(collapsedTiles = newList))
                }
            },
            onMoveDown = { index ->
                if (index < config.collapsedTiles.size - 1) {
                    val newList = config.collapsedTiles.toMutableList()
                    val tmp = newList[index]; newList[index] = newList[index + 1]; newList[index + 1] = tmp
                    updateConfig(config.copy(collapsedTiles = newList))
                }
            },
            onRemove = { tile ->
                updateConfig(config.copy(collapsedTiles = config.collapsedTiles.filter { it != tile }))
            },
            onAdd = { showAddCollapsedDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))
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
private fun TileListSection(
    title: String,
    tiles: List<String>,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.title2
                )
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tiles.isEmpty()) {
                Text(
                    text = "暂无磁贴",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            } else {
                tiles.forEachIndexed { index, tile ->
                    TileItem(
                        tileValue = tile,
                        index = index,
                        isFirst = index == 0,
                        isLast = index == tiles.size - 1,
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                        onRemove = { onRemove(tile) }
                    )
                }
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
            text = "${index + 1}.",
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.width(30.dp)
        )

        Text(
            text = TileMapping.getDisplayName(tileValue),
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = tileValue,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )

        IconButton(onClick = onMoveUp, enabled = !isFirst) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "上移")
        }

        IconButton(onClick = onMoveDown, enabled = !isLast) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "下移")
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.headline2
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    availableTiles.forEach { tile ->
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
}
