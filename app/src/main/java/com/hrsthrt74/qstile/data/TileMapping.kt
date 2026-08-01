package com.hrsthrt74.qstile.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.service.quicksettings.TileService
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

        // exTile
        TileInfo(EXTILE_CUSTOM, "exTile", "exTile"),

        // ===== 网络与连接 =====
        TileInfo("internet", "互联网", "网络与连接", DeviceType.AOSP_ONLY),
        TileInfo("wifi", "WLAN", "网络与连接"),
        TileInfo("bt", "蓝牙", "网络与连接"),
        TileInfo("cell", "移动数据", "网络与连接"),
        TileInfo("hotspot", "热点", "网络与连接"),
        TileInfo("airplane", "飞行模式", "网络与连接"),
        TileInfo("location", "位置信息", "网络与连接", DeviceType.AOSP_ONLY),
        TileInfo("custom(com.miui.mishare.connectivity/.tile.MiShareTileService)", "小米互传", "网络与连接", DeviceType.XIAOMI_ONLY),
        TileInfo("gps", "定位服务", "网络与连接", DeviceType.XIAOMI_ONLY),
        TileInfo("cast", "投屏", "网络与连接", DeviceType.AOSP_ONLY),
        TileInfo("custom(com.milink.service/com.milink.ui.service.MiLinkTileService)", "投屏", "网络与连接", DeviceType.XIAOMI_ONLY),
        TileInfo("nfc", "NFC", "网络与连接"),

        // ===== 显示 =====
        TileInfo("autobrightness", "自动亮度", "显示", DeviceType.XIAOMI_ONLY),
        TileInfo("rotation", "自动屏幕旋转", "显示", DeviceType.AOSP_ONLY),
        TileInfo("dark", "深色模式", "显示", DeviceType.AOSP_ONLY),
        TileInfo("night", "深色模式", "显示", DeviceType.XIAOMI_ONLY),
        TileInfo("papermode", "护眼模式", "显示", DeviceType.XIAOMI_ONLY),
        TileInfo("reduce_brightness", "极暗", "显示"),
        TileInfo("font_scaling", "字体缩放", "显示", DeviceType.AOSP_ONLY),

        // ===== 工具 =====
        TileInfo("flashlight", "手电筒", "工具"),
        TileInfo("aitranslate", "翻译", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("aisubtitles", "实时字幕", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("voicetrans", "对话翻译", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("screenlock", "锁屏", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.miui.screenrecorder/.service.QuickService)", "屏幕录制", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("freeformhang", "迷你小窗", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.miui.calculator/.service.QSTileService)", "计算器", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("scanner", "扫一扫", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.android.quicksearchbox/.tile.QsbTileService)", "搜索", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("taskmanager", "运行中的应用", "工具", DeviceType.XIAOMI_ONLY),
        TileInfo("wallet", "钱包", "工具", DeviceType.AOSP_ONLY),
        TileInfo("alarm", "闹钟", "工具", DeviceType.AOSP_ONLY),
        TileInfo("controls", "控制", "工具", DeviceType.AOSP_ONLY),
        TileInfo("screenrecord", "屏幕录制", "工具", DeviceType.AOSP_ONLY),
        TileInfo("qr_code_scanner", "二维码扫描", "工具", DeviceType.AOSP_ONLY),
        TileInfo("cameratoggle", "相机权限", "工具", DeviceType.AOSP_ONLY),
        TileInfo("mictoggle", "麦克风权限", "工具", DeviceType.AOSP_ONLY),

        // ===== 系统 =====
        TileInfo("dnd", "勿扰模式", "系统", DeviceType.AOSP_ONLY),
        TileInfo("quietmode", "勿扰模式", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("mute", "静音", "系统"),
        TileInfo("vibrate", "振动", "系统"),
        TileInfo("battery", "电池", "系统", DeviceType.AOSP_ONLY),
        TileInfo("saver", "省电模式", "系统", DeviceType.AOSP_ONLY),
        TileInfo("batterysaver", "省电", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.miui.securitycenter/com.miui.superpower.notification.SuperPowerTileService)", "超级省电", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.miui.securitycenter/com.miui.powercenter.powersaver.PerformanceModeTileService)", "性能模式", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("screenshot", "截屏", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("dolbyatomssound", "杜比全景声", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.miui.securitycenter/com.miui.permcenter.settings.InvisibleModeTileService)", "隐身模式", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("custom(com.miui.carlink/com.carwith.launcher.quick.start.QuickStartTileService)", "CarWith", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("wirelesspower", "无线反向充电", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("carsickness", "晕车缓解", "系统", DeviceType.XIAOMI_ONLY),
        TileInfo("settings", "设置", "系统", DeviceType.XIAOMI_ONLY),

        // ===== 无障碍 =====
        TileInfo("color_correction", "色彩校正", "无障碍"),
        TileInfo("hearing_devices", "助听设备", "无障碍", DeviceType.AOSP_ONLY),

        // ===== 开发者 =====
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$ShowTaps)", "显示点按操作反馈", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$WirelessDebugging)", "无线调试", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$ShowLayout)", "显示布局边界", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$AnimationSpeed)", "窗口动画缩放", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$GPUProfiling)", "HWUI 呈现模式分析", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$SensorsOff)", "传感器已关闭", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$ForceRTL)", "强制从右到左布局方向", "开发者"),

        // ===== 其他 =====
        TileInfo("edit", "编辑", "其他", DeviceType.XIAOMI_ONLY)
    )

    /** 获取当前设备可用的磁贴列表 */
    fun getAvailableTiles(isXiaomi: Boolean): List<TileInfo> {
        return systemTiles.filter { tile ->
            when (tile.deviceType) {
                DeviceType.UNIVERSAL -> true
                DeviceType.XIAOMI_ONLY -> isXiaomi
                DeviceType.AOSP_ONLY -> {
                    // internet 磁贴仅在 SDK < 37 且是 AOSP 设备时显示
                    if (tile.value == "internet") {
                        !isXiaomi && Build.VERSION.SDK_INT < 37
                    } else {
                        !isXiaomi
                    }
                }
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

    fun getCategory(value: String): String {
        return systemTiles.find { it.value == value }?.category
            ?: if (value.startsWith("custom(")) {
                "自定义"
            } else {
                "其他"
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
        "screenlock" -> R.drawable.tile_screen_lock
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
     * 获取 custom 磁贴的显示名与应用名。
     * 第一行应显示磁贴显示名（label），第二行应显示应用名（appName），
     * 而不是原始的 "custom(包名/类名)" 字符串。
     * @param context Context
     * @param value 磁贴值，格式为 "custom(包名/类名)"
     * @return Pair(显示名, 应用名)，无法解析时返回 null
     */
    fun getCustomTileNames(context: Context, value: String): Pair<String, String>? {
        val component = parseCustomComponent(value) ?: return null
        val pm = context.packageManager
        return try {
            val serviceInfo = pm.getServiceInfo(component, PackageManager.GET_META_DATA)
            val label = try {
                serviceInfo.loadLabel(pm).toString()
            } catch (_: Exception) {
                null
            }
            val appName = try {
                pm.getApplicationLabel(serviceInfo.applicationInfo).toString()
            } catch (_: Exception) {
                null
            }
            // 任一字段成功即返回，失败的字段用包名/类名兜底
            if (label != null || appName != null) {
                Pair(
                    label ?: component.className.substringAfterLast('.'),
                    appName ?: component.packageName.substringAfterLast('.')
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 获取所有可用的 Quick Settings Tile 服务
     * @param context Context
     * @return List<Triple<包名, 类名, 显示名称>>
     */
    fun getAllQSTileServices(context: Context): List<Triple<String, String, String>> {
        val pm = context.packageManager
        val result = mutableListOf<Triple<String, String, String>>()
        // 通过 queryIntentServices 查询已启用的 QS_TILE 服务
        try {
            val intent = Intent(TileService.ACTION_QS_TILE)
            val resolveInfos = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
            for (info in resolveInfos) {
                val serviceInfo = info.serviceInfo ?: continue
                val componentName = "${serviceInfo.packageName}/${serviceInfo.name}"
                val label = serviceInfo.loadLabel(pm).toString()
                result.add(Triple(serviceInfo.packageName, serviceInfo.name, label))
            }
        } catch (_: Exception) {}
        return result
    }

    /**
     * 第三方磁贴服务信息（含图标）。
     * 用于「添加第三方磁贴」列表：一次批量查询，避免渲染时逐个 IPC。
     */
    data class QSTileServiceInfo(
        val packageName: String,
        val className: String,
        val label: String,
        val appName: String,
        val icon: Drawable?
    )

    /**
     * 获取所有可用的 Quick Settings Tile 服务及其图标。
     * 一次 [queryIntentServices] 批量取回全部信息，避免每个磁贴单独 IPC 查询；
     * 应放到后台线程执行。
     * @param context Context
     * @return 磁贴服务信息列表（含图标 Drawable）
     */
    fun getAllQSTileServicesWithIcon(context: Context): List<QSTileServiceInfo> {
        val pm = context.packageManager
        val result = mutableListOf<QSTileServiceInfo>()
        try {
            val intent = Intent(TileService.ACTION_QS_TILE)
            val resolveInfos = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
            for (info in resolveInfos) {
                val serviceInfo = info.serviceInfo ?: continue
                val label = try {
                    serviceInfo.loadLabel(pm).toString()
                } catch (e: Exception) {
                    serviceInfo.name.substringAfterLast('.')
                }
                val appName = try {
                    pm.getApplicationLabel(serviceInfo.applicationInfo).toString()
                } catch (e: Exception) {
                    serviceInfo.packageName.substringAfterLast('.')
                }
                // 优先 Service 图标，回退到应用图标
                var icon: Drawable? = null
                if (serviceInfo.icon != 0) {
                    try {
                        val resources = pm.getResourcesForApplication(serviceInfo.applicationInfo)
                        icon = resources.getDrawable(serviceInfo.icon, null)
                    } catch (_: Exception) {}
                }
                if (icon == null) {
                    try {
                        icon = serviceInfo.applicationInfo.loadIcon(pm)
                    } catch (_: Exception) {}
                }
                result.add(
                    QSTileServiceInfo(
                        packageName = serviceInfo.packageName,
                        className = serviceInfo.name,
                        label = label,
                        appName = appName,
                        icon = icon
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    /**
     * 获取 custom 磁贴的图标 Drawable
     * @param context Context
     * @param value 磁贴值，格式为 "custom(包名/类名)"
     * @return 图标 Drawable 或 null（如果无法获取）
     */
    fun getCustomTileIcon(context: Context, value: String): Drawable? {
        val component = parseCustomComponent(value) ?: return null
        val pm = context.packageManager
        // 优先通过 getServiceInfo 获取（包括禁用的组件）
        try {
            val serviceInfo = pm.getServiceInfo(component, PackageManager.GET_META_DATA)
            // 尝试通过 Resources 获取 Service 的图标
            if (serviceInfo.icon != 0) {
                try {
                    val resources = pm.getResourcesForApplication(serviceInfo.applicationInfo)
                    val drawable = resources.getDrawable(serviceInfo.icon, null)
                    if (drawable != null) return drawable
                } catch (_: Exception) {}
            }
            // 回退到应用图标
            try {
                val icon = serviceInfo.applicationInfo.loadIcon(pm)
                if (icon != null) return icon
            } catch (_: Exception) {}
        } catch (_: Exception) {}
        // 通过 queryIntentServices 查询已启用的 QS_TILE 服务
        try {
            val intent = Intent(TileService.ACTION_QS_TILE)
            val resolveInfos = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
            for (info in resolveInfos) {
                val serviceInfo = info.serviceInfo ?: continue
                if (serviceInfo.packageName == component.packageName &&
                    serviceInfo.name == component.className) {
                    // 尝试通过 Resources 获取 Service 的图标
                    if (serviceInfo.icon != 0) {
                        try {
                            val resources = pm.getResourcesForApplication(serviceInfo.applicationInfo)
                            val drawable = resources.getDrawable(serviceInfo.icon, null)
                            if (drawable != null) return drawable
                        } catch (_: Exception) {}
                    }
                    // 回退到应用图标
                    try {
                        val icon = serviceInfo.applicationInfo.loadIcon(pm)
                        if (icon != null) return icon
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
        // 最后尝试获取应用图标
        try {
            val appInfo = pm.getApplicationInfo(component.packageName, 0)
            val icon = appInfo.loadIcon(pm)
            if (icon != null) return icon
        } catch (_: Exception) {}
        return null
    }
}
