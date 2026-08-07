package com.hrsthrt74.qstile.data

import com.hrsthrt74.qstile.R

/**
 * 系统磁贴目录：集中管理全部磁贴的静态声明（内部 spec 值、显示名、分类、图标、可用性需求）。
 *
 * 从原 `TileMapping` 拆分而来：本类只负责「磁贴数据 + 查询」，图标从独立的 `iconRes()` when 分支
 * 并入 [TileInfo.iconResId]，可用性从分散的 DeviceType 分支改为 flag 式需求集合（见 [TileRequirement]）。
 */
object TileCatalog {

    /** exTile 自身磁贴的 spec 值（custom 格式） */
    const val EXTILE_CUSTOM = "custom(com.hrsthrt74.qstile/.tile.ExTileService)"

    /**
     * 单个磁贴的静态声明。
     *
     * @param value 系统 Quick Settings spec 值，custom 磁贴为 "custom(包名/类名)" 格式
     * @param displayName 界面显示名
     * @param category 分类名（添加磁贴时按此分组）
     * @param requirements 可用性需求集合（AND 语义，空集合 = 所有设备可用）
     * @param iconResId 内置 drawable 图标资源 id；custom 磁贴或暂无图标时为 null
     */
    data class TileInfo(
        val value: String,
        val displayName: String,
        val category: String,
        val requirements: Set<TileRequirement> = emptySet(),
        val iconResId: Int? = null,
    ) {
        /**
         * 便捷方法：判断当前设备（[profile]）下此磁贴是否可用。
         * 委托 [DeviceProfile.satisfies] 统一求值。
         */
        fun isAvailable(profile: DeviceProfile): Boolean = profile.satisfies(requirements)
    }

