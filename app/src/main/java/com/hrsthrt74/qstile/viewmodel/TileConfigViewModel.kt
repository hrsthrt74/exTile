package com.hrsthrt74.qstile.viewmodel

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.CustomTileUtils
import com.hrsthrt74.qstile.data.DeviceProfile
import com.hrsthrt74.qstile.data.TileCatalog
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * TileConfigScreen 的 ViewModel，负责管理磁贴配置页面的状态和业务逻辑。
 *
 * 主要职责：
 * - 管理磁贴配置数据（config）
 * - 处理磁贴的增删改查操作
 * - 管理撤销/重做快照
 * - 加载第三方磁贴服务列表
 * - 提供派生状态（gridTiles、outerTiles、innerTiles 等）
 */
class TileConfigViewModel : ViewModel() {

    // ==================== 核心状态 ====================

    /** 当前磁贴配置 */
    var config by mutableStateOf(TileConfig())
        private set

    /** 配置是否已从 DataStore 加载完成 */
    var isConfigLoaded by mutableStateOf(false)
        private set

    // ==================== 撤销相关状态 ====================

    /** 撤销快照：记录最近一次操作前的网格磁贴列表 */
    var undoSnapshot by mutableStateOf<List<String>?>(null)
        private set

    /** 拖拽起点快照：拖拽手势开始时记录，松手时合并为一步 */
    var dragStartSnapshot by mutableStateOf<List<String>?>(null)
        private set

    // ==================== 搜索和输入状态 ====================

    /** 「添加磁贴」Sheet 的搜索关键字 */
    var addSearchQuery by mutableStateOf("")

    /** 「添加第三方磁贴」Sheet 的搜索关键字 */
    var customSearchQuery by mutableStateOf("")

    /** 第三方磁贴的自定义值输入 */
    var customTileValue by mutableStateOf("")

    // ==================== 第三方磁贴服务 ====================

    /** 第三方磁贴服务列表（含图标），null 表示尚未加载 */
    var allTileServices by mutableStateOf<List<CustomTileUtils.QSTileServiceInfo>?>(null)
        private set

    // ==================== 磁贴菜单状态 ====================

    /** 是否显示磁贴操作菜单 */
    var showTileMenu by mutableStateOf(false)

    /** 当前选中要操作的磁贴 */
    var selectedTileForMenu by mutableStateOf<String?>(null)

    // ==================== 设备信息（延迟初始化） ====================

    /** 设备能力快照 */
    private var _profile: DeviceProfile? = null

    /** 获取设备信息，需要先调用 initProfile 初始化 */
    val profile: DeviceProfile
        get() = _profile ?: throw IllegalStateException("Profile not initialized. Call initProfile() first.")

    /** 小米设备特化：需要固定在上方的磁贴值 */
    var fixedTileValues by mutableStateOf<List<String>>(emptyList())
        private set

    // ==================== 派生状态 ====================

    /** 单一数据源（过滤固定卡片和 edit），含 exTile */
    val gridTiles: List<String>
        get() = config.expandedTiles.filter { it !in fixedTileValues && it != "edit" }

    /** exTile 在 gridTiles 中的索引 */
    private val exTileIndex: Int
        get() = gridTiles.indexOf(TileCatalog.EXTILE_CUSTOM)

    /** 框外磁贴 = exTile 及其之前 */
    val outerTiles: List<String>
        get() {
            val idx = exTileIndex
            return if (idx >= 0) gridTiles.take(idx + 1) else gridTiles
        }

    /** 框内磁贴 = exTile 之后 */
    val innerTiles: List<String>
        get() {
            val idx = exTileIndex
            return if (idx >= 0) gridTiles.drop(idx + 1) else emptyList()
        }

    /** 是否存在框内区域（只要存在 exTile 就显示） */
    val hasInner: Boolean
        get() = exTileIndex >= 0

    /** 框内磁贴 key 集合（overlay 定位虚线框用） */
    val innerKeys: Set<String>
        get() = innerTiles.toSet()

