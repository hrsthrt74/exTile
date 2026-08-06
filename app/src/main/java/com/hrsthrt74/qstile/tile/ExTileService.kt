package com.hrsthrt74.qstile.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExTileService : TileService() {
    companion object {
        private const val TAG = "ExTileService"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        scope.launch {
            try {
                val config = ConfigRepository.getConfig(this@ExTileService)
                val newIsExpanded = !config.isExpanded

                val tilesToSet = if (newIsExpanded) {
                    config.expandedTiles
                } else {
                    config.collapsedTiles
                }

                val success = SecureSettingsHelper.setCurrentTiles(this@ExTileService, tilesToSet)

                if (success) {
                    ConfigRepository.saveIsExpanded(this@ExTileService, newIsExpanded)
                    Log.d(TAG, "Toggled to ${if (newIsExpanded) "expanded" else "collapsed"}")

                    // 「展开收起同时控制无字模式」：开启后，磁贴切换布局时同步 wordless_mode
                    // 展开 → wordless_mode=0（显示文字），收起 → wordless_mode=1（无字模式）
                    if (ConfigRepository.getWordlessModeSync(this@ExTileService)) {
                        SecureSettingsHelper.putSecureSetting(
                            this@ExTileService,
                            "wordless_mode",
                            if (newIsExpanded) "0" else "1"
                        )
                    }

                    // 「展开收起同时控制融合设备中心」：开启后，磁贴切换布局时同步 smart_device_control
                    // 展开 → smart_device_control=1，收起 → smart_device_control=0（与无字模式相反）
                    if (ConfigRepository.getSmartDeviceControlSync(this@ExTileService)) {
                        SecureSettingsHelper.putSecureSetting(
                            this@ExTileService,
                            "smart_device_control",
                            if (newIsExpanded) "1" else "0"
                        )
                    }
                } else {
                    Log.e(TAG, "Failed to set sysui_qs_tiles")
                }

                updateTileState()
            } catch (e: Exception) {
                Log.e(TAG, "onClick failed", e)
            }
        }
    }

    private fun updateTileState() {
        scope.launch {
            try {
                val config = ConfigRepository.getConfig(this@ExTileService)
                val tile = qsTile ?: return@launch

                tile.state = if (config.isExpanded) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                // ↓ 这是源代码，备份
                // tile.label = if (config.isExpanded) "收起" else "展开"
                // ↓ 这是调试用的，最终要改的
                tile.label = if (config.isExpanded) "更多磁贴" else "更多磁贴"

                tile.updateTile()
            } catch (e: Exception) {
                Log.e(TAG, "updateTileState failed", e)
            }
        }
    }
}
