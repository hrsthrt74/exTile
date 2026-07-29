package com.hrsthrt74.qstile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.DebugToolsActivity
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.data.TileMapping
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import com.hrsthrt74.qstile.shizuku.ShizukuHelper
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun DebugToolsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    var config by remember { mutableStateOf(TileConfig()) }
    var currentSysuiTiles by remember { mutableStateOf("") }
    var currentTiles by remember { mutableStateOf(emptyList<String>()) }
    var shizukuInstalled by remember { mutableStateOf(false) }
    var shizukuRunning by remember { mutableStateOf(false) }
    var hasWriteSecureSettings by remember { mutableStateOf(false) }
    var hasQueryAllPackages by remember { mutableStateOf(false) }

    fun refreshDebugInfo() {
        scope.launch {
            config = ConfigRepository.getConfig(context)
            currentSysuiTiles = SecureSettingsHelper.getSysuiQsTiles(context) ?: "无法获取"
            currentTiles = SecureSettingsHelper.getCurrentTiles(context)
            shizukuInstalled = ShizukuHelper.isShizukuInstalled(context)
            shizukuRunning = ShizukuHelper.isShizukuRunning()
            hasWriteSecureSettings = ShizukuHelper.hasWriteSecureSettingsPermission(context)
            hasQueryAllPackages = context.checkSelfPermission(android.Manifest.permission.QUERY_ALL_PACKAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    LaunchedEffect(Unit) {
        refreshDebugInfo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "调试工具",
                largeTitle = "调试工具",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { (context as? DebugToolsActivity)?.finish() }) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 12.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = NavigationBarDefaults.ItemHeight +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            )
        ) {
            // ===== 状态信息 =====
            item {
                SmallTitle(text = "状态信息")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        DebugInfoRow("Shizuku 已安装", if (shizukuInstalled) "是" else "否")
                        DebugInfoRow("Shizuku 运行中", if (shizukuRunning) "是" else "否")
                        DebugInfoRow("WRITE_SECURE_SETTINGS", if (hasWriteSecureSettings) "已授权" else "未授权")
                        DebugInfoRow("QUERY_ALL_PACKAGES", if (hasQueryAllPackages) "已授权" else "未授权")
                        DebugInfoRow("展开磁贴数", config.expandedTiles.size.toString())
                        DebugInfoRow("收起磁贴数", config.collapsedTiles.size.toString())
                        DebugInfoRow("系统磁贴数", currentTiles.size.toString())
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ===== 当前配置 =====
            item {
                SmallTitle(text = "当前配置")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "展开时磁贴：",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Text(
                            text = config.expandedTiles.joinToString(", "),
                            style = MiuixTheme.textStyles.footnote2,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "收起时磁贴：",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Text(
                            text = config.collapsedTiles.joinToString(", "),
                            style = MiuixTheme.textStyles.footnote2,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ===== 系统信息 =====
            item {
                SmallTitle(text = "系统信息")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        DebugInfoRow("sysui_qs_tiles", currentSysuiTiles)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ===== 操作 =====
            item {
                SmallTitle(text = "操作")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Button(
                            onClick = { refreshDebugInfo() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("刷新信息")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val debugInfo = buildString {
                                    appendLine("=== exTile 调试信息 ===")
                                    appendLine("Shizuku 已安装: ${if (shizukuInstalled) "是" else "否"}")
                                    appendLine("Shizuku 运行中: ${if (shizukuRunning) "是" else "否"}")
                                    appendLine("WRITE_SECURE_SETTINGS: ${if (hasWriteSecureSettings) "已授权" else "未授权"}")
                                    appendLine("QUERY_ALL_PACKAGES: ${if (hasQueryAllPackages) "已授权" else "未授权"}")
                                    appendLine("展开磁贴: ${config.expandedTiles.joinToString(", ")}")
                                    appendLine("收起磁贴: ${config.collapsedTiles.joinToString(", ")}")
                                    appendLine("系统磁贴数: ${currentTiles.size}")
                                    appendLine("sysui_qs_tiles: $currentSysuiTiles")
                                    appendLine("设备: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                                    appendLine("SDK: ${android.os.Build.VERSION.SDK_INT}")
                                    appendLine("包名: ${context.packageName}")
                                    appendLine("版本: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("exTile debug", debugInfo))
                                Toast.makeText(context, "调试信息已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("复制调试信息")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugInfoRow(label: String, value: String) {
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
