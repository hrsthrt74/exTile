package com.hrsthrt74.qstile.viewmodel

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrsthrt74.qstile.data.CREATIVE_LAYER_COUNT
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.CreativeConfig
import com.hrsthrt74.qstile.data.CreativeLayer
import com.hrsthrt74.qstile.data.CustomTileUtils
import com.hrsthrt74.qstile.data.DeviceProfile
import com.hrsthrt74.qstile.data.TileCatalog
import com.hrsthrt74.qstile.data.TileConfig
import com.hrsthrt74.qstile.shizuku.SecureSettingsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 「切换到 Release / Debug」操作的结果，供界面提示用户 */
enum class VariantSwitchResult { SUCCESS, FAILED, NO_TILE }

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

    // ==================== 创造模式状态 ====================

    /** 创造模式配置（两套独立布局 + 每层开关） */
    var creativeConfig by mutableStateOf(CreativeConfig())
        private set

    /**
     * 编辑页当前 Tab 在编辑哪一层（1 基）。
     *
     * 纯内存态，**不持久化**，也**不写系统**——它只是「我在改哪一层」，
     * 与系统当前生效的 [CreativeConfig.activeLayer] 是两个概念。
     * 切换 Tab 不会触发 sysui_qs_tiles 写入，只有点 QS 磁贴才真正切层。
     */
    var editingLayer by mutableIntStateOf(1)
        private set

    /** 是否处于创造模式（外部 UI 全部据此分叉） */
    val isCreativeMode: Boolean
        get() = creativeConfig.enabled

    /** 当前正在编辑的层的配置 */
    val editingLayerConfig: CreativeLayer
        get() = creativeConfig.layer(editingLayer)

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

    /**
     * 是否正在拖拽排序。
     *
     * 供顶栏的「层」Tab 禁用点击：拖拽过程中切层会整体替换列表，把 Reorderable 的内部
     * 拖拽状态打断成半截（松手事件落在新列表上）。
     */
    var isDragging by mutableStateOf(false)

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

    // ==================== 变体切换菜单项状态 ====================

    /**
     * 「切换到 Release / Debug」的目标包名（由系统配置里当前的 exTile 变体推出）。
     * null = 尚未检测；读取系统配置失败时也会给出兜底目标，保证菜单项显示但禁用。
     */
    var variantSwitchTarget by mutableStateOf<String?>(null)
        private set

    /** 变体切换菜单项是否可用：读取系统配置失败时为 false（菜单项禁用而非隐藏） */
    var variantSwitchEnabled by mutableStateOf(false)
        private set

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

    /** 单一数据源（过滤固定卡片和 edit），含 exTile；创造模式下取当前编辑层 */
    val gridTiles: List<String>
        get() {
            val source = if (isCreativeMode) editingLayerConfig.tiles else config.expandedTiles
            return source.filter { it !in fixedTileValues && it != "edit" }
        }

    /** exTile 在 gridTiles 中的索引 */
    private val exTileIndex: Int
        // 规范化比较：debug 变体包名带 .debug 后缀，且历史配置里可能是相对类名写法
        get() = gridTiles.indexOfFirst { CustomTileUtils.isExTileSpec(it) }

    /** 框外磁贴 = exTile 及其之前 */
    val outerTiles: List<String>
        get() {
            if (!hasInner) return gridTiles
            val idx = exTileIndex
            return if (idx >= 0) gridTiles.take(idx + 1) else gridTiles
        }

    /** 框内磁贴 = exTile 之后 */
    val innerTiles: List<String>
        get() {
            if (!hasInner) return emptyList()
            val idx = exTileIndex
            return if (idx >= 0) gridTiles.drop(idx + 1) else emptyList()
        }

    /**
     * 是否存在框内区域（只要存在 exTile 就显示）。
     * 创造模式没有「展开」的概念，因此恒为 false：不画虚线框、不插入标题行。
     */
    val hasInner: Boolean
        get() = !isCreativeMode && exTileIndex >= 0

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

    /**
     * 当前编辑目标的**完整**磁贴集合（含小米固定磁贴与 edit）。
     *
     * 供「添加磁贴」Sheet 判断某项是否已存在：不能直接用 [gridTiles]（它过滤掉了固定磁贴，
     * 会让 wifi/cell 变成可重复添加），也不能直接用 config.expandedTiles（创造模式下会错层）。
     */
    val currentTileSet: Set<String>
        get() = (if (isCreativeMode) editingLayerConfig.tiles else config.expandedTiles).toSet()

    // ==================== 初始化方法 ====================

    /**
     * 初始化设备信息（需要在 Composable 中调用，因为需要 Context）
     */
    fun initProfile(context: Context) {
        if (_profile == null) {
            val profile = DeviceProfile.from(context)
            _profile = profile
            // 先按默认预设（WLAN + 蓝牙）算一遍固定磁贴，保证首帧渲染时 gridTiles 的过滤集合已就绪；
            // 用户实际选择过的预设由 startConfigObservation 的观察流稍后覆盖。
            fixedTileValues = ConfigRepository.resolveFixedTiles(
                isXiaomi = profile.isXiaomi,
                isTablet = profile.isTablet,
                preset = ConfigRepository.TabletFixedTiles.WIFI_BT,
            )
        }
    }

    /**
     * 开始监听 DataStore 配置变更（普通配置 + 创造模式配置 + 平板固定磁贴预设三条流）。
     *
     * 注意 DataStore 的固有行为：**任意 key 写入都会让所有流重新发射**。
     * 这里靠两点保证不出问题：
     * 1. [TileConfig] / [CreativeConfig] 是 data class，值没变时 `mutableStateOf` 的结构化相等
     *    会拦掉重组（所以绝对不要换成 `neverEqualPolicy`）；
     * 2. [editingLayer] 只在「首次加载 / 模式切换」时对齐 activeLayer，普通写入不重排 Tab——
     *    否则用户在层 2 改个开关，Tab 会被弹回层 1。
     *
     * 第三条流（固定磁贴预设）也在这里消费：设置页切换预设后，编辑页打开着也能立刻更新
     * [fixedTileValues]，网格与固定卡片随之刷新，不需要退出重进。
     */
    fun startConfigObservation(context: Context) {
        viewModelScope.launch {
            combine(
                ConfigRepository.getConfigFlow(context),
                ConfigRepository.getCreativeConfigFlow(context),
                ConfigRepository.getTabletFixedTilesFlow(context),
            ) { normal, creative, fixedPreset -> Triple(normal, creative, fixedPreset) }
                .collect { (normal, creative, fixedPreset) ->
                    val wasLoaded = isConfigLoaded
                    val prevEnabled = creativeConfig.enabled
                    config = normal
                    creativeConfig = creative
                    // 固定磁贴跟着预设走（手机与非小米设备由 resolveFixedTiles 兜底）
                    fixedTileValues = ConfigRepository.resolveFixedTiles(
                        isXiaomi = profile.isXiaomi,
                        isTablet = profile.isTablet,
                        preset = fixedPreset,
                    )

                    if (!wasLoaded || prevEnabled != creative.enabled) {
                        // 首次加载 / 模式切换：编辑 Tab 对齐系统当前生效层
                        editingLayer = if (creative.enabled) creative.activeLayer else 1
                        // 快照记录的是「某一层」的磁贴列表，模式切换后语义失效，必须丢弃；
                        // 菜单选中项也可能指向已经不存在的磁贴
                        undoSnapshot = null
                        dragStartSnapshot = null
                        showTileMenu = false
                        selectedTileForMenu = null
                    }

                    // 首次收到持久化配置后再放行渲染
                    if (!wasLoaded) {
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
     * 保存磁贴列表（一页式单一数据源，含 exTile），并派生保存 expanded/collapsed 两个列表。
     *
     * 创造模式下走完全独立的分支：整份列表直接写进**当前编辑的层**，
     * 绝不触碰普通模式的 expanded/collapsed/is_expanded（否则退出创造模式后普通布局被污染）。
     */
    fun updateGridTiles(newGridTiles: List<String>, context: Context) {
        val isXiaomi = _profile?.isXiaomi ?: false

        if (creativeConfig.enabled) {
            // 兜底：任何路径都不允许层里失去**当前变体**的 exTile（它是唯一的切层入口）。
            // 注意不能用 isAnyExTileSpec：另一个 Release/Debug 变体的磁贴属于另一个应用、
            // 另一份 DataStore，点它不会切本应用的层，只应被当作普通磁贴（可删）。
            val safe = if (newGridTiles.any { CustomTileUtils.isExTileSpec(it) }) {
                newGridTiles
            } else {
                ConfigRepository.ensureExTile(newGridTiles)
            }
            // 重新拼装完整列表。gridTiles 已滤掉固定磁贴与 "edit"，这里按该层原有的结构补回：
            // - 小米设备：固定磁贴必须在最前、edit 在最后（系统约定，FixedTilesRow 也按此渲染）；
            // - 非小米设备：不强制 edit，但该层原本带 edit 时必须保留——层是「完整列表」，
            //   丢掉就原样写回系统，编辑一次后控制中心里的编辑入口会永久消失。
            val hadEdit = editingLayerConfig.tiles.contains("edit")
            val full = buildList {
                if (isXiaomi) addAll(fixedTileValues)
                addAll(safe)
                if (isXiaomi || hadEdit) add("edit")
            }
            // 内存态先同步（与普通模式的 updateConfig 一致），再写 DataStore。
            // 只写库要等一次流回流，期间 gridTiles / currentTileSet 仍是旧值：
            // - onDragEnd 用「拖拽快照 != gridTiles」判断是否记录撤销，读到旧值会把整次拖拽判成没动；
            // - 添加磁贴 Sheet 的去重集合 currentTileSet 也会慢一拍，能重复添加同一个磁贴。
            updateEditingLayerInMemory { it.copy(tiles = full) }
            viewModelScope.launch {
                ConfigRepository.saveCreativeLayerTiles(context, editingLayer, full)
            }
            return
        }

        // 规范化比较：避免 debug 变体或旧版相对类名 spec 导致锚点定位失败
        val exTileIdx = newGridTiles.indexOfFirst { CustomTileUtils.isExTileSpec(it) }
        val outerTiles = if (exTileIdx >= 0) newGridTiles.take(exTileIdx + 1) else newGridTiles
        val expanded = if (isXiaomi) fixedTileValues + newGridTiles + listOf("edit") else newGridTiles
        val collapsed = if (isXiaomi) fixedTileValues + outerTiles + listOf("edit") else outerTiles
        updateConfig(config.copy(expandedTiles = expanded, collapsedTiles = collapsed), context)
    }

    /**
     * 由系统磁贴列表推出「切换到 Release / Debug」的目标包名。
     *
     * 目标取**系统配置里当前 exTile 变体**的相反版本，而不是当前 app 的相反版本：
     * 用户可能已经从另一个变体切换过，菜单文案必须对应系统里真实存在的那个磁贴。
     * 两个变体的 spec 同时存在时以第一个为准；系统里没有 exTile 时兜底为当前 app 的相对版本。
     */
    private fun targetPackageFor(tiles: List<String>): String {
        val currentPackage = tiles.firstOrNull { CustomTileUtils.isAnyExTileSpec(it) }
            ?.let { CustomTileUtils.parseCustomComponent(it)?.packageName }
        return when (currentPackage) {
            CustomTileUtils.DEBUG_PACKAGE -> CustomTileUtils.RELEASE_PACKAGE
            CustomTileUtils.RELEASE_PACKAGE -> CustomTileUtils.DEBUG_PACKAGE
            else -> CustomTileUtils.oppositePackage
        }
    }

    /**
     * 打开菜单时刷新变体切换项的目标与可用性。
     *
     * 读取失败时给出兜底目标（当前 app 的相对版本）并把 [variantSwitchEnabled] 置 false，
     * 让菜单项保持**显示但禁用**，而不是凭空消失。
     */
    suspend fun refreshVariantSwitch(context: Context) {
        // 先清空上一次的结果：重新检测期间菜单项保持禁用，文案回落到「当前版本的相对版本」
        variantSwitchTarget = null
        variantSwitchEnabled = false
        val rawTiles = SecureSettingsHelper.getSysuiQsTiles(context)
        if (rawTiles == null) {
            variantSwitchTarget = CustomTileUtils.oppositePackage
            variantSwitchEnabled = false
            return
        }
        val tiles = rawTiles.split(",").map { it.trim() }.filter { it.isNotBlank() }
        variantSwitchTarget = targetPackageFor(tiles)
        variantSwitchEnabled = true
    }

    /**
     * 把系统配置（sysui_qs_tiles）里的 exTile 磁贴切换到相反变体。
     *
     * 只改系统配置，**不动应用内的 expanded/collapsed 列表**：用户切换的是系统里实际启用的
     * 那个磁贴服务，应用内的编辑数据（顺序、框内/框外划分）保持原样。
     *
     * 目标由**系统配置里当前的变体**推出（见 [targetPackageFor]），与菜单文案保持一致。
     * 两个变体的 spec 若同时存在，统一收敛为一个目标 spec，避免出现两个 exTile 磁贴。
     * 若系统配置里原本没有 exTile 锚点，则补到 "edit" 之前，保证切换后磁贴确实存在。
     *
     * @param context Context
     * @return 操作结果（成功 / 写入失败 / 读不到系统配置）
     */
    suspend fun switchExTileVariant(context: Context): VariantSwitchResult {
        val currentTiles = SecureSettingsHelper.getCurrentTiles(context)
        if (currentTiles.isEmpty()) return VariantSwitchResult.NO_TILE

        val targetSpec = CustomTileUtils.exTileSpec(targetPackageFor(currentTiles))
        val newTiles = mutableListOf<String>()
        var replaced = false
        currentTiles.forEach { tile ->
            if (CustomTileUtils.isAnyExTileSpec(tile)) {
                // 只保留第一个锚点位置，其余变体的重复项一并去掉
                if (!replaced) {
                    newTiles.add(targetSpec)
                    replaced = true
                }
            } else {
                newTiles.add(tile)
            }
        }
        if (!replaced) {
            val editIndex = newTiles.indexOf("edit")
            if (editIndex >= 0) newTiles.add(editIndex, targetSpec) else newTiles.add(targetSpec)
        }
        return if (SecureSettingsHelper.setCurrentTiles(context, newTiles)) {
            VariantSwitchResult.SUCCESS
        } else {
            VariantSwitchResult.FAILED
        }
    }

    /**
     * 撤销上一步操作
     */
    fun undo(context: Context) {
        undoSnapshot?.let { updateGridTiles(it, context) }
        undoSnapshot = null
    }

    /**
     * 从系统导入磁贴：读取当前系统 QS 的 sysui_qs_tiles 完整列表。
     *
     * - 普通模式：保存为展开状态（与设置页「从系统导入磁贴」行为一致）；
     * - 创造模式：整份写入**当前编辑的层**（系统此刻显示的正是某个层，导回来最直观），
     *   并兜底保证 exTile 存在。
     *
     * @return 读取成功且列表非空返回 true，否则返回 false（如 Shizuku 不可用）
     */
    suspend fun importFromSystem(context: Context): Boolean {
        val tiles = SecureSettingsHelper.getCurrentTiles(context)
        if (tiles.isEmpty()) return false

        if (creativeConfig.enabled) {
            val full = ConfigRepository.ensureExTile(tiles)
            // 与 updateGridTiles 同一口径：内存先同步，避免 gridTiles / currentTileSet 慢一拍
            updateEditingLayerInMemory { it.copy(tiles = full) }
            ConfigRepository.saveCreativeLayerTiles(context, editingLayer, full)
            return true
        }

        ConfigRepository.saveExpandedTiles(context, tiles)
        config = config.copy(expandedTiles = tiles)
        return true
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
        // 创造模式没有标题行：网格 item 与数据一一对应（末尾还有「添加磁贴」与层设置卡片，
        // 它们不参与排序，越过末位的落点一律忽略）
        if (!hasInner) return if (ri <= gridTiles.size) ri else -1
        // 标题行：空框时作为落点
        if (ri == outerTiles.size) return if (innerTiles.isEmpty()) gridTiles.size else -1
        // 框内磁贴（跳过标题行）
        return if (ri > outerTiles.size) ri - 1 else ri
    }

    // ==================== 创造模式：层与开关 ====================

    /**
     * 就地替换「当前编辑层」的内存数据。
     *
     * 创造模式的保存接口都是「只写一个 key」的增量写入，若只写 DataStore 不更新内存，
     * 上层派生状态（[gridTiles] / [currentTileSet] / [editingLayerConfig]）要等一次
     * 数据流回流才刷新。这期间：
     * - 拖拽结束的撤销判定会读到旧列表，把整次拖拽判成「没移动」；
     * - 添加磁贴 Sheet 的去重集合慢一拍，可能重复添加；
     * - 层内开关在 UI 上要闪一下才变。
     * 所以统一「先改内存、再写 DataStore」，流回流时值相同（data class 相等）不会造成多余重组。
     */
    private fun updateEditingLayerInMemory(transform: (CreativeLayer) -> CreativeLayer) {
        val index = editingLayer - 1
        if (index !in creativeConfig.layers.indices) return
        val layers = creativeConfig.layers.toMutableList()
        layers[index] = transform(layers[index])
        creativeConfig = creativeConfig.copy(layers = layers)
    }

    /**
     * 切换编辑页正在编辑的层（不写系统）。
     *
     * 切层后必须丢弃撤销/拖拽快照与菜单选中项：它们记录的是「上一层」的磁贴列表，
     * 留着会让「撤销」把上一层的列表写到当前层里。
     *
     * @param layer 目标层（1 基），越界自动收敛
     */
    fun selectEditingLayer(layer: Int) {
        val safe = layer.coerceIn(1, CREATIVE_LAYER_COUNT)
        if (safe == editingLayer) return
        editingLayer = safe
        undoSnapshot = null
        dragStartSnapshot = null
        showTileMenu = false
        selectedTileForMenu = null
    }

    /**
     * 指定磁贴是否允许删除。
     *
     * 创造模式下**当前变体**的 exTile 是唯一的切层入口，删掉就再也切不回来（只能进 App 改），
     * 因此菜单里的删除项置灰；普通模式不受影响。
     *
     * 这里必须用 [CustomTileUtils.isExTileSpec]（只认当前变体）而不是 isAnyExTileSpec：
     * 另一个 Release/Debug 变体的磁贴是另一个应用，点它切的是它自己的布局，
     * 对本应用的层没有作用，用户应该能把它删掉。
     */
    fun canDeleteTile(tile: String): Boolean {
        return !(creativeConfig.enabled && CustomTileUtils.isExTileSpec(tile))
    }

    /** 保存当前编辑层的「显示磁贴名」开关（下次切到本层时生效） */
    fun saveCreativeShowLabels(context: Context, show: Boolean) {
        // 内存先改，UI 立即响应（DataStore 回流时值相同，不会再触发重组）
        updateEditingLayerInMemory { it.copy(showTileLabels = show) }
        viewModelScope.launch {
            ConfigRepository.saveCreativeLayerShowLabels(context, editingLayer, show)
        }
    }

    /** 保存当前编辑层的「显示融合设备中心」开关（下次切到本层时生效） */
    fun saveCreativeShowDeviceCenter(context: Context, show: Boolean) {
        updateEditingLayerInMemory { it.copy(showDeviceCenter = show) }
        viewModelScope.launch {
            ConfigRepository.saveCreativeLayerShowDeviceCenter(context, editingLayer, show)
        }
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
