package com.hrsthrt74.qstile.ui.screens.tileconfig

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.CustomTileUtils
import com.hrsthrt74.qstile.data.TileCatalog
import com.hrsthrt74.qstile.ui.asymmetricDropdownPositionProvider
import com.hrsthrt74.qstile.ui.components.GlassTileOperationMenu
import com.hrsthrt74.qstile.ui.components.rememberTileIcon
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.hrsthrt74.qstile.viewmodel.TileConfigViewModel
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowListPopup

/** 形变副本里图标的尺寸，与页面内磁贴一致（见 TileGridCell 的 TileIconSize）。 */
private val MenuAnchorIconSize = 36.dp

/**
 * 删除时菜单整体淡出的时长。
 *
 * 删除不走 `GlassTransformPopup` 的退场（面板缩回锚点）：那段 spring 用
 * `GlassMotion.transformBounds(exiting)`（damping 0.8 / response 0.28s），收敛要 400ms 上下，
 * 连续删除时每次都要等它跑完，手感明显发滞。这里改为「整层快速淡出 + 立即删数据」。
 * 80ms：刚好还能读作一次淡出，又不会在连续删除时拖住节奏（再短就接近瞬移了）。
 */
private const val DeleteFadeDurationMs = 80

/**
 * 磁贴操作菜单的**页面级容器**。
 *
 * 由 `TileConfigScreen` 在 Scaffold 之外调用，原因只有一个但很关键：
 * `pageBackdrop` 挂在 Scaffold 的 modifier 上，录制的是 Scaffold 的整个子树；
 * 而玻璃菜单用 `Modifier.glass(backdrop = pageBackdrop)` 采样它。
 * miuix 明确要求 backdrop 必须位于被采样子树**之外**，否则 RenderNode 树成环——
 * 绘制阶段 `prepareTreeImpl` 会无限递归，直接 native 栈溢出闪退（不是 Kotlin 异常，logcat 里
 * 只能看到 libhwui 的递归栈）。所以菜单必须渲染在 Scaffold 之后、且不在它的子树内。
 *
 * 状态来源：`viewModel.selectedTileForMenu` 决定当前操作的是哪个磁贴，
 * `anchors` 由网格里的每个磁贴注册（见 `TileConfigGrid.TileGridItem`）。
 *
 * @param viewModel 磁贴配置 ViewModel（提供选中磁贴、网格顺序与增删移动动作）
 * @param anchors 磁贴 value → 玻璃菜单锚点的映射
 * @param backdrop 页面级 backdrop（挂在 Scaffold 上，本组件在其子树之外）
 * @param isDynamicColor 是否启用动态取色（关闭时「移动网络」图标保留品牌绿）
 */
