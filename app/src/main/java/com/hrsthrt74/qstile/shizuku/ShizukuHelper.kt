package com.hrsthrt74.qstile.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    /**
     * WRITE_SECURE_SETTINGS 权限状态机。
     * 设计原则：Shizuku 是可选权限，仅用于首次/失效时自动授权；
     * 一旦应用持有 WRITE_SECURE_SETTINGS（pm grant 或 adb 授权），Shizuku 即不再必需。
     * Root (su) 是备用的授权通道，以权限卡片上的独立按钮提供，不进入本状态机。
     */
    enum class PermissionStatus {
        /** 已持有 WRITE_SECURE_SETTINGS，可直接使用，Shizuku 完全可选 */
        GRANTED,
        /** Shizuku 未安装 */
        SHIZUKU_NOT_INSTALLED,
        /** Shizuku 已安装但未运行 */
        SHIZUKU_NOT_RUNNING,
        /** Shizuku 已运行但未授予本应用权限 */
        SHIZUKU_NOT_GRANTED,
        /** Shizuku 权限已授予，可自动执行 pm grant */
        NEEDS_PM_GRANT,
        /** pm grant 执行失败 */
        GRANT_FAILED
    }

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

    /**
     * 检查 WRITE_SECURE_SETTINGS 权限状态。
     * 应用已持有权限时直接返回 [PermissionStatus.GRANTED]（Shizuku 完全可选）；
     * 否则根据 Shizuku 的安装/运行/授权情况返回对应的引导状态。
     * @param context Context
     * @return 当前权限状态
     */
    fun checkPermissionStatus(context: Context): PermissionStatus {
        // 已持有 WRITE_SECURE_SETTINGS：核心功能可用，授权通道无关紧要
        if (hasWriteSecureSettingsPermission(context)) return PermissionStatus.GRANTED
        // 未持有：需要引导用户通过 Shizuku、Root 按钮或 adb 授权
        if (!isShizukuInstalled(context)) return PermissionStatus.SHIZUKU_NOT_INSTALLED
        if (!isShizukuRunning()) return PermissionStatus.SHIZUKU_NOT_RUNNING
        if (!checkPermission()) return PermissionStatus.SHIZUKU_NOT_GRANTED
        return PermissionStatus.NEEDS_PM_GRANT
    }

    /**
     * 利用已授权的 Shizuku 执行 pm grant，为应用授予 WRITE_SECURE_SETTINGS。
     * 需在 Shizuku 已运行且已授予本应用权限的前提下调用。
     * @param context Context
     * @return true 表示授权成功
     */
    suspend fun grantWriteSecureSettings(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val command = "pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
            // 不要在这里预先调用 ensureBound()。
            // Android 14 的部分小米/HyperOS 设备会在创建 UserService 独立进程时
            // 触发 LoadedApk.makeApplicationInner 的系统级空指针，导致绑定必然超时。
            // SecureSettingsHelper.executeCommand() 会先尝试 Shizuku.newProcess，
            // 该路径不需要创建应用侧独立进程，正是这类设备所需的兼容方案；只有
            // newProcess 不可用时，才会继续回退到 UserService。
            val result = SecureSettingsHelper.executeCommand(command)
            Log.d(TAG, "grantWriteSecureSettings result: $result")

            // pm grant 成功时 shell 无输出（空串也算成功），失败时以 "ERROR" 开头
            result != null && !result.startsWith("ERROR")
        } catch (e: Exception) {
            Log.e(TAG, "grantWriteSecureSettings failed", e)
            false
        }
    }

    fun hasWriteSecureSettingsPermission(context: Context): Boolean {
        return context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED
    }
}
