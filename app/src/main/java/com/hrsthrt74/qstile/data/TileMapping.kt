package com.hrsthrt74.qstile.data

object TileMapping {
    data class TileInfo(
        val value: String,
        val displayName: String,
        val category: String
    )

    const val EXTILE_CUSTOM = "custom(com.hrsthrt74.qstile/.tile.ExTileService)"

    val systemTiles = listOf(
        TileInfo("wifi", "WLAN", "网络"),
        TileInfo("bt", "蓝牙", "网络"),
        TileInfo("cell", "移动数据", "网络"),
        TileInfo("airplane", "飞行模式", "网络"),
        TileInfo("hotspot", "热点", "网络"),
        TileInfo("nfc", "NFC", "网络"),
        TileInfo("location", "位置信息", "网络"),
        TileInfo("cast", "投屏", "网络"),
        TileInfo("vpn", "VPN", "网络"),

        TileInfo("flashlight", "手电筒", "工具"),
        TileInfo("screenlock", "自动旋转", "工具"),
        TileInfo("rotation", "旋转", "工具"),
        TileInfo("dnd", "勿扰模式", "工具"),
        TileInfo("mute", "静音", "工具"),
        TileInfo("vibrate", "振动", "工具"),
        TileInfo("volume", "音量", "工具"),

        TileInfo("battery", "电池", "系统"),
        TileInfo("saver", "省电模式", "系统"),
        TileInfo("dark", "深色模式", "系统"),
        TileInfo("sync", "自动同步", "系统"),
        TileInfo("adb", "USB调试", "系统"),
        TileInfo("gps", "GPS", "系统"),
        TileInfo("brightness", "亮度", "系统"),

        TileInfo("edit", "编辑磁贴", "其他"),
        TileInfo(EXTILE_CUSTOM, "exTile 收纳", "其他")
    )

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
}
