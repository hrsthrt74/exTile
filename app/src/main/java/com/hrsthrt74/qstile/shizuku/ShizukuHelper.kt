package com.hrsthrt74.qstile.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun checkPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.e(TAG, "checkPermission failed", e)
            false
        }
    }

    fun shouldShowRequestPermissionRationale(): Boolean {
        return try {
            Shizuku.shouldShowRequestPermissionRationale()
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission(requestCode: Int) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Log.e(TAG, "requestPermission failed", e)
        }
    }

    suspend fun grantWriteSecureSettings(context: Context): Boolean {
        return try {
            if (!SecureSettingsHelper.isBound) {
                SecureSettingsHelper.bindService()
                Thread.sleep(500)
            }

            val command = "pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
            val result = SecureSettingsHelper.executeCommand(command)
            Log.d(TAG, "grantWriteSecureSettings result: $result")

            !result.isNullOrEmpty() && !result.startsWith("ERROR")
        } catch (e: Exception) {
            Log.e(TAG, "grantWriteSecureSettings failed", e)
            false
        }
    }

    fun hasWriteSecureSettingsPermission(context: Context): Boolean {
        return context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED
    }
}
