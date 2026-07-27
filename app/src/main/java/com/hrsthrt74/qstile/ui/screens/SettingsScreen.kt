package com.hrsthrt74.qstile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.ThemeRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private val dayNightModeLabels = listOf("跟随系统", "浅色", "深色")
private val paletteStyleLabels = listOf("TonalSpot", "Neutral", "Vibrant", "Expressive")
private val colorSpecLabels = listOf("Spec2021", "Spec2025")

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    val themeSettings by ThemeRepository.getThemeSettingsFlow(context)
        .collectAsState(initial = com.hrsthrt74.qstile.data.ThemeSettings())

    var config by remember { mutableStateOf(TileConfig()) }
    var currentSysuiTiles by remember { mutableStateOf("") }
    var backupText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        config = ConfigRepository.getConfig(context)
        currentSysuiTiles = SecureSettingsHelper.getSysuiQsTiles(context) ?: ""
    }

    fun generateBackupJson(): String {
        return buildString {
            append("{\n")
            append("  \"expandedTiles\": [${config.expandedTiles.joinToString(", ") { "\"$it\"" }}],\n")
            append("  \"collapsedTiles\": [${config.collapsedTiles.joinToString(", ") { "\"$it\"" }}],\n")
            append("  \"currentSysuiTiles\": \"$currentSysuiTiles\"\n")
            append("}")
        }
    }

    fun parseBackupJson(json: String): TileConfig? {
        return try {
            val expandedMatch = Regex("\"expandedTiles\":\\s*\\[([^\\]]+)\\]").find(json)
            val collapsedMatch = Regex("\"collapsedTiles\":\\s*\\[([^\\]]+)\\]").find(json)
            if (expandedMatch != null && collapsedMatch != null) {
                val expanded = expandedMatch.groupValues[1].split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }
                val collapsed = collapsedMatch.groupValues[1].split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }
                TileConfig(expandedTiles = expanded, collapsedTiles = collapsed)
            } else null
        } catch (_: Exception) { null }
    }

    val dayNightModeOptions = remember { dayNightModeLabels.map { DropdownItem(text = it) } }
    val paletteStyleOptions = remember { paletteStyleLabels.map { DropdownItem(text = it) } }
    val colorSpecOptions = remember { colorSpecLabels.map { DropdownItem(text = it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "设置",
                largeTitle = "设置",
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .scrollEndHaptic(
                    hapticFeedbackType = HapticFeedbackType.TextHandleMove
                ),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 16.dp
            )
        ) {
            // ===== 主题设置板块 =====
            item {
                SmallTitle(text = "主题")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    WindowSpinnerPreference(
                        title = "配色模式",
                        items = dayNightModeOptions,
                        selectedIndex = themeSettings.dayNightMode,
                        onSelectedIndexChange = { mode ->
                            scope.launch {
                                ThemeRepository.saveDayNightMode(context, mode)
                            }
                        }
                    )

                    SwitchPreference(
                        title = "动态取色",
                        summary = "从壁纸中提取颜色方案",
                        checked = themeSettings.isDynamicColorMode,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                ThemeRepository.saveIsDynamicColorMode(context, enabled)
                            }
                        }
                    )

                    if (themeSettings.isDynamicColorMode) {
                        WindowSpinnerPreference(
                            title = "调色板风格",
                            items = paletteStyleOptions,
                            selectedIndex = themeSettings.paletteStyle,
                            onSelectedIndexChange = { style ->
                                scope.launch {
                                    ThemeRepository.savePaletteStyle(context, style)
                                }
                            }
                        )
                        WindowSpinnerPreference(
                            title = "颜色规范",
                            items = colorSpecOptions,
                            selectedIndex = themeSettings.colorSpec,
                            onSelectedIndexChange = { spec ->
                                scope.launch {
                                    ThemeRepository.saveColorSpec(context, spec)
                                }
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ===== 备份与恢复板块 =====
            item {
                SmallTitle(text = "备份与恢复")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "当前系统磁贴",
                            style = MiuixTheme.textStyles.title2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentSysuiTiles.ifEmpty { "无数据" },
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "备份配置", style = MiuixTheme.textStyles.title2)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "将当前配置导出为 JSON", style = MiuixTheme.textStyles.body2)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { backupText = generateBackupJson() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("生成备份")
                        }
                    }
                }
            }

            if (backupText.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "备份数据", style = MiuixTheme.textStyles.title2)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = backupText, style = MiuixTheme.textStyles.body2)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("exTile backup", backupText))
                                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("复制")
                                }
                                Button(
                                    onClick = {
                                        val restored = parseBackupJson(backupText)
                                        if (restored != null) {
                                            scope.launch {
                                                ConfigRepository.saveExpandedTiles(context, restored.expandedTiles)
                                                ConfigRepository.saveCollapsedTiles(context, restored.collapsedTiles)
                                                config = restored
                                                Toast.makeText(context, "配置已恢复", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "格式错误", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("恢复")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ===== 系统导入板块 =====
            item {
                SmallTitle(text = "系统导入")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "从系统导入磁贴", style = MiuixTheme.textStyles.title2)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "将当前系统磁贴配置保存为展开状态", style = MiuixTheme.textStyles.body2)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val tiles = SecureSettingsHelper.getCurrentTiles(context)
                                    if (tiles.isNotEmpty()) {
                                        ConfigRepository.saveExpandedTiles(context, tiles)
                                        config = config.copy(expandedTiles = tiles)
                                        Toast.makeText(context, "已导入", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "无法读取", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("从系统导入")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
