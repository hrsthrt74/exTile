package com.hrsthrt74.qstile.ui.screens

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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 磁贴配置编辑页面。
 * 使用 TabRow 切换「展开时」/「收起时」两个列表，
 * 用户可以添加、移除、排序各状态下的磁贴。
 */
@Composable
fun TileConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    // 两个 Tab 标签
    val tabs = listOf("展开时", "收起时")
    /** 当前选中的 Tab 索引（0=展开时, 1=收起时） */
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    /** 当前磁贴配置 */
    var config by remember { mutableStateOf(TileConfig()) }
    /** 添加磁贴对话框显示开关 */
    var showAddDialog by remember { mutableStateOf(false) }
    /** 添加自定义磁贴对话框显示开关 */
    var showCustomDialog by remember { mutableStateOf(false) }
    /** 自定义磁贴输入值 */
    var customTileValue by remember { mutableStateOf("") }

    // 首次加载从 DataStore 读取配置
    LaunchedEffect(Unit) {
        config = ConfigRepository.getConfig(context)
    }

    /**
     * 更新配置并持久化到 DataStore。
     * 同时更新内存中的 config 对象以驱动 UI 刷新。
     */
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
                scrollBehavior = scrollBehavior
            )
        }
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
            // ---- Tab 切换栏 ----
            item {
                TabRow(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // ---- 磁贴列表卡片 ----
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 根据当前 Tab 选择展示「展开时」还是「收起时」的磁贴列表
                        val currentTiles = if (selectedTabIndex == 0) {
                            config.expandedTiles
                        } else {
                            config.collapsedTiles
                        }

                        // 遍历当前列表，每个磁贴一行（序号 + 名称 + 上移/下移/移除按钮）
                        currentTiles.forEachIndexed { index, tile ->
                            TileItem(
                                tileValue = tile,
                                index = index,
                                isFirst = index == 0,
                                isLast = index == currentTiles.size - 1,
                                // 上移：与前一元素交换位置
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
                                // 下移：与后一元素交换位置
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
                                // 移除：从列表中过滤掉当前磁贴
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

                        // 添加磁贴按钮 → 打开选择弹窗
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(MiuixIcons.AddCircle, contentDescription = "添加")
                            Text("添加磁贴", modifier = Modifier.padding(start = 8.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 将展开时配置复制到收起时
                        Button(
                            onClick = {
                                updateConfig(config.copy(collapsedTiles = config.expandedTiles))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("展开配置 → 收起")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 添加自定义磁贴按钮 → 打开输入弹窗
                        Button(
                            onClick = { showCustomDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(MiuixIcons.AddCircle, contentDescription = "添加自定义")
                            Text("添加自定义磁贴", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // ---- 添加磁贴弹窗 ----
    if (showAddDialog) {
        val currentTiles = if (selectedTabIndex == 0) {
            config.expandedTiles
        } else {
            config.collapsedTiles
        }

        AddTileDialog(
            title = if (selectedTabIndex == 0) "添加展开磁贴" else "添加收起磁贴",
            existingTiles = currentTiles,
            onDismiss = { showAddDialog = false },
            onAdd = { tile ->
                val newConfig = if (selectedTabIndex == 0) {
                    config.copy(expandedTiles = currentTiles + tile)
                } else {
                    config.copy(collapsedTiles = currentTiles + tile)
                }
                updateConfig(newConfig)
                showAddDialog = false
            }
        )
    }

    // ---- 添加自定义磁贴弹窗 ----
    if (showCustomDialog) {
        CustomTileDialog(
            onDismiss = {
                showCustomDialog = false
                customTileValue = ""
            },
            onConfirm = { value ->
                val currentTiles = if (selectedTabIndex == 0) {
                    config.expandedTiles
                } else {
                    config.collapsedTiles
                }
                val newConfig = if (selectedTabIndex == 0) {
                    config.copy(expandedTiles = currentTiles + value)
                } else {
                    config.copy(collapsedTiles = currentTiles + value)
                }
                updateConfig(newConfig)
                showCustomDialog = false
                customTileValue = ""
            },
            value = customTileValue,
            onValueChange = { customTileValue = it }
        )
    }
}

/**
 * 单个磁贴条目组件。
 * 展示序号、磁贴显示名/原始值，以及上移/下移/移除操作按钮。
 *
 * @param tileValue 磁贴的系统标识值（如 "wifi"、"bt" 等）
 * @param index     在列表中的位置（0-based）
 * @param isFirst   是否是列表第一个元素（控制上移按钮灰显）
 * @param isLast    是否是列表最后一个元素（控制下移按钮灰显）
 * @param onMoveUp  上移回调
 * @param onMoveDown 下移回调
 * @param onRemove  移除回调
 */
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
        // 序号
        Text(
            text = "${index + 1}",
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.width(24.dp)
        )

        // 名称 + 原始值（两行）
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

        // 上移按钮（首项禁用）
        IconButton(onClick = onMoveUp, enabled = !isFirst) {
            Icon(Icons.Default.ExpandLess, contentDescription = "上移")
        }

        // 下移按钮（末项禁用）
        IconButton(onClick = onMoveDown, enabled = !isLast) {
            Icon(Icons.Default.ExpandMore, contentDescription = "下移")
        }

        // 移除按钮
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Remove, contentDescription = "移除")
        }
    }
}

/**
 * 添加磁贴选择弹窗。
 * 以全屏 Dialog 展示所有未被选中的磁贴，点击即可添加到当前列表。
 *
 * @param title         弹窗标题
 * @param existingTiles 当前列表中已有的磁贴值（用于过滤）
 * @param onDismiss     关闭回调
 * @param onAdd         选中磁贴后的回调，传入磁贴标识值
 */
@Composable
private fun AddTileDialog(
    title: String,
    existingTiles: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    // 从磁贴映射表中筛出尚未被选中的磁贴
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
                // 标题行
                item {
                    Text(text = title, style = MiuixTheme.textStyles.headline2)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // 可选磁贴列表（每行显示名称 + 原始值 + 添加图标）
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

/**
 * 自定义磁贴输入弹窗。
 * 让用户手动输入磁贴标识值（如 custom(包名/类名)），确认后添加到当前列表。
 */
@Composable
private fun CustomTileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    value: String,
    onValueChange: (String) -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(text = "添加自定义磁贴", style = MiuixTheme.textStyles.headline2)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "请输入磁贴标识值，如 custom(包名/类名)",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = "磁贴值",
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = { if (value.isNotBlank()) onConfirm(value.trim()) },
                        modifier = Modifier.weight(1f),
                        enabled = value.isNotBlank()
                    ) {
                        Text("添加")
                    }
                }
            }
        }
    }
}
