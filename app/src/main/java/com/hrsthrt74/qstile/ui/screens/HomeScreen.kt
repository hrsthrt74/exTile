package com.hrsthrt74.qstile.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.StatsRepository
import com.hrsthrt74.qstile.data.TileStats
import com.hrsthrt74.qstile.data.TipsRepository
import com.hrsthrt74.qstile.ui.AppTopBar
import com.hrsthrt74.qstile.ui.components.DonateSheet
import com.hrsthrt74.qstile.ui.components.PermissionStatusCard
import com.hrsthrt74.qstile.ui.components.rememberPermissionState
import com.hrsthrt74.qstile.ui.components.rememberSheetState
import com.hrsthrt74.qstile.ui.contentBottomPadding
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.screens.home.DonateCard
import com.hrsthrt74.qstile.ui.screens.home.FeedbackCard
import com.hrsthrt74.qstile.ui.screens.home.StatsCard
import com.hrsthrt74.qstile.ui.screens.home.TipsCard
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 主页屏幕（只负责页面骨架：顶栏 + 滚动列表 + 卡片编排）。
 *
 * 展示 Shizuku 权限状态卡片 + 使用统计卡片 + 「你知道吗？」卡片 + 「反馈」卡片，
 * 是用户首次打开应用时看到的第一屏，引导完成 Shizuku 授权流程。
 *
 * 各卡片已拆分为独立文件，便于单独维护：
 * - 权限状态卡片：[PermissionStatusCard]（OOBE 也复用，故位于 ui/components）
 * - 使用统计卡片：[com.hrsthrt74.qstile.ui.screens.home.StatsCard]
 * - 「反馈」卡片：[com.hrsthrt74.qstile.ui.screens.home.FeedbackCard]
 * - 「你知道吗？」卡片：[com.hrsthrt74.qstile.ui.screens.home.TipsCard]
 *
 * @param onRequestShizukuPermission Shizuku 权限请求入口，接收结果回调
 */
@Composable
fun HomeScreen(
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    // Toast 文案在 Composable 上下文预解析,避免 onClick 内 context.getString 触发「非配置感知」lint 错误
    val noShizukuToast = stringResource(R.string.home_no_shizuku_toast)
    // 顶部栏滚动行为，标题会根据滚动折叠
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    // 顶部栏模糊：创建 backdrop 捕获滚动内容，模糊开关关闭或 RuntimeShader 不支持时退回纯色
    // 主题设置由 ExTileTheme 通过 LocalThemeSettings 同步提供（数据就绪后才组合到这里，不会闪烁）
    val themeSettings = LocalThemeSettings.current
    val backdrop = rememberBlurBackdrop(enabled = themeSettings.enableBlur)

    // ---- 状态声明 ----
    /** 使用统计（累计使用次数 + 最近 7 天分桶），实时订阅 DataStore */
    val stats by StatsRepository.getStatsFlow(context)
        .collectAsState(initial = TileStats())
    /** WRITE_SECURE_SETTINGS 权限状态机（Shizuku 可选，授权后不再依赖），含初始加载与前台自动衔接 */
    val permState = rememberPermissionState(onRequestShizukuPermission)
    /** 打赏 Sheet 的显示状态（微信 / 支付宝收款码） */
    val donateSheetState = rememberSheetState()

    /**
     * 「你知道吗？」卡片的全部文案。
     *
     * 以当前语言为 key：`res/raw` 与 `res/raw-zh` 下同名文件由资源系统按语言挑选，
     * 切换应用语言后必须重新读一次，否则会继续显示旧语言的文案。
     * 文案文件只有几 KB，这里同步读取（而不是异步）：避免卡片晚一帧才插入、下方卡片位置闪一下。
     */
    val tips = remember(LocalConfiguration.current.locales[0]) {
        TipsRepository.load(context)
    }

    Scaffold(
        topBar = {
            // 统一自适应顶栏：渐进模糊开→官方 GlassTopAppBar，关→原版顶栏（普通模糊）
            // 主页无标题（Logo 在内容区展示），宽窄屏均为无大标题形态
            AppTopBar(
                title = "",
                largeTitle = "",
                useLargeTitle = false,
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                progressive = themeSettings.progressiveBlur,
            )
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
                    // 水平边距 = Scaffold 已算好的挖孔/导航条 insets + 固定间距：
                    // 竖屏挖孔在顶部、水平 insets 为 0（保持原 16dp 间距）；
                    // 横屏/反向横屏挖孔在左右任一侧时自动避让（避免内容顶进挖孔区）；
                    // 宽屏起始侧已被 App.kt 消费，这里自动为 0，不会与 NavigationRail 重复避让
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current) + 16.dp,
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current) + 16.dp,
                    bottom = contentBottomPadding()
                )
            ) {

                item {
                    Image(
                        imageVector = ImageVector.vectorResource(R.drawable.logo),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MiuixTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .offset(y = (-8).dp)
                            .padding(start = 8.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                // 权限状态卡片：按状态机展示引导文案和对应操作按钮
                item {
                    PermissionStatusCard(
                        status = permState.status,
                        isLoading = permState.isLoading,
                        // 主页卡片保留按压 Sink 反馈（其它页面复用本组件时不开启）
                        pressFeedback = true,
                        onRootGrant = { permState.rootGrant() },
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
                                permState.markAutoRequestAfterResume()
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(context, noShizukuToast, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRequestShizukuPermission = { permState.request() },
                        onAutoGrant = { permState.autoGrant() },
                        // 开启 adb 手动授权入口（指引对话框内置于 PermissionStatusCard）
                        showAdbGuide = true
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                // 使用统计卡片：展示累计使用次数 + 最近 7 天趋势（由磁贴切换成功时记录）
                item {
                    StatsCard(stats = stats)
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                // 「你知道吗？」卡片：展示一条使用小知识，点击卡片切换到下一条（循环）。
                // 文案文件为空（例如某个语言还没写文案）时只跳过卡片本身，上下间距仍保留给统计卡与反馈卡。
                if (tips.isNotEmpty()) {
                    item {
                        TipsCard(tips = tips)
                    }

                    // 卡片与下方「反馈」卡片之间的间距：跟着卡片一起出现 / 消失
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                // 「反馈」卡片：简中环境点击跳 QQ 加群（复制群号），其他语言点击打开 GitHub Issues
                item {
                    FeedbackCard()
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                // 打赏卡片：点击弹出支持作者 Sheet（微信 / 支付宝收款码，与设置页共用同一实现）
                item {
                    DonateCard(onClick = { donateSheetState.show() })
                }
            }
        }
    }

    // 打赏 Sheet：放在 Scaffold 之外，内容与设置页「支持作者」完全一致
    DonateSheet(state = donateSheetState)
}
