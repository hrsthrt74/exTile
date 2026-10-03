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
import rikka.shizuku.ShizukuRemoteProcess

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
                // 不设置 debuggable（默认 false）：若置 true，Shizuku 会要求 app 为 debuggable 构建，release 包会被拒绝绑定
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
        // 前置检查（快速失败）：Shizuku 未运行、或本应用未被 Shizuku 授权时，
        // bindUserService 必然无法连接（未授权时 onServiceConnected 永远不会回调），
        // 若直接进入下面的等待循环会白白空转 10 秒（100×100ms），必须提前放弃。
        if (!ShizukuHelper.isShizukuRunning()) {
            Log.w(TAG, "ensureBound aborted: Shizuku is not running")
            return@withContext false
        }
        if (!ShizukuHelper.checkPermission()) {
            Log.w(TAG, "ensureBound aborted: Shizuku permission not granted")
            return@withContext false
        }
        // 已绑定则直接返回成功
        if (isBound && commandService != null) {
            return@withContext true
        }
        // 尝试绑定并等待连接（最多 10 秒，覆盖慢设备首次冷启动绑定的情况）
        bindService()
        var waitCount = 0
        while (!isBound && waitCount < 100) {
            delay(100)
            waitCount++
        }
        if (!isBound) {
            // 超时/失败时打印诊断，便于区分「绑定被拒」与「绑定过慢」
            Log.e(
                TAG,
                "ensureBound failed. isBound=$isBound " +
                    "shizukuRunning=${ShizukuHelper.isShizukuRunning()} " +
                    "ping=${try { Shizuku.pingBinder() } catch (e: Exception) { "ERR:${e.message}" }} " +
                    "version=${try { Shizuku.getVersion() } catch (e: Exception) { "ERR:${e.message}" }} " +
                    "selfPerm=${try { Shizuku.checkSelfPermission() } catch (e: Exception) { "ERR:${e.message}" }}"
            )
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
                Log.w(TAG,
                    "Direct read failed: " +
                            "writeSecure=${ShizukuHelper.hasWriteSecureSettingsPermission(context)} " +
                            "shizukuPerm=${runCatching { Shizuku.checkSelfPermission() }.getOrNull()} " +
                            "uid=${runCatching { Shizuku.getUid() }.getOrNull()}",
                    e
                )
            }

            // 2. 兜底：通过 Shizuku 执行 settings 命令读取。
            //
            // Android 14 起，系统会根据 targetSdkVersion 限制直接读取部分
            // Secure Settings。sysui_qs_tiles 对 targetSdkVersion > 33 的应用
            // 会直接抛出 SecurityException。
            //
            // 这里必须直接调用 executeCommand()，不能预先调用 ensureBound()：
            // executeCommand() 会优先使用 Shizuku.newProcess，不依赖容易在部分
            // 设备上启动失败的 UserService 独立进程。
            if (!ShizukuHelper.isShizukuRunning()) {
                Log.w(TAG, "Shizuku is not running")
                return@withContext null
            }

            // 使用 Shizuku 特权执行读取，规避 targetSdkVersion=34+ 的系统限制。
            val result = executeCommand("settings get secure $SYSUI_QS_TILES")
            Log.d(TAG, "getSysuiQsTiles via Shizuku command: $result")
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

            // 使用 Shizuku 特权执行写入
            val result = executeCommand("settings put secure $SYSUI_QS_TILES $value")
            Log.d(TAG, "setSysuiQsTiles via UserService: $result")
            result == null || !result.startsWith("ERROR")
        } catch (e: Exception) {
            Log.e(TAG, "setSysuiQsTiles failed", e)
            false
        }
    }

    suspend fun executeCommand(command: String): String? = withContext(Dispatchers.IO) {
        // 1. 优先：反射调用 Shizuku.newProcess 直接经 Shizuku 特权执行命令。
        //    Shizuku API 13 起 newProcess 被私有化（计划 v14 移除），但仍是唯一
        //    不依赖 UserService 独立进程的执行方式——可绕开小米 HyperOS (Android 14)
        //    等 ROM 上 UserService 独立进程启动时 Application 创建崩溃的兼容性问题。
        runCatching {
            executeViaNewProcess(command)
        }.getOrNull()?.let { return@withContext it }

        // 2. 兜底：Shizuku UserService 绑定后执行（在能正常启动独立进程的 ROM 上可用）
        try {
            if (!ensureBound()) {
                Log.e(TAG, "Shizuku service not bound, command='$command'")
                return@withContext null
            }
            commandService?.executeCommand(command)
        } catch (e: Exception) {
            Log.e(TAG, "executeCommand failed", e)
            null
        }
    }

    /**
     * 通过反射调用 Shizuku.newProcess 在 Shizuku 特权环境下执行命令。
     * 不依赖 UserService 独立进程，规避小米 ROM 上独立进程 Application 创建崩溃。
     * @param command 要执行的 shell 命令
     * @return 命令输出；执行失败时以 "ERROR" 开头
     * @throws Exception 反射或执行失败时抛出（由调用方决定是否回退 UserService）
     */
    private fun executeViaNewProcess(command: String): String {
        @Suppress("DEPRECATION", "JAVA_9_REFLECTION")
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as ShizukuRemoteProcess
        val output = process.inputStream.bufferedReader().readText().trim()
        val error = process.errorStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()

        Log.d(TAG, "executeCommand via newProcess($command): $output")
        if (error.isNotEmpty()) {
            Log.e(TAG, "Command error: $error")
        }
        return if (exitCode == 0) output else "ERROR: $error"
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

            // 使用 Shizuku 特权执行写入
            val result = executeCommand("settings put secure $key $value")
            Log.d(TAG, "putSecureSetting($key) via UserService: $result")
            result == null || !result.startsWith("ERROR")
        } catch (e: Exception) {
            Log.e(TAG, "putSecureSetting($key) failed", e)
            false
        }
    }

    /**
     * 引导用户去系统的「磁贴编辑页」排布磁贴。
     *
     * 命名保留「打开编辑页」的业务语义，但**目前实际只有回退分支**生效，原因如下
     * （结论来自对 com.android.systemui 16.03（Android 16）与 miui.systemui.plugin
     * 17/18 的逆向，对照设备上 services.jar 的 StatusBarShellCommand）：
     *
     * 1. 系统磁贴编辑页是控制中心**内部的 View**，不是 Activity：
     *    - SystemUI：`com.android.systemui.qs.customize.MiuiQSCustomizer`、
     *      `com.android.systemui.qs.panels.ui.compose.EditModeKt`
     *    - 插件：`miui.systemui.controlcenter.panel.main.qs.EditButtonController`、
     *      `QSListController`
     *    两个 APK 的 Manifest 里都没有对应的 Activity，也就无法 `am start`。
     * 2. 没有任何能触发编辑态的广播：插件里 QS 控制器注册的广播接收器只监听
     *    `PACKAGE_ADDED` / `PACKAGE_REMOVED`（用于刷新磁贴列表）。
     * 3. `cmd statusbar` 的子命令全集为 help / expand-notifications / expand-settings /
     *    collapse / add-tile / remove-tile / set-tiles / click-tile / check-support /
     *    get-status-icons / disable-for-setup / send-disable-flag / tracing / run-gc / dump，
     *    **没有 edit 相关命令**；未收录的命令会透传给 SystemUI 的
     *    `statusbar.commandline.CommandRegistry`，而它注册的 Command 只有
     *    BottomMargin / StatusBarInsets / CompositionTracing / Disable / Enable /
     *    LogcatEchoTracker / ShadePrimaryDisplay / Prefs，同样没有编辑入口
     *    （MIUI 也未在插件里注册任何 Command）。
     *
     * ⇒ 结论：至多 Shizuku（shell）权限下无法直接进入编辑态，只能退回到
     * `cmd statusbar expand-settings` 展开控制中心，由用户自己点「编辑」。
     * 若将来某版 ROM 提供可用入口，在本函数里加一层「优先尝试」分支即可。
     *
     * 仅依赖 Shizuku：展开面板需要 shell 身份，Shizuku 未运行/未授权时静默失败。
     * @return 是否成功展开控制面板
     */
    suspend fun openQsTileEditor(): Boolean = withContext(Dispatchers.IO) {
        // 前置检查：`cmd` 需要 shell 身份；Shizuku 不可用时直接放弃，不打扰用户
        if (!ShizukuHelper.isShizukuRunning() || !ShizukuHelper.checkPermission()) {
            Log.w(TAG, "openQsTileEditor skipped: Shizuku unavailable")
            return@withContext false
        }
        // 回退分支：展开快捷设置面板（控制中心）
        val result = executeCommand("cmd statusbar expand-settings")
        Log.d(TAG, "openQsTileEditor expand-settings: $result")
        result != null && !result.startsWith("ERROR")
    }
}
