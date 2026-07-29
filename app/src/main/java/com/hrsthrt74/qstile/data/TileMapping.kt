package com.hrsthrt74.qstile.data

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Drawable
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
        // ===== 网络与连接 =====
        TileInfo("wifi", "WLAN", "网络与连接"),
        TileInfo("bt", "蓝牙", "网络与连接"),
        TileInfo("cell", "移动数据", "网络与连接"),
        TileInfo("airplane", "飞行模式", "网络与连接"),
        TileInfo("hotspot", "热点", "网络与连接"),
        TileInfo("nfc", "NFC", "网络与连接"),
        TileInfo("location", "位置信息", "网络与连接", DeviceType.AOSP_ONLY),
        TileInfo("cast", "投屏", "网络与连接", DeviceType.AOSP_ONLY),
        TileInfo("internet", "互联网", "网络与连接", DeviceType.AOSP_ONLY),

        // ===== 工具 =====
        TileInfo("flashlight", "手电筒", "工具"),
        TileInfo("screenlock", "自动旋转", "工具"),
        TileInfo("rotation", "旋转", "工具", DeviceType.AOSP_ONLY),
        TileInfo("dnd", "勿扰模式", "工具", DeviceType.AOSP_ONLY),
        TileInfo("quietmode", "勿扰模式", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("mute", "静音", "工具"),
        TileInfo("vibrate", "振动", "工具"),
        TileInfo("freeformhang", "迷你小窗", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("scanner", "扫一扫", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("taskmanager", "运行中的应用", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("wallet", "钱包", "工具", DeviceType.AOSP_ONLY),
        TileInfo("alarm", "闹钟", "工具", DeviceType.AOSP_ONLY),
        TileInfo("controls", "控制", "工具", DeviceType.AOSP_ONLY),
        TileInfo("screenrecord", "屏幕录制", "工具", DeviceType.AOSP_ONLY),
        TileInfo("qr_code_scanner", "二维码扫描", "工具", DeviceType.AOSP_ONLY),
        TileInfo("font_scaling", "字体缩放", "工具", DeviceType.AOSP_ONLY),
        TileInfo("cameratoggle", "相机开关", "工具", DeviceType.AOSP_ONLY),
        TileInfo("mictoggle", "麦克风开关", "工具", DeviceType.AOSP_ONLY),
        TileInfo("hearing_devices", "助听设备", "工具", DeviceType.AOSP_ONLY),

        // ===== 系统（通用） =====
        TileInfo("battery", "电池", "系统", DeviceType.AOSP_ONLY),
        TileInfo("dark", "深色模式", "系统", DeviceType.AOSP_ONLY),
        // TileInfo("adb", "USB调试", "系统"),
        TileInfo("night", "深色模式", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("reduce_brightness", "极暗", "系统"),

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
        TileInfo("screenshot", "截屏", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("wirelesspower", "无线反向充电", "系统", DeviceType.XIAOMI_ONLY),

        // ===== 小米应用（custom 格式） =====
        TileInfo("custom(com.miui.mishare.connectivity/.tile.MiShareTileService)", "小米互传", "小米应用", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.milink.service/com.milink.ui.service.MiLinkTileService)", "投屏", "小米应用", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.miui.securitycenter/com.miui.permcenter.settings.InvisibleModeTileService)", "隐身模式", "小米应用", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.miui.securitycenter/com.miui.powercenter.powersaver.PerformanceModeTileService)", "性能模式", "小米应用", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.miui.calculator/.service.QSTileService)", "计算器", "小米应用", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.android.quicksearchbox/.tile.QsbTileService)", "搜索", "小米应用", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.miui.screenrecorder/.service.QuickService)", "屏幕录制", "小米应用", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.miui.securitycenter/com.miui.superpower.notification.SuperPowerTileService)", "超级省电", "小米应用", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.miui.carlink/com.carwith.launcher.quick.start.QuickStartTileService)", "CarWith", "小米应用", DeviceType.XIAOMI_ONLY),
        
        // ===== 无障碍 =====
        TileInfo("color_correction", "色彩校正", "无障碍"),

        // ===== 开发者 =====
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$ShowTaps)", "点按操作反馈", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$WirelessDebugging)", "无线调试", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$SensorsOff)", "传感器已关闭", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$AnimationSpeed)", "窗口动画缩放", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$ForceRTL)", "强制从右到左", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$GPUProfiling)", "GPU 呈现模式分析", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$ShowLayout)", "显示布局边界", "开发者"),
        

        // ===== 系统（原生专用） =====
        TileInfo("saver", "省电模式", "系统", DeviceType.AOSP_ONLY),

        // ===== 其他 =====
        TileInfo("edit", "编辑", "其他"),
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

    // 图标绑定
    fun iconRes(value: String): Int? = when (value) {
        "wifi" -> R.drawable.tile_wifi
        "bt" -> R.drawable.tile_bluetooth
        "cell" -> R.drawable.tile_data
        "airplane" -> R.drawable.tile_airplane
        "flashlight" -> R.drawable.tile_flashlight
        "dnd" -> R.drawable.tile_dnd
        "quietmode" -> R.drawable.tile_dnd
        "mute" -> R.drawable.tile_mute
        "vibrate" -> R.drawable.tile_vibrate
        "nfc" -> R.drawable.tile_nfc
        "hotspot" -> R.drawable.tile_hotspot
        "gps" -> R.drawable.tile_location
        "location" -> R.drawable.tile_location
        "screenlock" -> R.drawable.tile_auto_rotate
        "auto_brightness" -> R.drawable.tile_auto_brightness
        "autobrightness" -> R.drawable.tile_auto_brightness
        "saver" -> R.drawable.tile_power_save
        "batterysaver" -> R.drawable.tile_power_save
        "dark" -> R.drawable.tile_darkmode
        "night" -> R.drawable.tile_darkmode
        "taskmanager" -> R.drawable.tile_taskmanager
        "wirelesspower" -> R.drawable.tile_wireless_charge
        "carsickness" -> R.drawable.tile_carsickness
        "aisubtitles" -> R.drawable.tile_aisubtitles
        "aitranslate" -> R.drawable.tile_aitranslate
        "voicetrans" -> R.drawable.tile_voice_trans
        "papermode" -> R.drawable.tile_paper_mode
        "dolbyatomssound" -> R.drawable.tile_dolby_atoms
        "screenshot" -> R.drawable.tile_screenshot
        "scanner" -> R.drawable.tile_scanner
        "freeformhang" -> R.drawable.tile_freeformhang
        "color_correction" -> R.drawable.tile_color_correction
        "settings" -> R.drawable.tile_settings
        EXTILE_CUSTOM -> R.drawable.ic_tile
        else -> null
    }

    /**
     * 解析 custom 磁贴的 ComponentName
     * @param value 磁贴值，格式为 "custom(包名/类名)"
     * @return ComponentName 或 null（如果不是 custom 格式）
     */
    fun parseCustomComponent(value: String): ComponentName? {
        if (!value.startsWith("custom(")) return null
        val componentStr = value.removePrefix("custom(").removeSuffix(")")
        return ComponentName.unflattenFromString(componentStr)
    }

    /**
     * 获取 custom 磁贴的图标 Drawable
     * @param context Context
     * @param value 磁贴值，格式为 "custom(包名/类名)"
     * @return 图标 Drawable 或 null（如果无法获取）
     */
    fun getCustomTileIcon(context: Context, value: String): Drawable? {
        val component = parseCustomComponent(value) ?: return null
        return try {
            val pm = context.packageManager
            // 先尝试获取 Service 的图标
            val serviceInfo = pm.getServiceInfo(component, 0)
            serviceInfo.loadIcon(pm)
        } catch (e: Exception) {
            // 如果获取 Service 图标失败，尝试获取应用图标
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(component.packageName, 0)
                appInfo.loadIcon(pm)
            } catch (e: Exception) {
                null
            }
        }
    }
}