@Composable
fun TileConfigTileActionMenu(
    viewModel: TileConfigViewModel,
    anchors: MutableMap<String, GlassPopupAnchor>,
    backdrop: Backdrop?,
    isDynamicColor: Boolean,
) {
    val isGlassStyle = LocalThemeSettings.current.progressiveBlur
    // 经典风格的菜单不在这里渲染，而是回到磁贴内部（见 TileConfigGrid 的 circleContent）：
    // WindowListPopup 取「父布局节点」的窗口矩形当锚点，在本组件里调用时父节点是整屏，
    // 锚点就成了全屏，菜单会贴到屏幕角上。
    // 本组件只服务玻璃形态——它必须有页面级全屏宿主（全屏 dismiss 层 + 窗口坐标定位）。
    if (!isGlassStyle) return

    val context = LocalContext.current

    // 删除时的整层淡出进度（1 = 正常，0 = 完全透明）。
    // 只服务于玻璃菜单：删除时保持 show=true 让面板维持展开形态，整层快速淡出后立刻删数据，
    // 不播放「缩回磁贴」的退场——那段 spring 要 400ms 才收敛，连续删除时纯属干等。
    // 经典风格的 WindowListPopup 是系统弹窗、关闭本身瞬时，恒为 1f，不参与淡出。
    val deleteFade = remember { Animatable(1f) }

    // 待删除的磁贴（仅玻璃菜单使用）。
    //
    // 删除会立刻把磁贴从 gridTiles 移除、网格当场重排；而菜单退场动画的终点取 anchor 的
    // 窗口坐标——磁贴没了，坐标就冻结在最后一帧，面板会「退回」到删除前的旧位置再凭空消失。
    // 所以顺序必须是「先淡出菜单，再删数据」。
    // 状态声明在下面两个 return **之前**：菜单一关 selectedTileForMenu 就变 null，
    // 声明在后面会连待删除状态一起丢掉。
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingDelete) {
        val tile = pendingDelete ?: return@LaunchedEffect
        deleteFade.snapTo(1f)
        deleteFade.animateTo(0f, tween(DeleteFadeDurationMs))
        viewModel.deleteTile(tile, context)
        viewModel.showTileMenu = false
        // 删除后由这里清空选中态：TileConfigTileActionMenu 会随之退出渲染。
        // 只清「还是它」的情况——若期间用户已经打开了别的磁贴的菜单，不能把那个也关掉。
        if (viewModel.selectedTileForMenu == tile) viewModel.selectedTileForMenu = null
        // 先把淡出进度复位，再清 pendingDelete：清空会取消本 effect，
        // 放在后面就执行不到了，下一次打开菜单会整层透明。
        deleteFade.snapTo(1f)
        pendingDelete = null
    }

    // 没有选中磁贴 / 锚点尚未注册时不渲染（首次点击时锚点已在上一帧注册完毕）
    val selectedTile = viewModel.selectedTileForMenu ?: return
    val anchor = anchors[selectedTile] ?: return

    // 标题文案：第三方磁贴用 Service 自己声明的 label，系统磁贴用静态清单名
    val displayName = remember(selectedTile, viewModel.profile) {
        val custom = if (selectedTile.startsWith("custom(")) {
            CustomTileUtils.getCustomTileNames(context, selectedTile)?.first
        } else {
            null
        }
        custom ?: TileCatalog.getDisplayName(selectedTile, viewModel.profile)
    }
    val gridTiles = viewModel.gridTiles

    // GlassTransformPopup 用 `updateTransition(show)` 驱动动画：如果它**首次组合**时 show 已经是 true，
    // transition 的 currentState 直接等于目标状态，`animateFloat` 的初始值就是 1，0→1 的行程不存在，
    // 动画整个被跳过——表现就是「首次点击磁贴菜单直接出现，之后才有动画」。
    // 这里先以 show=false 组合一帧，下一帧再置 true，保证每次打开都完整播一遍入场。
    var menuVisible by remember(selectedTile) { mutableStateOf(false) }
    LaunchedEffect(selectedTile, viewModel.showTileMenu) {
        if (viewModel.showTileMenu) {
            menuVisible = false
            withFrameNanos { }
            menuVisible = true
        } else {
            menuVisible = false
        }
    }

    // 整层淡出：删除时让菜单快速消失，而不是等 GlassTransformPopup 把面板缩回磁贴。
    // alpha 挂在全屏宿主上，面板 / dismiss 层 / 形变副本一起淡出；
    // 玻璃采样的是页面级 backdrop（在子树之外），不受这层 alpha 影响。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(deleteFade.value),
    ) {
        TileActionMenu(
            show = menuVisible,
            anchor = anchor,
            backdrop = backdrop,
            tileName = displayName,
            // 与网格里的格子各自持一份独立位图（见 TileGridCell.rememberTileIcon 的注释）
            tileIcon = rememberTileIcon(selectedTile),
            // 与网格一致：「移动网络」在关闭动态取色时保留品牌绿
            iconTint = if (selectedTile == "cell" && !isDynamicColor) Color(0xFF1FCD39)
                       else MiuixTheme.colorScheme.primary,
            isGlassStyle = isGlassStyle,
            // 顶端 / 底端是否可点：按磁贴在整个网格数据中的位置判断
            canMoveTop = gridTiles.indexOf(selectedTile) > 0,
            canMoveBottom = gridTiles.indexOf(selectedTile) < gridTiles.size - 1,
            onMoveTop = {
                viewModel.moveTileToTop(selectedTile, context)
                viewModel.showTileMenu = false
            },
            onMoveBottom = {
                viewModel.moveTileToBottom(selectedTile, context)
                viewModel.showTileMenu = false
            },
            onDelete = {
                if (isGlassStyle) {
                    // 保持 show=true 让面板维持展开形态，由上面的 effect 先整层淡出、
                    // 淡出结束再删数据——不播「缩回磁贴」的退场，连续删除才不卡手
                    pendingDelete = selectedTile
                } else {
                    // 系统弹窗关闭本身就是瞬时的，没有动画可等，直接删
                    viewModel.deleteTile(selectedTile, context)
                    viewModel.showTileMenu = false
                    viewModel.selectedTileForMenu = null
                }
            },
            onDismissRequest = { viewModel.showTileMenu = false },
        )
    }
}


