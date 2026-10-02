package com.hrsthrt74.qstile.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.shizuku.ShizukuHelper
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 权限状态卡片组件（主页与 OOBE 共用）。
 *
 * 按 WRITE_SECURE_SETTINGS 权限状态机展示：状态图标 + 标题 + 引导文案 + 按状态显示的主操作按钮，
 * 是 [PermissionStatus] 各分支的通用引导 UI。设计原则：Shizuku 是可选权限，仅用于首次/失效时
 * 自动授权；未安装 Shizuku 时可使用 adb 手动授权兜底。
 *
 * 复用说明：
 * - 主页（HomeScreen）与 OOBE（OobeScreen）均使用完整能力：传入 [isLoading]（防闪烁）与
 *   [showAdbGuide]（adb 手动授权入口）；两个参数均有默认值，不传即隐藏加载态与 adb 入口。
 * - adb 手动授权指引对话框已内置于本组件（[AppDialog]），由 [showAdbGuide] 开启。
 *
 * @param status WRITE_SECURE_SETTINGS 权限状态机当前状态
 * @param modifier 作用于卡片根节点的 Modifier
 * @param isLoading 是否正在检查权限（加载中：图标置灰、显示「正在检查权限...」；按钮区域不渲染，
 *   卡片保持紧凑矮高度；加载完成后若仍未授权，按钮区域通过 AnimatedVisibility 平滑展开出现）
 * @param onInstallShizuku 状态为 [ShizukuHelper.PermissionStatus.SHIZUKU_NOT_INSTALLED] 时主按钮点击回调（引导下载安装）
 * @param onLaunchShizuku 状态为 [ShizukuHelper.PermissionStatus.SHIZUKU_NOT_RUNNING] 时主按钮点击回调（引导启动 Shizuku）
 * @param onRequestShizukuPermission 状态为 [ShizukuHelper.PermissionStatus.SHIZUKU_NOT_GRANTED] 时主按钮点击回调（请求 Shizuku 权限）
 * @param onAutoGrant 状态为 [ShizukuHelper.PermissionStatus.NEEDS_PM_GRANT] 或
 *   [ShizukuHelper.PermissionStatus.GRANT_FAILED] 时主按钮点击回调（直接执行 pm grant）
 * @param showAdbGuide 是否显示 adb 手动授权入口（「使用 adb 手动授权」按钮 + 指引对话框）；
 *   默认 false（隐藏）
 */