    /** 标题行标记对象（用于 renderList 中区分标题行和磁贴） */
    private val headerMarker = Any()

    /** 统一渲染列表：框外磁贴 + 标题行 + 框内磁贴 */
    val renderList: List<Any>
        get() = buildList {
            addAll(outerTiles)
            if (hasInner) add(headerMarker)
            addAll(innerTiles)
        }

    /** 标题行的固定 key */
    val headerKey = "inner-section-header"

    /** 固定卡片 item 数（网格中占 1 个不可拖拽 item） */
    val fixedItemCount: Int
        get() = if (fixedTileValues.isNotEmpty()) 1 else 0

    // ==================== 初始化方法 ====================

    /**
     * 初始化设备信息（需要在 Composable 中调用，因为需要 Context）
     */
    fun initProfile(context: Context) {
        if (_profile == null) {
            _profile = DeviceProfile.from(context)
            val profile = _profile!!
            val isXiaomi = profile.isXiaomi

            // 判断是否为平板
            val configuration = context.resources.configuration
            val screenSize = configuration.screenLayout
            val sizeMask = screenSize and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
            val isTablet = sizeMask >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE

            fixedTileValues = if (isXiaomi) {
                if (isTablet) listOf("wifi", "bt") else listOf("wifi", "cell")
            } else {
                emptyList()
            }
        }
    }

    /**
     * 开始监听 DataStore 配置变更
     */
    fun startConfigObservation(context: Context) {
        viewModelScope.launch {
            ConfigRepository.getConfigFlow(context).collect { newConfig ->
                config = newConfig
                // 首次收到持久化配置后再放行渲染
                if (!isConfigLoaded) {
                    isConfigLoaded = true
                }
            }
        }
    }

    /**
     * 加载第三方磁贴服务列表（在后台线程执行）
     */
    fun loadCustomTileServices(context: Context) {
        if (allTileServices != null) return // 已加载，跳过

        viewModelScope.launch {
            allTileServices = withContext(Dispatchers.IO) {
                CustomTileUtils.getAllQSTileServicesWithIcon(context)
            }
        }
    }

    // ==================== 业务逻辑方法 ====================

    /**
     * 通用保存：直接写入给定的完整配置（恢复默认设置使用）
     */
    fun updateConfig(newConfig: TileConfig, context: Context) {
        config = newConfig
        viewModelScope.launch {
            ConfigRepository.saveExpandedTiles(context, newConfig.expandedTiles)
            ConfigRepository.saveCollapsedTiles(context, newConfig.collapsedTiles)
        }
    }

    /**
     * 保存磁贴列表（一页式单一数据源，含 exTile），并派生保存 expanded/collapsed 两个列表
     */
    fun updateGridTiles(newGridTiles: List<String>, context: Context) {
        val isXiaomi = _profile?.isXiaomi ?: false
        val exTileIdx = newGridTiles.indexOf(TileCatalog.EXTILE_CUSTOM)
        val outerTiles = if (exTileIdx >= 0) newGridTiles.take(exTileIdx + 1) else newGridTiles
        val expanded = if (isXiaomi) fixedTileValues + newGridTiles + listOf("edit") else newGridTiles
        val collapsed = if (isXiaomi) fixedTileValues + outerTiles + listOf("edit") else outerTiles
        updateConfig(config.copy(expandedTiles = expanded, collapsedTiles = collapsed), context)
    }

    /**
     * 撤销上一步操作
     */
    fun undo(context: Context) {
        undoSnapshot?.let { updateGridTiles(it, context) }
        undoSnapshot = null
    }