/**
 * 磁贴操作菜单：移动到顶端 / 移动到底端 / 删除。
 *
 * - **玻璃风格**走 [GlassTileOperationMenu]：官方 [GlassTransformPopup] 形变动画（磁贴变成菜单），
 *   需要 [anchor] 提供锚点几何，并自带一份「磁贴视觉」作为形变起点的副本；
 * - **经典风格**走 [WindowListPopup] + [DropdownImpl]：系统弹窗，不隐藏磁贴、也不做形变。
 *
 * 两者共用同一份动作定义与可用性判断，只是外壳不同。
 *
 * 注意：调用点不要用 `show` 参与「是否组合」的条件——关闭时弹窗会直接从组合树移除，
 * 退场动画来不及播放。调用方应只在「目标磁贴匹配」时组合本组件，显隐交给 [show]。
 *
 * @param show 是否展开（关闭动画期间仍需保持组合）
 * @param anchor 玻璃菜单锚点。必须由调用方用 `Modifier.glassPopupAnchor` 挂在磁贴格子上
 *   （上报边界并在展开期间隐藏磁贴），并用 `Modifier.glassPopupAnchorContent` 在圆形底上
 *   上报形变副本的落点；经典风格不消费它
 * @param backdrop 玻璃形态的模糊采样背景
 * @param tileName 菜单顶部展示的磁贴名（不可点击的标题行）
 * @param tileIcon 磁贴图标。仅用于形变副本（磁贴变成菜单的起点视觉）；
 *   标题行左侧的图标已停用，见 `AppStyleTileMenu` 里的说明
 * @param iconTint 磁贴图标着色，由调用方按磁贴规则决定（如「移动网络」保留品牌绿）
 * @param isGlassStyle 是否玻璃风格
 * @param canMoveTop 「移动到顶端」是否可用（已经是第一个时置灰）
 * @param canMoveBottom 「移动到底端」是否可用（已经是最后一个时置灰）
 * @param onMoveTop / onMoveBottom / onDelete 三个操作回调（由调用方负责关闭菜单）
 * @param onDismissRequest 菜单被点击外部 / 返回键关闭时回调
 */
@Composable
fun TileActionMenu(
    show: Boolean,
    anchor: GlassPopupAnchor,
    backdrop: Backdrop?,
    tileName: String,
    tileIcon: Painter,
    iconTint: Color,
    isGlassStyle: Boolean,
    canMoveTop: Boolean,
    canMoveBottom: Boolean,
    onMoveTop: () -> Unit,
    onMoveBottom: () -> Unit,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (isGlassStyle) {
        GlassTileOperationMenu(
            show = show,
            anchor = anchor,
            backdrop = backdrop,
            tileName = tileName,
            // 形变起点的副本：画成与网格里磁贴一致的「圆形底 + 图标」。
            // 尺寸不是这里定的——由调用方挂在圆形底上的 glassPopupAnchorContent 上报，
            // 所以直接 fillMaxSize() 铺满即可。
            anchorContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = tileIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(MenuAnchorIconSize),
                    )
                }
            },
            moveTopLabel = stringResource(R.string.config_move_top),
            moveBottomLabel = stringResource(R.string.config_move_bottom),
            deleteLabel = stringResource(R.string.common_delete),
            moveTopEnabled = canMoveTop,
            moveBottomEnabled = canMoveBottom,
            onMoveTop = onMoveTop,
            onMoveBottom = onMoveBottom,
            onDelete = onDelete,
            onDismissRequest = onDismissRequest,
        )
    } else {
        // 经典风格：原版 WindowListPopup（吸附在锚点附近，按屏幕边缘对齐）。
        // 系统弹窗自带遮罩与返回键处理，不会隐藏磁贴。
        val errorColors = DropdownDefaults.dropdownColors(
            contentColor = MiuixTheme.colorScheme.error,
            selectedContentColor = MiuixTheme.colorScheme.error
        )
        WindowListPopup(
            show = show,
            popupPositionProvider = asymmetricDropdownPositionProvider(
                verticalMargin = 8.dp,
                startMargin = 0.dp,
                endMargin = 8.dp
            ),
            onDismissRequest = onDismissRequest
        ) {
            ListPopupColumn {
                // 标题行：当前磁贴名（灰色小字，不可点击）
                Text(
                    text = tileName,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .padding(start = 20.dp)
                        .padding(bottom = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                DropdownImpl(
                    text = stringResource(R.string.config_move_top),
                    optionSize = 3,
                    isSelected = false,
                    index = 0,
                    enabled = canMoveTop,
                    onSelectedIndexChange = { onMoveTop() }
                )
                DropdownImpl(
                    text = stringResource(R.string.config_move_bottom),
                    optionSize = 3,
                    isSelected = false,
                    index = 1,
                    enabled = canMoveBottom,
                    onSelectedIndexChange = { onMoveBottom() }
                )
                DropdownImpl(
                    text = stringResource(R.string.common_delete),
                    optionSize = 3,
                    isSelected = false,
                    index = 2,
                    dropdownColors = errorColors,
                    onSelectedIndexChange = { onDelete() }
                )
            }
        }
    }
}
