package com.hrsthrt74.qstile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.tile.TileAddHelper
import com.hrsthrt74.qstile.ui.LocalNavPageAction
import com.hrsthrt74.qstile.ui.awaitStartupFrames
import com.hrsthrt74.qstile.ui.components.rememberDialogState
import com.hrsthrt74.qstile.ui.components.rememberSheetState
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.screens.tileconfig.AddTileSheet
import com.hrsthrt74.qstile.ui.screens.tileconfig.CreativeLayerTabBar
import com.hrsthrt74.qstile.ui.screens.tileconfig.TileConfigDialogs
import com.hrsthrt74.qstile.ui.screens.tileconfig.TileConfigGridPane
import com.hrsthrt74.qstile.ui.screens.tileconfig.TileConfigTileActionMenu
import com.hrsthrt74.qstile.ui.screens.tileconfig.TileConfigTopBar
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.hrsthrt74.qstile.viewmodel.TileConfigViewModel
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor

/**
 * 磁贴编辑页（主装配层）。
 *
 * 本文件只保留「页面装配」职责：创建页面级状态（ViewModel、滚动行为、模糊采样源、
 * 各 Sheet / Dialog 的显示状态）、注册导航栏动作桥，然后把 UI 分派给 `tileconfig` 子包里的
 * 各个独立组件。拆出的组件如下（拆分前它们都是本文件里的页内局部函数，单文件曾达 1500+ 行）：
 *
 * | 组件 | 文件 | 职责 |
 * |------|------|------|
 * | [TileConfigTopBar] | `tileconfig/TileConfigTopBar.kt` | 顶栏：标题 + 撤销 + 更多菜单（含玻璃菜单层） |
 * | [TileConfigGridPane] | `tileconfig/TileConfigGrid.kt` | 网格：拖拽排序、虚线框、固定卡片、添加按钮、磁贴操作菜单 |
 * | [AddTileSheet] | `tileconfig/AddTileSheet.kt` | 添加磁贴 Sheet：TabRow / 搜索框 / 连续放置 |
 * | `SystemTilePicker` / `ThirdPartyTilePicker` | `tileconfig/TilePickerLists.kt` | Sheet 内两个来源的候选列表 |
 * | [TileConfigDialogs] | `tileconfig/TileConfigDialogs.kt` | 清除配置 / 从系统导入的确认对话框 |
 *
 * 布局结构：
 * ```
 * Scaffold(topBar = TileConfigTopBar)
 *   └─ Box(顶栏模糊采样层)
 *        └─ TileConfigGridPane
 * AddTileSheet / TileConfigDialogs（浮层，组合在页面末尾）
 * ```
 */