@Composable
fun PermissionStatusCard(
    status: ShizukuHelper.PermissionStatus,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onInstallShizuku: () -> Unit,
    onLaunchShizuku: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onAutoGrant: () -> Unit,
    showAdbGuide: Boolean = false,
) {
    val context = LocalContext.current
    // adb 手动授权指引对话框的显示状态（统一由 AppDialog 管理）
    val adbGuideDialogState = rememberDialogState()
    // Toast 文案在 Composable 上下文预解析,避免 onConfirm 回调内 context.getString 触发「非配置感知」lint 错误
    val commandCopiedToast = stringResource(R.string.perm_command_copied)

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 权限状态图标：已授权/加载中=对勾，其他状态=叉号。
                // 加载中对勾置灰表示「检查中」，避免未授权瞬间闪红色叉号。
                Icon(
                    imageVector = if (isLoading || status == ShizukuHelper.PermissionStatus.GRANTED) {
                        MiuixIcons.Demibold.Ok
                    } else {
                        MiuixIcons.Demibold.Close
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

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // 标题：随加载态 / 授权结果变化
                    Text(
                        text = when {
                            isLoading -> stringResource(R.string.perm_checking_title)
                            status == ShizukuHelper.PermissionStatus.GRANTED -> stringResource(R.string.perm_granted_title)
                            else -> stringResource(R.string.perm_denied_title)
                        },
                        style = MiuixTheme.textStyles.title4,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    // 引导文案：按状态机给出下一步提示，加载态固定为「请稍候」
                    Text(
                        text = when {
                            isLoading -> stringResource(R.string.perm_checking_hint)
                            status == ShizukuHelper.PermissionStatus.GRANTED ->
                                stringResource(R.string.perm_granted_desc)
                            status == ShizukuHelper.PermissionStatus.SHIZUKU_NOT_INSTALLED ->
                                stringResource(R.string.perm_shizuku_not_installed_desc)
                            status == ShizukuHelper.PermissionStatus.SHIZUKU_NOT_RUNNING ->
                                stringResource(R.string.perm_shizuku_not_running_desc)
                            status == ShizukuHelper.PermissionStatus.SHIZUKU_NOT_GRANTED ->
                                stringResource(R.string.perm_shizuku_not_granted_desc)
                            status == ShizukuHelper.PermissionStatus.NEEDS_PM_GRANT ->
                                stringResource(R.string.perm_needs_pm_grant_desc)
                            else -> stringResource(R.string.perm_grant_failed_desc)
                        },
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
            }

            // 操作按钮区域：未授权（status != GRANTED）且不在加载中时，用 AnimatedVisibility 平滑展开显示
            // 主操作按钮与 adb 兜底入口。
            //
            // 方案说明（已回退「加载中透明占位」方案）：
            // - 加载中（isLoading=true）：不渲染任何按钮占位，卡片保持紧凑矮高度，不再有「透明占位大下巴」；
            // - 加载完成且最终未授权（status != GRANTED）：visible 变 true，AnimatedVisibility 默认的
            //   expandVertically + fadeIn 动画让按钮区域高度与透明度渐变出现，消除原「瞬间跳变 / 坍塌」感；
            // - 加载完成且已授权（status == GRANTED）：visible 保持 false，按钮区域收起，卡片矮，无任何变化。
            AnimatedVisibility(
                visible = status != ShizukuHelper.PermissionStatus.GRANTED && !isLoading
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 主按钮回调：按状态机映射到调用方传入的对应动作
                    val mainAction: (() -> Unit)? = when (status) {
                        ShizukuHelper.PermissionStatus.SHIZUKU_NOT_INSTALLED -> onInstallShizuku
                        ShizukuHelper.PermissionStatus.SHIZUKU_NOT_RUNNING -> onLaunchShizuku
                        ShizukuHelper.PermissionStatus.SHIZUKU_NOT_GRANTED -> onRequestShizukuPermission
                        ShizukuHelper.PermissionStatus.NEEDS_PM_GRANT -> onAutoGrant
                        ShizukuHelper.PermissionStatus.GRANT_FAILED -> onAutoGrant
                        else -> null
                    }
                    // 主按钮文案：与回调一一对应
                    val mainText = when (status) {
                        ShizukuHelper.PermissionStatus.SHIZUKU_NOT_INSTALLED -> stringResource(R.string.perm_install_shizuku)
                        ShizukuHelper.PermissionStatus.SHIZUKU_NOT_RUNNING -> stringResource(R.string.perm_launch_shizuku)
                        ShizukuHelper.PermissionStatus.SHIZUKU_NOT_GRANTED -> stringResource(R.string.perm_grant_shizuku)
                        ShizukuHelper.PermissionStatus.NEEDS_PM_GRANT -> stringResource(R.string.perm_auto_grant)
                        ShizukuHelper.PermissionStatus.GRANT_FAILED -> stringResource(R.string.perm_retry_grant)
                        else -> null
                    }

                    // 未授权状态必然映射到主按钮（上方映射全覆盖），因此主按钮始终渲染，无需占位逻辑。
                    if (mainAction != null && mainText != null) {
                        Button(
                            onClick = mainAction,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(mainText)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 兜底方案：仅当调用方开启了 adb 手动授权入口（showAdbGuide=true）时展示。
                    // showAdbGuide 在加载前后固定不变，随外层 AnimatedVisibility 一起展开收起。
                    if (showAdbGuide) {
                        TextButton(
                            text = stringResource(R.string.perm_adb_button),
                            onClick = { adbGuideDialogState.show() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // ---- adb 手动授权指引对话框（内置于卡片组件）----
    // Shizuku 完全可选的兜底方案：用户不装 Shizuku 时，可通过 adb 手动授权后使用核心功能
    val adbGrantCommand = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    AppDialog(
        state = adbGuideDialogState,
        title = stringResource(R.string.perm_adb_title),
        summary = stringResource(R.string.perm_adb_summary),
        cancelText = stringResource(R.string.common_close),
        confirmText = stringResource(R.string.perm_copy_command),
        onConfirm = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("exTile adb grant", adbGrantCommand))
            Toast.makeText(context, commandCopiedToast, Toast.LENGTH_SHORT).show()
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
