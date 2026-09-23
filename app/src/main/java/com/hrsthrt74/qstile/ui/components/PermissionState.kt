package com.hrsthrt74.qstile.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.shizuku.ShizukuHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * WRITE_SECURE_SETTINGS 权限状态控制器。
 *
 * 主页与 OOBE 引导页高度复用了同一套权限状态机（状态查询、Shizuku 授权、
 * pm grant、回到前台自动衔接），为避免两份几乎相同的代码，统一抽取到这里。
 *
 * 对外暴露：
 * - [status]  当前权限状态（PermissionStatus 状态机）
 * - [isLoading] 初始加载标志（主页权限卡片用；OOBE 不关心，忽略即可）
 * - [refresh] 重新查询权限状态
 * - [request] 请求 Shizuku 授权，授予后自动执行 pm grant
 * - [autoGrant] Shizuku 已授权时直接执行 pm grant
 * - [markAutoRequestAfterResume] 标记「从 Shizuku 返回后自动衔接请求授权」
 *
 * 生命周期与初始刷新（ON_RESUME 刷新 + 自动衔接）在 [rememberPermissionState]
 * 内部统一挂接，调用方无需再手动注册 DisposableEffect / LaunchedEffect。
 */
class PermissionState(
    private val context: Context,
    private val onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit,
    private val scope: CoroutineScope,
) {
    /** 当前权限状态，默认 GRANTED（避免未加载时闪烁错误引导） */
    var status by mutableStateOf(ShizukuHelper.PermissionStatus.GRANTED)
        private set

    /** 是否正在做初始加载（首次查询完成前为 true） */
    var isLoading by mutableStateOf(true)
        private set

    /** 是否在返回前台时自动请求 Shizuku 权限（用户点击「启动 Shizuku」后置位） */
    private var autoRequestAfterResume by mutableStateOf(false)

    /** 重新查询权限状态（轻量查询，主线程同步完成） */
    fun refresh() {
        status = ShizukuHelper.checkPermissionStatus(context)
    }

    /**
     * 请求 Shizuku 权限；授予后自动执行 pm grant 并刷新状态。
     * 供「请求授权」按钮和「从 Shizuku 返回自动衔接」复用。
     */
    fun request() {
        onRequestShizukuPermission { granted ->
            if (granted) {
                scope.launch {
                    grantWriteSecureSettingsCore()
                    refresh()
                }
            } else {
                // 非 Composable 上下文,用 context.getString 取资源文案
                Toast.makeText(context, context.getString(R.string.perm_shizuku_denied_toast), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Shizuku 已授权，直接执行 pm grant（不需要再走授权弹窗） */
    fun autoGrant() {
        scope.launch {
            grantWriteSecureSettingsCore()
            refresh()
        }
    }

    /** 标记：用户从 Shizuku 应用返回后自动衔接请求授权 */
    fun markAutoRequestAfterResume() {
        autoRequestAfterResume = true
    }

    /** 初始加载完成回调（由 rememberPermissionState 内部在首次刷新后调用） */
    internal fun markLoaded() {
        isLoading = false
    }

    /**
     * 读取并重置「返回后自动请求」标记。
     * 供 rememberPermissionState 的 ON_RESUME 观察器消费（读后即清，避免重复触发）。
     */
    internal fun consumeAutoRequestAfterResume(): Boolean {
        val flag = autoRequestAfterResume
        autoRequestAfterResume = false
        return flag
    }

    /**
     * 执行 pm grant 并弹出结果提示（request / autoGrant 共用）。
     * grantWriteSecureSettings 内部走 Shizuku 或 adb 授权通道。
     */
    private suspend fun grantWriteSecureSettingsCore() {
        val success = ShizukuHelper.grantWriteSecureSettings(context)
        val message = if (success) {
            context.getString(R.string.perm_ws_granted_toast)
        } else {
            context.getString(R.string.perm_grant_failed_toast)
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

/**
 * 创建并接管权限状态控制器。
 *
 * 内部统一完成两件事，调用方无需再手动处理：
 * 1. 首次进入时刷新一次权限状态并结束初始加载；
 * 2. 应用回到前台（ON_RESUME）时自动刷新，若用户刚从 Shizuku 返回且已运行
 *    但未授权（SHIZUKU_NOT_GRANTED），则自动衔接请求授权。
 *
 * @param onRequestShizukuPermission Shizuku 权限请求入口（由 MainActivity 下传，接收结果回调）
 */
@Composable
fun rememberPermissionState(
    onRequestShizukuPermission: ((Boolean) -> Unit) -> Unit,
): PermissionState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val state = remember(onRequestShizukuPermission) {
        PermissionState(context, onRequestShizukuPermission, scope)
    }

    // 首次进入：查询一次权限状态，结束后清除初始加载标志（防闪烁）
    LaunchedEffect(Unit) {
        state.refresh()
        state.markLoaded()
    }

    // 生命周期：回到前台自动刷新权限状态。
    // 用户从「启动 Shizuku」返回后，若 Shizuku 已运行但未授权，自动衔接请求授权。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.refresh()
                if (state.consumeAutoRequestAfterResume()) {
                    if (state.status == ShizukuHelper.PermissionStatus.SHIZUKU_NOT_GRANTED) {
                        state.request()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state
}
