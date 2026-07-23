package com.hrsthrt74.qstile.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.hrsthrt74.qstile.ICommandService
import kotlinx.coroutines.Dispatchers
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

    suspend fun getSysuiQsTiles(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            if (!isBound || commandService == null) {
                bindService()
                Thread.sleep(500)
            }

            val result = commandService?.executeCommand("settings get secure $SYSUI_QS_TILES")
            Log.d(TAG, "getSysuiQsTiles result: $result")

            if (result == "null" || result.isNullOrEmpty() || result.startsWith("ERROR")) null else result
        } catch (e: Exception) {
            Log.e(TAG, "getSysuiQsTiles failed", e)
            null
        }
    }

    suspend fun setSysuiQsTiles(context: Context, value: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isBound || commandService == null) {
                bindService()
                Thread.sleep(500)
            }

            val result = commandService?.executeCommand("settings put secure $SYSUI_QS_TILES $value")
            Log.d(TAG, "setSysuiQsTiles result: $result")
            // settings put 成功时返回空字符串，只有出错时才返回 ERROR
            result == null || !result.startsWith("ERROR")
        } catch (e: Exception) {
            Log.e(TAG, "setSysuiQsTiles failed", e)
            false
        }
    }

    suspend fun executeCommand(command: String): String? = withContext(Dispatchers.IO) {
        try {
            if (!isBound || commandService == null) {
                bindService()
                Thread.sleep(500)
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
}
