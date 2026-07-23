package com.hrsthrt74.qstile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.data.TileMapping
import kotlinx.coroutines.launch

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
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "磁贴配置",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (tiles.isEmpty()) {
                Text(
                    text = "暂无磁贴",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(30.dp)
        )

        Text(
            text = TileMapping.getDisplayName(tileValue),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = tileValue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(availableTiles, key = { it.value }) { tile ->
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
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = tile.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
