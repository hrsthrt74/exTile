package com.hrsthrt74.qstile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.hrsthrt74.qstile.DebugToolsActivity
import com.hrsthrt74.qstile.LicensesActivity
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.ThemeRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import top.yukonga.miuix.kmp.window.WindowDialog

private val dayNightModeLabels = listOf("跟随系统", "浅色", "深色")
private val paletteStyleLabels = listOf("TonalSpot", "Neutral", "Vibrant", "Expressive")
private val colorSpecLabels = listOf("Spec2021", "Spec2025")
private val longPressBehaviorLabels = listOf("跳转到 exTile", "跳转到系统设置", "跳转到自定义应用")

/** 应用选择器数据模型：包名 + 显示名 + 图标 */
private data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

/**
 * 获取所有可启动的应用（有 LAUNCHER intent 的应用）
 * @param context Context
 * @return 可启动应用列表
 */
private fun getLaunchableApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0).mapNotNull { resolveInfo ->
        val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
        val packageName = activityInfo.packageName
        val label = try {
            activityInfo.loadLabel(pm).toString()
        } catch (e: Exception) {
            packageName
        }
        val icon = try {
            activityInfo.loadIcon(pm)
        } catch (e: Exception) {
            null
        }
        if (icon != null) LaunchableApp(packageName, label, icon) else null
    }.distinctBy { it.packageName }.sortedBy { it.label }
}

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

    // 长按 exTile 磁贴行为设置
    var longPressBehavior by remember { mutableIntStateOf(ConfigRepository.LongPressBehavior.OPEN_EXTILE) }
    var customAppPackage by remember { mutableStateOf("") }
    var showAppPickerSheet by remember { mutableStateOf(false) }
    // 应用选择器数据：null 表示尚未加载，列表在后台线程加载避免阻塞主线程
    var launchableApps by remember { mutableStateOf<List<LaunchableApp>?>(null) }

    var showSystemTilesSheet by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showImportBackupSheet by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importBackupJson by remember { mutableStateOf("") }

    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

    LaunchedEffect(Unit) {
        config = ConfigRepository.getConfig(context)
        currentSysuiTiles = SecureSettingsHelper.getSysuiQsTiles(context) ?: ""
        longPressBehavior = ConfigRepository.getLongPressBehavior(context)
        customAppPackage = ConfigRepository.getLongPressCustomApp(context)
    }

    // 打开应用选择器时，在后台线程加载应用列表（查询所有应用 + 加载图标较耗时）
    LaunchedEffect(showAppPickerSheet) {
        if (showAppPickerSheet && launchableApps == null) {
            launchableApps = withContext(Dispatchers.IO) {
                getLaunchableApps(context)
            }
        }
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
    val longPressBehaviorOptions = remember { longPressBehaviorLabels.map { DropdownItem(text = it) } }

    // 已选自定义应用的显示名（包名 → 应用名）
    val customAppLabel = remember(customAppPackage) {
        if (customAppPackage.isBlank()) {
            "未选择"
        } else {
            try {
                context.packageManager.getApplicationInfo(customAppPackage, 0)
                    .loadLabel(context.packageManager).toString()
            } catch (e: Exception) {
                customAppPackage
            }
        }
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
                    hapticFeedbackType = HapticFeedbackType.TextHandleMove
                ),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = NavigationBarDefaults.ItemHeight +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            )
        ) {
            // ===== 调试工具板块 =====
            item {
                SmallTitle(text = "调试工具")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    ArrowPreference(
                        title = "调试工具",
                        summary = "查看详细调试信息和执行调试操作",
                        onClick = {
                            context.startActivity(Intent(context, DebugToolsActivity::class.java))
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ===== 磁贴行为板块 =====
            item {
                SmallTitle(text = "磁贴行为")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    WindowSpinnerPreference(
                        title = "长按 exTile 磁贴行为",
                        summary = "设置长按快捷设置面板中 exTile 磁贴时执行的操作",
                        items = longPressBehaviorOptions,
                        selectedIndex = longPressBehavior,
                        onSelectedIndexChange = { index ->
                            longPressBehavior = index
                            scope.launch {
                                ConfigRepository.saveLongPressBehavior(context, index)
                            }
                        }
                    )

                    // 仅当选择了「跳转到自定义应用」时显示应用选择入口
                    if (longPressBehavior == ConfigRepository.LongPressBehavior.OPEN_CUSTOM_APP) {
                        ArrowPreference(
                            title = "自定义应用",
                            summary = customAppLabel,
                            onClick = { showAppPickerSheet = true }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

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
                    ArrowPreference(
                        title = "查看系统磁贴配置",
                        summary = "查看当前系统的 QS 磁贴原始值",
                        onClick = { showSystemTilesSheet = true }
                    )

                    ArrowPreference(
                        title = "生成备份",
                        summary = "将当前配置导出为 JSON 格式",
                        onClick = {
                            backupText = generateBackupJson()
                            showBackupSheet = true
                        }
                    )

                    ArrowPreference(
                        title = "导入备份",
                        summary = "从 JSON 文本恢复磁贴配置",
                        onClick = {
                            importBackupJson = ""
                            showImportBackupSheet = true
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ===== 系统导入板块 =====
            item {
                SmallTitle(text = "数据导入")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    ArrowPreference(
                        title = "从系统导入磁贴",
                        summary = "将当前系统磁贴配置保存为展开状态",
                        onClick = { showImportDialog = true }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // ===== 关于板块 =====
            item {
                SmallTitle(text = "关于")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    ArrowPreference(
                        title = "版本",
                        summary = try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
                        } catch (e: Exception) {
                            "未知"
                        },
                        onClick = {}
                    )

                    ArrowPreference(
                        title = "GitHub",
                        summary = "hrsthrt74/exTile",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/hrsthrt74/exTile")))
                        }
                    )

                    ArrowPreference(
                        title = "开源许可",
                        summary = "查看本应用使用的开源库及许可证",
                        onClick = {
                            context.startActivity(Intent(context, LicensesActivity::class.java))
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // ---- 系统磁贴查看 Sheet ----
    WindowBottomSheet(
        show = showSystemTilesSheet,
        title = "系统磁贴配置",
        onDismissRequest = { showSystemTilesSheet = false }
    ) {
        BackHandler { showSystemTilesSheet = false }
        Text(
            text = currentSysuiTiles.ifEmpty { "无数据" },
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.padding(bottom = navBarBottomPadding)
        )
    }

    // ---- 备份结果 Sheet ----
    WindowBottomSheet(
        show = showBackupSheet,
        title = "备份数据",
        onDismissRequest = { showBackupSheet = false }
    ) {
        BackHandler { showBackupSheet = false }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = navBarBottomPadding)
        ) {
            Text(
                text = backupText,
                // 别问为什么是脚注2
                style = MiuixTheme.textStyles.footnote2,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 8.dp)
                    .verticalScroll(rememberScrollState())
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("exTile backup", backupText))
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        showBackupSheet = false
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("复制")
                }
        }
    }

    // ---- 导入备份 Sheet ----
    WindowBottomSheet(
        show = showImportBackupSheet,
        title = "导入备份",
        onDismissRequest = {
            showImportBackupSheet = false
            importBackupJson = ""
        }
    ) {
        BackHandler {
            showImportBackupSheet = false
            importBackupJson = ""
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = navBarBottomPadding)
        ) {
            TextField(
                value = importBackupJson,
                onValueChange = { importBackupJson = it },
                label = "粘贴 JSON 备份数据",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val restored = parseBackupJson(importBackupJson)
                    if (restored != null) {
                        scope.launch {
                            ConfigRepository.saveExpandedTiles(context, restored.expandedTiles)
                            ConfigRepository.saveCollapsedTiles(context, restored.collapsedTiles)
                            config = restored
                            Toast.makeText(context, "配置已恢复", Toast.LENGTH_SHORT).show()
                        }
                        showImportBackupSheet = false
                        importBackupJson = ""
                    } else {
                        Toast.makeText(context, "格式错误", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("恢复")
            }
        }
    }

    // ---- 系统导入确认 Dialog ----
    WindowDialog(
        show = showImportDialog,
        title = "从系统导入磁贴",
        summary = "将当前系统磁贴配置保存为展开状态，现有的展开配置将被覆盖。",
        onDismissRequest = { showImportDialog = false }
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        BackHandler { showImportDialog = false }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                text = "取消",
                onClick = { showImportDialog = false },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                text = "确认",
                colors = ButtonDefaults.textButtonColorsPrimary(),
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
                    showImportDialog = false
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // ---- 自定义应用选择器 Sheet ----
    WindowBottomSheet(
        show = showAppPickerSheet,
        title = "选择应用",
        onDismissRequest = { showAppPickerSheet = false }
    ) {
        BackHandler { showAppPickerSheet = false }

        val haptic = LocalHapticFeedback.current
        // 列表为空时是加载中，需要区分"加载中"和"确实没有应用"
        val apps = launchableApps
        val isLoading = apps == null

        when {
            isLoading -> {
                // 加载状态：使用 Miuix 无限进度指示器 + 加载文本，居中显示
                // 用 fillMaxHeight 撑满，与列表加载完成后的 sheet 高度一致，避免高度突变
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    InfiniteProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "加载中...",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }
            apps.isEmpty() -> {
                Text(
                    text = "没有可用的应用",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
                    contentPadding = PaddingValues(bottom = navBarBottomPadding)
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    customAppPackage = app.packageName
                                    scope.launch {
                                        ConfigRepository.saveLongPressCustomApp(context, app.packageName)
                                    }
                                    showAppPickerSheet = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 应用图标
                            val icon = app.icon
                            if (icon != null) {
                                val bitmap = remember(icon) { icon.toBitmap() }
                                Icon(
                                    painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) },
                                    contentDescription = app.label,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = app.label,
                                    style = MiuixTheme.textStyles.body1
                                )
                                Text(
                                    text = app.packageName,
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 调试信息行组件。
 * 左对齐显示标签文字，右对齐显示对应的数值。
 */
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
