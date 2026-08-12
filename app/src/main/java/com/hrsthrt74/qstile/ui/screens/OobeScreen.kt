package com.hrsthrt74.qstile.ui.screens

// android.graphics.drawable.Icon 与 miuix 的 Icon 组件重名，故用别名 AndroidIcon 区分
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.DeviceProfile
import com.hrsthrt74.qstile.data.TileCatalog
import com.hrsthrt74.qstile.shizuku.ShizukuHelper
import com.hrsthrt74.qstile.tile.ExTileService
import com.hrsthrt74.qstile.ui.components.AppDialog
import com.hrsthrt74.qstile.ui.components.PermissionStatusCard
import com.hrsthrt74.qstile.ui.components.rememberDialogState
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.function.Consumer
import android.graphics.drawable.Icon as AndroidIcon

/**
 * OOBE（首次使用引导）页面。
 *
 * 四步用 [HorizontalPager] 右到左滑动翻页（0→1→2→3）；系统返回键在非首页时
 * 回到上一页，首页退出应用（页面级 BackHandler，非弹窗场景 miuix 不提供）。
 * P1/P2/P3 左上角 IconButton 仍保留，点击同样动画翻页。
 *
 * - P0 欢迎页：跳过（二次确认）或「开始使用」
 * - P1 权限页：WRITE_SECURE_SETTINGS 授权引导（Shizuku 可选），可跳过（二次提醒）
 * - P2 首次配置页：引导在控制中心摆放 exTile 磁贴，然后从系统导入生成展开/收起配置
 * - P3 配置完成页：onCompleted() 回调完成整个 OOBE
 *
 * @param onRequestShizukuPermission Shizuku 权限请求入口（由 MainActivity 下传，接收结果回调）
 * @param onCompleted OOBE 完成回调（由壳层标记 oobe_completed=true）
 */
