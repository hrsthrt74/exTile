package com.hrsthrt74.qstile.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.StatsRepository
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
                // onClick = 切换：当前展开则收起，当前收起则展开
                val newIsExpanded = !config.isExpanded
                applyLayout(newIsExpanded, config)
                updateTileState()
            } catch (e: Exception) {
                Log.e(TAG, "onClick failed", e)
            }
        }
    }

    /**
     * 用户收起 QS 快速设置面板时系统回调所有已注册磁贴服务。
     * 利用此公开 API（零权限）实现「收起面板时自动收起」功能：
     * 若开关开启 且 磁贴当前处于展开布局，则自动执行一次收起操作。
     * 幂等：若已是收起状态则跳过，避免重复写 sysui_qs_tiles。
     */
    override fun onStopListening() {
        super.onStopListening()
        scope.launch {
            try {
                val autoCollapse = ConfigRepository.getAutoCollapseOnClose(this@ExTileService)
                if (!autoCollapse) return@launch

                val config = ConfigRepository.getConfig(this@ExTileService)
                // 仅在展开状态下触发收起；已是收起状态则跳过（幂等）
                if (!config.isExpanded) return@launch

                Log.d(TAG, "Auto-collapsing on QS panel close")
                applyLayout(newIsExpanded = false, config)
                updateTileState()
            } catch (e: Exception) {
                Log.e(TAG, "onStopListening auto-collapse failed", e)
            }
        }
    }

    /**
     * 将磁贴布局切换到指定状态并同步所有关联设置。
     * onClick（手动切换）和 onStopListening（自动收起）共用此函数，
     * 避免重复代码。执行顺序：
     * 1. 写 sysui_qs_tiles（目标布局）
     * 2. 保存 isExpanded 标志
     * 3. 记录统计（展开/收起）
     * 4. 按开关同步无字模式 / 融合设备中心
     *
     * @param newIsExpanded 目标展开状态：true = 展开，false = 收起
     * @param config 当前配置（用于获取展开/收起磁贴列表，避免重复读取）
     */
    private suspend fun applyLayout(newIsExpanded: Boolean, config: com.hrsthrt74.qstile.data.TileConfig) {
        val tilesToSet = if (newIsExpanded) {
            config.expandedTiles
        } else {
            config.collapsedTiles
        }

        val success = SecureSettingsHelper.setCurrentTiles(this@ExTileService, tilesToSet)

        if (success) {
            ConfigRepository.saveIsExpanded(this@ExTileService, newIsExpanded)
            Log.d(TAG, "Layout applied: ${if (newIsExpanded) "expanded" else "collapsed"}")

            // 记录统计：仅统计布局切换成功的情况（失败不算）
            if (newIsExpanded) {
                StatsRepository.recordExpand(this@ExTileService)
            } else {
                StatsRepository.recordCollapse(this@ExTileService)
            }

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
