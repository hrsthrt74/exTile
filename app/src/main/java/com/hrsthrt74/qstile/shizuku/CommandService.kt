package com.hrsthrt74.qstile.shizuku

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.hrsthrt74.qstile.ICommandService

/**
 * Shizuku UserService 实现。
 *
 * 重要：Shizuku 的 `bindUserService` 是由 Shizuku 服务进程以它自己的 uid 来
 * `bindService` 本应用中**已在 AndroidManifest.xml 注册、且 `exported="true"`**
 * 的标准 Android Service，并通过 `onBind()` 拿到 AIDL binder。
 * 因此这里必须继承 [Service] 并重写 [onBind]，否则绑定必然失败
 * （表现为 "cannot bind UserService" / `onServiceConnected` 永不回调）。
 */
class CommandService : Service() {

    companion object {
        private const val TAG = "CommandService"
    }

    /**
     * 实际执行 shell 命令的 AIDL binder 实现。
     * 每次 `executeCommand` 会新起一个 `sh -c` 进程执行并读取输出，
     * 供 Shizuku 特权环境下执行 `pm grant` / `settings put` 等命令。
     */
    private val binder = object : ICommandService.Stub() {
        override fun executeCommand(command: String?): String {
            return try {
                Log.d(TAG, "Executing command: $command")
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                val output = process.inputStream.bufferedReader().readText().trim()
                val error = process.errorStream.bufferedReader().readText().trim()
                val exitCode = process.waitFor()

                Log.d(TAG, "Command output: $output")
                if (error.isNotEmpty()) {
                    Log.e(TAG, "Command error: $error")
                }

                // pm grant 成功时 shell 无输出；失败时以 ERROR 开头便于上层判断
                if (exitCode == 0) output else "ERROR: $error"
            } catch (e: Exception) {
                Log.e(TAG, "executeCommand failed", e)
                "ERROR: ${e.message}"
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