@Composable
fun OobeScreen(
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 触觉反馈：底部按钮按下时提供震动反馈，与主页交互手感一致
    val haptic = LocalHapticFeedback.current

    // 四步 Pager 状态：0=欢迎 1=权限 2=首次配置 3=完成
    val pagerState = rememberPagerState(initialPage = 0) { 4 }

    // 翻页动画：统一使用较缓的 tween，避免默认动画过快，让引导页切换更从容。
    // 各翻页入口（按钮 / 返回键 / 对话框确认）共用此 spec，保持手感一致。
    val pageChangeSpec = tween<Float>(
        durationMillis = 400,
        easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    )

    // 页面级返回：非弹窗场景，miuix 不提供，故手动注册。
    // 不在第 0 页时按系统返回键动画翻回上一页；第 0 页时不拦截（退出应用）。
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = pageChangeSpec) }
    }

    // P0 跳过引导确认对话框
    val skipDialogState = rememberDialogState()
    // P1 未授权时跳过确认对话框
    val skipNoPermissionDialogState = rememberDialogState()
    // P2 系统中未找到 exTile 磁贴提示对话框
    val noExtileDialogState = rememberDialogState()
    // P2 无收纳磁贴提示对话框（收纳数 <=0 时阻止进入下一步）
    val noHiddenTileDialogState = rememberDialogState()

    // P1 权限状态（Shizuku 完全可选，授权后不再依赖）
    var permissionStatus by remember { mutableStateOf(ShizukuHelper.PermissionStatus.GRANTED) }
    // 是否在返回前台时自动请求 Shizuku 权限（用户点击「启动 Shizuku」后置位）
    var autoRequestAfterResume by remember { mutableStateOf(false) }

    // P2 系统导入结果与加载状态（异步操作必须加 isLoading 防闪烁）
    var importResult by remember { mutableStateOf<ConfigRepository.SystemImportResult?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    // 是否已确认添加过 exTile 磁贴（系统弹窗回调返回 TILE_ADDED/ALREADY_ADDED 后置 true，用于置灰「添加磁贴」按钮）
    var tileAddRequested by remember { mutableStateOf(false) }

    // 设备能力快照（P2 磁贴摘要的显示名映射用）
    val profile = remember { DeviceProfile.from(context) }

    // 不支持设备的判断：类原生 AOSP 系统（非厂商定制 ROM）且 Android 15（SDK 35）及以上。
    // 该场景下本应用依赖的 WRITE_SECURE_SETTINGS 磁贴切换方案不可用，因此在欢迎页（P0）直接拦截：
    // 展示错误提示、禁用「开始使用」按钮、隐藏右上角「跳过」入口，阻止用户进入引导后续步骤。
    val isUnsupportedDevice = profile.isAosp && profile.sdkInt >= 35

    // 导航栏 inset：OOBE 页最外层有统一 TopAppBar（自带状态栏安全区），
    // 但各页底部按钮仍需手动处理底部导航栏安全区
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    /** 刷新权限状态（轻量查询，主线程同步完成） */
    fun refreshPermissionStatus() {
        permissionStatus = ShizukuHelper.checkPermissionStatus(context)
    }

    /**
     * 请求 Shizuku 权限；授予后自动执行 pm grant 并刷新状态。
     * 复用 MainActivity 下传的 [onRequestShizukuPermission]。
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
                    refreshPermissionStatus()
                }
            } else {
                Toast.makeText(context, "Shizuku 授权被拒绝", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 执行系统导入（P2 主按钮） */
    fun runImport() {
        scope.launch {
            isImporting = true
            importResult = ConfigRepository.importSystemTiles(context)
            isImporting = false
            when (importResult) {
                // 未找到 exTile 锚点：弹窗引导先添加磁贴
                is ConfigRepository.SystemImportResult.NoExtile -> noExtileDialogState.show()
                // 读取失败：Toast 提示
                is ConfigRepository.SystemImportResult.ReadFailed ->
                    Toast.makeText(context, "无法读取系统配置", Toast.LENGTH_SHORT).show()
                // 导入成功：若没有收纳磁贴（hideCount<=0）则配置无效，弹窗提醒并阻止进入「确认保存」
                is ConfigRepository.SystemImportResult.Success -> {
                    val result = importResult as ConfigRepository.SystemImportResult.Success
                    if (result.hideCount <= 0) noHiddenTileDialogState.show()
                }
                // importResult 尚未初始化（null）或其他分支：无需处理
                else -> Unit
            }
        }
    }

    /**
     * 请求系统把 exTile 磁贴加入当前 QS 磁贴集合（SDK 33+）。
     * 系统会弹出确认框，用户同意后磁贴即加入集合；回调仅作兜底，无需强提示。
     *
     * @param tileLabel 磁贴显示名（由调用方用 [stringResource] 获取，避免在普通函数里
     *   用 context.getString 触发「Querying resource values」lint 警告）
     */
    fun requestAddTile(tileLabel: String) {
        val statusBarManager = context.getSystemService(StatusBarManager::class.java) ?: return
        val componentName = ComponentName(context, ExTileService::class.java)
        // 图标加载：android.graphics.drawable.Icon（别名 AndroidIcon），
        // 与 miuix 的 Icon 组件重名，故用 import alias 区分
        val tileIcon = AndroidIcon.createWithResource(context, R.drawable.ic_tile)
        statusBarManager.requestAddTileService(
            componentName,
            tileLabel,
            tileIcon,
            context.mainExecutor,
            Consumer { result ->
                // result 为 StatusBarManager 的回调常量：
                // TILE_ADD_REQUEST_RESULT_TILE_ADDED(2) / TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED(1)
                // 表示磁贴已在集合中，此时把按钮置灰；其余（未添加 0 或各种错误码）保持可用
                if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
                ) {
                    tileAddRequested = true
                }
            }
        )
    }

    // 进入页面时先检查一次权限
    LaunchedEffect(Unit) {
        refreshPermissionStatus()
    }

    // 监听生命周期：应用回到前台时自动刷新权限状态。
    // 用户从「启动 Shizuku」返回后，若 Shizuku 已运行但未授权，自动衔接请求授权。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissionStatus()
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

    // ==================== 局部 UI 函数（闭包捕获，不显式传参） ====================

    /** P0 欢迎页：app 图标 + 特性列表，「跳过」图标由外层统一 TopAppBar 的 actions 提供 */
    @Composable
    fun WelcomeStep() {
            // 内容区：垂直水平居中
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // APP 图标
                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp)
                    ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_extile),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.size(88.dp)
                    )
                    Text(
                        text = "欢迎使用 exTile",
                        style = MiuixTheme.textStyles.title1,
                    )
                }
                // 特性列表卡片
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureCard(
                        icon = MiuixIcons.Demibold.Lock,
                        title = "本地工具，无需联网",
                        summary = "所有数据仅存本机"
                    )
                    FeatureCard(
                        icon = MiuixIcons.Demibold.Backup,
                        title = "简洁而又不失高效",
                        summary = "次级功能，点击即出"
                    )
                    // 不支持的设备（类原生 AOSP + Android 15 及以上）：追加醒目错误提示卡片，
                    // 告知用户本应用在该环境下无法使用（此时「开始使用」已禁用、「跳过」入口已隐藏）
                    if (isUnsupportedDevice) {
                        UnsupportedDeviceCard()
                    }
                }
            }
    }

    /** P1 权限页：授权引导（底部按钮由外层统一提供，已授权显示「下一步」，未授权显示「跳过」） */
    @Composable
    fun PermissionStep() {
        Column(modifier = Modifier.fillMaxSize()) {
            // 内容区
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 插图
                Card(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.illus_permission),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                // 标题
                Text(
                    text = "授权权限",
                    style = MiuixTheme.textStyles.title1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "exTile 需要 WRITE_SECURE_SETTINGS 权限\n来切换磁贴布局",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 醒目提醒
                Text(
                    text = "*未授权将无法使用磁贴切换等核心功能",
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // 权限引导卡片（通用组件）：OOBE 复用主页卡片，仅传 4 个回调；
                // 不传 isLoading（默认 false）与 onShowAdbGuide（默认 null，隐藏 adb 入口）
                PermissionStatusCard(
                    status = permissionStatus,
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
                    onRequestShizukuPermission = { requestShizukuPermission() },
                    onAutoGrant = {
                        // Shizuku 已授权，直接执行 pm grant
                        scope.launch {
                            val success = ShizukuHelper.grantWriteSecureSettings(context)
                            if (success) {
                                Toast.makeText(context, "WRITE_SECURE_SETTINGS 权限已授予", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "权限授予失败，请重试或使用 adb 手动授权", Toast.LENGTH_SHORT).show()
                            }
                            refreshPermissionStatus()
                        }
                    }
                )
            }
        }
    }

    /** P2 首次配置页：引导摆放磁贴 + 从系统导入并预览（底部按钮由外层统一提供） */
    @Composable
    fun SetupStep() {
        val successResult = importResult as? ConfigRepository.SystemImportResult.Success
        // 磁贴显示名在 Composable 作用域用 stringResource 获取，
        // 避免在 onClick（普通 lambda）里查资源触发 lint 警告
        val tileLabel = stringResource(R.string.tile_label)
        Column(modifier = Modifier.fillMaxSize()) {
            // 内容区
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 插图
                Card(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.illus_sort),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                // 标题
                Text(
                    text = "排列磁贴",
                    style = MiuixTheme.textStyles.title1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "打开控制中心，进入磁贴编辑界面，\n把 exTile 磁贴拖动到一行的末尾。",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 锚点规则说明（两条要点）
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp),
                        ) {
                        RuleRow(
                            icon = MiuixIcons.ChevronForward,
                            title = "exTile 之前的磁贴",
                            summary = "始终显示"
                        )
                        HorizontalDivider()
                        RuleRow(
                            icon = MiuixIcons.ChevronBackward,
                            title = "exTile 之后的磁贴",
                            summary = "展开显示，收起隐藏"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 导入进度：异步操作加载中
                AnimatedVisibility(visible = isImporting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfiniteProgressIndicator()
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "正在读取系统配置...",
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }

                // 导入成功且配置合法（有收纳磁贴）时展示预览卡片；
                // 无收纳磁贴时配置无效，不展示卡片（避免「我已配置完成」时出现卡片造成误导）
                val showPreview = successResult != null && !isImporting && successResult.hideCount > 0
                AnimatedVisibility(visible = showPreview) {
                    ImportPreviewCard(
                        result = successResult!!,
                        profile = profile
                    )
                }

                // 一键引导在系统里添加 exTile 磁贴（SDK 33+ 系统弹窗）
                // 已确认添加过（回调返回 TILE_ADDED/ALREADY_ADDED）则按钮置灰禁用
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        requestAddTile(tileLabel)
                    },
                    enabled = !tileAddRequested,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("添加 exTile 磁贴")
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    /** P3 配置完成页：对勾 + 文案（点击「开始使用」完成 OOBE） */
    @Composable
    fun DoneStep() {
        Column(modifier = Modifier.fillMaxSize()) {
            // 内容区：垂直水平居中
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = MiuixIcons.Demibold.Ok,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "配置完成！",
                    style = MiuixTheme.textStyles.title1,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "点击 exTile 磁贴，即可展开 / 收起磁贴~",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(128.dp))
            }
        }
    }

    // ==================== 最外层结构：统一 TopAppBar + 页面切换区 ====================
    Column(modifier = Modifier.fillMaxSize()) {
        // 统一 TopAppBar：仅承载交互元素，标题文字已移入各页面内容区
        // - P0 欢迎页：actions 显示「跳过」Close 图标（点击弹二次确认）
        // - P1 权限页：返回按钮（回 P0）
        // - P2 配置页：返回按钮（回 P1）
        // - P3 完成页：全空（避免遮挡大对勾与文案）
        SmallTopAppBar(
            title = "",
            navigationIcon = {
                if (pagerState.currentPage == 1) {
                    IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(0, animationSpec = pageChangeSpec) } }) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                } else if (pagerState.currentPage == 2) {
                    IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(1, animationSpec = pageChangeSpec) } }) {
                        Icon(MiuixIcons.Back, contentDescription = "返回")
                    }
                }
            },
            actions = {
                // P0 欢迎页：右上角「跳过」入口（点击弹二次确认）。
                // 不支持的设备（类原生 AOSP + Android 15 及以上）时直接隐藏该入口：
                // 跳过后同样无法使用本应用，无需给用户提供继续入口
                if (pagerState.currentPage == 0 && !isUnsupportedDevice) {
                    IconButton(onClick = { skipDialogState.show() }) {
                        Icon(MiuixIcons.Close, contentDescription = "跳过")
                    }
                }
            }
        )

        // 页面切换区：HorizontalPager 右到左翻页动画（0→1→2→3），
        // userScrollEnabled=false 禁止用户手势滑动，翻页只由按钮/返回键触发；
        // beyondViewportPageCount 预加载相邻页面
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            beyondViewportPageCount = 4,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> WelcomeStep()
                1 -> PermissionStep()
                2 -> SetupStep()
                3 -> DoneStep()
            }
        }

        // 底部按钮区：固定在底部，不随 HorizontalPager 页面滑动切换。
        // 各页按钮统一放在外层，按当前页 currentPage 动态渲染对应按钮。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = navBarBottom + 24.dp)
        ) {
            when (pagerState.currentPage) {
                // P0 欢迎页：开始使用。
                // 不支持的设备（类原生 AOSP + Android 15 及以上）时禁用按钮（保留展示，置灰不可点），
                // 阻止用户进入引导后续步骤
                0 -> Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { pagerState.animateScrollToPage(1, animationSpec = pageChangeSpec) }
                    },
                    enabled = !isUnsupportedDevice,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("开始使用") }

                // P1 权限页：已授权→下一步；未授权→跳过（需二次确认）
                1 -> if (permissionStatus == ShizukuHelper.PermissionStatus.GRANTED) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch { pagerState.animateScrollToPage(2, animationSpec = pageChangeSpec) }
                        },
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("下一步") }
                } else {
                    TextButton(
                        text = "跳过",
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = { skipNoPermissionDialogState.show() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // P2 配置页：导入成功且有收纳磁贴→确认保存；否则→我已配置完成（触发导入）。
                // 只有 hideCount > 0（配置有效）才显示「确认保存」，无收纳时停留在「我已配置完成」。
                2 -> {
                    val successResult = importResult as? ConfigRepository.SystemImportResult.Success
                    if (successResult != null
                        && !isImporting
                        && successResult.hideCount > 0
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch { pagerState.animateScrollToPage(3, animationSpec = pageChangeSpec) }
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("确认保存") }
                    } else {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                runImport()
                            },
                            enabled = !isImporting,
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("我已配置完成") }
                    }
                }

                // P3 完成页：开始使用（完成整个 OOBE）
                3 -> Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCompleted()
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("开始使用") }
            }
        }
    }

    // ==================== 对话框 ====================

    // P0 跳过引导确认
    AppDialog(
        state = skipDialogState,
        title = "确定跳过引导？",
        summary = "将使用默认配置直接进入。不授权将无法使用核心功能，可稍后在主页完成授权。",
        confirmText = "跳过",
        onConfirm = { onCompleted() }
    )

    // P1 未授权跳过确认（再次提醒后放行）
    AppDialog(
        state = skipNoPermissionDialogState,
        title = "确定跳过？",
        summary = "不授权将无法使用核心功能，可稍后在主页完成授权",
        confirmText = "跳过",
        onConfirm = { scope.launch { pagerState.animateScrollToPage(2, animationSpec = pageChangeSpec) } }
    )

    // P2 未找到 exTile 磁贴提示
    AppDialog(
        state = noExtileDialogState,
        title = "未找到 exTile 磁贴",
        summary = "未在系统配置中找到 exTile 磁贴，请先在控制中心添加它",
        cancelText = "关闭",
        confirmText = "知道了",
        onConfirm = {}
    )

    // P2 无收纳磁贴提示：收纳数 <=0 时配置无效，阻止进入下一步
    AppDialog(
        state = noHiddenTileDialogState,
        title = "没有需要收纳的磁贴",
        summary = "提示：请将需要收纳的磁贴放在「exTile」磁贴之后。",
        confirmText = "知道了",
        onConfirm = {}
    )
}

