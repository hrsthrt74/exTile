package com.hrsthrt74.qstile.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 本地备份文件的信息摘要，用于在备份列表 UI 中展示。
 *
 * @param fileName 文件名（不含路径），如 "backup_20260815_143022.json"
 * @param createdAt 创建时间戳（毫秒）
 * @param expandedCount 展开配置中的磁贴数量
 * @param collapsedCount 收起配置中的磁贴数量
 * @param creativeLayerCount 备份中创造模式数据的层数（两层里有多少层非空）。
 *        0 表示这份备份不含创造模式数据——旧版本导出的备份就是这种，导入时只恢复普通配置。
 */
data class BackupInfo(
    val fileName: String,
    val createdAt: Long,
    val expandedCount: Int,
    val collapsedCount: Int,
    val creativeLayerCount: Int = 0
)

/**
 * 一份备份的完整内容。
 *
 * @param config 普通模式配置（展开 / 收起列表）
 * @param creative 创造模式配置；**null 表示该备份不含可用的创造模式数据**
 *        （旧版本备份没有该字段，或解析出来是开关关闭 + 两层皆空的空配置），
 *        恢复时应当完全不碰当前的创造模式配置，而不是把它重置成默认值。
 *
 * 普通模式只备份展开 / 收起两份列表，`isExpanded` 刻意不进备份（恢复时保持设备当前状态）。
 * 创造模式则连 [CreativeConfig.enabled] 与 `activeLayer` 一起存：它们决定「恢复后要不要
 * 把某一层写回系统」，见 `SettingsViewModel` 的恢复逻辑。
 */
data class BackupSnapshot(
    val config: TileConfig,
    val creative: CreativeConfig? = null
)

/**
 * 本地备份文件的管理仓库。
 *
 * 备份以 JSON 文件形式存储在应用内部目录 `files/backups/` 下，
 * 文件名格式为 `backup_yyyyMMdd_HHmmss.json`（自动生成时间戳）。
 * 支持多备份管理：保存、列出、加载、删除。
 *
 * ## 备份格式与前后兼容（重要）
 *
 * 顶层字段 `expandedTiles` / `collapsedTiles` **永远不变**，创造模式作为**新增**的
 * 可选字段 `creative` 附加：
 *
 * ```json
 * {
 *   "expandedTiles": ["wifi", "cell", "custom(...)"],
 *   "collapsedTiles": ["wifi", "cell"],
 *   "creative": {
 *     "enabled": true,
 *     "activeLayer": 2,
 *     "layers": [
 *       { "tiles": [...], "showTileLabels": true, "showDeviceCenter": false },
 *       { "tiles": [...], "showTileLabels": false, "showDeviceCenter": true }
 *     ]
 *   }
 * }
 * ```
 *
 * 两种方向都必须成立：
 * - **新版本读旧备份**：没有 `creative` 字段 → [BackupSnapshot.creative] 为 null，
 *   只恢复普通配置，当前创造模式配置原样保留（不能因为导入旧备份就把用户的两层清空）。
 * - **旧版本读新备份**：旧版本只用正则取顶层 `expandedTiles` / `collapsedTiles`，
 *   忽略多余的 `creative` 字段，仍能正常导入。因此新字段只能加在顶层，禁止改动既有字段的结构。
 */
object BackupRepository {

    /** 备份文件存储的子目录名 */
    private const val BACKUP_DIR = "backups"

    /** 备份文件名的时间戳格式 */
    private const val TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss"

    /** 备份文件的日期显示格式（中文友好） */
    private const val DATE_DISPLAY_FORMAT = "yyyy-MM-dd HH:mm:ss"

    // ==================== JSON 字段名 ====================

    private const val KEY_EXPANDED = "expandedTiles"
    private const val KEY_COLLAPSED = "collapsedTiles"
    private const val KEY_SYSUI_TILES = "currentSysuiTiles"

    /** 创造模式整块（可选字段，旧备份没有） */
    private const val KEY_CREATIVE = "creative"
    private const val KEY_CREATIVE_ENABLED = "enabled"
    private const val KEY_CREATIVE_ACTIVE_LAYER = "activeLayer"
    private const val KEY_CREATIVE_LAYERS = "layers"

    private const val KEY_LAYER_TILES = "tiles"
    private const val KEY_LAYER_SHOW_LABELS = "showTileLabels"
    private const val KEY_LAYER_SHOW_DEVICE_CENTER = "showDeviceCenter"

