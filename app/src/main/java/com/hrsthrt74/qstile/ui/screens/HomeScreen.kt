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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import com.hrsthrt74.qstile.shizuku.ShizukuHelper
import com.hrsthrt74.qstile.ui.BlurredBar
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

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

    // 顶部栏模糊：创建 backdrop 捕获滚动内容，模糊开关关闭或 RuntimeShader 不支持时退回纯色
    val themeSettings by com.hrsthrt74.qstile.data.ThemeRepository.getThemeSettingsFlow(context)
        .collectAsState(initial = com.hrsthrt74.qstile.data.ThemeSettings())
    val backdrop = rememberBlurBackdrop(enabled = themeSettings.enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    // ---- 状态声明 ----
    /** WRITE_SECURE_SETTINGS 权限状态机（Shizuku 可选，授权后不再依赖） */
    var permissionStatus by remember { mutableStateOf(ShizukuHelper.PermissionStatus.GRANTED) }
    /** 是否显示 adb 手动授权指引 */
    var showAdbGuide by remember { mutableStateOf(false) }
    /** 是否在返回前台时自动请求 Shizuku 权限（用户点击「启动 Shizuku」后置位） */
    var autoRequestAfterResume by remember { mutableStateOf(false) }
    /** 当前磁贴配置（展开/收起列表 + 开关状态） */
    var config by remember { mutableStateOf(TileConfig()) }
    /** 系统当前磁贴数量 */
    var currentTilesCount by remember { mutableStateOf(0) }
    /** 是否正在加载 */
    var isLoading by remember { mutableStateOf(true) }

    /**
     * 刷新所有状态。
     * 权限检查在主线程完成（轻量查询），配置读取走协程（涉及 DataStore 和 shell 命令）。
     */
    fun refreshStatus() {
        permissionStatus = ShizukuHelper.checkPermissionStatus(context)
        scope.launch {
            config = ConfigRepository.getConfig(context)
            currentTilesCount = SecureSettingsHelper.getCurrentTiles(context).size
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
                        onShowAdbGuide = { showAdbGuide = true }
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

    // ---- adb 手动授权指引对话框 ----
    // Shizuku 完全可选的兜底方案：用户不装 Shizuku 时，可通过 adb 手动授权后使用核心功能
    val adbGrantCommand = "adb shell pm grant com.hrsthrt74.qstile android.permission.WRITE_SECURE_SETTINGS"
    WindowDialog(
        show = showAdbGuide,
        title = "adb 手动授权",
        summary = "不使用 Shizuku 时，可通过以下命令手动授予 WRITE_SECURE_SETTINGS 权限：",
        onDismissRequest = { showAdbGuide = false }
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Text(
                text = adbGrantCommand,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    text = "关闭",
                    onClick = { showAdbGuide = false },
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    text = "复制命令",
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("exTile adb grant", adbGrantCommand))
                        Toast.makeText(context, "命令已复制", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 权限状态卡片组件。
 * 按 WRITE_SECURE_SETTINGS 权限状态机展示引导文案和对应操作按钮。
 * 设计原则：Shizuku 是可选权限，仅用于首次/失效时自动授权；
 * 未安装 Shizuku 时提供 adb 手动授权兜底。
 *
 * @param status 权限状态
 * @param isLoading 是否正在加载
 * @param onInstallShizuku Shizuku 未安装时点击
 * @param onLaunchShizuku Shizuku 未运行时点击
 * @param onRequestShizukuPermission 请求 Shizuku 权限
 * @param onAutoGrant 直接执行 pm grant
 * @param onShowAdbGuide 展示 adb 手动授权指引
 */
@Composable
private fun PermissionStatusCard(
    status: ShizukuHelper.PermissionStatus,
    isLoading: Boolean,
    onInstallShizuku: () -> Unit,
    onLaunchShizuku: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onAutoGrant: () -> Unit,
    onShowAdbGuide: () -> Unit
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
                // 权限状态图标：已授权=绿色对勾，加载中=灰色圆圈，其他状态=红色叉号
                Icon(
                    imageVector = if (isLoading || status == ShizukuHelper.PermissionStatus.GRANTED) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Close
                    },
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = when {
                        isLoading -> MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.5f)
                        status == ShizukuHelper.PermissionStatus.GRANTED -> MiuixTheme.colorScheme.primary
                        else -> MiuixTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = when {
                            isLoading -> "正在检查权限..."
                            status == ShizukuHelper.PermissionStatus.GRANTED -> "已获得权限"
                            else -> "未获得必须权限"
                        },
                        style = MiuixTheme.textStyles.title2,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    // 根据状态机展示不同的引导文案
                    Text(
                        text = when {
                            isLoading -> "请稍候"
                            status == ShizukuHelper.PermissionStatus.GRANTED -> "WRITE_SECURE_SETTINGS 已授权，磁贴切换可用"
                            status == ShizukuHelper.PermissionStatus.SHIZUKU_NOT_INSTALLED ->
                                "未安装 Shizuku，可安装后自动授权，或使用 adb 手动授权"
                            status == ShizukuHelper.PermissionStatus.SHIZUKU_NOT_RUNNING ->
                                "Shizuku 未运行，请先启动它"
                            status == ShizukuHelper.PermissionStatus.SHIZUKU_NOT_GRANTED ->
                                "需要授予 Shizuku 权限以自动授权"
                            status == ShizukuHelper.PermissionStatus.NEEDS_PM_GRANT ->
                                "Shizuku 已就绪，可一键自动授权"
                            else -> "自动授权失败，请重试或使用 adb 手动授权"
                        },
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }

            // 未授权且非加载中时，按状态显示对应操作按钮
            if (status != ShizukuHelper.PermissionStatus.GRANTED && !isLoading) {
                Spacer(modifier = Modifier.height(16.dp))

                val mainAction: (() -> Unit)? = when (status) {
                    ShizukuHelper.PermissionStatus.SHIZUKU_NOT_INSTALLED -> onInstallShizuku
                    ShizukuHelper.PermissionStatus.SHIZUKU_NOT_RUNNING -> onLaunchShizuku
                    ShizukuHelper.PermissionStatus.SHIZUKU_NOT_GRANTED -> onRequestShizukuPermission
                    ShizukuHelper.PermissionStatus.NEEDS_PM_GRANT -> onAutoGrant
                    ShizukuHelper.PermissionStatus.GRANT_FAILED -> onAutoGrant
                    else -> null
                }
                val mainText = when (status) {
                    ShizukuHelper.PermissionStatus.SHIZUKU_NOT_INSTALLED -> "安装 Shizuku"
                    ShizukuHelper.PermissionStatus.SHIZUKU_NOT_RUNNING -> "启动 Shizuku"
                    ShizukuHelper.PermissionStatus.SHIZUKU_NOT_GRANTED -> "授予 Shizuku 权限"
                    ShizukuHelper.PermissionStatus.NEEDS_PM_GRANT -> "自动授权"
                    ShizukuHelper.PermissionStatus.GRANT_FAILED -> "重试授权"
                    else -> null
                }

                if (mainAction != null && mainText != null) {
                    Button(
                        onClick = mainAction,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(mainText)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 兜底方案：始终提供 adb 手动授权入口
                TextButton(
                    text = "使用 adb 手动授权",
                    onClick = onShowAdbGuide,
                    modifier = Modifier.fillMaxWidth()
                )
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