/**
 * 特性列表卡片（P0 欢迎页）。
 * 图标 + 标题 + 说明文案，主题色图标，卡片底色 surfaceContainer。
 */
@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    summary: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text( text = title )
                Text(
                    text = summary,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}

/**
 * 不支持设备提示卡片（P0 欢迎页）。
 *
 * 当设备为「类原生 AOSP 系统 + Android 15（SDK 35）及以上」时展示，版式与 [FeatureCard] 保持一致
 * （图标 + 标题 + 说明两行），但改用 errorContainer 背景 + error 色图标做醒目错误提醒。
 * 配合外层逻辑：此时「开始使用」按钮已禁用、「跳过」入口已隐藏，用户无法继续引导。
 */
@Composable
private fun UnsupportedDeviceCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = MiuixIcons.Demibold.Close,
                contentDescription = null,
                // 图标用 error 色强调错误语义
                tint = MiuixTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "本应用暂不支持类原生 Android 15 及以上，抱歉~",
                    // 文字用 onErrorContainer，保证在 errorContainer 底上清晰可读
                    color = MiuixTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "有误判？请联系作者",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * 锚点规则要点行（P2 首次配置页）。
 * 图标 + 加粗标题 + 说明文案。
 */
@Composable
private fun RuleRow(
    icon: ImageVector,
    title: String,
    summary: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.graphicsLayer(rotationZ = -90f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1
            )
            Text(
                text = summary,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}

/**
 * 系统导入结果预览卡片（P2 首次配置页）。
 * 两行布局：每行左侧为计数（标签 + 大数字），右侧为该组磁贴的显示名摘要。
 *
 * @param result 系统导入成功结果
 * @param profile 设备能力快照（磁贴显示名映射用）
 */
@Composable
private fun ImportPreviewCard(
    result: ConfigRepository.SystemImportResult.Success,
    profile: DeviceProfile
) {
    // 收纳磁贴 = 展开配置中去掉保留部分（去掉补回的 edit 后即 exTile 之后的磁贴）
    val hiddenTiles = result.expandedTiles.filter { it !in result.collapsedTiles }
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 第一行：保留组（左侧计数 + 右侧磁贴名）
            ImportPreviewRow(
                label = "保留",
                count = result.keepCount,
                tileNames = result.collapsedTiles.joinToString("、") { TileCatalog.getDisplayName(it, profile) }
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // 第二行：收纳组（左侧计数 + 右侧磁贴名）
            ImportPreviewRow(
                label = "收纳",
                count = result.hideCount,
                tileNames = if (hiddenTiles.isEmpty()) "无" else hiddenTiles.joinToString("、") { TileCatalog.getDisplayName(it, profile) }
            )
        }
    }
}

/**
 * 导入预览单行（保留 / 收纳各一行）。
 * 左侧为计数 Column（标签 + 主题色大数字），右侧为磁贴显示名摘要，顶部对齐。
 *
 * @param label 行标签（保留 / 收纳）
 * @param count 该组磁贴数量
 * @param tileNames 该组磁贴的显示名摘要文本
 */
@Composable
private fun ImportPreviewRow(
    label: String,
    count: Int,
    tileNames: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 计数列：标签在上、大数字在下，与右侧磁贴名区分主次
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MiuixTheme.textStyles.footnote2
            )
            Text(
                text = "$count",
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        // 磁贴名摘要：占据剩余宽度，可折行
        Text(
            text = tileNames,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.weight(1f)
        )
    }
}
