package com.hrsthrt74.qstile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.hrsthrt74.qstile.DebugToolsActivity
import com.hrsthrt74.qstile.LicensesActivity
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.ThemeRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import com.hrsthrt74.qstile.ui.BlurredBar
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.AppDialog
import com.hrsthrt74.qstile.ui.components.rememberDialogState
import com.hrsthrt74.qstile.ui.components.rememberSheetState
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Background
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Forward
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Months
import top.yukonga.miuix.kmp.icon.extended.Paste
import top.yukonga.miuix.kmp.icon.extended.Remove
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.Unpin
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private val dayNightModeLabels = listOf("跟随系统", "浅色", "深色")
// 设置项行首图标（或空白占位）与标题文字之间的间距，统一由此变量控制，方便整体调整
private val PreferenceIconEndPadding = 8.dp
// 设置项行首图标（或空白占位）左侧的间距，用于与卡片内边距拉开一点距离
private val PreferenceIconStartPadding = 4.dp
// 无图标设置项的行首空白占位宽度，与图标宽度（24dp）保持一致以对齐
private val PreferenceIconSize = 24.dp
// 调色板风格/颜色规范开关暂未启用，相关标签定义一并注释
// private val paletteStyleLabels = listOf("TonalSpot", "Neutral", "Vibrant", "Expressive")
// private val colorSpecLabels = listOf("Spec2021", "Spec2025")
private val longPressBehaviorLabels = listOf("exTile", "系统设置", "自定义应用")

/**
 * 设置页可展开分类的标识。
 * 用于手风琴互斥展开（同一时间仅一个分类展开，点击当前展开的分类则折叠）。
 * 调试工具分类点击直接跳转 Activity，不参与展开，故不在枚举中。
 */
private enum class SettingsCategory {
    /** 通用：磁贴行为设置 */
    GENERAL,

    /** 外观：主题设置 */
    APPEARANCE,

    /** 数据：备份 / 恢复 / 系统导入 */
    DATA,

