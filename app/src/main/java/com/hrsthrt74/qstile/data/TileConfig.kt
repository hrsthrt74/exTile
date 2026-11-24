package com.hrsthrt74.qstile.data

data class TileConfig(
    val expandedTiles: List<String> = listOf("wifi", "bt", "cell", "airplane", "flashlight", "hotspot", TileCatalog.EXTILE_CUSTOM),
    val collapsedTiles: List<String> = listOf("wifi", "bt", "cell"),
    val isExpanded: Boolean = false
)

/** 创造模式的层数（固定两层，层号从 1 开始，与 VIA 的 Layer 概念对齐） */
const val CREATIVE_LAYER_COUNT = 2

/**
 * 创造模式中的「一层」。
 *
 * 与普通模式的 [TileConfig] 不同：这里没有「展开 / 收起」的概念，[tiles] 就是该层要写进
 * 系统的**完整** `sysui_qs_tiles` 列表——包含小米固定磁贴（wifi / cell / bt）、末尾的 `edit`、
 * 以及必须存在的 exTile 磁贴本身。因此写回系统时只需原样提交，不需要像普通模式那样派生。
 *
 * @param tiles 完整磁贴列表（可直接写入 sysui_qs_tiles）
 * @param showTileLabels 切换到本层时是否显示磁贴名字（true → wordless_mode=0，即关闭无字模式）
 * @param showDeviceCenter 切换到本层时是否显示融合设备中心（true → smart_device_control=1）
 */
data class CreativeLayer(
    val tiles: List<String> = emptyList(),
    val showTileLabels: Boolean = true,
    val showDeviceCenter: Boolean = false,
)

/**
 * 创造模式的整体配置（两套互相独立的布局）。
 *
 * 注意区分两个「层」：
 * - [activeLayer]：**持久化**的、系统当前生效的层，由点击 QS 磁贴切换；
 * - `TileConfigViewModel.editingLayer`：**内存态**的、编辑页当前 Tab 编辑的层，不写系统。
 *
 * @param enabled 创造模式开关
 * @param activeLayer 当前生效层（1 基）
 * @param layers 两层的配置，索引 0 = 层 1
 */
data class CreativeConfig(
    val enabled: Boolean = false,
    val activeLayer: Int = 1,
    val layers: List<CreativeLayer> = List(CREATIVE_LAYER_COUNT) { CreativeLayer() },
) {
    /**
     * 取指定层（1 基）的配置，越界时收敛到合法范围，避免脏数据导致崩溃。
     * @param index1Based 层号，1 或 2
     */
    fun layer(index1Based: Int): CreativeLayer =
        layers[(index1Based - 1).coerceIn(0, layers.lastIndex)]

    /**
     * 是否含有**实际可备份 / 可恢复**的数据：开关开着，或者任意一层有磁贴。
     *
     * 从未用过创造模式的用户，内存里也是一份 [CreativeConfig]（全默认值）。备份时若把这份
     * 空配置写进 `creative` 字段，恢复它就会把用户后来精心调好的两层清空——所以备份前
     * 必须先过这个判断，空配置按「不含创造模式数据」处理（与旧版本备份完全一致）。
     */
    val hasData: Boolean
        get() = enabled || layers.any { it.tiles.isNotEmpty() }
}
