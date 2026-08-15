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
 */
data class BackupInfo(
    val fileName: String,
    val createdAt: Long,
    val expandedCount: Int,
    val collapsedCount: Int
)

/**
 * 本地备份文件的管理仓库。
 *
 * 备份以 JSON 文件形式存储在应用内部目录 `files/backups/` 下，
 * 文件名格式为 `backup_yyyyMMdd_HHmmss.json`（自动生成时间戳）。
 * 支持多备份管理：保存、列出、加载、删除。
 */
object BackupRepository {

    /** 备份文件存储的子目录名 */
    private const val BACKUP_DIR = "backups"

    /** 备份文件名的时间戳格式 */
    private const val TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss"

    /** 备份文件的日期显示格式（中文友好） */
    private const val DATE_DISPLAY_FORMAT = "yyyy-MM-dd HH:mm:ss"

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

    /**
     * 保存备份文件到应用内部存储。
     *
     * @param context Context
     * @param expandedTiles 展开状态下的磁贴列表
     * @param collapsedTiles 收起状态下的磁贴列表
     * @return 保存后的文件名，失败返回 null
     */
    suspend fun saveBackup(
        context: Context,
        expandedTiles: List<String>,
        collapsedTiles: List<String>
    ): String? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("expandedTiles", JSONArray(expandedTiles))
                put("collapsedTiles", JSONArray(collapsedTiles))
            }
            val fileName = generateFileName()
            val file = File(getBackupDir(context), fileName)
            file.writeText(json.toString(2))
            fileName
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 获取所有备份文件的信息列表，按创建时间倒序排列（最新的在前）。
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
                        val json = JSONObject(file.readText())
                        val expanded = json.optJSONArray("expandedTiles")
                        val collapsed = json.optJSONArray("collapsedTiles")
                        BackupInfo(
                            fileName = file.name,
                            createdAt = file.lastModified(),
                            expandedCount = expanded?.length() ?: 0,
                            collapsedCount = collapsed?.length() ?: 0
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
     * 加载指定备份文件的内容并解析为 [TileConfig]。
     *
     * @param context Context
     * @param fileName 备份文件名
     * @return 解析后的磁贴配置，文件不存在或解析失败返回 null
     */
    suspend fun loadBackup(context: Context, fileName: String): TileConfig? = withContext(Dispatchers.IO) {
        try {
            val file = File(getBackupDir(context), fileName)
            if (!file.exists()) return@withContext null
            val json = JSONObject(file.readText())
            val expanded = json.optJSONArray("expandedTiles")
                ?.let { arr -> (0 until arr.length()).map { arr.getString(it) } }
                ?: return@withContext null
            val collapsed = json.optJSONArray("collapsedTiles")
                ?.let { arr -> (0 until arr.length()).map { arr.getString(it) } }
                ?: return@withContext null
            TileConfig(expandedTiles = expanded, collapsedTiles = collapsed)
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
