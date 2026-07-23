package com.hrsthrt74.qstile.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import com.hrsthrt74.qstile.shizuku.ShizukuHelper
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HomeScreen(
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    var hasPermission by remember { mutableStateOf(false) }
    var isShizukuInstalled by remember { mutableStateOf(false) }
    var isShizukuRunning by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf(TileConfig()) }
    var currentTilesCount by remember { mutableStateOf(0) }

    fun refreshStatus() {
        hasPermission = ShizukuHelper.hasWriteSecureSettingsPermission(context)
        isShizukuInstalled = ShizukuHelper.isShizukuInstalled(context)
        isShizukuRunning = ShizukuHelper.isShizukuRunning()
        scope.launch {
            config = ConfigRepository.getConfig(context)
            currentTilesCount = SecureSettingsHelper.getCurrentTiles(context).size
        }
    }

    LaunchedEffect(Unit) {
        refreshStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "exTile",
                largeTitle = "exTile",
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
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
        ) {
            item {
                PermissionStatusCard(
                    hasPermission = hasPermission,
                    isShizukuInstalled = isShizukuInstalled,
                    isShizukuRunning = isShizukuRunning,
                    onRequestPermission = {
                        onRequestShizukuPermission { granted ->
                            if (granted) {
                                scope.launch {
                                    val success = ShizukuHelper.grantWriteSecureSettings(context)
                                    if (success) {
                                        Toast.makeText(context, "WRITE_SECURE_SETTINGS 权限已授予", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "权限授予失败", Toast.LENGTH_SHORT).show()
                                    }
                                    refreshStatus()
                                }
                            }
                        }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                CurrentStatusCard(
                    isExpanded = config.isExpanded,
                    expandedTilesCount = config.expandedTiles.size,
                    collapsedTilesCount = config.collapsedTiles.size,
                    currentTilesCount = currentTilesCount
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    hasPermission: Boolean,
    isShizukuInstalled: Boolean,
    isShizukuRunning: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (hasPermission) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (hasPermission) "已获得权限" else "未获得权限",
                        style = MiuixTheme.textStyles.title2,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            hasPermission -> "WRITE_SECURE_SETTINGS 已授权"
                            !isShizukuInstalled -> "请先安装 Shizuku 应用"
                            !isShizukuRunning -> "请先启动 Shizuku 服务"
                            else -> "点击下方按钮获取权限"
                        },
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }

            if (!hasPermission) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRequestPermission,
                    enabled = isShizukuInstalled && isShizukuRunning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("获取权限")
                }
            }
        }
    }
}

@Composable
private fun CurrentStatusCard(
    isExpanded: Boolean,
    expandedTilesCount: Int,
    collapsedTilesCount: Int,
    currentTilesCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isExpanded) "当前：展开状态" else "当前：收起状态",
                    style = MiuixTheme.textStyles.title2
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StatusRow("展开时磁贴数", expandedTilesCount.toString())
            StatusRow("收起时磁贴数", collapsedTilesCount.toString())
            StatusRow("当前系统磁贴数", currentTilesCount.toString())
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface
        )
    }
}
