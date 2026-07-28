package com.hrsthrt74.qstile.data

import com.hrsthrt74.qstile.R

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
        // for HyperOS / MIUI
        TileInfo("batterysaver", "省电", "系统"),
        // for other system
        TileInfo("saver", "省电模式", "系统"),
        TileInfo("dark", "深色模式", "系统"),
        TileInfo("sync", "自动同步", "系统"),
        TileInfo("adb", "USB调试", "系统"),
        TileInfo("gps", "GPS", "系统"),
        TileInfo("auto_brightness", "自动亮度", "系统"),

        // HyperOS 专区
        TileInfo("aisubtitles", "实时字幕", "系统"),
        TileInfo("aitranslate", "翻译", "系统"),
        TileInfo("carsickness", "晕车缓解", "系统"),

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

    fun iconRes(value: String): Int? = when (value) {
        "wifi" -> R.drawable.tile_wifi
        "bt" -> R.drawable.tile_bluetooth
        "cell" -> R.drawable.tile_data
        "flashlight" -> R.drawable.tile_flashlight
        "dnd" -> R.drawable.tile_dnd
        "vibrate" -> R.drawable.tile_vibrate
        "nfc" -> R.drawable.tile_nfc
        "hotspot" -> R.drawable.tile_hotspot
        "screenlock" -> R.drawable.tile_auto_rotate
        "auto_brightness" -> R.drawable.tile_auto_brightness
        "saver" -> R.drawable.tile_power_save
        "batterysaver" -> R.drawable.tile_power_save
        EXTILE_CUSTOM -> R.drawable.ic_tile
        else -> null
    }
}
