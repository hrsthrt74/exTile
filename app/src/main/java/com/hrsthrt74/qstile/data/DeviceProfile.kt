package com.hrsthrt74.qstile.data

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.VibratorManager
import com.hjq.device.compat.DeviceOs

/**
 * 设备能力快照：一次性采集当前设备的全部相关能力，供磁贴可用性统一求值。
 *
 * 之前「是否可用」的判断散落在各处（有的看系统类别、有的看 SDK 版本、有的看设备形态），
 * 且 isXiaomi 判定在多个页面重复书写。这里统一为单一数据源，[DeviceProfile.from] 负责采集并缓存。
 *
 * 注意：设备能力不会运行时变化，因此使用伴生对象缓存，避免每次过滤都重复读取系统属性。
 */
data class DeviceProfile(
    /** 是否为小米系系统（MIUI / HyperOS） */
    val isXiaomi: Boolean,
    /**
     * 是否为类原生 AOSP 系统（非厂商定制 ROM，如 Pixel 原版 Android、LineageOS、GSI 等）。
     * 判定依据：[DeviceOs.getOsName()] 在厂商定制系统上返回非空系统名（如 "HyperOS" / "MIUI"），
     * 而在类原生 AOSP 系统上返回空字符串，因此空串即视为 AOSP。
     * 注意：存在个别冷门 ROM 不上报系统名导致误判的可能（见 OOBE 使用处的风险说明）。
     */
    val isAosp: Boolean,
    /** 是否为平板（屏幕尺寸 >= SCREENLAYOUT_SIZE_LARGE） */
    val isTablet: Boolean,
    /** Android SDK 版本 */
    val sdkInt: Int,
    /** 是否支持卫星通讯。TODO 占位：恒为 true，待 prop 检测实现后收紧 */
    val hasSatellite: Boolean = true,
    /**
     * 是否带主动散热风扇。
     * 判定依据为机型代号（[Build.DEVICE]）是否命中白名单 [COOLING_FAN_DEVICE_CODENAMES]——
     * 小米系目前只有 REDMI K90 Max（prague）与 REDMI K90 至尊版（warsaw）两款机型搭载实体散热风扇，
     * 其余设备一律视为不支持。
     */
    val hasCoolingFan: Boolean,
    /**
     * prop 门控能力表：key 为系统属性名（见 [TileRequirement.gatedPropKeys]），value 为解析后的布尔能力。
     * 由 [from] 一次性批量读取并缓存，供 [TileRequirement.RequiresProp] 求值。
     */
    val gatedProps: Map<String, Boolean> = emptyMap(),
    /**
     * 硬件特性能力表：key 为系统特性字符串（见 [TileRequirement.gatedFeatureKeys]），value 为
     * `PackageManager.hasSystemFeature` 检测结果。由 [from] 一次性批量检测并缓存，
     * 供 [TileRequirement.RequiresFeature] 求值。
     */
    val features: Map<String, Boolean> = emptyMap(),
) {

    /**
     * 是否为第三方厂商定制系统（既非小米系也非类原生 AOSP，如 ColorOS / OneUI / OriginOS 等）。
     * 该类系统对 QS 磁贴机制有不同程度的修改，磁贴切换功能可能无法正常工作，
     * 调用方（OOBE 欢迎页）据此展示「可能不兼容」的软提示（仅提示，不拦截流程）。
     * 派生值而非构造字段：由 [isXiaomi] / [isAosp] 互补计算，不参与 equals / copy。
     */
    val isThirdPartyRom: Boolean
        get() = !isXiaomi && !isAosp

    /**
     * 求值器：判断设备是否满足一组磁贴需求（AND 语义）。
     * 空需求集合 = 所有设备可用。
     *
     * 卫星/风扇能力优先使用 [TileCapabilityFlags] 的调试覆盖值（便于调试工具页模拟），
     * 未覆盖时才回退到设备检测的真实能力 [hasSatellite]/[hasCoolingFan]；
     * prop 门控需求 [TileRequirement.RequiresProp] 查 [gatedProps]。
     *
     * @param requirements 磁贴声明的需求集合
     * @return 全部满足返回 true
     */
    fun satisfies(requirements: Set<TileRequirement>): Boolean {
        return requirements.all { requirement ->
            when (requirement) {
                TileRequirement.XIAOMI_ONLY -> isXiaomi
                TileRequirement.AOSP_ONLY -> !isXiaomi
                TileRequirement.TABLET_ONLY -> isTablet
                TileRequirement.MAX_SDK_36 -> sdkInt < 37
                TileRequirement.SATELLITE -> TileCapabilityFlags.satelliteOverride ?: hasSatellite
                TileRequirement.COOLING_FAN -> TileCapabilityFlags.coolingFanOverride ?: hasCoolingFan
                is TileRequirement.RequiresProp -> TileCapabilityFlags.propOverrides[requirement.key]
                    ?: gatedProps[requirement.key]
                    ?: true
                is TileRequirement.RequiresFeature -> TileCapabilityFlags.featureOverrides[requirement.feature]
                    ?: features[requirement.feature]
                    ?: true
            }
        }
    }

    companion object {
        /**
         * 支持主动散热风扇的机型代号白名单（对应 [Build.DEVICE]，统一小写比较）。
         *
         * 代号与机型对照：
         * - `prague` → REDMI K90 Max
         * - `warsaw` → REDMI K90 至尊版
         *
         * 小米系目前仅有这两款机型搭载了实体散热风扇，其余设备一律视为不支持。
         * 新增机型时只需在此追加代号，其余判定逻辑无需改动。
         */
        private val COOLING_FAN_DEVICE_CODENAMES = setOf("prague", "warsaw")

        /** 缓存，避免重复采集系统属性 */
        @Volatile
        private var cached: DeviceProfile? = null

        /**
         * 采集当前设备能力并缓存。
         * 使用 applicationContext，避免持有 Activity 引用。
         *
         * @param context Context（取 applicationContext 使用）
         * @return 设备能力快照
         */
        fun from(context: Context): DeviceProfile {
            cached?.let { return it }
            synchronized(this) {
                cached?.let { return it }
                val appContext = context.applicationContext
                val profile = DeviceProfile(
                    isXiaomi = DeviceOs.isMiui() || DeviceOs.isHyperOs(),
                    // 类原生 AOSP：DeviceOs.getOsName() 返回空字符串；
                    // 与 isXiaomi 互补，任何厂商定制系统（MIUI/HyperOS/ColorOS/OneUI 等）都会返回非空系统名
                    isAosp = DeviceOs.getOsName().isEmpty(),
                    isTablet = run {
                        val sizeMask = appContext.resources.configuration.screenLayout and
                            Configuration.SCREENLAYOUT_SIZE_MASK
                        sizeMask >= Configuration.SCREENLAYOUT_SIZE_LARGE
                    },
                    sdkInt = Build.VERSION.SDK_INT,
                    // TODO 占位：待实现 prop 检测后改为真实能力
                    hasSatellite = true,
                    // 散热风扇：仅有实体风扇的机型（REDMI K90 Max / K90 至尊版）支持，按机型代号白名单判定
                    hasCoolingFan = Build.DEVICE.lowercase() in COOLING_FAN_DEVICE_CODENAMES,
                    // 一次性批量读取全部受控 prop（prop 为只读属性，缓存安全）
                    gatedProps = TileRequirement.gatedPropKeys.associateWith { readBooleanProp(it) },
                    // 一次性批量检测全部受控硬件特性（PackageManager 检测开销小，缓存安全）
                    features = TileRequirement.gatedFeatureKeys.associateWith { feature ->
                        // 振动马达不能用 hasSystemFeature 可靠判断：不少 ROM 未声明
                        // "android.hardware.vibrator" feature 但设备实际有马达，会导致误判为不支持。
                        // 改用运行时 VibratorManager.defaultVibrator.hasVibrator() 检测，更准确
                        // （minSdk 33 >= VibratorManager 所需 API 31，可直接使用）。
                        if (feature == TileRequirement.FEATURE_VIBRATOR) {
                            val vibratorManager = appContext.getSystemService(VibratorManager::class.java)
                            vibratorManager?.defaultVibrator?.hasVibrator() ?: false
                        } else {
                            appContext.packageManager.hasSystemFeature(feature)
                        }
                    },
                )
                cached = profile
                return profile
            }
        }

        /**
         * 反射读取系统属性 `android.os.SystemProperties.get(key)`（隐藏 API，反射调用）。
         * 读取失败或受限时返回 null（调用方按「未定义」处理，不隐藏磁贴）。
         */
        private fun readProp(key: String): String? {
            return try {
                val clazz = Class.forName("android.os.SystemProperties")
                val method = clazz.getMethod("get", String::class.java)
                method.invoke(null, key) as? String
            } catch (_: Exception) {
                null
            }
        }

        /**
         * 解析布尔型 prop 能力。
         * 语义：「prop 明确为否定值（0/false/no）」→ 不支持；「true/1/其他非空」或「未定义」→ 支持。
         * 即只在设备明确声明不支持时隐藏磁贴，避免未定义该 prop 的设备（如 AOSP 单手模式）被误伤。
         */
        private fun readBooleanProp(key: String): Boolean {
            val value = readProp(key)?.trim()?.lowercase() ?: return true
            return value != "0" && value != "false" && value != "no"
        }
    }
}
