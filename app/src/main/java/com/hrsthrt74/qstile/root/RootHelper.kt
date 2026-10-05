package com.hrsthrt74.qstile.root

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Root (su) 特权命令执行通道，作为 Shizuku 的替代方案。
 *
 * 与 [com.hrsthrt74.qstile.shizuku.SecureSettingsHelper] 的命令执行约定保持一致：
 * - 命令成功时返回 stdout（可能为空串）；
 * - 失败时返回以 "ERROR" 开头的字符串（或 null 表示通道完全不可用）；
 * - 调用方以「输出非 null 且不以 "ERROR" 开头」判定成功。
 *
 * 设计要点：
 * - **会话级缓存**：Root 可用性检测结果在进程生命周期内缓存。首次探测 `su -c id` 时，
 *   Magisk / KernelSU 等管理器会弹出授权对话框，若不缓存，每次刷新权限状态都会反复弹框
 *   骚扰用户；拒绝授权同样缓存（false），本次会话内不再探测。
 * - **超时控制**：管理器授权对话框会阻塞 su 进程退出，统一 30 秒超时（覆盖用户慢慢掏手机
 *   点「允许」的场景），超时强制结束进程并视为不可用。
 * - **probe 语义**：[isRootAvailable] 的 probe=false 表示「只查缓存、不主动探测」——
 *   供兜底链（如读取 sysui_qs_tiles）使用，避免在后台或非预期时机突然触发管理器弹框。
 */
object RootHelper {
    private const val TAG = "RootHelper"

    /** 单条命令的执行超时（秒）。管理器授权弹框会阻塞进程退出，需要留足用户响应时间 */
    private const val COMMAND_TIMEOUT_SECONDS = 30L

    /** Root 可用性检测结果（会话级缓存）：null = 尚未检测过 */
    @Volatile
    private var cachedResult: Boolean? = null

    /**
     * 只读获取已缓存的 Root 可用性结果（不探测、不执行 su，可在主线程直接调用）。
     * @return true/false = 本会话内已探测过的结果；null = 尚未探测过
     *   （探测仅在用户点击权限卡片的「Root 授权」按钮时发生，详见 [isRootAvailable]）
     */
    fun cachedRootAvailability(): Boolean? = cachedResult

    /**
     * 检测当前设备是否具备可用的 Root 权限（su 能以 uid=0 执行命令）。
     *
     * 首次探测会真实执行 `su -c id`：装有 Magisk / KernelSU 的设备此时会弹出授权对话框，
     * 用户批准且输出含 uid=0 才判定可用；拒绝、超时、无 su 二进制均判定不可用并缓存。
     *
     * @param probe 是否允许主动探测（执行 su）。false 时只读缓存，未缓存视为不可用——
     *   供兜底链等非交互场景使用，避免意外触发管理器授权弹框
     * @return Root 是否可用
     */
    suspend fun isRootAvailable(probe: Boolean = true): Boolean {
        // 已检测过（无论结果）：直接返回缓存，不再执行 su（也就不会再弹授权框）
        cachedResult?.let { return it }
        // 只查不探：非交互场景下未检测过时视为不可用，静默放弃 Root 兜底
        if (!probe) return false

        val result = withContext(Dispatchers.IO) {
            runCatching {
                // `su -c id`：标准探测命令，root 环境输出 "uid=0(root) gid=0(root) ..."
                val output = executeCommandInternal("id")
                output != null && output.contains("uid=0")
            }.getOrDefault(false)
        }
        Log.d(TAG, "isRootAvailable probe result: $result")
        cachedResult = result
        return result
    }

    /**
     * 以 Root 执行一条 shell 命令。
     *
     * 实现方式：`ProcessBuilder("su", "-c", command)`——su 会先把当前 uid 提升为 0
     * 再交给系统 shell 执行。所有命令输出都很短（id / pm grant / settings get），
     * 先 waitFor 再读流的顺序不会撑满管道缓冲区，可安全支撑超时控制。
     *
     * @param command 要执行的 shell 命令（与 Shizuku 通道中的命令字符串相同）
     * @return 命令 stdout；失败时返回以 "ERROR" 开头的字符串；通道完全不可用时返回 null
     */
    suspend fun executeCommand(command: String): String? = withContext(Dispatchers.IO) {
        runCatching { executeCommandInternal(command) }
            .onFailure { Log.e(TAG, "executeCommand failed: $command", it) }
            .getOrNull()
    }

    /**
     * 以 Root 执行 `pm grant`，为本应用授予 WRITE_SECURE_SETTINGS。
     * 与 [com.hrsthrt74.qstile.shizuku.ShizukuHelper.grantWriteSecureSettings] 对齐：
     * 同样的命令、同样的成功判定，仅执行通道不同（su 而非 Shizuku shell）。
     *
     * @return true 表示授权成功
     */
    suspend fun grantWriteSecureSettings(context: Context): Boolean {
        val command = "pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
        val result = executeCommand(command)
        Log.d(TAG, "grantWriteSecureSettings result: $result")
        // pm grant 成功时 shell 无输出（空串也算成功），失败时以 "ERROR" 开头
        return result != null && !result.startsWith("ERROR")
    }

    /**
     * 实际执行 su 命令的内部实现（须在 IO 线程调用）。
     * @return 命令 stdout；命令失败返回 "ERROR: <stderr>"；超时返回 "ERROR: timeout"
     */
    private fun executeCommandInternal(command: String): String? {
        val process = ProcessBuilder("su", "-c", command).start()
        try {
            // 先等进程退出（带超时）：管理器授权弹框场景下进程会一直挂起，
            // 超时则强制结束并按失败处理；进程已退出后读流不会阻塞
            val exited = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!exited) {
                process.destroyForcibly()
                Log.w(TAG, "executeCommand timeout: $command")
                return "ERROR: timeout"
            }
            val output = process.inputStream.bufferedReader().readText().trim()
            val error = process.errorStream.bufferedReader().readText().trim()
            Log.d(TAG, "executeCommand($command): $output")
            if (error.isNotEmpty()) {
                Log.e(TAG, "Command error: $error")
            }
            // 与 CommandService 的返回约定一致：退出码非 0 时把 stderr 以 "ERROR" 前缀带回
            return if (process.exitValue() == 0) output else "ERROR: $error"
        } finally {
            process.destroy()
        }
    }
}
