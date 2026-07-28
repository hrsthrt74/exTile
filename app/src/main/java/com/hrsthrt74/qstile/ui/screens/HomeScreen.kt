package com.hrsthrt74.qstile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 主页屏幕。
 * 展示 Shizuku 权限状态卡片 + 当前磁贴展开/收起状态卡片。
 * 是用户首次打开应用时看到的第一屏，引导完成 Shizuku 授权流程。
 *
 * @param onRequestShizukuPermission Shizuku 权限请求入口，接收结果回调
 */
@Composable
fun HomeScreen(
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 顶部栏滚动行为，标题会根据滚动折叠
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    // ---- 状态声明 ----
    /** 是否已获得 WRITE_SECURE_SETTINGS 权限 */
    var hasPermission by remember { mutableStateOf(false) }
    /** Shizuku 应用是否已安装 */
    var isShizukuInstalled by remember { mutableStateOf(false) }
    /** Shizuku 服务是否正在运行 */
    var isShizukuRunning by remember { mutableStateOf(false) }
    /** 当前磁贴配置（展开/收起列表 + 开关状态） */
    var config by remember { mutableStateOf(TileConfig()) }
    /** 系统当前磁贴数量 */
    var currentTilesCount by remember { mutableStateOf(0) }

    /**
     * 刷新所有状态。
     * 权限检查在主线程完成（轻量查询），配置读取走协程（涉及 DataStore 和 shell 命令）。
     */
    fun refreshStatus() {
        hasPermission = ShizukuHelper.hasWriteSecureSettingsPermission(context)
        isShizukuInstalled = ShizukuHelper.isShizukuInstalled(context)
        isShizukuRunning = ShizukuHelper.isShizukuRunning()
        scope.launch {
            config = ConfigRepository.getConfig(context)
            currentTilesCount = SecureSettingsHelper.getCurrentTiles(context).size
        }
    }

    // 首次进入屏幕时加载状态
    LaunchedEffect(Unit) {
        refreshStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "exTile",
                largeTitle = "exTile",
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 12.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = NavigationBarDefaults.ItemHeight +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            )
        ) {
            // 权限状态卡片：展示 Shizuku 安装/运行/授权状态，提供授权入口
            item {
                PermissionStatusCard(
                    hasPermission = hasPermission,
                    isShizukuInstalled = isShizukuInstalled,
                    isShizukuRunning = isShizukuRunning,
                    onRequestPermission = {
                        // 点击"获取权限"按钮时 → 先请求 Shizuku 权限 → 再通过 Shizuku 执行 pm grant
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

            // 间距
            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 当前状态卡片：展示展开/收起状态及磁贴数量
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

/**
 * 权限状态卡片组件。
 * 根据 Shizuku 的安装、运行、权限状态展示不同的提示信息和操作按钮。
 */
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
                // 权限状态图标：已授权=绿色对勾，未授权=红色叉号
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
                    // 根据状态组合展现不同的引导文案
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

            // 未授权时显示授权按钮，按钮在 Shizuku 安装且运行时才可用
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

/**
 * 当前磁贴状态卡片组件。
 * 展示展开/收起状态、各模式下的磁贴数量，以及系统当前磁贴总数。
 */
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
                // 展开/收起状态图标
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

            // 三行键值对：展示各项磁贴数量
            StatusRow("展开时磁贴数", expandedTilesCount.toString())
            StatusRow("收起时磁贴数", collapsedTilesCount.toString())
            StatusRow("当前系统磁贴数", currentTilesCount.toString())
        }
    }
}

/**
 * 单行「标签-值」展示组件。
 * 左对齐显示标签文字，右对齐显示对应的数值。
 */
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
