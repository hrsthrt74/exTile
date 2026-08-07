package com.hrsthrt74.qstile.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap

/**
 * 设备能力 flag 调试开关。
 *
 * 用于「调试工具」页手动模拟设备能力，测试依赖该能力的磁贴的可见性。
 * 仅本次运行生效（内存态），重启应用恢复默认。
 *
 * 语义：null = 不覆盖（跟随设备真实能力 / 占位默认），true / false = 强制模拟开启 / 关闭。
 * 求值发生在 [DeviceProfile.satisfies]，调试覆盖优先于设备检测的真实能力；
 * 使用 Compose State，使依赖方（如磁贴配置页）能感知切换并刷新可用磁贴列表。
 */
object TileCapabilityFlags {

    /** 卫星通讯能力覆盖开关（null = 不覆盖，跟随 [DeviceProfile.hasSatellite]） */
    var satelliteOverride by mutableStateOf<Boolean?>(null)

    /** 散热风扇能力覆盖开关（null = 不覆盖，跟随 [DeviceProfile.hasCoolingFan]） */
    var coolingFanOverride by mutableStateOf<Boolean?>(null)

    /**
     * prop 门控能力覆盖表：key 为系统属性名（见 [TileRequirement.PROP_*]），value 为强制模拟值。
     * 仅存「已覆盖」的 key；未覆盖时回退到设备真实 prop 读取值（[DeviceProfile.gatedProps]）。
     * 使用 SnapshotStateMap，遍历/读取时会被 Compose 订阅，改动触发依赖方重组。
     */
    val propOverrides: SnapshotStateMap<String, Boolean> = mutableStateMapOf()

    /**
     * 硬件特性门控覆盖表：key 为系统特性字符串（见 [TileRequirement.FEATURE_*]），value 为强制模拟值。
     * 仅存「已覆盖」的 key；未覆盖时回退到设备真实 `hasSystemFeature` 检测值（[DeviceProfile.features]）。
     * 使用 SnapshotStateMap，遍历/读取时会被 Compose 订阅，改动触发依赖方重组。
     */
    val featureOverrides: SnapshotStateMap<String, Boolean> = mutableStateMapOf()
}