    /**
     * 全部磁贴静态清单。
     * 注意：存在同 value 多条记录的情况（如 rotation），查询显示名时需结合设备 profile 取正确条目
     * （见 [getDisplayName]）。
     */
    val systemTiles = listOf(

        // ===== 设备特定 =====
        TileInfo("satellite", "卫星通信", "设备特定", setOf(TileRequirement.XIAOMI_ONLY, TileRequirement.SATELLITE), R.drawable.tile_satellite),
        TileInfo("dtmdtm", "工作台", "设备特定", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_desktop),
        TileInfo("coolingfan", "散热风扇", "设备特定", setOf(TileRequirement.XIAOMI_ONLY, TileRequirement.COOLING_FAN), R.drawable.tile_cooling_fan),

        // exTile
        TileInfo(EXTILE_CUSTOM, "exTile", "exTile", iconResId = R.drawable.ic_tile),

        // ===== 网络与连接 =====
        TileInfo("internet", "互联网", "网络与连接", setOf(TileRequirement.AOSP_ONLY, TileRequirement.MAX_SDK_36)),
        TileInfo("wifi", "WLAN", "网络与连接", iconResId = R.drawable.tile_wifi),
        TileInfo("cell", "移动数据", "网络与连接", setOf(TileRequirement.RequiresFeature(TileRequirement.FEATURE_TELEPHONY)), R.drawable.tile_data),
        TileInfo("bt", "蓝牙", "网络与连接", iconResId = R.drawable.tile_bluetooth),
        TileInfo("hotspot", "热点", "网络与连接", iconResId = R.drawable.tile_hotspot),
        TileInfo("airplane", "飞行模式", "网络与连接", iconResId = R.drawable.tile_airplane),
        TileInfo("location", "位置信息", "网络与连接", setOf(TileRequirement.AOSP_ONLY), R.drawable.tile_location),
        TileInfo("gps", "定位服务", "网络与连接", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_location),
        TileInfo("custom(com.miui.mishare.connectivity/.tile.MiShareTileService)", "小米互传", "网络与连接", setOf(TileRequirement.XIAOMI_ONLY)),
        TileInfo("cast", "投屏", "网络与连接", setOf(TileRequirement.AOSP_ONLY)),
        TileInfo("custom(com.milink.service/com.milink.ui.service.MiLinkTileService)", "投屏", "网络与连接", setOf(TileRequirement.XIAOMI_ONLY)),
        TileInfo("nfc", "NFC", "网络与连接", setOf(TileRequirement.RequiresFeature(TileRequirement.FEATURE_NFC)), R.drawable.tile_nfc),

        // ===== 显示 =====
        TileInfo("autobrightness", "自动亮度", "显示", setOf(TileRequirement.XIAOMI_ONLY, TileRequirement.RequiresFeature(TileRequirement.FEATURE_AMBIENT_LIGHT)), R.drawable.tile_auto_brightness),
        // rotation 在 AOSP 与小米上同名不同文案，分开声明，显示名按设备 profile 取
        TileInfo("rotation", "自动屏幕旋转", "显示", setOf(TileRequirement.AOSP_ONLY), R.drawable.tile_auto_rotate),
        TileInfo("rotation", "方向锁定", "显示", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_auto_rotate),
        TileInfo("dark", "深色模式", "显示", setOf(TileRequirement.AOSP_ONLY), R.drawable.tile_darkmode),
        TileInfo("night", "深色模式", "显示", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_darkmode),
        TileInfo("papermode", "护眼模式", "显示", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_paper_mode),
        TileInfo("reduce_brightness", "极暗", "显示", iconResId = R.drawable.tile_reduce_brightness),
        TileInfo("font_scaling", "字体缩放", "显示", setOf(TileRequirement.AOSP_ONLY)),

        // ===== 工具 =====
        TileInfo("flashlight", "手电筒", "工具", setOf(TileRequirement.RequiresFeature(TileRequirement.FEATURE_CAMERA_FLASH)), R.drawable.tile_flashlight),
        TileInfo("aitranslate", "翻译", "工具", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_aitranslate),
        TileInfo("aisubtitles", "实时字幕", "工具", setOf(TileRequirement.XIAOMI_ONLY, TileRequirement.RequiresProp(TileRequirement.PROP_AI_SUBTITLES)), R.drawable.tile_aisubtitles),
        TileInfo("voicetrans", "对话翻译", "工具", setOf(TileRequirement.XIAOMI_ONLY, TileRequirement.RequiresProp(TileRequirement.PROP_VOICE_TRANS)), R.drawable.tile_voice_trans),
        TileInfo("screenlock", "锁屏", "工具", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_screen_lock),
        TileInfo("custom(com.miui.screenrecorder/.service.QuickService)", "屏幕录制", "工具", setOf(TileRequirement.XIAOMI_ONLY)),
        TileInfo("freeformhang", "迷你小窗", "工具", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_freeformhang),
        TileInfo("custom(com.miui.calculator/.service.QSTileService)", "计算器", "工具", setOf(TileRequirement.XIAOMI_ONLY)),
        TileInfo("scanner", "扫一扫", "工具", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_scanner),
        TileInfo("custom(com.android.quicksearchbox/.tile.QsbTileService)", "搜索", "工具", setOf(TileRequirement.XIAOMI_ONLY)),
        TileInfo("taskmanager", "运行中的应用", "工具", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_taskmanager),
        TileInfo("wallet", "钱包", "工具", setOf(TileRequirement.AOSP_ONLY)),
        TileInfo("alarm", "闹钟", "工具", setOf(TileRequirement.AOSP_ONLY)),
        TileInfo("controls", "控制", "工具", setOf(TileRequirement.AOSP_ONLY)),
        TileInfo("screenrecord", "屏幕录制", "工具", setOf(TileRequirement.AOSP_ONLY)),
        TileInfo("qr_code_scanner", "二维码扫描", "工具", setOf(TileRequirement.AOSP_ONLY)),
        TileInfo("cameratoggle", "相机权限", "工具", setOf(TileRequirement.AOSP_ONLY, TileRequirement.RequiresFeature(TileRequirement.FEATURE_CAMERA))),
        TileInfo("mictoggle", "麦克风权限", "工具", setOf(TileRequirement.AOSP_ONLY)),

        // ===== 系统 =====
        TileInfo("dnd", "勿扰模式", "系统", setOf(TileRequirement.AOSP_ONLY), R.drawable.tile_dnd),
        TileInfo("quietmode", "勿扰模式", "系统", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_dnd),
        TileInfo("mute", "静音", "系统", iconResId = R.drawable.tile_mute),
        TileInfo("vibrate", "振动", "系统", setOf(TileRequirement.RequiresFeature(TileRequirement.FEATURE_VIBRATOR)), R.drawable.tile_vibrate),
        TileInfo("battery", "电池", "系统", setOf(TileRequirement.AOSP_ONLY)),
        TileInfo("saver", "省电模式", "系统", setOf(TileRequirement.AOSP_ONLY), R.drawable.tile_power_save),
        TileInfo("batterysaver", "省电", "系统", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_power_save),
        TileInfo("custom(com.miui.securitycenter/com.miui.superpower.notification.SuperPowerTileService)", "超级省电", "系统", setOf(TileRequirement.XIAOMI_ONLY)),
        TileInfo("custom(com.miui.securitycenter/com.miui.powercenter.powersaver.PerformanceModeTileService)", "性能模式", "系统", setOf(TileRequirement.XIAOMI_ONLY)),
        TileInfo("screenshot", "截屏", "系统", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_screenshot),
        TileInfo("dolbyatomssound", "杜比全景声", "系统", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_dolby_atoms),
        TileInfo("custom(com.miui.securitycenter/com.miui.permcenter.settings.InvisibleModeTileService)", "隐身模式", "系统", setOf(TileRequirement.XIAOMI_ONLY)),
        TileInfo("custom(com.miui.carlink/com.carwith.launcher.quick.start.QuickStartTileService)", "CarWith", "系统", setOf(TileRequirement.XIAOMI_ONLY)),
        TileInfo("wirelesspower", "无线反向充电", "系统", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_wireless_charge),
        TileInfo("carsickness", "晕车缓解", "系统", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_carsickness),
        TileInfo("settings", "设置", "系统", setOf(TileRequirement.XIAOMI_ONLY), R.drawable.tile_settings),

        // ===== 无障碍 =====
        TileInfo("inversion", "颜色反转", "无障碍", iconResId = R.drawable.tile_color_inversion),
        TileInfo("onehanded", "单手模式", "无障碍", setOf(TileRequirement.RequiresProp(TileRequirement.PROP_ONE_HANDED)), R.drawable.tile_one_handed),
        TileInfo("color_correction", "色彩校正", "无障碍", iconResId = R.drawable.tile_color_correction),
        TileInfo("hearing_devices", "助听设备", "无障碍", setOf(TileRequirement.AOSP_ONLY)),

        // ===== 开发者 =====
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$ShowTaps)", "显示点按操作反馈", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$WirelessDebugging)", "无线调试", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$ShowLayout)", "显示布局边界", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$AnimationSpeed)", "窗口动画缩放", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$GPUProfiling)", "HWUI 呈现模式分析", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$SensorsOff)", "传感器已关闭", "开发者"),
        TileInfo("custom(com.android.settings/.development.qstile.DevelopmentTiles\$ForceRTL)", "强制从右到左布局方向", "开发者"),

        // ===== 其他 =====
        TileInfo("edit", "编辑", "其他", setOf(TileRequirement.XIAOMI_ONLY))
    )

    /**
     * 获取当前设备可用的磁贴列表。
     * @param profile 设备能力快照
     * @return 可用磁贴列表
     */
    fun getAvailableTiles(profile: DeviceProfile): List<TileInfo> {
        return systemTiles.filter { it.isAvailable(profile) }
    }

    /**
     * 获取磁贴的界面显示名。
     * 对同 value 多条记录（如 rotation）会优先取「当前设备可用」的那条，
     * 避免 AOSP/小米文案串用（如小米设备显示「自动屏幕旋转」）。
     * @param value 磁贴 spec 值
     * @param profile 设备能力快照
     * @return 显示名；未知 value 时对 custom 磁贴返回包名、其余返回原值
     */
    fun getDisplayName(value: String, profile: DeviceProfile): String {
        // 优先当前设备可用的条目（解决 rotation 双条目文案串用）
        val available = systemTiles.filter { it.value == value && it.isAvailable(profile) }
        if (available.isNotEmpty()) return available.first().displayName
        // 兜底：无可用条目时取清单中第一条
        val fallback = systemTiles.find { it.value == value }
        if (fallback != null) return fallback.displayName
        // custom 磁贴显示名回退为包名
        if (value.startsWith("custom(")) {
            val pkg = value.removePrefix("custom(").removeSuffix(")")
            return pkg.substringBefore("/").substringAfterLast(".")
        }
        return value
    }

    /**
     * 获取磁贴的内置图标资源 id。
     * 图标已并入清单数据（[TileInfo.iconResId]），这里做查询兜底：
     * @param value 磁贴 spec 值
     * @return drawable 资源 id，清单外或 custom 磁贴返回 null
     */
    fun iconRes(value: String): Int? {
        return systemTiles.find { it.value == value }?.iconResId
    }

    /**
     * 获取全部磁贴 spec 值列表。
     * 用于第三方磁贴列表过滤（识别系统预定义的 custom 磁贴）。
     */
    fun getAllValues(): List<String> {
        return systemTiles.map { it.value }
    }
}
