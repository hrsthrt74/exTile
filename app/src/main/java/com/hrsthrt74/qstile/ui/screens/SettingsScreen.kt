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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 配色模式选项标签，索引对应 ColorSchemeMode 枚举值 */
private val themeModeLabels = listOf("动态取色跟随系统", "跟随系统", "动态取色浅色", "动态取色深色", "浅色", "深色")

/** 调色板风格选项标签 */
private val paletteStyleLabels = listOf("TonalSpot", "Neutral", "Vibrant", "Expressive")

/** 颜色规范选项标签 */
private val colorSpecLabels = listOf("Spec2021", "Spec2025")

/**
 * 设置页面。
 * 包含三大板块：主题配色、备份与恢复、从系统导入磁贴配置。
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    // ---- 主题设置状态 ----
    // 从 DataStore Flow 收集主题偏好，自动响应变化
    val themeSettings by ThemeRepository.getThemeSettingsFlow(context)
        .collectAsState(initial = com.hrsthrt74.qstile.data.ThemeSettings())

    // ---- 备份与恢复状态 ----
    /** 当前磁贴配置 */
    var config by remember { mutableStateOf(TileConfig()) }
    /** 当前系统的 sysui_qs_tiles 原始值（备份时引用） */
    var currentSysuiTiles by remember { mutableStateOf("") }
    /** 生成的备份 JSON 文本 */
    var backupText by remember { mutableStateOf("") }

    // ---- 对话框开关 ----
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showSpecDialog by remember { mutableStateOf(false) }

    // 首次进入屏幕时加载配置和系统当前磁贴数据
    LaunchedEffect(Unit) {
        config = ConfigRepository.getConfig(context)
        currentSysuiTiles = SecureSettingsHelper.getSysuiQsTiles(context) ?: ""
    }

    /**
     * 将当前磁贴配置 + 系统磁贴数据序列化为 JSON 字符串。
     * 格式：{ expandedTiles: [...], collapsedTiles: [...], currentSysuiTiles: "..." }
     */
    fun generateBackupJson(): String {
        return buildString {
            append("{\n")
            append("  \"expandedTiles\": [${config.expandedTiles.joinToString(", ") { "\"$it\"" }}],\n")
            append("  \"collapsedTiles\": [${config.collapsedTiles.joinToString(", ") { "\"$it\"" }}],\n")
            append("  \"currentSysuiTiles\": \"$currentSysuiTiles\"\n")
            append("}")
        }
    }

    /**
     * 从 JSON 字符串解析出 TileConfig。
     * 使用简易正则匹配 expandedTiles 和 collapsedTiles 数组。
     * 解析失败或无匹配时返回 null。
     */
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
                    hapticFeedbackType = HapticFeedbackType.TextHandleMove // 默认值
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
                    // 配色模式选择入口
                    ArrowPreference(
                        title = "配色模式",
                        summary = themeModeLabels.getOrElse(themeSettings.colorSchemeMode) { "动态取色跟随系统" },
                        onClick = { showThemeModeDialog = true },
                        holdDownState = showThemeModeDialog
                    )

                    // 仅在动态取色模式下显示调色板风格和颜色规范选项
                    if (themeSettings.colorSchemeMode in 0..3) {
                        // 调色板风格选择入口
                        ArrowPreference(
                            title = "调色板风格",
                            summary = paletteStyleLabels.getOrElse(themeSettings.paletteStyle) { "TonalSpot" },
                            onClick = { showPaletteDialog = true },
                            holdDownState = showPaletteDialog
                        )
                        // 颜色规范选择入口
                        ArrowPreference(
                            title = "颜色规范",
                            summary = colorSpecLabels.getOrElse(themeSettings.colorSpec) { "Spec2021" },
                            onClick = { showSpecDialog = true },
                            holdDownState = showSpecDialog
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ===== 备份与恢复板块 =====
            item {
                SmallTitle(text = "备份与恢复")
            }

            // 显示当前系统磁贴原始值（只读）
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

            // 生成备份按钮
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

            // 备份结果展示卡片：仅在生成备份后出现
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
                            // 复制 / 恢复 双按钮
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 复制 JSON 到系统剪贴板
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
                                // 从 JSON 恢复磁贴配置
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

            // 一键从系统当前的 sysui_qs_tiles 导入为展开状态配置
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

    // ---- 弹窗：配色模式选择 ----
    if (showThemeModeDialog) {
        ThemeModeDialog(
            currentMode = themeSettings.colorSchemeMode,
            onDismiss = { showThemeModeDialog = false },
            onSelect = { mode ->
                scope.launch {
                    ThemeRepository.saveColorSchemeMode(context, mode)
                    showThemeModeDialog = false
                }
            }
        )
    }

    // ---- 弹窗：调色板风格选择 ----
    if (showPaletteDialog) {
        PaletteStyleDialog(
            currentStyle = themeSettings.paletteStyle,
            onDismiss = { showPaletteDialog = false },
            onSelect = { style ->
                scope.launch {
                    ThemeRepository.savePaletteStyle(context, style)
                    showPaletteDialog = false
                }
            }
        )
    }

    // ---- 弹窗：颜色规范选择 ----
    if (showSpecDialog) {
        ColorSpecDialog(
            currentSpec = themeSettings.colorSpec,
            onDismiss = { showSpecDialog = false },
            onSelect = { spec ->
                scope.launch {
                    ThemeRepository.saveColorSpec(context, spec)
                    showSpecDialog = false
                }
            }
        )
    }
}

/**
 * 配色模式选择弹窗。
 * 以全屏 Dialog 呈现所有配色模式选项列表，点击即选并关闭。
 */
@Composable
private fun ThemeModeDialog(
    currentMode: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            LazyColumn(contentPadding = PaddingValues(24.dp)) {
                item {
                    Text(text = "配色模式", style = MiuixTheme.textStyles.headline2)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(themeModeLabels.size) { index ->
                    ArrowPreference(
                        title = themeModeLabels[index],
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}

/**
 * 调色板风格选择弹窗。
 * 仅在动态取色模式下可见，选择后即时生效。
 */
@Composable
private fun PaletteStyleDialog(
    currentStyle: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            LazyColumn(contentPadding = PaddingValues(24.dp)) {
                item {
                    Text(text = "调色板风格", style = MiuixTheme.textStyles.headline2)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(paletteStyleLabels.size) { index ->
                    ArrowPreference(
                        title = paletteStyleLabels[index],
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}

/**
 * 颜色规范选择弹窗。
 * 在 Spec2021（Android 12 风格）和 Spec2025（Material 3 新规范）之间切换。
 */
@Composable
private fun ColorSpecDialog(
    currentSpec: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            LazyColumn(contentPadding = PaddingValues(24.dp)) {
                item {
                    Text(text = "颜色规范", style = MiuixTheme.textStyles.headline2)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(colorSpecLabels.size) { index ->
                    ArrowPreference(
                        title = colorSpecLabels[index],
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}
