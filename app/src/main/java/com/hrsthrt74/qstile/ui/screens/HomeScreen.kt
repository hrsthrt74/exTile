package com.hrsthrt74.qstile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import com.hrsthrt74.qstile.shizuku.ShizukuHelper
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToConfig: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(false) }
    var isShizukuInstalled by remember { mutableStateOf(false) }
    var isShizukuRunning by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf(TileConfig()) }
    var currentTilesCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        hasPermission = ShizukuHelper.hasWriteSecureSettingsPermission(context)
        isShizukuInstalled = ShizukuHelper.isShizukuInstalled(context)
        isShizukuRunning = ShizukuHelper.isShizukuRunning()
        config = ConfigRepository.getConfig(context)
        currentTilesCount = SecureSettingsHelper.getCurrentTiles(context).size
    }

    fun refreshStatus() {
        scope.launch {
            hasPermission = ShizukuHelper.hasWriteSecureSettingsPermission(context)
            isShizukuInstalled = ShizukuHelper.isShizukuInstalled(context)
            isShizukuRunning = ShizukuHelper.isShizukuRunning()
            config = ConfigRepository.getConfig(context)
            currentTilesCount = SecureSettingsHelper.getCurrentTiles(context).size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "exTile",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        PermissionStatusCard(
            hasPermission = hasPermission,
            isShizukuInstalled = isShizukuInstalled,
            isShizukuRunning = isShizukuRunning,
            onRequestPermission = {
                scope.launch {
                    if (isShizukuRunning && ShizukuHelper.checkPermission()) {
                        val granted = ShizukuHelper.grantWriteSecureSettings(context)
                        if (granted) {
                            refreshStatus()
                        }
                    }
                }
            }
        )

        CurrentStatusCard(
            isExpanded = config.isExpanded,
            expandedTilesCount = config.expandedTiles.size,
            collapsedTilesCount = config.collapsedTiles.size,
            currentTilesCount = currentTilesCount
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateToConfig,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("磁贴配置")
            }

            OutlinedButton(
                onClick = onNavigateToBackup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("备份与恢复")
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (hasPermission) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasPermission) "已获得权限" else "未获得权限",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!hasPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        !isShizukuInstalled -> "请先安装 Shizuku 应用"
                        !isShizukuRunning -> "请先启动 Shizuku 服务"
                        else -> "点击下方按钮获取 WRITE_SECURE_SETTINGS 权限"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRequestPermission,
                    enabled = isShizukuRunning
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
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isExpanded) "当前：展开状态" else "当前：收起状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "展开时磁贴数：$expandedTilesCount",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "收起时磁贴数：$collapsedTilesCount",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "当前系统磁贴数：$currentTilesCount",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
