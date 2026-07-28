package com.hrsthrt74.qstile.data

import com.hrsthrt74.qstile.R

object TileMapping {

    /** 设备类型枚举 */
    enum class DeviceType {
        UNIVERSAL,      // 所有设备可用
        XIAOMI_ONLY,    // 仅小米设备（MIUI/HyperOS）
        AOSP_ONLY       // 仅原生 Android
    }

    data class TileInfo(
        val value: String,
        val displayName: String,
        val category: String,
        val deviceType: DeviceType = DeviceType.UNIVERSAL
    )

    const val EXTILE_CUSTOM = "custom(com.hrsthrt74.qstile/.tile.ExTileService)"

    val systemTiles = listOf(
        // ===== 网络 =====
        TileInfo("wifi", "WLAN", "网络"),
        TileInfo("bt", "蓝牙", "网络"),
        TileInfo("cell", "移动数据", "网络"),
        TileInfo("airplane", "飞行模式", "网络"),
        TileInfo("hotspot", "热点", "网络"),
        TileInfo("nfc", "NFC", "网络"),
        TileInfo("location", "位置信息", "网络"),
        TileInfo("cast", "投屏", "网络"),
        TileInfo("vpn", "VPN", "网络"),

        // ===== 工具 =====
        TileInfo("flashlight", "手电筒", "工具"),
        TileInfo("screenlock", "自动旋转", "工具"),
        TileInfo("rotation", "旋转", "工具"),
        TileInfo("dnd", "勿扰模式", "工具", DeviceType.AOSP_ONLY),
        TileInfo("quietmode", "勿扰模式", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("mute", "静音", "工具"),
        TileInfo("vibrate", "振动", "工具"),
        TileInfo("volume", "音量", "工具"),
        TileInfo("freeformhang", "迷你小窗", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("scanner", "扫一扫", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("taskmanager", "运行中的应用", "工具", DeviceType.XIAOMI_ONLY),

        // ===== 系统（通用） =====
        TileInfo("battery", "电池", "系统"),
        TileInfo("dark", "深色模式", "系统"),
        TileInfo("sync", "自动同步", "系统"),
        TileInfo("adb", "USB调试", "系统"),
        TileInfo("night", "深色模式", "系统", DeviceType.XIAOMI_ONLY),

        // ===== 系统（小米专用） =====
        TileInfo("batterysaver", "省电", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("aisubtitles", "实时字幕", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("aitranslate", "翻译", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("carsickness", "晕车缓解", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("gps", "GPS", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("autobrightness", "自动亮度", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("settings", "设置", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("voicetrans", "对话翻译", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("papermode", "护眼模式", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("dolbyatomssound", "杜比全景声", "系统", DeviceType.XIAOMI_ONLY),

        // ===== 系统（原生专用） =====
        TileInfo("saver", "省电模式", "系统", DeviceType.AOSP_ONLY),

        // ===== 其他 =====
        TileInfo("edit", "编辑磁贴", "其他"),
        TileInfo(EXTILE_CUSTOM, "exTile 收纳", "其他")
    )

    /** 获取当前设备可用的磁贴列表 */
    fun getAvailableTiles(isXiaomi: Boolean): List<TileInfo> {
        return systemTiles.filter { tile ->
            when (tile.deviceType) {
                DeviceType.UNIVERSAL -> true
                DeviceType.XIAOMI_ONLY -> isXiaomi
                DeviceType.AOSP_ONLY -> !isXiaomi
            }
        }
    }

    /** 获取当前设备可用的磁贴，按分类分组 */
    fun getAvailableTilesByCategory(isXiaomi: Boolean): Map<String, List<TileInfo>> {
        return getAvailableTiles(isXiaomi).groupBy { it.category }
    }

    fun getDisplayName(value: String): String {
        return systemTiles.find { it.value == value }?.displayName
            ?: if (value.startsWith("custom(")) {
                val pkg = value.removePrefix("custom(").removeSuffix(")")
                pkg.substringBefore("/").substringAfterLast(".")
            } else {
                value
            }
    }

    fun getTilesByCategory(): Map<String, List<TileInfo>> {
        return systemTiles.groupBy { it.category }
    }

    fun getAllValues(): List<String> {
        return systemTiles.map { it.value }
    }

    fun isValidTile(value: String): Boolean {
        return systemTiles.any { it.value == value }
    }

    /** 检查指定磁贴是否在当前设备可用 */
    fun isTileAvailable(value: String, isXiaomi: Boolean): Boolean {
        val tile = systemTiles.find { it.value == value } ?: return false
        return when (tile.deviceType) {
            DeviceType.UNIVERSAL -> true
            DeviceType.XIAOMI_ONLY -> isXiaomi
            DeviceType.AOSP_ONLY -> !isXiaomi
        }
    }

    fun iconRes(value: String): Int? = when (value) {
        "wifi" -> R.drawable.tile_wifi
        "bt" -> R.drawable.tile_bluetooth
        "cell" -> R.drawable.tile_data
        "flashlight" -> R.drawable.tile_flashlight
        "dnd" -> R.drawable.tile_dnd
        "quietmode" -> R.drawable.tile_dnd
        "vibrate" -> R.drawable.tile_vibrate
        "nfc" -> R.drawable.tile_nfc
        "hotspot" -> R.drawable.tile_hotspot
        "screenlock" -> R.drawable.tile_auto_rotate
        "auto_brightness" -> R.drawable.tile_auto_brightness
        "autobrightness" -> R.drawable.tile_auto_brightness
        "saver" -> R.drawable.tile_power_save
        "batterysaver" -> R.drawable.tile_power_save
        "settings" -> R.drawable.tile_settings
        EXTILE_CUSTOM -> R.drawable.ic_tile
        else -> null
    }
}