    /**
     * 从系统导入磁贴：读取当前系统 QS 的 sysui_qs_tiles 完整列表，
     * 保存为展开状态（与设置页「从系统导入磁贴」行为一致）。
     * @return 读取成功且列表非空返回 true，否则返回 false（如 Shizuku 不可用）
     */
    suspend fun importFromSystem(context: Context): Boolean {
        val tiles = SecureSettingsHelper.getCurrentTiles(context)
        if (tiles.isNotEmpty()) {
            ConfigRepository.saveExpandedTiles(context, tiles)
            config = config.copy(expandedTiles = tiles)
            return true
        }
        return false
    }

    /**
     * 记录拖拽开始快照
     */
    fun onDragStart() {
        dragStartSnapshot = gridTiles
    }

    /**
     * 记录拖拽结束快照（如果确实发生了移动）
     */
    fun onDragEnd() {
        val start = dragStartSnapshot
        if (start != null && start != gridTiles) {
            undoSnapshot = start
        }
        dragStartSnapshot = null
    }

    /**
     * 添加磁贴到网格末尾
     */
    fun addTile(tileValue: String, context: Context) {
        val currentGridTiles = gridTiles
        undoSnapshot = currentGridTiles
        updateGridTiles(currentGridTiles + tileValue, context)
    }

    /**
     * 移动磁贴到顶端
     */
    fun moveTileToTop(tile: String, context: Context) {
        val currentGridTiles = gridTiles
        val tileIndex = currentGridTiles.indexOf(tile)
        if (tileIndex <= 0) return

        undoSnapshot = currentGridTiles
        val newList = currentGridTiles.toMutableList().apply {
            removeAt(tileIndex)
            add(0, tile)
        }
        updateGridTiles(newList, context)
    }

    /**
     * 移动磁贴到底端
     */
    fun moveTileToBottom(tile: String, context: Context) {
        val currentGridTiles = gridTiles
        val tileIndex = currentGridTiles.indexOf(tile)
        if (tileIndex < 0 || tileIndex >= currentGridTiles.size - 1) return

        undoSnapshot = currentGridTiles
        val newList = currentGridTiles.toMutableList().apply {
            removeAt(tileIndex)
            add(currentGridTiles.size - 1, tile)
        }
        updateGridTiles(newList, context)
    }

    /**
     * 删除磁贴
     */
    fun deleteTile(tile: String, context: Context) {
        val currentGridTiles = gridTiles
        undoSnapshot = currentGridTiles
        val newList = currentGridTiles.toMutableList().apply {
            remove(tile)
        }
        updateGridTiles(newList, context)
    }

    /**
     * 处理拖拽排序（从 fromIndex 到 toIndex）
     */
    fun onDragMove(fromIndex: Int, toIndex: Int, context: Context) {
        val currentGridTiles = gridTiles
        if (fromIndex < 0 || fromIndex >= currentGridTiles.size ||
            toIndex < 0 || toIndex > currentGridTiles.size ||
            fromIndex == toIndex
        ) {
            return
        }

        val newList = currentGridTiles.toMutableList().apply {
            val insertAt = if (toIndex == currentGridTiles.size) currentGridTiles.size - 1 else toIndex
            add(insertAt, removeAt(fromIndex))
        }
        updateGridTiles(newList, context)
    }

    /**
     * 获取 gridToData 映射（用于拖拽索引转换）
     * @return 数据索引，-1 表示无效
     */
    fun gridToData(gridIndex: Int): Int {
        val ri = gridIndex - fixedItemCount
        if (ri < 0) return -1
        if (!hasInner) return ri
        // 标题行：空框时作为落点
        if (ri == outerTiles.size) return if (innerTiles.isEmpty()) gridTiles.size else -1
        // 框内磁贴（跳过标题行）
        return if (ri > outerTiles.size) ri - 1 else ri
    }

    /**
     * 清空搜索状态
     */
    fun clearAddSearchQuery() {
        addSearchQuery = ""
    }

    fun clearCustomSearchQuery() {
        customSearchQuery = ""
    }

    fun clearCustomTileValue() {
        customTileValue = ""
    }

    /**
     * 重置菜单状态
     */
    fun dismissTileMenu() {
        showTileMenu = false
    }
}
