package com.hrsthrt74.qstile.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.hrsthrt74.qstile.DebugToolsActivity
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.CustomTileUtils
import com.hrsthrt74.qstile.data.DeviceProfile
import com.hrsthrt74.qstile.data.TileCapabilityFlags
import com.hrsthrt74.qstile.data.TileCatalog
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.data.TileRequirement
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import com.hrsthrt74.qstile.shizuku.ShizukuHelper
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.rememberSheetState
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun DebugToolsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    // 主题设置：由 ExTileTheme 通过 LocalThemeSettings 同步提供（数据就绪后才组合到这里，不会闪烁）
    val haptic = LocalHapticFeedback.current
    // 模糊浮现动画演示状态：初始激活（图标常驻显示），「禁用 icon」播放退出动画（模糊缩小消失），「激活 icon」播放进入动画（模糊缩小浮现）
    var demoActive by remember { mutableStateOf(true) }

    var config by remember { mutableStateOf(TileConfig()) }
    var currentSysuiTiles by remember { mutableStateOf("") }
    var currentTiles by remember { mutableStateOf(emptyList<String>()) }
    var shizukuInstalled by remember { mutableStateOf(false) }
    var shizukuRunning by remember { mutableStateOf(false) }
    var hasWriteSecureSettings by remember { mutableStateOf(false) }
    var hasQueryAllPackages by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    // 添加磁贴 / 添加自定义磁贴 Sheet 的显示状态（统一由 AppBottomSheet 管理）
    val addTileSheetState = rememberSheetState()
    val addCustomTileSheetState = rememberSheetState()
    // 能力开关（调试）Sheet 的显示状态（统一由 AppBottomSheet 管理）
    val capabilitySheetState = rememberSheetState()
    var customTileInput by remember { mutableStateOf("") }

    // Toast 文案在 Composable 上下文预解析(带 %1$s 参数的解析为格式模板,回调内用 format 填参),
    // 避免 onClick/协程 lambda 内 context.getString 触发「非配置感知」lint 错误(LocalContextGetResourceValueCall)
    val unavailableText = stringResource(R.string.debug_unavailable)
    val cannotGetTilesToast = stringResource(R.string.debug_cannot_get_tiles_toast)
    val addedToastTemplate = stringResource(R.string.debug_added_toast)
    val addFailedToast = stringResource(R.string.debug_add_failed_toast)
    val operationFailedToastTemplate = stringResource(R.string.debug_operation_failed_toast)
    val inputRequiredToast = stringResource(R.string.debug_input_required_toast)

    // 设备能力快照（isXiaomi/isTablet/SDK 等一次性采集并缓存，见 DeviceProfile）
    val profile = remember { DeviceProfile.from(context) }

    // 获取可用的系统磁贴（排除已添加的和第三方磁贴）
    val availableTiles = remember(currentTiles, profile) {
        TileCatalog.getAvailableTiles(profile).filter { it.value !in currentTiles && !it.value.startsWith("custom(") }
    }

    // 获取磁贴图标的辅助函数：内置磁贴查静态清单；custom 磁贴运行时取第三方 Service 自身图标；
    // 拿不到时一律回退空白占位图标（tile_blank，不回退应用图标——染色后会变成实心圆角矩形），
    // 保证始终有图标可渲染。与 TileConfigScreen 的同名函数保持一致。
    @Composable
    fun rememberTileIcon(tile: String): Painter {
        // 内置磁贴：静态清单直查
        TileCatalog.iconRes(tile)?.let { return painterResource(it) }
        // custom 磁贴：getCustomTileIcon 内部已保证非空（blank 兜底）；其他未知磁贴值直接用 blank 占位
        val drawable = remember(tile) {
            if (tile.startsWith("custom(")) CustomTileUtils.getCustomTileIcon(context, tile)
            else null
        } ?: context.getDrawable(R.drawable.tile_blank)!!
        val bitmap = remember(drawable) { drawable.toBitmap() }
        return remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
    }

    /**
     * 模糊浮现动画演示：单个尺寸图标的格子。
     * 固定占位高度（80dp），无论图标是否可见都保持相同高度，避免退出动画（图标从组合树移除）时卡片高度坍塌。
     * 注意：RowScope 存在 `AnimatedVisibility` 扩展（供 Modifier.weight 等使用），若在 Row 的 lambda 内
     * 直接调用会产生隐式 receiver 歧义（编译报错），故提取为无 receiver 的局部函数消除歧义。
     */
    @Composable
    fun DemoIconCell(size: Dp) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            // 激活/禁用共用 demoActive，一起播放进入（模糊缩小浮现）/退出（模糊缩小消失）动画，
            // 与磁贴配置页撤销按钮同款参数（scale 0.5↔1、模糊 0↔6dp、进 200ms / 出 150ms）。
            AnimatedVisibility(
                visible = demoActive,
                enter = fadeIn(animationSpec = tween(durationMillis = 200)) +
                    scaleIn(initialScale = 0.5f, animationSpec = tween(durationMillis = 200)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                    scaleOut(targetScale = 0.5f, animationSpec = tween(durationMillis = 150))
            ) {
                // 模糊半径随进出过渡：隐藏 6dp、显示 0dp。
                // 联动「设置-外观-模糊效果」：enableBlur 关闭时恒定 0dp，总是不模糊。
                val blurRadius by if (LocalThemeSettings.current.enableBlur) {
                    transition.animateFloat(
                        transitionSpec = {
                            if (targetState == EnterExitState.Visible) {
                                tween(durationMillis = 200)
                            } else {
                                tween(durationMillis = 150)
                            }
                        },
                        label = "demoBlur"
                    ) { state ->
                        if (state == EnterExitState.Visible) 0f else 6f
                    }
                } else {
                    remember { mutableFloatStateOf(0f) }
                }
                Icon(
                    MiuixIcons.Undo,
                    contentDescription = stringResource(R.string.debug_demo_icon_desc, size.value.toInt()),
                    modifier = Modifier
                        .size(size)
                        .blur(blurRadius.dp)
                )
            }
        }
    }

    fun refreshDebugInfo() {
        scope.launch {
            isLoading = true
            config = ConfigRepository.getConfig(context)
            currentSysuiTiles = SecureSettingsHelper.getSysuiQsTiles(context) ?: unavailableText
            currentTiles = SecureSettingsHelper.getCurrentTiles(context)
            shizukuInstalled = ShizukuHelper.isShizukuInstalled(context)
            shizukuRunning = ShizukuHelper.isShizukuRunning()
            hasWriteSecureSettings = ShizukuHelper.hasWriteSecureSettingsPermission(context)
            hasQueryAllPackages = context.checkSelfPermission(android.Manifest.permission.QUERY_ALL_PACKAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshDebugInfo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.debug_title),
                largeTitle = stringResource(R.string.debug_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { (context as? DebugToolsActivity)?.finish() }) {
                        Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    // 刷新入口：右上角刷新图标按钮，点击重新加载调试信息
                    IconButton(onClick = { refreshDebugInfo() }) {
                        Icon(MiuixIcons.Refresh, contentDescription = stringResource(R.string.debug_refresh_desc))
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
                bottom = NavigationBarDefaults.ItemHeight +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ===== 操作 =====
            item {
                SmallTitle(text = stringResource(R.string.debug_section_actions))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { addTileSheetState.show() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.debug_add_tile))
                        }

                        Button(
                            onClick = {
                                customTileInput = ""
                                addCustomTileSheetState.show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.debug_add_custom))
                        }

                        Button(
                            onClick = { capabilitySheetState.show() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.debug_capability))
                        }

                        // 更改应用语言
                        Button(
                            onClick = {
                                try {
                                    val intent =
                                        Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                            data = "package:${context.packageName}".toUri()
                                        }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // 兜底：个别 ROM 缺少该设置页时避免直接崩溃
                                    Toast.makeText(context, operationFailedToastTemplate.format(e.message ?: ""), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.debug_app_locale))
                        }

//                        Button(
//                            onClick = {
//                                val debugInfo = buildString {
//                                    appendLine("=== exTile 调试信息 ===")
//                                    appendLine("Shizuku 已安装: ${if (shizukuInstalled) "是" else "否"}")
//                                    appendLine("Shizuku 运行中: ${if (shizukuRunning) "是" else "否"}")
//                                    appendLine("WRITE_SECURE_SETTINGS: ${if (hasWriteSecureSettings) "已授权" else "未授权"}")
//                                    appendLine("QUERY_ALL_PACKAGES: ${if (hasQueryAllPackages) "已授权" else "未授权"}")
//                                    appendLine("展开磁贴: ${config.expandedTiles.joinToString(", ")}")
//                                    appendLine("收起磁贴: ${config.collapsedTiles.joinToString(", ")}")
//                                    appendLine("系统磁贴数: ${currentTiles.size}")
//                                    appendLine("sysui_qs_tiles: $currentSysuiTiles")
//                                    appendLine("设备: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
//                                    appendLine("SDK: ${android.os.Build.VERSION.SDK_INT}")
//                                    appendLine("包名: ${context.packageName}")
//                                    appendLine("版本: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
//                                }
//                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                                clipboard.setPrimaryClip(ClipData.newPlainText("exTile debug", debugInfo))
//                                Toast.makeText(context, "调试信息已复制到剪贴板", Toast.LENGTH_SHORT).show()
//                            },
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Text("复制调试信息")
//                        }
                    }
                }
            }

            // ===== 系统信息 / sysui_qs_tiles =====
            item {
                SmallTitle(text = "sysui_qs_tiles")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = currentSysuiTiles,
                        style = MiuixTheme.textStyles.footnote2.copy(  // 👈 用 copy 保留原有样式
                            lineBreak = LineBreak(
                                strategy = LineBreak.Strategy.HighQuality,  // 高质量换行算法，不这么写会在 / 断开
                                strictness = LineBreak.Strictness.Loose,    // 👈 宽松规则，允许在任意字符处断开
                                wordBreak = LineBreak.WordBreak.Default     // 默认单词断开规则
                            )
                        ),
                        lineHeight = 16.sp
                    )
                }
            }



            // ===== 状态信息 =====
            item {
                SmallTitle(text = stringResource(R.string.debug_section_status))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (isLoading) {
                            Text(
                                text = stringResource(R.string.common_loading),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        } else {
                            DebugInfoRow(
                                stringResource(R.string.debug_shizuku_installed),
                                stringResource(if (shizukuInstalled) R.string.debug_yes else R.string.debug_no)
                            )
                            DebugInfoRow(
                                stringResource(R.string.debug_shizuku_running),
                                stringResource(if (shizukuRunning) R.string.debug_yes else R.string.debug_no)
                            )
                            DebugInfoRow(
                                "WRITE_SECURE_SETTINGS",
                                stringResource(if (hasWriteSecureSettings) R.string.debug_granted else R.string.debug_not_granted)
                            )
                            DebugInfoRow(
                                "QUERY_ALL_PACKAGES",
                                stringResource(if (hasQueryAllPackages) R.string.debug_granted else R.string.debug_not_granted)
                            )
                            DebugInfoRow(stringResource(R.string.debug_expanded_count), config.expandedTiles.size.toString())
                            DebugInfoRow(stringResource(R.string.debug_collapsed_count), config.collapsedTiles.size.toString())
                            DebugInfoRow(stringResource(R.string.debug_system_tiles_count), currentTiles.size.toString())
                        }
                    }
                }
            }


            // ===== 当前配置 =====
            item {
                SmallTitle(text = stringResource(R.string.debug_section_config))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.debug_when_expanded),
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Text(
                            text = config.expandedTiles.joinToString(","),
                            style = MiuixTheme.textStyles.footnote2.copy(  // 👈 用 copy 保留原有样式
                                lineBreak = LineBreak(
                                    strategy = LineBreak.Strategy.HighQuality,  // 高质量换行算法，不这么写会在 / 断开
                                    strictness = LineBreak.Strictness.Loose,    // 👈 宽松规则，允许在任意字符处断开
                                    wordBreak = LineBreak.WordBreak.Default     // 默认单词断开规则
                                )
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider()

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.debug_when_collapsed),
                            style = MiuixTheme.textStyles.subtitle,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Text(
                            text = config.collapsedTiles.joinToString(","),
                            style = MiuixTheme.textStyles.footnote2.copy(  // 👈 用 copy 保留原有样式
                                lineBreak = LineBreak(
                                    strategy = LineBreak.Strategy.HighQuality,  // 高质量换行算法，不这么写会在 / 断开
                                    strictness = LineBreak.Strictness.Loose,    // 👈 宽松规则，允许在任意字符处断开
                                    wordBreak = LineBreak.WordBreak.Default     // 默认单词断开规则
                                )
                            ),
                            modifier = Modifier.padding(top = 8.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // ===== 震动测试 =====
            item {
                SmallTitle(text = stringResource(R.string.debug_section_haptic))
                // 震动反馈实例：通过 LocalHapticFeedback 获取，点击按钮时仅触发对应类型的震动
                val haptic = LocalHapticFeedback.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp, 16.dp)
                    ) {
                        // 所有震动类型按每行两个分成一组（两列布局），多余的拼不齐的行单列显示
                        HapticFeedbackType.values().chunked(2).forEachIndexed { index, rowTypes ->
                            // 每行之间加一个垂直间距
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTypes.forEach { type ->
                                    Button(
                                        onClick = {
                                            // 只触发震动，不做其它操作
                                            haptic.performHapticFeedback(type)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            type.toString(),
                                            style = MiuixTheme.textStyles.footnote2
                                        )
                                    }
                                }
                                // 当某行只有一个按钮时，用占位符补齐宽度，保持两列对齐
                                if (rowTypes.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // ===== 模糊浮现动画演示 =====
            item {
                SmallTitle(text = stringResource(R.string.debug_section_blur_demo))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 演示图标尺寸列表（dp）：多尺寸排成一行，方便对比不同大小的模糊浮现效果
                        val demoIconSizes = remember { listOf(24.dp, 36.dp, 48.dp, 60.dp, 72.dp) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 每个图标由 DemoIconCell 渲染（固定占位高度，防止退出动画时卡片高度坍塌）
                            demoIconSizes.forEach { size ->
                                DemoIconCell(size)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 激活 / 禁用两个按钮：控制演示图标显隐，反复切换即可欣赏模糊浮现动画
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    demoActive = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.debug_demo_activate))
                            }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    demoActive = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.debug_demo_deactivate))
                            }
                        }
                    }
                }
            }

        }
    }

    // 能力开关（调试）Sheet：集中管理 flag / prop / 硬件特性的调试开关
    AppBottomSheet(
        state = capabilitySheetState,
        title = stringResource(R.string.debug_capability),
    ) {
        // 内容可滚动，避免开关过多时超出窗口高度
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
            contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            )
        ) {

            item {
                Text(
                    text = stringResource(R.string.debug_capability_note),
                    textAlign = TextAlign.Center,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }

            // ===== 能力 flag =====
            item {
                SmallTitle(
                    text = stringResource(R.string.debug_section_flags),
                    insideMargin = PaddingValues(16.dp, 8.dp)
                )
            }

            // 一组开关用一个 Card 作为背景容器（圆角/内边距由 Card 自动处理，背景色统一 secondaryVariant）
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryVariant
                    )
                ) {
                    Column {
                        SwitchPreference(
                            title = stringResource(R.string.debug_satellite),
                            summary = stringResource(R.string.debug_satellite_summary),
                            checked = TileCapabilityFlags.satelliteOverride ?: profile.hasSatellite,
                            onCheckedChange = { enabled ->
                                TileCapabilityFlags.satelliteOverride = enabled
                            }
                        )

                        SwitchPreference(
                            title = stringResource(R.string.debug_cooling_fan),
                            summary = stringResource(R.string.debug_cooling_fan_summary),
                            checked = TileCapabilityFlags.coolingFanOverride ?: profile.hasCoolingFan,
                            onCheckedChange = { enabled ->
                                TileCapabilityFlags.coolingFanOverride = enabled
                            }
                        )
                    }
                }
            }

            // ===== prop 门控 =====
            item {
                SmallTitle(
                    text = stringResource(R.string.debug_section_props),
                    insideMargin = PaddingValues(16.dp, 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryVariant
                    )
                ) {
                    Column {
                        // 遍历注册表自动生成开关：新增 prop 后无需改 UI
                        TileRequirement.gatedPropKeys.forEach { key ->
                            // 注册表持有资源 id,在 Composable 上下文解析为文案
                            val label = TileRequirement.gatedPropLabels[key]?.let { stringResource(it) } ?: key
                            SwitchPreference(
                                title = label,
                                summary = stringResource(R.string.debug_simulate, key),
                                checked = TileCapabilityFlags.propOverrides[key]
                                    ?: profile.gatedProps[key]
                                    ?: true,
                                onCheckedChange = { enabled ->
                                    TileCapabilityFlags.propOverrides[key] = enabled
                                }
                            )
                        }
                    }
                }
            }

            // ===== 硬件特性 =====
            item {
                SmallTitle(
                    text = stringResource(R.string.debug_section_features),
                    insideMargin = PaddingValues(16.dp, 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryVariant
                    )
                ) {
                    Column {
                        // 遍历注册表自动生成开关：新增 feature 后无需改 UI
                        TileRequirement.gatedFeatureKeys.forEach { feature ->
                            // 注册表持有资源 id,在 Composable 上下文解析为文案
                            val label = TileRequirement.gatedFeatureLabels[feature]?.let { stringResource(it) } ?: feature
                            SwitchPreference(
                                title = label,
                                summary = stringResource(R.string.debug_simulate, feature),
                                checked = TileCapabilityFlags.featureOverrides[feature]
                                    ?: profile.features[feature]
                                    ?: true,
                                onCheckedChange = { enabled ->
                                    TileCapabilityFlags.featureOverrides[feature] = enabled
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 添加磁贴到末尾的 Sheet
    AppBottomSheet(
        state = addTileSheetState,
        title = stringResource(R.string.debug_add_tile),
    ) {
        val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

        if (availableTiles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.debug_no_system_tiles),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // 按分类分组
            val tilesByCategory = remember(availableTiles) {
                availableTiles.groupBy { it.category }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
                contentPadding = PaddingValues(top = 8.dp, bottom = navBarBottomPadding + 16.dp)
            ) {
                tilesByCategory.forEach { (category, tiles) ->
                    // 分类标题
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = category,
                            style = MiuixTheme.textStyles.subtitle,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }

                    // 磁贴网格
                    gridItems(tiles) { tile ->
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val icon = rememberTileIcon(tile.value)
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MiuixTheme.colorScheme.secondaryVariant)
                                    .clickable {
                                        scope.launch {
                                            try {
                                                // 获取当前磁贴列表（内部优先直接 API，Shizuku 兜底）
                                                val currentTileList = SecureSettingsHelper.getCurrentTiles(context).toMutableList()
                                                if (currentTileList.isEmpty()) {
                                                    Toast.makeText(context, cannotGetTilesToast, Toast.LENGTH_SHORT).show()
                                                    return@launch
                                                }
                                                // 检查是否需要添加 ,edit
                                                val isXiaomi = profile.isXiaomi
                                                val hasEdit = currentTileList.contains("edit")
                                                // 添加磁贴到末尾（在 edit 之前）
                                                if (isXiaomi && hasEdit) {
                                                    val editIndex = currentTileList.indexOf("edit")
                                                    currentTileList.add(editIndex, tile.value)
                                                } else {
                                                    currentTileList.add(tile.value)
                                                }
                                                // 立即设置到系统
                                                val success = SecureSettingsHelper.setCurrentTiles(context, currentTileList)
                                                if (success) {
                                                    Toast.makeText(context, addedToastTemplate.format(tile.displayName), Toast.LENGTH_SHORT).show()
                                                    addTileSheetState.dismiss()
                                                    refreshDebugInfo()
                                                } else {
                                                    Toast.makeText(context, addFailedToast, Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, operationFailedToastTemplate.format(e.message ?: ""), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = icon,
                                    contentDescription = tile.displayName,
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = tile.displayName,
                                style = MiuixTheme.textStyles.body2,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = tile.value,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // 添加自定义磁贴到末尾的 Sheet：输入任意磁贴值（wifi/bt 或 custom(包名/类名) 等），不做校验
    AppBottomSheet(
        state = addCustomTileSheetState,
        title = stringResource(R.string.debug_add_custom),
        // 关闭动画完成后清空上次输入的磁贴值，避免下次打开残留
        onDismissed = { customTileInput = "" },
    ) {
        val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = navBarBottomPadding)
        ) {
            TextField(
                value = customTileInput,
                onValueChange = { customTileInput = it },
                label = stringResource(R.string.debug_input_hint),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val value = customTileInput.trim()
                    if (value.isEmpty()) {
                        Toast.makeText(context, inputRequiredToast, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        try {
                            // 获取当前磁贴列表（内部优先直接 API，Shizuku 兜底）
                            val currentTileList = SecureSettingsHelper.getCurrentTiles(context).toMutableList()
                            if (currentTileList.isEmpty()) {
                                Toast.makeText(context, cannotGetTilesToast, Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            // 检查是否需要添加 ,edit
                            val isXiaomi = profile.isXiaomi
                            val hasEdit = currentTileList.contains("edit")
                            // 添加磁贴到末尾（在 edit 之前）
                            if (isXiaomi && hasEdit) {
                                val editIndex = currentTileList.indexOf("edit")
                                currentTileList.add(editIndex, value)
                            } else {
                                currentTileList.add(value)
                            }
                            // 立即设置到系统
                            val success = SecureSettingsHelper.setCurrentTiles(context, currentTileList)
                            if (success) {
                                Toast.makeText(context, addedToastTemplate.format(value), Toast.LENGTH_SHORT).show()
                                addCustomTileSheetState.dismiss()
                                customTileInput = ""
                                refreshDebugInfo()
                            } else {
                                Toast.makeText(context, addFailedToast, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, operationFailedToastTemplate.format(e.message ?: ""), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.debug_add))
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