    /** 关于：版本 / GitHub / 开源许可 */
    ABOUT
}

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

    // 主题设置：由 ExTileTheme 通过 LocalThemeSettings 同步提供（数据就绪后才组合到这里，不会闪烁）
    val themeSettings = LocalThemeSettings.current

    // 顶部栏模糊：创建 backdrop 捕获滚动内容，模糊开关关闭或 RuntimeShader 不支持时退回纯色
    val backdrop = rememberBlurBackdrop(enabled = themeSettings.enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    var config by remember { mutableStateOf(TileConfig()) }
    var currentSysuiTiles by remember { mutableStateOf("") }
    var backupText by remember { mutableStateOf("") }

    // ===== 设置项状态：本地可写状态 + 首次加载门控（防闪烁） =====
    // 这些开关 UI 会「乐观更新」（点击立即改本地 + 写 DataStore），因此需要可写变量，
    // 不能直接用 flow 的只读状态。防闪烁做法：初始均为各设置的默认值仅作占位，
    // 在 LaunchedEffect 从 DataStore 读回真实值并置 settingsLoaded=true 之前 return 不渲染，
    // 因此用户看不到默认值闪现（未就绪就空白）。

    // 长按 exTile 磁贴行为设置
    var longPressBehavior by remember { mutableIntStateOf(ConfigRepository.LongPressBehavior.OPEN_EXTILE) }
    var customAppPackage by remember { mutableStateOf("") }
    // 展开收起同时控制无字模式：磁贴切换展开/收起布局时同步 wordless_mode（见 ExTileService）
    var wordlessModeSync by remember { mutableStateOf(false) }
    // 展开收起同时控制融合设备中心：磁贴切换展开/收起布局时同步 smart_device_control（见 ExTileService）
    var smartDeviceControlSync by remember { mutableStateOf(false) }
    // 收起 QS 面板时自动收起磁贴布局（见 ExTileService.onStopListening）
    var autoCollapseOnClose by remember { mutableStateOf(false) }
    // 设置是否已全部从 DataStore 读出：false 时不渲染页面主体，避免闪默认值
    var settingsLoaded by remember { mutableStateOf(false) }

    // 自定义应用选择器 Sheet 的显示状态（统一由 AppBottomSheet 管理）
    val appPickerSheetState = rememberSheetState()
    // 应用选择器数据：null 表示尚未加载，列表在后台线程加载避免阻塞主线程
    var launchableApps by remember { mutableStateOf<List<LaunchableApp>?>(null) }
    // 应用选择器的搜索关键字：同时匹配应用名和包名
    var appSearchQuery by remember { mutableStateOf("") }

    // 系统磁贴查看 / 备份结果 / 导入备份 三个 Sheet 的显示状态（统一由 AppBottomSheet 管理）
    val systemTilesSheetState = rememberSheetState()
    val backupSheetState = rememberSheetState()
    val importBackupSheetState = rememberSheetState()
    // 打赏（微信收款码）Sheet 的显示状态（统一由 AppBottomSheet 管理）
    val donateSheetState = rememberSheetState()
    // 从系统导入磁贴的确认对话框显示状态（统一由 AppDialog 管理）
    val importDialogState = rememberDialogState()
    var importBackupJson by remember { mutableStateOf("") }
    // 当前展开的设置分类（手风琴模式：同一时间仅一个展开，null 表示全部折叠）
    var expandedCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

    // 首次进入页面：一次性读回全部设置项后再放行渲染（未就绪就空白，与主题/OOBE 门控策略一致）
    LaunchedEffect(Unit) {
        config = ConfigRepository.getConfig(context)
        currentSysuiTiles = SecureSettingsHelper.getSysuiQsTiles(context) ?: ""
        longPressBehavior = ConfigRepository.getLongPressBehavior(context)
        customAppPackage = ConfigRepository.getLongPressCustomApp(context)
        wordlessModeSync = ConfigRepository.getWordlessModeSync(context)
        smartDeviceControlSync = ConfigRepository.getSmartDeviceControlSync(context)
        autoCollapseOnClose = ConfigRepository.getAutoCollapseOnClose(context)
        settingsLoaded = true
    }

    if (!settingsLoaded) return

    // 打开应用选择器时，在后台线程加载应用列表（查询所有应用 + 加载图标较耗时）
    LaunchedEffect(appPickerSheetState.show) {
        if (appPickerSheetState.show && launchableApps == null) {
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

    // ==================== 局部 UI 函数（闭包捕获，不显式传参） ====================
    // 拆分仅出于可读性 / IDE 导航考虑，不改变重组范围与任何业务逻辑。

    /** 通用分类卡片：磁贴行为设置 */
    @Composable
    fun GeneralCategoryCard() {
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

        ExpandableSettingsCard(
            title = "通用",
            icon = MiuixIcons.Tune,
            expanded = expandedCategory == SettingsCategory.GENERAL,
            onToggle = {
                // 手风琴互斥：点击当前展开的分类则折叠，否则切换展开
                expandedCategory = if (expandedCategory == SettingsCategory.GENERAL) {
                    null
                } else {
                    SettingsCategory.GENERAL
                }
            }
        ) {
            SmallTitle(
                text = "磁贴联动",
                insideMargin = PaddingValues(start = 20.dp, top = 12.dp)
            )
            SwitchPreference(
                title = "无字模式",
                summary = "展开显示磁贴名，收起隐藏",
                checked = wordlessModeSync,
                onCheckedChange = { enabled ->
                    wordlessModeSync = enabled
                    scope.launch {
                        ConfigRepository.saveWordlessModeSync(context, enabled)
                    }
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Months) }
            )

            SwitchPreference(
                title = "融合设备中心",
                summary = "展开显示设备中心，收起隐藏",
                checked = smartDeviceControlSync,
                onCheckedChange = { enabled ->
                    smartDeviceControlSync = enabled
                    scope.launch {
                        ConfigRepository.saveSmartDeviceControlSync(context, enabled)
                    }
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.GridView) }
            )

            SwitchPreference(
                title = "自动收起",
                summary = "收起控制中心时，自动折叠",
                checked = autoCollapseOnClose,
                onCheckedChange = { enabled ->
                    autoCollapseOnClose = enabled
                    scope.launch {
                        ConfigRepository.saveAutoCollapseOnClose(context, enabled)
                    }
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Unpin) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            WindowSpinnerPreference(
                title = "长按 exTile 磁贴\n跳转到",
                // summary = "设置长按快捷设置面板中 exTile 磁贴时执行的操作",
                items = longPressBehaviorOptions,
                selectedIndex = longPressBehavior,
                onSelectedIndexChange = { index ->
                    longPressBehavior = index
                    scope.launch {
                        ConfigRepository.saveLongPressBehavior(context, index)
                    }
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Forward) }
            )

            // 仅当选择了「跳转到自定义应用」时显示应用选择入口
            // 使用 AnimatedVisibility 实现平滑的展开/收起动画
            AnimatedVisibility(
                visible = longPressBehavior == ConfigRepository.LongPressBehavior.OPEN_CUSTOM_APP
            ) {
                ArrowPreference(
                    title = "自定义应用",
                    summary = customAppLabel,
                    onClick = { appPickerSheetState.show() },
                    startAction = { PreferenceLeadingPlaceholder() }
                )
            }
        }
    }

    /** 外观分类卡片：主题设置 */
    @Composable
    fun AppearanceCategoryCard() {
        val dayNightModeOptions = remember { dayNightModeLabels.map { DropdownItem(text = it) } }

        ExpandableSettingsCard(
            title = "外观",
            icon = MiuixIcons.Background,
            expanded = expandedCategory == SettingsCategory.APPEARANCE,
            onToggle = {
                expandedCategory = if (expandedCategory == SettingsCategory.APPEARANCE) {
                    null
                } else {
                    SettingsCategory.APPEARANCE
                }
            }
        ) {
            WindowSpinnerPreference(
                title = "配色模式",
                items = dayNightModeOptions,
                selectedIndex = themeSettings.dayNightMode,
                onSelectedIndexChange = { mode ->
                    scope.launch {
                        ThemeRepository.saveDayNightMode(context, mode)
                    }
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Theme) }
            )

            SwitchPreference(
                title = "动态取色",
                summary = "使用 Monet 配色",
                checked = themeSettings.isDynamicColorMode,
                onCheckedChange = { enabled ->
                    scope.launch {
                        ThemeRepository.saveIsDynamicColorMode(context, enabled)
                    }
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Album) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SwitchPreference(
                title = "模糊效果",
                summary = "顶栏 / 底栏的模糊",
                checked = themeSettings.enableBlur,
                onCheckedChange = { enabled ->
                    scope.launch {
                        ThemeRepository.saveEnableBlur(context, enabled)
                    }
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Paste) }
            )

            // 这两个开关仅在指定颜色时可用，暂不提供指定颜色功能，故禁用
            // if (themeSettings.isDynamicColorMode) {
            //     WindowSpinnerPreference(
            //         title = "调色板风格",
            //         items = paletteStyleOptions,
            //         selectedIndex = themeSettings.paletteStyle,
            //         onSelectedIndexChange = { style ->
            //             scope.launch {
            //                 ThemeRepository.savePaletteStyle(context, style)
            //             }
            //         }
            //     )
            //     WindowSpinnerPreference(
            //         title = "颜色规范",
            //         items = colorSpecOptions,
            //         selectedIndex = themeSettings.colorSpec,
            //         onSelectedIndexChange = { spec ->
            //             scope.launch {
            //                 ThemeRepository.saveColorSpec(context, spec)
            //             }
            //         }
            //     )
            // }
        }
    }

    /** 数据分类卡片：备份 / 恢复 / 系统导入（合并原两个板块） */
    @Composable
    fun DataCategoryCard() {
        ExpandableSettingsCard(
            title = "数据",
            icon = MiuixIcons.Backup,
            expanded = expandedCategory == SettingsCategory.DATA,
            onToggle = {
                expandedCategory = if (expandedCategory == SettingsCategory.DATA) {
                    null
                } else {
                    SettingsCategory.DATA
                }
            }
        ) {
            ArrowPreference(
                title = "查看系统磁贴配置",
                summary = "sysui_qs_tiles 值",
                onClick = { systemTilesSheetState.show() },
                startAction = { PreferenceLeadingIcon(MiuixIcons.File) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ArrowPreference(
                title = "备份",
//                summary = "将当前配置导出为 JSON 格式",
                onClick = {
                    backupText = generateBackupJson()
                    backupSheetState.show()
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Remove) }
            )

            ArrowPreference(
                title = "恢复",
//                summary = "从 JSON 文本恢复磁贴配置",
                onClick = {
                    importBackupJson = ""
                    importBackupSheetState.show()
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Import) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            ArrowPreference(
                title = "从系统导入磁贴",
                summary = "将当前系统磁贴配置保存为展开状态",
                onClick = { importDialogState.show() },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Download) }
            )
        }
    }

    /** 关于分类卡片：版本 / GitHub / 开源许可 */
    @Composable
    fun AboutCategoryCard() {
        ExpandableSettingsCard(
            title = "关于",
            icon = MiuixIcons.Info,
            expanded = expandedCategory == SettingsCategory.ABOUT,
            onToggle = {
                expandedCategory = if (expandedCategory == SettingsCategory.ABOUT) {
                    null
                } else {
                    SettingsCategory.ABOUT
                }
            }
        ) {
            ArrowPreference(
                title = "版本",
                summary = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
                } catch (e: Exception) {
                    "未知"
                },
                onClick = {},
                startAction = { PreferenceLeadingIcon(R.drawable.ic_settings_extile) }
            )

            ArrowPreference(
                title = "GitHub",
                summary = "hrsthrt74/exTile",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/hrsthrt74/exTile")))
                },
                startAction = { PreferenceLeadingIcon(R.drawable.ic_github) }
            )

            ArrowPreference(
                title = "开源许可",
                summary = "查看本应用使用的开源库及许可证",
                onClick = {
                    context.startActivity(Intent(context, LicensesActivity::class.java))
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.ListView) }
            )

            // 打赏：点击弹出微信收款码图片
            ArrowPreference(
                title = "打赏",
                summary = "如果这个应用对你有帮助，请我喝杯奶茶",
                onClick = { donateSheetState.show() },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Favorites) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // 重新运行首次配置引导：仅重置 OOBE 标志（配置数据保留，可再次导入覆盖），
            // 壳层监听到标志变 false 后自动切回 OOBE 页面
            ArrowPreference(
                title = "重新运行引导",
                onClick = {
                    scope.launch {
                        ConfigRepository.saveOobeCompleted(context, false)
                    }
                },
                startAction = { PreferenceLeadingPlaceholder() }
            )
        }
    }

    /** 系统磁贴查看 Sheet */
    @Composable
    fun SystemTilesSheet() {
        AppBottomSheet(
            state = systemTilesSheetState,
            title = "系统磁贴配置",
        ) {
            Text(
                text = currentSysuiTiles.ifEmpty { "无数据" },
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.padding(bottom = navBarBottomPadding)
            )
        }
    }

    /** 备份结果 Sheet */
    @Composable
    fun BackupSheet() {
        AppBottomSheet(
            state = backupSheetState,
            title = "备份数据",
        ) {
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
                            backupSheetState.dismiss()
                        },
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("复制")
                    }
            }
        }
    }

    /** 打赏 Sheet：展示微信收款码 */
    @Composable
    fun DonateSheet() {
        AppBottomSheet(
            state = donateSheetState,
            title = "打赏",
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = navBarBottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "如果这个应用对你有帮助，请我喝杯奶茶～",
                    style = MiuixTheme.textStyles.body2,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Image(
                    painter = painterResource(R.drawable.pic_donate_wx),
                    contentDescription = "微信收款码",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                        .squircleClip(16.dp)
                )
            }
        }
    }

    /** 导入备份 Sheet */
    @Composable
    fun ImportBackupSheet() {
        AppBottomSheet(
            state = importBackupSheetState,
            title = "导入备份",
            // 关闭动画完成后清空输入的 JSON，避免下次打开残留
            onDismissed = { importBackupJson = "" },
        ) {
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
                            importBackupSheetState.dismiss()
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
    }

    /** 系统导入确认 Dialog */
    @Composable
    fun ImportSystemTilesDialog() {
        AppDialog(
            state = importDialogState,
            title = "从系统导入磁贴",
            summary = "将当前系统磁贴配置保存为展开状态，现有的展开配置将被覆盖。",
            confirmText = "确认",
            onConfirm = {
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
            }
        )
    }

    /** 自定义应用选择器 Sheet */
    @Composable
    fun AppPickerSheet() {
        AppBottomSheet(
            state = appPickerSheetState,
            title = "选择应用",
            // 关闭动画完成后清空搜索关键字，避免下次打开残留
            onDismissed = { appSearchQuery = "" },
        ) {
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
                    // 根据搜索关键字过滤：同时匹配应用名和包名（不区分大小写）
                    val filteredApps = remember(apps, appSearchQuery) {
                        val query = appSearchQuery.trim()
                        if (query.isEmpty()) {
                            apps
                        } else {
                            apps.filter {
                                it.label.contains(query, ignoreCase = true) ||
                                    it.packageName.contains(query, ignoreCase = true)
                            }
                        }
                    }
                    // 使用 Miuix SearchBar 提供胶囊搜索框（支持按应用名或包名搜索）
                    // insideMargin 设为 0 去掉左右边距；InputField expanded=false 避免打开 sheet 时自动聚焦弹键盘
                    // 注意：SearchBar 必须用 expanded=false，否则其内置 NavigationBackHandler（isBackEnabled=expanded）
                    // 会优先消费返回事件（组合在 sheet 内容内层，后注册者优先），导致 sheet 自带的跟手返回动画失效。
                    // 搜索结果列表因此放到 SearchBar 外部渲染，content 传空。
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SearchBar(
                            expanded = false,
                            onExpandedChange = { expanded ->
                                if (!expanded) {
                                    appPickerSheetState.dismiss()
                                }
                            },
                            insideMargin = DpSize(0.dp, 0.dp),
                            inputField = {
                                InputField(
                                    query = appSearchQuery,
                                    onQueryChange = { appSearchQuery = it },
                                    onSearch = {},
                                    expanded = false,
                                    onExpandedChange = {},
                                    label = "搜索",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { }

                        // 搜索框与结果列表之间的间距（用 Spacer 实现，确保有效）
//                    Spacer(modifier = Modifier.height(4.dp))

                        if (filteredApps.isEmpty()) {
                            // 搜索无结果提示
                            Text(
                                text = "没有匹配的应用",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
                                contentPadding = PaddingValues(bottom = navBarBottomPadding)
                            ) {
                                items(filteredApps, key = { it.packageName }) { app ->
                            // 每个应用项使用 Miuix Card 包裹，提供卡片背景（无阴影）+ 圆角
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        customAppPackage = app.packageName
                                        scope.launch {
                                            ConfigRepository.saveLongPressCustomApp(context, app.packageName)
                                        }
                                        appPickerSheetState.dismiss()
                                    },
                                insideMargin = PaddingValues(16.dp),
                                colors = CardDefaults.defaultColors(
                                    color = MiuixTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 应用图标
                                    val icon = app.icon
                                    val bitmap = remember(icon) { icon.toBitmap() }
                                    Icon(
                                        painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) },
                                        contentDescription = app.label,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(48.dp)
                                    )

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
        }
    }
}

    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive) {
                TopAppBar(
                    title = "设置",
                    largeTitle = "设置",
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        // 调试工具入口：右上角设置图标按钮，点击直接跳转 DebugToolsActivity
                        IconButton(onClick = {
                            context.startActivity(Intent(context, DebugToolsActivity::class.java))
                        }) {
                            Icon(MiuixIcons.Settings, contentDescription = "调试工具")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        // 滚动内容挂载 backdrop，供顶部栏模糊捕获
        Box(
            modifier = if (backdrop != null) Modifier.fillMaxSize().layerBackdrop(backdrop) else Modifier.fillMaxSize()
        ) {
        // 外层 Column 将卡片列表整体垂直居中：
            // LazyColumn 用 weight(fill = false) 只占内容实际高度，内容不足一屏时居中，
            // 展开后超出屏幕时自动受限并可正常滚动
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                verticalArrangement = Arrangement.Center
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .scrollEndHaptic(
                            hapticFeedbackType = HapticFeedbackType.TextHandleMove
                        ),
                    contentPadding = PaddingValues(
                        bottom = NavigationBarDefaults.ItemHeight +
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                    )
                ) {

                    // 写 0 是因为 item 之间有间距
            item { Spacer(modifier = Modifier.height(0.dp)) }

            // ===== 通用分类：磁贴行为设置 =====
            item { GeneralCategoryCard() }

            // ===== 外观分类：主题设置 =====
            item { AppearanceCategoryCard() }

            // ===== 数据分类：备份 / 恢复 / 系统导入（合并原两个板块） =====
            item { DataCategoryCard() }

            // ===== 关于分类 =====
            item { AboutCategoryCard() }

                    // 同上面那个 Spacer
            item { Spacer(modifier = Modifier.height(0.dp)) }
                }
            }
        }
    }

    // ---- 系统磁贴查看 Sheet ----
    SystemTilesSheet()

    // ---- 备份结果 Sheet ----
    BackupSheet()

    // ---- 打赏 Sheet ----
    DonateSheet()

    // ---- 导入备份 Sheet ----
    ImportBackupSheet()

    // ---- 系统导入确认 Dialog ----
    ImportSystemTilesDialog()

    // ---- 自定义应用选择器 Sheet ----
    AppPickerSheet()
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

/**
 * 设置项行首图标。
 * 放在各 Preference 的 startAction 中，使用图标自身配色 + 图标宽度（[PreferenceIconSize]），
 * 左右留出间距（[PreferenceIconStartPadding] / [PreferenceIconEndPadding]）。
 * @param icon 图标
 */
@Composable
private fun PreferenceLeadingIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        // 不指定 tint，使用图标自身的默认配色
        // 注意 Modifier 顺序：padding 在前、size 在后，size 约束的是内容区域，
        // 图标始终完整占据 24dp，左右的 padding 是在图标外侧额外留出空隙，不会把图标挤小
        modifier = Modifier
            .padding(start = PreferenceIconStartPadding, end = PreferenceIconEndPadding)
            .size(PreferenceIconSize)
            .alpha(0.8f)
    )
}

/**
 * 设置项行首图标（drawable 版本）。
 * 用于加载 res/drawable 下的自定义图标（如磁贴图标 tile_*.xml），
 * 用法与 [PreferenceLeadingIcon]（ImageVector 版本）一致，调用 `PreferenceLeadingIcon(R.drawable.xxx)` 即可。
 * 通过 painter 加载并使用 [LocalContentColor] 染色，使图标跟随主题前景色
 * （适用于纯色 drawable，如黑色填充的图标在深色模式下也能正常显示）；
 * 若想保留 drawable 的彩色原图，请移除 tint 参数（改用 Color.Unspecified）。
 * @param iconRes drawable 资源 id（R.drawable.xxx）
 */
@Composable
private fun PreferenceLeadingIcon(iconRes: Int) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        // 使用主题前景色染色，适配深浅色模式；miuix Icon 的默认 tint 即 LocalContentColor.current
        tint = LocalContentColor.current,
        modifier = Modifier
            .padding(start = PreferenceIconStartPadding, end = PreferenceIconEndPadding)
            .size(PreferenceIconSize)
            .alpha(0.8f)
    )
}

