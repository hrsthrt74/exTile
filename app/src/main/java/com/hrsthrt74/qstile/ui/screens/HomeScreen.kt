package com.hrsthrt74.qstile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hrsthrt74.qstile.data.StatsRepository
import com.hrsthrt74.qstile.data.TileStats
import com.hrsthrt74.qstile.shizuku.ShizukuHelper
import com.hrsthrt74.qstile.ui.BlurredBar
import com.hrsthrt74.qstile.ui.components.AppDialog
import com.hrsthrt74.qstile.ui.components.PermissionStatusCard
import com.hrsthrt74.qstile.ui.components.rememberDialogState
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.SearchDevice
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 主页屏幕。
 * 展示 Shizuku 权限状态卡片 + 使用统计卡片。
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

    // 顶部栏模糊：创建 backdrop 捕获滚动内容，模糊开关关闭或 RuntimeShader 不支持时退回纯色
    val themeSettings by com.hrsthrt74.qstile.data.ThemeRepository.getThemeSettingsFlow(context)
        .collectAsState(initial = com.hrsthrt74.qstile.data.ThemeSettings())
    val backdrop = rememberBlurBackdrop(enabled = themeSettings.enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    // ---- 状态声明 ----
    /** WRITE_SECURE_SETTINGS 权限状态机（Shizuku 可选，授权后不再依赖） */
    var permissionStatus by remember { mutableStateOf(ShizukuHelper.PermissionStatus.GRANTED) }
    /** adb 手动授权指引对话框的显示状态（统一由 AppDialog 管理） */
    val adbGuideDialogState = rememberDialogState()
    /** 是否在返回前台时自动请求 Shizuku 权限（用户点击「启动 Shizuku」后置位） */
    var autoRequestAfterResume by remember { mutableStateOf(false) }
    /** 使用统计（累计展开/收起次数），实时订阅 DataStore */
    val stats by StatsRepository.getStatsFlow(context)
        .collectAsState(initial = TileStats())
    /** 是否正在加载 */
    var isLoading by remember { mutableStateOf(true) }

    /**
     * 刷新所有状态。
     * 权限检查在主线程完成（轻量查询），其余状态读取走协程。
     */
    fun refreshStatus() {
        permissionStatus = ShizukuHelper.checkPermissionStatus(context)
        scope.launch {
            isLoading = false
        }
    }

    /**
     * 请求 Shizuku 权限；授予后自动执行 pm grant 并刷新状态。
     * 供按钮点击和「从 Shizuku 返回自动衔接」复用。
     */
    fun requestShizukuPermission() {
        onRequestShizukuPermission { granted ->
            if (granted) {
                scope.launch {
                    val success = ShizukuHelper.grantWriteSecureSettings(context)
                    if (success) {
                        Toast.makeText(context, "WRITE_SECURE_SETTINGS 权限已授予", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "权限授予失败，请重试或使用 adb 手动授权", Toast.LENGTH_SHORT).show()
                    }
                    refreshStatus()
                }
            } else {
                Toast.makeText(context, "Shizuku 授权被拒绝", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 首次进入屏幕时加载状态
    LaunchedEffect(Unit) {
        refreshStatus()
    }

    // 监听生命周期：应用回到前台时自动刷新权限状态
    // 用户从「启动 Shizuku」返回后，若 Shizuku 已运行但未授权，自动衔接请求授权
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStatus()
                // 刚引导用户启动 Shizuku 且返回后 Shizuku 已运行但未授权 → 自动请求
                if (autoRequestAfterResume) {
                    autoRequestAfterResume = false
                    if (permissionStatus == ShizukuHelper.PermissionStatus.SHIZUKU_NOT_GRANTED) {
                        requestShizukuPermission()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive) {
                TopAppBar(
                    title = "exTile",
                    largeTitle = "exTile",
                    color = barColor,
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { paddingValues ->
        // 滚动内容挂载 backdrop，供顶部栏模糊捕获
        Box(
            modifier = if (backdrop != null) Modifier.fillMaxSize().layerBackdrop(backdrop) else Modifier.fillMaxSize()
        ) {
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
                // 权限状态卡片：按状态机展示引导文案和对应操作按钮
                item {
                    PermissionStatusCard(
                        status = permissionStatus,
                        isLoading = isLoading,
                        onInstallShizuku = {
                            // 未安装 Shizuku：引导到 GitHub Releases 下载
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases"))
                            )
                        },
                        onLaunchShizuku = {
                            // 已安装未运行：引导打开 Shizuku 应用
                            val launchIntent = context.packageManager
                                .getLaunchIntentForPackage("moe.shizuku.privileged.api")
                            if (launchIntent != null) {
                                // 标记：用户从 Shizuku 返回后自动衔接请求授权
                                autoRequestAfterResume = true
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(context, "未找到 Shizuku 应用", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRequestShizukuPermission = {
                            // 请求 Shizuku 权限，授予后自动执行 pm grant
                            requestShizukuPermission()
                        },
                        onAutoGrant = {
                            // Shizuku 已授权，直接执行 pm grant
                            scope.launch {
                                val success = ShizukuHelper.grantWriteSecureSettings(context)
                                if (success) {
                                    Toast.makeText(context, "WRITE_SECURE_SETTINGS 权限已授予", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "权限授予失败，请重试或使用 adb 手动授权", Toast.LENGTH_SHORT).show()
                                }
                                refreshStatus()
                            }
                        },
                        onShowAdbGuide = { adbGuideDialogState.show() }
                    )
                }

                // 间距
                item { Spacer(modifier = Modifier.height(12.dp)) }

                // 使用统计卡片：展示累计展开/收起次数（由磁贴切换成功时记录）
                item {
                    StatsCard(
                        totalExpandCount = stats.totalExpandCount,
                        totalCollapseCount = stats.totalCollapseCount
                    )
                }
            }
        }
    }

    // ---- adb 手动授权指引对话框 ----
    // Shizuku 完全可选的兜底方案：用户不装 Shizuku 时，可通过 adb 手动授权后使用核心功能
    val adbGrantCommand = "adb shell pm grant com.hrsthrt74.qstile android.permission.WRITE_SECURE_SETTINGS"
    AppDialog(
        state = adbGuideDialogState,
        title = "adb 手动授权",
        summary = "不使用 Shizuku 时，可通过以下命令手动授予 WRITE_SECURE_SETTINGS 权限：",
        cancelText = "关闭",
        confirmText = "复制命令",
        onConfirm = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("exTile adb grant", adbGrantCommand))
            Toast.makeText(context, "命令已复制", Toast.LENGTH_SHORT).show()
        }
    ) {
        Text(
            text = adbGrantCommand,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        
    }
}

/**
 * 使用统计卡片组件。
 * 展示累计展开/收起次数（磁贴布局切换成功时由 StatsRepository 记录）。
 *
 * @param totalExpandCount 累计展开次数
 * @param totalCollapseCount 累计收起次数
 */
@Composable
private fun StatsCard(
    totalExpandCount: Long,
    totalCollapseCount: Long
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
                // 使用统计图标（记录/历史语义）
                Icon(
                    imageVector = MiuixIcons.Demibold.SearchDevice,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MiuixTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "使用统计",
                    style = MiuixTheme.textStyles.title3
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 两行键值对：累计展开/收起次数
            StatusRow("累计展开次数", totalExpandCount.toString())
            StatusRow("累计收起次数", totalCollapseCount.toString())
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
