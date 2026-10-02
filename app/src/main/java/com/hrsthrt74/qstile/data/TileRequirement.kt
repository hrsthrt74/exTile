package com.hrsthrt74.qstile.data

import androidx.annotation.StringRes
import com.hrsthrt74.qstile.R

/**
 * 磁贴可用性需求（flag 式约束）。
 *
 * 每个磁贴（[TileCatalog.TileInfo]）通过 [requirements] 声明一组需求，语义为「全部满足」才可用（AND 关系）。
 * 空集合表示所有设备可用。
 *
 * 与 [DeviceProfile]（设备能力快照）配合：`DeviceProfile.satisfies(requirements)` 统一求值，
 * 新增约束维度只需增加子类型并在求值器中补一个分支，无需改动磁贴清单之外的过滤逻辑。
 */
sealed interface TileRequirement {

    /** 仅小米系系统（MIUI / HyperOS）可用 */
    data object XIAOMI_ONLY : TileRequirement

    /** 仅原生 Android（非小米系）可用 */
    data object AOSP_ONLY : TileRequirement

    /** 仅平板设备可用（依据「编辑磁贴固定末尾」同一套 isTablet 判定，见 [DeviceProfile]） */
    data object TABLET_ONLY : TileRequirement

    /**
     * 仅 Android 16 (API 36) 及以下可用。
     * 用于替代原先对 internet 磁贴的硬编码特判 `SDK_INT < 37`。
     */
    data object MAX_SDK_36 : TileRequirement

    /**
     * 需要设备支持卫星通讯。
     * TODO 占位：恒为 true（保持现状可见性），待 prop 检测实现后收紧。
     */
    data object SATELLITE : TileRequirement

    /**
     * 需要设备带散热风扇（如部分小米旗舰机型）。
     * TODO 占位：恒为 true（保持现状可见性），待 prop 检测实现后收紧。
     */
    data object COOLING_FAN : TileRequirement

    /**
     * 需要系统属性（prop）声明支持对应功能。
     *
     * prop 读取语义（见 [DeviceProfile]）：「prop 明确为 false/0」时视为不支持；「true/1」或「prop 未定义」时视为支持。
     * 即仅在小米等定义了该 prop 且为 false 的机型上隐藏，避免误伤未定义该 prop 的设备（如 AOSP 的单手模式）。
     *
     * @param key 系统属性名，key 常量见 [TileRequirement.PROP_*]
     */
    data class RequiresProp(val key: String) : TileRequirement

    /**
     * 需要设备具备某硬件特性（通过 [android.content.pm.PackageManager.hasSystemFeature] 检测）。
     *
     * 语义：`hasSystemFeature` 返回 false（明确不支持，如平板无闪光灯/无震动马达）→ 隐藏磁贴；
     * true 或检测异常（map 无记录）→ 视为支持，避免误伤。
     *
     * @param feature 系统特性字符串，常量见 [TileRequirement.FEATURE_*]
     */
    data class RequiresFeature(val feature: String) : TileRequirement

    companion object {
        // ===== prop 门控的磁贴（小米系统属性，控制对应磁贴显隐）=====

        /** 实时字幕（aisubtitles）是否支持的 prop */
        const val PROP_AI_SUBTITLES = "persist.sys.translate.state.supportAiSubtitles"

        /** 对话翻译（voicetrans）是否支持的 prop */
        const val PROP_VOICE_TRANS = "persist.sys.translate.state.supportVoiceTrans"

        /** 单手模式（onehanded）是否支持的 prop */
        const val PROP_ONE_HANDED = "ro.support_one_handed_mode"

        /**
         * 全部受 prop 控制的 prop key 注册表。
         * [DeviceProfile.from] 一次性批量读取并缓存；后续挖到新 prop 时，
         * 只需在此注册表加一行 + 在磁贴清单声明对应 [RequiresProp] 需求，调试工具页的开关会自动出现。
         */
        val gatedPropKeys: List<String> = listOf(
            PROP_AI_SUBTITLES,
            PROP_VOICE_TRANS,
            PROP_ONE_HANDED,
        )

        /**
         * prop key → 磁贴显示名资源 映射，用于调试工具页开关标题。
         * 持有 @StringRes 而非字面量，支持多语言；消费方在 Composable 上下文解析。
         * 新增 prop 时需同步补充，保证与 [gatedPropKeys] 一一对应。
         */
        val gatedPropLabels: Map<String, Int> = mapOf(
            PROP_AI_SUBTITLES to R.string.debug_label_ai_subtitles,
            PROP_VOICE_TRANS to R.string.debug_label_voice_trans,
            PROP_ONE_HANDED to R.string.debug_label_one_handed,
        )

        // ===== 硬件特性门控的磁贴（PackageManager.hasSystemFeature 检测）=====

        /** NFC（nfc）磁贴需要的硬件特性 */
        const val FEATURE_NFC = "android.hardware.nfc"

        /** 自动亮度（autobrightness）磁贴需要的硬件特性：环境光传感器 */
        const val FEATURE_AMBIENT_LIGHT = "android.hardware.sensor.light"

        /** 振动（vibrate）磁贴需要的硬件特性：振动马达（平板通常没有） */
        const val FEATURE_VIBRATOR = "android.hardware.vibrator"

        /** 手电筒（flashlight）磁贴需要的硬件特性：闪光灯（平板通常没有） */
        const val FEATURE_CAMERA_FLASH = "android.hardware.camera.flash"

        /** 移动数据（cell）磁贴需要的硬件特性：蜂窝网（WiFi 平板通常没有） */
        const val FEATURE_TELEPHONY = "android.hardware.telephony"

        /** 相机权限（cameratoggle）磁贴需要的硬件特性 */
        const val FEATURE_CAMERA = "android.hardware.camera"

        /**
         * 全部受硬件特性门控的 feature 注册表。
         * [DeviceProfile.from] 一次性批量检测并缓存；新增 feature 时在此注册表加一行 +
         * 在磁贴清单声明对应 [RequiresFeature] 需求，调试工具页的开关会自动出现。
         */
        val gatedFeatureKeys: List<String> = listOf(
            FEATURE_NFC,
            FEATURE_AMBIENT_LIGHT,
            FEATURE_VIBRATOR,
            FEATURE_CAMERA_FLASH,
            FEATURE_TELEPHONY,
            FEATURE_CAMERA,
        )

        /**
         * feature → 磁贴显示名资源 映射，用于调试工具页开关标题。
         * 持有 @StringRes 而非字面量，支持多语言；消费方在 Composable 上下文解析。
         * 新增 feature 时需同步补充，保证与 [gatedFeatureKeys] 一一对应。
         */
        val gatedFeatureLabels: Map<String, Int> = mapOf(
            FEATURE_NFC to R.string.debug_label_nfc,
            FEATURE_AMBIENT_LIGHT to R.string.debug_label_auto_brightness,
            FEATURE_VIBRATOR to R.string.debug_label_vibrate,
            FEATURE_CAMERA_FLASH to R.string.debug_label_camera_flash,
            FEATURE_TELEPHONY to R.string.debug_label_telephony,
            FEATURE_CAMERA to R.string.debug_label_camera,
        )
    }
}
