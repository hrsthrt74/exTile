package com.hrsthrt74.qstile.data

data class TileConfig(
    val expandedTiles: List<String> = listOf("wifi", "bt", "cell", "airplane", "flashlight", "hotspot", TileCatalog.EXTILE_CUSTOM),
    val collapsedTiles: List<String> = listOf("wifi", "bt", "cell"),
    val isExpanded: Boolean = false
)
