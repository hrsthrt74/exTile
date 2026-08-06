package com.hrsthrt74.qstile.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.hrsthrt74.qstile.ICommandService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

object SecureSettingsHelper {
    private const val TAG = "SecureSettingsHelper"
    private const val SYSUI_QS_TILES = "sysui_qs_tiles"

    private var commandService: ICommandService? = null
    var isBound = false
        private set

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            commandService = ICommandService.Stub.asInterface(service)
            isBound = true
            Log.d(TAG, "CommandService connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            commandService = null
            isBound = false
            Log.d(TAG, "CommandService disconnected")
        }
    }

    fun bindService() {
        try {
            val componentName = ComponentName(
                "com.hrsthrt74.qstile",
                CommandService::class.java.name
            )
            val userServiceArgs = Shizuku.UserServiceArgs(componentName)
                .daemon(false)
                .processNameSuffix("command")
                .debuggable(true)
                .version(1)

            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Exception) {
            Log.e(TAG, "bindService failed", e)
        }
    }

    fun unbindService() {
        try {
            val componentName = ComponentName(
                "com.hrsthrt74.qstile",
                CommandService::class.java.name
            )
            val userServiceArgs = Shizuku.UserServiceArgs(componentName)
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            commandService = null
            isBound = false
        } catch (e: Exception) {
            Log.e(TAG, "unbindService failed", e)
        }
    }

    /**
     * 确保 Shizuku UserService 已绑定。
     * 若未绑定则尝试绑定并等待连接成功。
     * @return 是否绑定成功
     */
    suspend fun ensureBound(): Boolean = withContext(Dispatchers.IO) {
        // 已绑定则直接返回成功
        if (isBound && commandService != null) {
            return@withContext true
        }
        // 尝试绑定并等待连接
        bindService()
        var waitCount = 0
        while (!isBound && waitCount < 10) {
            delay(100)
            waitCount++
        }
        isBound && commandService != null
    }

    /**
     * 获取 sysui_qs_tiles 的值。
     * 优先使用直接 API 读取（应用持有 WRITE_SECURE_SETTINGS 时可用，Shizuku 完全可选），
     * 失败或无值时兜底到 Shizuku UserService。
     * @return 磁贴字符串，读取失败则返回 null
     */
    suspend fun getSysuiQsTiles(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            // 1. 优先直接读取（不需要 Shizuku）
            try {
                val directResult = Settings.Secure.getString(context.contentResolver, SYSUI_QS_TILES)
                if (directResult != null) {
                    Log.d(TAG, "getSysuiQsTiles via API: $directResult")
                    return@withContext directResult
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Direct read failed, fallback to Shizuku")
            }

            // 2. 兜底：Shizuku 不可用时放弃
            if (!ShizukuHelper.isShizukuRunning()) {
                Log.w(TAG, "Shizuku is not running")
                return@withContext null
            }
            // 确保服务已绑定
            if (!ensureBound()) {
                Log.w(TAG, "Shizuku service not bound")
                return@withContext null
            }

            // 使用 Shizuku UserService 读取
            val result = commandService?.executeCommand("settings get secure $SYSUI_QS_TILES")
            Log.d(TAG, "getSysuiQsTiles via UserService: $result")
            if (result != null && !result.startsWith("ERROR") && result != "null") {
                return@withContext result
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "getSysuiQsTiles failed", e)
            null
        }
    }

    /**
     * 写入 sysui_qs_tiles。
     * 优先使用直接 API（应用持有 WRITE_SECURE_SETTINGS 时可用，Shizuku 完全可选），
     * 权限不足抛 SecurityException 时兜底到 Shizuku UserService。
     * @return 是否写入成功
     */
    suspend fun setSysuiQsTiles(context: Context, value: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. 优先直接写入（需要 WRITE_SECURE_SETTINGS；已授权则直接成功，无需 Shizuku）
            try {
                val success = Settings.Secure.putString(context.contentResolver, SYSUI_QS_TILES, value)
                Log.d(TAG, "setSysuiQsTiles via API: $success")
                if (success) {
                    return@withContext true
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Direct write failed, fallback to Shizuku")
            }

            // 2. 兜底：Shizuku 不可用时放弃
            if (!ShizukuHelper.isShizukuRunning()) {
                Log.w(TAG, "Shizuku is not running")
                return@withContext false
            }
            // 确保服务已绑定
            if (!ensureBound()) {
                Log.w(TAG, "Shizuku service not bound")
                return@withContext false
            }

            // 使用 Shizuku UserService 写入
            val result = commandService?.executeCommand("settings put secure $SYSUI_QS_TILES $value")
            Log.d(TAG, "setSysuiQsTiles via UserService: $result")
            result == null || !result.startsWith("ERROR")
        } catch (e: Exception) {
            Log.e(TAG, "setSysuiQsTiles failed", e)
            false
        }
    }

    suspend fun executeCommand(command: String): String? = withContext(Dispatchers.IO) {
        try {
            // 统一使用协程友好的绑定逻辑
            if (!ensureBound()) {
                Log.w(TAG, "Shizuku service not bound")
                return@withContext null
            }

            commandService?.executeCommand(command)
        } catch (e: Exception) {
            Log.e(TAG, "executeCommand failed", e)
            null
        }
    }

    suspend fun getCurrentTiles(context: Context): List<String> {
        val tilesString = getSysuiQsTiles(context) ?: return emptyList()
        return tilesString.split(",").filter { it.isNotBlank() }
    }

    suspend fun setCurrentTiles(context: Context, tiles: List<String>): Boolean {
        val tilesString = tiles.joinToString(",")
        return setSysuiQsTiles(context, tilesString)
    }

    /**
     * 写入任意 Secure Settings 项（如小米 HyperOS 的 wordless_mode 无字模式）。
     * 优先使用直接 API（应用持有 WRITE_SECURE_SETTINGS 时可用，Shizuku 完全可选），
     * 权限不足抛 SecurityException 时兜底到 Shizuku UserService。
     * @param key Secure Settings 项名
     * @param value 写入的值（字符串形式）
     * @return 是否写入成功
     */
    suspend fun putSecureSetting(context: Context, key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. 优先直接写入（需要 WRITE_SECURE_SETTINGS；已授权则直接成功，无需 Shizuku）
            try {
                val success = Settings.Secure.putString(context.contentResolver, key, value)
                Log.d(TAG, "putSecureSetting($key) via API: $success")
                if (success) {
                    return@withContext true
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Direct write failed for $key, fallback to Shizuku")
            }

            // 2. 兜底：Shizuku 不可用时放弃
            if (!ShizukuHelper.isShizukuRunning()) {
                Log.w(TAG, "Shizuku is not running")
                return@withContext false
            }
            // 确保服务已绑定
            if (!ensureBound()) {
                Log.w(TAG, "Shizuku service not bound")
                return@withContext false
            }

            // 使用 Shizuku UserService 写入
            val result = commandService?.executeCommand("settings put secure $key $value")
            Log.d(TAG, "putSecureSetting($key) via UserService: $result")
            result == null || !result.startsWith("ERROR")
        } catch (e: Exception) {
            Log.e(TAG, "putSecureSetting($key) failed", e)
            false
        }
    }
}