    /**
     * 获取备份文件存储目录，不存在时自动创建。
     * @param context Context
     * @return 备份目录的 [File] 对象
     */
    private fun getBackupDir(context: Context): File {
        val dir = File(context.filesDir, BACKUP_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 生成备份文件名：backup_yyyyMMdd_HHmmss.json
     * @return 文件名
     */
    private fun generateFileName(): String {
        val sdf = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.getDefault())
        return "backup_${sdf.format(Date())}.json"
    }

    // ==================== 序列化 / 反序列化 ====================

    /**
     * 生成备份 JSON 文本。
     *
     * 「复制到剪贴板」「另存为文件」与「保存到本地」共用这一份实现，避免三处格式漂移。
     *
     * @param expandedTiles 展开配置
     * @param collapsedTiles 收起配置
     * @param creative 创造模式配置；为 null（或空配置）时不写 `creative` 字段，
     *        保持与旧版格式一致，也避免「一份空配置」在恢复时清空用户现有的两层
     * @param currentSysuiTiles 当前系统 `sysui_qs_tiles` 原文，仅作为便于用户排查的信息字段；
     *        传 null 时不写（本地备份文件不需要）
     * @return 格式化后的 JSON 文本
     */
    fun buildBackupJson(
        expandedTiles: List<String>,
        collapsedTiles: List<String>,
        creative: CreativeConfig? = null,
        currentSysuiTiles: String? = null
    ): String {
        val json = JSONObject().apply {
            put(KEY_EXPANDED, JSONArray(expandedTiles))
            put(KEY_COLLAPSED, JSONArray(collapsedTiles))
            // 双保险：调用方即使直接传了默认 CreativeConfig()，也不会写出空的 creative
            if (creative != null && creative.hasData) put(KEY_CREATIVE, creativeToJson(creative))
            if (currentSysuiTiles != null) put(KEY_SYSUI_TILES, currentSysuiTiles)
        }
        return json.toString(2)
    }

    /**
     * 解析备份 JSON 文本。
     *
     * 三条恢复入口（本地备份 / 从文件导入 / 粘贴 JSON）全部走这里，保证行为一致。
     *
     * 兼容性：`creative` 字段缺失时 [BackupSnapshot.creative] 为 null；
     * `layers` 数组缺项、字段类型不对时逐项回退默认值，不因单点脏数据整份失败。
     *
     * @param json JSON 文本（允许首尾空白与 UTF-8 BOM）
     * @return 解析结果；`expandedTiles` 或 `collapsedTiles` 缺失/不是数组时返回 null
     */
    fun parseBackupJson(json: String): BackupSnapshot? {
        return try {
            // 部分编辑器/剪贴板会带上 BOM，JSONObject 不认，先清掉（前后各 trim 一次，
            // 覆盖「BOM 后面还跟空格」的写法）
            val cleaned = json.trim().removePrefix("\uFEFF").trim()
            val root = JSONObject(cleaned)
            // 两个核心字段必须都是数组：缺一个就当格式非法，
            // 绝不能当成空列表去覆盖用户现有配置
            val expanded = root.optJSONArray(KEY_EXPANDED)?.let(::parseStringList) ?: return null
            val collapsed = root.optJSONArray(KEY_COLLAPSED)?.let(::parseStringList) ?: return null
            BackupSnapshot(
                config = TileConfig(expandedTiles = expanded, collapsedTiles = collapsed),
                creative = parseCreative(root)
            )
        } catch (_: Exception) {
            null
        }
    }

    /** 创造模式配置 → JSON 对象（层数固定为 [CREATIVE_LAYER_COUNT]，越界层号自动收敛） */
    private fun creativeToJson(creative: CreativeConfig): JSONObject {
        val layers = JSONArray()
        for (layer in 1..CREATIVE_LAYER_COUNT) {
            val value = creative.layer(layer)
            layers.put(JSONObject().apply {
                put(KEY_LAYER_TILES, JSONArray(value.tiles))
                put(KEY_LAYER_SHOW_LABELS, value.showTileLabels)
                put(KEY_LAYER_SHOW_DEVICE_CENTER, value.showDeviceCenter)
            })
        }
        return JSONObject().apply {
            put(KEY_CREATIVE_ENABLED, creative.enabled)
            put(KEY_CREATIVE_ACTIVE_LAYER, creative.activeLayer)
            put(KEY_CREATIVE_LAYERS, layers)
        }
    }

    /**
     * JSON → 创造模式配置。
     * @return 没有 `creative` 字段、或解析出来是「空配置」（开关关着且两层都没磁贴）时返回 null——
     *        统一按「这份备份不含创造模式数据」处理，恢复时就不会去清空用户现有的两层。
     */
    private fun parseCreative(root: JSONObject): CreativeConfig? {
        val obj = root.optJSONObject(KEY_CREATIVE) ?: return null
        val layersJson = obj.optJSONArray(KEY_CREATIVE_LAYERS)
        // 固定按 CREATIVE_LAYER_COUNT 补足：备份里少一层也不会让上层索引越界
        val layers = List(CREATIVE_LAYER_COUNT) { index ->
            val layerJson = layersJson?.optJSONObject(index)
            CreativeLayer(
                tiles = layerJson?.optJSONArray(KEY_LAYER_TILES)?.let(::parseStringList)
                    ?: emptyList(),
                showTileLabels = layerJson?.optBoolean(KEY_LAYER_SHOW_LABELS, true) ?: true,
                showDeviceCenter = layerJson?.optBoolean(KEY_LAYER_SHOW_DEVICE_CENTER, false)
                    ?: false
            )
        }
        return CreativeConfig(
            enabled = obj.optBoolean(KEY_CREATIVE_ENABLED, false),
            activeLayer = obj.optInt(KEY_CREATIVE_ACTIVE_LAYER, 1)
                .coerceIn(1, CREATIVE_LAYER_COUNT),
            layers = layers
        ).takeIf { it.hasData }
    }

    /** JSONArray → 字符串列表（丢弃空条目，防止 ",," 之类的脏数据变成空磁贴） */
    private fun parseStringList(array: JSONArray): List<String> {
        return (0 until array.length()).mapNotNull { index ->
            array.optString(index).takeIf { it.isNotBlank() }
        }
    }

    // ==================== 文件操作 ====================

    /**
     * 保存备份文件到应用内部存储。
     *
     * @param context Context
     * @param expandedTiles 展开状态下的磁贴列表
     * @param collapsedTiles 收起状态下的磁贴列表
     * @param creative 创造模式配置；为 null 时备份不含创造模式数据
     * @return 保存后的文件名，失败返回 null
     */
    suspend fun saveBackup(
        context: Context,
        expandedTiles: List<String>,
        collapsedTiles: List<String>,
        creative: CreativeConfig? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val text = buildBackupJson(expandedTiles, collapsedTiles, creative)
            val fileName = generateFileName()
            val file = File(getBackupDir(context), fileName)
            file.writeText(text)
            fileName
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 获取所有备份文件的信息列表，按创建时间倒序排列（最新的在前）。
     *
     * 复用 [parseBackupJson] 提取计数：解析失败的文件直接跳过，
     * 不再为「读列表」单独维护一套字段读取逻辑。
     *
     * @param context Context
     * @return 备份信息列表
     */
    suspend fun getBackupList(context: Context): List<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val dir = getBackupDir(context)
            dir.listFiles()
                ?.filter { it.isFile && it.extension == "json" }
                ?.mapNotNull { file ->
                    try {
                        val snapshot = parseBackupJson(file.readText()) ?: return@mapNotNull null
                        BackupInfo(
                            fileName = file.name,
                            createdAt = file.lastModified(),
                            expandedCount = snapshot.config.expandedTiles.size,
                            collapsedCount = snapshot.config.collapsedTiles.size,
                            creativeLayerCount = snapshot.creative
                                ?.layers?.count { it.tiles.isNotEmpty() } ?: 0
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 加载指定备份文件的内容。
     *
     * @param context Context
     * @param fileName 备份文件名
     * @return 解析后的备份快照，文件不存在或解析失败返回 null
     */
    suspend fun loadBackup(context: Context, fileName: String): BackupSnapshot? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(getBackupDir(context), fileName)
                if (!file.exists()) return@withContext null
                parseBackupJson(file.readText())
            } catch (_: Exception) {
                null
            }
        }

    /**
     * 删除指定的备份文件。
     *
     * @param context Context
     * @param fileName 要删除的备份文件名
     * @return 是否删除成功
     */
    suspend fun deleteBackup(context: Context, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(getBackupDir(context), fileName)
            file.exists() && file.delete()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 将创建时间戳格式化为中文友好的显示字符串。
     * @param timestamp 毫秒时间戳
     * @return 格式化后的日期字符串，如 "2026-08-15 14:30:22"
     */
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat(DATE_DISPLAY_FORMAT, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
