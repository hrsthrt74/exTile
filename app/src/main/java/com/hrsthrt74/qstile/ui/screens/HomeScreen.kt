package com.hrsthrt74.qstile.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.StatsRepository
import com.hrsthrt74.qstile.data.TileStats
import com.hrsthrt74.qstile.ui.BlurredBar
import com.hrsthrt74.qstile.ui.components.PermissionStatusCard
import com.hrsthrt74.qstile.ui.components.rememberPermissionState
import com.hrsthrt74.qstile.ui.contentBottomPadding
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.microsoft.clarity.modifiers.clarityUnmask
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
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
    // Toast 文案在 Composable 上下文预解析,避免 onClick 内 context.getString 触发「非配置感知」lint 错误
    val noShizukuToast = stringResource(R.string.home_no_shizuku_toast)
    // 顶部栏滚动行为，标题会根据滚动折叠
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    // 顶部栏模糊：创建 backdrop 捕获滚动内容，模糊开关关闭或 RuntimeShader 不支持时退回纯色
    // 主题设置由 ExTileTheme 通过 LocalThemeSettings 同步提供（数据就绪后才组合到这里，不会闪烁）
    val backdrop = rememberBlurBackdrop(enabled = LocalThemeSettings.current.enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    // ---- 状态声明 ----
    /** 使用统计（累计展开/收起次数），实时订阅 DataStore */
    val stats by StatsRepository.getStatsFlow(context)
        .collectAsState(initial = TileStats())
    /** WRITE_SECURE_SETTINGS 权限状态机（Shizuku 可选，授权后不再依赖），含初始加载与前台自动衔接 */
    val permState = rememberPermissionState(onRequestShizukuPermission)

    Scaffold(
        topBar = {
            // 渐进模糊开启时使用渐变模糊（顶部最强、向下过渡到清晰），关闭时回退普通模糊
            BlurredBar(backdrop, blurActive, progressive = LocalThemeSettings.current.progressiveBlur) {
                SmallTopAppBar(
                    title = "",
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
                    text = stringResource(R.string.home_stats_title),
                    style = MiuixTheme.textStyles.title3
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 两行键值对：累计展开/收起次数
            StatusRow(stringResource(R.string.home_stats_expand_total), totalExpandCount.toString())
            StatusRow(stringResource(R.string.home_stats_collapse_total), totalCollapseCount.toString())
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
            .padding(vertical = 4.dp)
            // 统计数据 -> Clarity
            .clarityUnmask(),
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