/**
 * 设置项行首空白占位。
 * 没有配图标的设置项用它占位，保证所有设置项图标列对齐；
 * 宽度与图标一致（[PreferenceIconSize]），左右两侧同样留出与图标相同的间距。
 */
@Composable
private fun PreferenceLeadingPlaceholder() {
    Spacer(
        modifier = Modifier
            .padding(start = PreferenceIconStartPadding, end = PreferenceIconEndPadding)
            .size(PreferenceIconSize)
    )
}

/**
 * 分类图标：圆角矩形背景（primary）+ 图标（onPrimary），用于分类标题行左侧。
 * @param icon 图标
 */
@Composable
private fun CategoryIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(32.dp)
            // .clip(RoundedCornerShape(8.dp))
            .squircleClip(cornerRadius = 8.dp)
            .background(MiuixTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * 可展开的设置分类卡片。
 * 点击标题行在展开/收起之间切换，内容使用 AnimatedVisibility 平滑展开/收起。
 * 标题行左侧为分类图标（[CategoryIcon]），右侧箭头图标随展开状态旋转
 * （收起时指向右，展开时指向下）。
 * @param title 分类标题
 * @param icon 分类图标
 * @param expanded 是否处于展开状态
 * @param onToggle 点击标题行时的回调（切换展开状态）
 * @param content 展开后展示的设置项内容
 */
@Composable
private fun ExpandableSettingsCard(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    // 箭头旋转动画：展开时旋转 90°（指向下），收起时回到 0°（指向右）
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "expandArrowRotation"
    )
    // 卡片背景不透明度动画：收起时 0.45，展开时 1（颜色 token 不变，仅不透明度变化）
    val cardAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.45f,
        animationSpec = tween(durationMillis = 200),
        label = "expandCardAlpha"
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = cardAlpha)
        )
    ) {
        // 标题行：布局样式对齐 Miuix Preference（headline1 字号 + Medium 字重 + 16dp 内边距）
        // 点击时触发触觉反馈（LongPress 震动）并切换展开状态
        val haptic = LocalHapticFeedback.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标
            CategoryIcon(icon = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            // 展开/收起箭头图标，与 Preference 的箭头样式保持一致（10x16dp）
            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier = Modifier
                    .size(width = 10.dp, height = 16.dp)
                    .rotate(arrowRotation)
            )
        }
        // 展开内容：与标题行之间用分隔线隔开，动画复用 AnimatedVisibility 默认展开/收起动画
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                content()
            }
        }
    }
}