@Composable
fun TileConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)

    // 获取 ViewModel 实例（页面内所有子组件共用同一份配置状态）
    val viewModel: TileConfigViewModel = viewModel()

    // 初始化 ViewModel 的设备信息并开始监听配置变更。
    // 先让出启动首帧后的几帧：本页在冷启动首帧就会随 Pager 一起被组合，若此处立即开跑，
    // 配置流回填后的「整页网格首次组合」（每个磁贴还要 pm.getServiceInfo 取图标）会压住
    // 玻璃底栏指示器所等的那次重组，表现为高亮块晚到、期间点击无响应。详见 awaitStartupFrames。
    LaunchedEffect(Unit) {
        awaitStartupFrames()
        viewModel.initProfile(context)
        viewModel.startConfigObservation(context)
    }

    // 主题设置：由 ExTileTheme 通过 LocalThemeSettings 同步提供（数据就绪后才组合到这里，不会闪烁）
    val themeSettings = LocalThemeSettings.current
    val isDynamicColor = themeSettings.isDynamicColorMode

    // 顶部栏模糊：创建 backdrop 捕获滚动内容，模糊开关关闭或 RuntimeShader 不支持时退回纯色。
    // 顶栏形态（官方玻璃渐进模糊 / 原版顶栏）由 AppTopBar 按「应用风格」开关统一处理。
    val backdrop = rememberBlurBackdrop(enabled = themeSettings.enableBlur)
    // 玻璃弹窗专用 backdrop：为弹窗单独建一个 backdrop 挂在页面根节点，
    // 让弹窗形变动画能采样到含顶栏在内的整页内容（顶栏 backdrop 只挂在滚动内容 Box 上，
    // 覆盖不到顶栏区域，形变时会因采样缺失导致材质跳变）。
    val pageBackdrop = rememberBlurBackdrop(enabled = themeSettings.enableBlur)

    // 「添加磁贴」Sheet 的显示状态（统一由 AppBottomSheet 管理）。
    // 系统磁贴与第三方磁贴已合并到同一个 Sheet，通过顶部的 TabRow 切换来源；
    // Tab / 搜索 / 连续放置等内部状态由 AddTileSheet 自行持有。
    val addSheetState = rememberSheetState()

    // 把「添加磁贴」入口注册到导航栏动作桥：窄屏 + 玻璃风格时，底栏右侧会多出一个跟随按钮
    // （App 层按「当前选中页 == 编辑页」决定是否显示，见 App.kt 的 navTrailingAction）
    val navPageAction = LocalNavPageAction.current
    DisposableEffect(navPageAction, addSheetState) {
        navPageAction?.onAddTile = { addSheetState.show() }
        onDispose { navPageAction?.onAddTile = null }
    }

    // 两个确认操作对话框的显示状态（统一由 AppDialog 管理）
    val clearConfirmDialogState = rememberDialogState()
    // 从系统导入磁贴的确认对话框显示状态
    val importConfirmDialogState = rememberDialogState()

    // Toast 文案在 Composable 上下文预解析，避免协程/onClick 内触发非配置感知的资源读取
    val importedToast = stringResource(R.string.settings_imported_toast)
    val importFailedToast = stringResource(R.string.settings_read_failed_toast)
    // 磁贴显示名同样在此预解析（供「更多」菜单里的「添加 exTile 磁贴」使用，
    // 与 OOBE 的 requestAddTile 保持一致，避免在普通函数里读资源）
    val tileLabel = stringResource(R.string.tile_label)

    // 配置尚未从 DataStore 读出前不渲染页面主体（未就绪就空白，与主题/OOBE 的门控策略一致）
    if (!viewModel.isConfigLoaded) return

    // 磁贴操作菜单的锚点表：由 TileConfigGridPane 里每个磁贴注册（见 TileGridItem），
    // 菜单层在下面 Scaffold 之外读取。放在页面级而不是网格内，是为了让菜单脱离
    // pageBackdrop 的录制子树（详见菜单层处的注释）。
    val tileMenuAnchors = remember { mutableMapOf<String, GlassPopupAnchor>() }

    // 外层 Box：把「磁贴操作菜单层」放到 Scaffold **之外**、但绘制在它之上。
    // 这是硬性要求，不能把菜单塞回 Scaffold 子树：
    // pageBackdrop 挂在 Scaffold 的 modifier 上，录制的是 Scaffold 的整个子树；
    // 菜单的 Modifier.glass(backdrop = pageBackdrop) 一旦位于该子树内，就成了
    // 「采样包含自己的图层」——miuix 的 Modifier.glass 明确要求 backdrop 必须在被采样子树之外。
    // 违反的后果不是画错，而是 RenderNode 树成环，绘制阶段 prepareTreeImpl 无限递归 → native 栈溢出闪退。
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        // 弹窗专用 backdrop 挂在页面根节点（覆盖含顶栏的整页），供玻璃菜单形变动画采样
        modifier = if (pageBackdrop != null) Modifier.layerBackdrop(pageBackdrop) else Modifier,
        topBar = {
            TileConfigTopBar(
                canUndo = viewModel.undoSnapshot != null,
                onUndo = { viewModel.undo(context) },
                // 「添加 exTile 磁贴」：纯粹请求系统把它加入控制中心，与 OOBE P3 同一套逻辑
                //（系统弹确认框 → 成功/已存在则延迟展开控制中心）。这条路径不动本应用内的配置数据，
                // 用户之后可在控制中心里自由拖动磁贴，需要时再用「从系统导入」同步回来。
                onAddTileClick = {
                    TileAddHelper.requestAdd(
                        context = context,
                        tileLabel = tileLabel,
                        scope = scope,
                    )
                },
                // 菜单项只负责弹出确认对话框，真正的数据操作在对话框确认后执行
                onClearClick = { clearConfirmDialogState.show() },
                onImportClick = { importConfirmDialogState.show() },
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                overlayBackdrop = pageBackdrop,
                // 创造模式：顶栏底部挂「层 1 / 层 2」切换 Tab。
                // 这里必须传**内容区**的 backdrop（录在下面那个 Box 上、不含顶栏），
                // 不能传 pageBackdrop（它录在 Scaffold 上、把顶栏也包进去了）——
                // 在顶栏内部采样自己的祖先 backdrop 会让 RenderNode 成环，绘制期 native 栈溢出。
                layerTabs = {
                    if (viewModel.isCreativeMode) {
                        CreativeLayerTabBar(
                            selectedLayer = viewModel.editingLayer,
                            onSelect = { viewModel.selectEditingLayer(it) },
                            backdrop = backdrop,
                            isGlassStyle = themeSettings.progressiveBlur,
                            // 拖拽排序期间禁止切层：整体替换列表会打断 Reorderable 的拖拽状态
                            enabled = !viewModel.isDragging,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        // 滚动内容挂载 backdrop，供顶部栏模糊捕获
        Box(
            modifier = if (backdrop != null) {
                Modifier.fillMaxSize().layerBackdrop(backdrop)
            } else {
                Modifier.fillMaxSize()
            }
        ) {
            TileConfigGridPane(
                viewModel = viewModel,
                contentPadding = paddingValues,
                isDynamicColor = isDynamicColor,
                onAddTileClick = { addSheetState.show() },
                scrollBehavior = scrollBehavior,
                tileMenuAnchors = tileMenuAnchors,
            )
        }
    }

    // 磁贴操作菜单层：在 Scaffold 之后绘制（覆盖顶栏），但**不在** pageBackdrop 的录制子树内，
    // 所以它采样 pageBackdrop 不会形成循环。菜单自身在关闭动画结束后自动停止渲染，
    // 不需要像 PopupLayout 那样手动管理宿主挂载。
    TileConfigTileActionMenu(
        viewModel = viewModel,
        anchors = tileMenuAnchors,
        backdrop = pageBackdrop,
        isDynamicColor = isDynamicColor,
    )
    }

    // 「添加磁贴」Sheet（内置 TabRow 切换系统 / 第三方来源）
    AddTileSheet(state = addSheetState, viewModel = viewModel)

    // 两个确认对话框：清除配置 / 从系统导入
    TileConfigDialogs(
        clearState = clearConfirmDialogState,
        importState = importConfirmDialogState,
        // 创造模式下这两个动作只作用于「当前编辑的层」，文案必须说清楚，
        // 否则「清除配置」看起来像会把两层一起清空
        creativeLayer = viewModel.editingLayer.takeIf { viewModel.isCreativeMode },
        onClearConfirm = { viewModel.updateGridTiles(emptyList(), context) },
        onImportConfirm = {
            scope.launch {
                val success = viewModel.importFromSystem(context)
                Toast.makeText(
                    context,
                    if (success) importedToast else importFailedToast,
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
    )
}
