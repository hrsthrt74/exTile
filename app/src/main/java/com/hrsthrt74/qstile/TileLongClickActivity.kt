package com.hrsthrt74.qstile

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.hrsthrt74.qstile.data.ConfigRepository
import kotlinx.coroutines.launch

/**
 * 幽灵桥接 Activity。
 *
 * 系统在长按 Quick Settings 中的第三方磁贴时，会通过
 * [android.service.quicksettings.TileService.ACTION_QS_TILE_PREFERENCES]
 * 启动磁贴所属应用声明的偏好 Activity。
 *
 * 本 Activity 不显示任何界面，仅作为中转站：
 * 读取「长按 exTile 磁贴行为」配置，将用户引导到对应目标后立即关闭，
 * 从而把系统固定的"长按 → 打开偏好设置页"行为替换为可自定义的行为。
 */
class TileLongClickActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 在后台协程中执行分发逻辑，避免阻塞主线程
        lifecycleScope.launch {
            try {
                val behavior = ConfigRepository.getLongPressBehavior(this@TileLongClickActivity)
                val customAppPackage = ConfigRepository.getLongPressCustomApp(this@TileLongClickActivity)

                when (behavior) {
                    // 1. 跳转到 exTile 应用
                    ConfigRepository.LongPressBehavior.OPEN_EXTILE -> {
                        val intent = Intent(this@TileLongClickActivity, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        Log.d(TAG, "LongClick: open exTile")
                    }
                    // 2. 跳转到系统设置
                    ConfigRepository.LongPressBehavior.OPEN_SETTINGS -> {
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        Log.d(TAG, "LongClick: open settings")
                    }
                    // 3. 跳转到融合设备中心（小米互联服务的设备互联页面）
                    ConfigRepository.LongPressBehavior.OPEN_DEVICE_CENTER -> {
                        val intent = Intent().setClassName(
                            "com.milink.service",
                            "com.miui.circulate.world.CirculateWorldActivity"
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        Log.d(TAG, "LongClick: open device center")
                    }
                    // 4. 跳转到自定义应用
                    ConfigRepository.LongPressBehavior.OPEN_CUSTOM_APP -> {
                        if (customAppPackage.isNotBlank()) {
                            val launchIntent = packageManager.getLaunchIntentForPackage(customAppPackage)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(launchIntent)
                                Log.d(TAG, "LongClick: open $customAppPackage")
                            } else {
                                Log.e(TAG, "LongClick: no launch intent for $customAppPackage")
                            }
                        } else {
                            Log.e(TAG, "LongClick: custom app not set")
                        }
                    }
                    else -> Log.e(TAG, "LongClick: unknown behavior $behavior")
                }
            } catch (e: Exception) {
                Log.e(TAG, "LongClick dispatch failed", e)
            } finally {
                // 幽灵活动：分发完成后立即关闭自身，不留下任何痕迹
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "TileLongClickActivity"
    }
}
