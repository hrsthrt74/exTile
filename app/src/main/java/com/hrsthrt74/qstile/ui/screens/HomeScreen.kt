package com.hrsthrt74.qstile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.StatsRepository
import com.hrsthrt74.qstile.data.TileStats
import com.hrsthrt74.qstile.ui.BlurredBar
import com.hrsthrt74.qstile.ui.components.AppDialog
import com.hrsthrt74.qstile.ui.components.PermissionStatusCard
import com.hrsthrt74.qstile.ui.components.rememberDialogState
import com.hrsthrt74.qstile.ui.components.rememberPermissionState
import com.hrsthrt74.qstile.ui.contentBottomPadding
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
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
    // 顶部栏滚动行为，标题会根据滚动折叠
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    // 顶部栏模糊：创建 backdrop 捕获滚动内容，模糊开关关闭或 RuntimeShader 不支持时退回纯色
    // 主题设置由 ExTileTheme 通过 LocalThemeSettings 同步提供（数据就绪后才组合到这里，不会闪烁）
    val backdrop = rememberBlurBackdrop(enabled = LocalThemeSettings.current.enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    // ---- 状态声明 ----
    /** adb 手动授权指引对话框的显示状态（统一由 AppDialog 管理） */
    val adbGuideDialogState = rememberDialogState()
    /** 使用统计（累计展开/收起次数），实时订阅 DataStore */
    val stats by StatsRepository.getStatsFlow(context)
        .collectAsState(initial = TileStats())
    /** WRITE_SECURE_SETTINGS 权限状态机（Shizuku 可选，授权后不再依赖），含初始加载与前台自动衔接 */
    val permState = rememberPermissionState(onRequestShizukuPermission)

    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive) {
                SmallTopAppBar(
                    title = "",
//                    largeTitle = "exTile",
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
                                Toast.makeText(context, "未找到 Shizuku 应用", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRequestShizukuPermission = { permState.request() },
                        onAutoGrant = { permState.autoGrant() },
                        onShowAdbGuide = { adbGuideDialogState.show() }
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
