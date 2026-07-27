package com.hrsthrt74.qstile.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.hrsthrt74.qstile.MainActivity
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
