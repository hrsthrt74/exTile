package com.hrsthrt74.qstile.ui.screens.tileconfig

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.withoutEventHandling
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.CustomTileUtils
import com.hrsthrt74.qstile.data.TileCatalog
import com.hrsthrt74.qstile.ui.LocalIsWideScreen
import com.hrsthrt74.qstile.ui.LocalNavPageAction
import com.hrsthrt74.qstile.ui.components.TileGridCell
import com.hrsthrt74.qstile.ui.components.rememberTileIcon
import com.hrsthrt74.qstile.ui.contentBottomPadding
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.hrsthrt74.qstile.viewmodel.TileConfigViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PlainTooltip
import top.yukonga.miuix.kmp.basic.RichTooltip
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TooltipDefaults
import top.yukonga.miuix.kmp.basic.rememberTooltipState
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.foundation.lazy.grid.items as gridItems

/**
 * 磁贴编辑页的网格主体（一页式布局，含虚线框 overlay）。
 *
 * 由原 `TileConfigScreen` 的页内局部函数 `TileGridPane` 拆分而来，行为保持不变：
 * 按住任意磁贴直接拖动，4 列布局；exTile 磁贴是「分界锚点」——它及其之前的磁贴 = 收起时可见（框外），
 * 之后的磁贴 = 仅展开时可见（框内，画在虚线框内）。拖动磁贴跨过 exTile 即自动进出框。
 *
 * 内部还包含三个只在本网格内使用的子块：
 * - `FixedTilesRow`：小米设备特化，固定在顶部、不参与拖拽的两张卡片；
 * - `AddTilesSection`：网格末尾的「添加磁贴」按钮（窄屏玻璃风格时会隐藏，避免与底栏跟随按钮重复）；
 * - 磁贴格子与标题行，以及锚定在格子上的 [TileActionMenu]。
 *
 * 之所以保留为「页内局部函数 + 闭包捕获」的写法：磁贴格子、标题行都必须是
 * `LazyGridItemScope` 的扩展，且要共享同一份 reorderable / lazyGrid 状态与索引映射；
 * 若全部改成顶层函数并显式传参，参数表会膨胀到十几个且更容易传错。
 *
 * @param viewModel 磁贴配置 ViewModel（提供派生列表、索引映射与拖拽 / 增删动作）
 * @param contentPadding Scaffold 下发的内容内边距（顶栏高度 + 挖孔 / 导航条 insets）
 * @param isDynamicColor 是否启用动态取色（关闭时「移动网络」图标保留品牌绿）
 * @param overlayBackdrop 玻璃风格磁贴菜单的模糊采样背景
 * @param onAddTileClick 点击「添加磁贴」按钮（打开添加磁贴 Sheet）
 * @param scrollBehavior 与顶栏共享的滚动行为（网格滚动驱动大标题折叠）
 */
@Composable
fun TileConfigGridPane(
    viewModel: TileConfigViewModel,
    contentPadding: PaddingValues,
    isDynamicColor: Boolean,
    overlayBackdrop: Backdrop?,
    onAddTileClick: () -> Unit,
    scrollBehavior: ScrollBehavior,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // 应用风格：开 = Glass，关 = 经典（虚线框颜色等与风格无关，仅菜单与按钮形态相关）
    val isGlassStyle = LocalThemeSettings.current.progressiveBlur

    // 玻璃磁贴菜单的锚点窗口坐标：每个磁贴圆形底在布局时上报自己的矩形，
    // 打开菜单时按 selectedTileForMenu 取对应值（避免首次点击时坐标尚未上报的问题）。
    val tileMenuAnchorBounds = remember { mutableMapOf<String, Rect>() }

    // 网格内容顶部避让：Scaffold 内容区从 (0,0) 开始、topBar 叠在上方，
    // 用 contentPadding 让内容从顶栏下方开始（同时内容顶部会进入顶栏采样区供模糊捕获）。
    // 注意：LazyGrid 的 item offset 不包含 contentPadding——contentPadding 是在
    // place 阶段作为 visualOffset 叠加的（见 Compose LazyGridMeasuredItem.place），
    // 虚线框基于 item offset 绘制时必须补回该偏移才能与磁贴/标题行对齐。
    val gridContentTopPadding = contentPadding.calculateTopPadding() + 4.dp

    // 获取 custom 磁贴的显示名与应用名（缓存避免重复查询）
    @Composable
    fun rememberCustomTileNames(tile: String): Pair<String, String>? {
        if (!tile.startsWith("custom(")) return null
        return remember(tile) { CustomTileUtils.getCustomTileNames(context, tile) }
    }

    /**
     * 小米设备特化：固定卡片行（网格中占 1 个不可拖拽 item，不参与排序）。
     * 展示的是由系统固定、无法移动的磁贴（手机 WLAN+移动数据 / 平板 WLAN+蓝牙）。
     */
    @Composable
    fun FixedTilesRow() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 俩卡片的外边距
                .padding(horizontal = 6.dp, vertical = 4.dp),
            // 俩卡片中间的间距
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 卡片点击弹出「固定磁贴」说明气泡，需要独立的协程作用域
            val tooltipScope = rememberCoroutineScope()
            viewModel.fixedTileValues.forEach { tile ->
                val tooltipState = rememberTooltipState(isPersistent = true)

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(
                                text = stringResource(R.string.config_fixed_tooltip),
                                style = MiuixTheme.textStyles.footnote1
                            )
                        }
                    },
                    state = tooltipState,
                    modifier = Modifier.weight(1f)
                ) {
                    Card {
                        Row(
                            // 卡片内边距
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    tooltipScope.launch { tooltipState.show() }
                                }
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = rememberTileIcon(tile)
                            Icon(
                                painter = icon,
                                contentDescription = TileCatalog.getDisplayName(tile, viewModel.profile),
                                // 「移动网络」图标用绿色（仅关闭动态取色时覆盖，保留品牌色）
                                tint = if (tile == "cell" && !isDynamicColor) Color(0xFF1FCD39)
                                       else MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )

                            // 磁贴名 和「固定磁贴」说明
                            Column(
                                // 图标 <=> 文字 间距
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = TileCatalog.getDisplayName(tile, viewModel.profile),
                                    style = MiuixTheme.textStyles.body1
                                )
                                Text(
                                    text = stringResource(R.string.config_fixed_label),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 网格末尾的添加按钮区域，占满整行。
    // 系统磁贴与第三方磁贴已合并到同一个 Sheet（用 TabRow 切换来源），故只需一个入口按钮。
    @Composable
    fun AddTilesSection() {
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAddTileClick()
            },
            colors = ButtonDefaults.buttonColorsPrimary(),
            // 与网格内的磁贴保持同样的左右 8dp 边距，上下留出与列表的间距
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .pressable(
                    interactionSource = null,
                    indication = SinkFeedback(),
                    delay = null
                )
        ) {
            Icon(MiuixIcons.AddCircle, contentDescription = stringResource(R.string.config_add_desc))
            Text(stringResource(R.string.config_add_tiles), modifier = Modifier.padding(start = 8.dp))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 从 ViewModel 获取派生状态
        val gridTiles = viewModel.gridTiles
        val innerTiles = viewModel.innerTiles
        val hasInner = viewModel.hasInner
        val innerKeys = viewModel.innerKeys
        val renderList = viewModel.renderList
        val headerKey = viewModel.headerKey
        // 底栏「添加磁贴」跟随按钮是否可见：必须与 App.kt 里的判断保持一致
        // （只能挂在本页面提供的动作桥存在时，且窄屏 + 玻璃风格）
        val addTileOnNavBar = LocalNavPageAction.current != null &&
            isGlassStyle && !LocalIsWideScreen.current

        // gridItems 的 key 生成：磁贴用 spec 值，标题行用固定 key
        fun itemKeyOf(item: Any): Any = if (item is String) item else headerKey

        val lazyGridState = rememberLazyGridState()
        val reorderableState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
            // 使用 ViewModel 的 gridToData 方法进行索引映射
            val fromData = viewModel.gridToData(from.index)
            val toData = viewModel.gridToData(to.index)
            // 目标落在标题/固定卡片/添加按钮等非数据 item 上时忽略本次移动。
            if (fromData < 0 || toData < 0) return@rememberReorderableLazyGridState
            viewModel.onDragMove(fromData, toData, context)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        // 监听拖拽状态变化，把一次完整拖拽（长按→移动跨多格→松手）合并为「一步」可撤销操作。
        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (reorderableState.isAnyItemDragging) {
                // 拖拽开始：记录起点
                viewModel.onDragStart()
            } else {
                // 拖拽结束：合并为一步
                viewModel.onDragEnd()
            }
        }

        // 虚线框颜色（@Composable 属性，需在 Composable 上下文取值后供 drawBehind 捕获）
        val frameColor = MiuixTheme.colorScheme.primary

        /**
         * 单个磁贴格子的渲染（含拖拽、点击菜单）。虚线框不再画在磁贴上（会随拖拽移动），
         * 改由网格 overlay 统一绘制。
         * 使用 LazyGridItemScope receiver：ReorderableItem 是其扩展函数，只能在网格 item 作用域内调用。
         */
        @Composable
        fun LazyGridItemScope.TileGridItem(tile: String) {
            ReorderableItem(reorderableState, key = tile) { isDragging ->
                // 拖起时轻微放大并抬高阴影，松手回弹
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.1f else 1f,
                    label = "tileScale"
                )
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 12.dp else 0.dp,
                    label = "tileElevation"
                )
                // 标题来源由调用方决定：第三方磁贴用 Service 自己的 label + 应用名，
                // 系统磁贴用静态清单名 + 原始 value
                val customNames = rememberCustomTileNames(tile)
                val displayName = customNames?.first
                    ?: TileCatalog.getDisplayName(tile, viewModel.profile)
                Box {
                    TileGridCell(
                        icon = rememberTileIcon(tile),
                        title = displayName,
                        subtitle = customNames?.second ?: tile,
                        onClick = {
                            viewModel.selectedTileForMenu = tile
                            viewModel.showTileMenu = true
                        },
                        // 「移动网络」图标用绿色（关闭动态取色时才覆盖，保留品牌色）
                        iconTint = if (tile == "cell" && !isDynamicColor) Color(0xFF1FCD39)
                                   else MiuixTheme.colorScheme.primary,
                        contentDescription = displayName,
                        elevation = elevation,
                        // 玻璃菜单用窗口坐标定位：每个磁贴都上报自己的窗口矩形，
                        // 打开时按 selectedTileForMenu 取用（mutableMap 读取触发不了重组，
                        // 但 showTileMenu 变化会重组，届时 map 已是最新值）
                        onCirclePositioned = { tileMenuAnchorBounds[tile] = it },
                        // 长按拖拽把手 + 拖起缩放挂在最外层；间距与尺寸由组件统一
                        modifier = Modifier
                            .longPressDraggableHandle(
                                onDragStarted = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                            .scale(scale)
                    ) {
                        // 操作菜单锚定在圆形底内部（Popup 自身不受父布局限制）
                        if (viewModel.selectedTileForMenu == tile) {
                            TileActionMenu(
                                show = viewModel.showTileMenu,
                                anchorBounds = tileMenuAnchorBounds[tile] ?: Rect.Zero,
                                backdrop = overlayBackdrop,
                                tileName = displayName,
                                isGlassStyle = isGlassStyle,
                                // 顶端 / 底端是否可点：按磁贴在整个网格数据中的位置判断
                                canMoveTop = gridTiles.indexOf(tile) > 0,
                                canMoveBottom = gridTiles.indexOf(tile) < gridTiles.size - 1,
                                onMoveTop = {
                                    viewModel.moveTileToTop(tile, context)
                                    viewModel.showTileMenu = false
                                },
                                onMoveBottom = {
                                    viewModel.moveTileToBottom(tile, context)
                                    viewModel.showTileMenu = false
                                },
                                onDelete = {
                                    viewModel.deleteTile(tile, context)
                                    viewModel.showTileMenu = false
                                },
                                onDismissRequest = { viewModel.showTileMenu = false },
                            )
                        }
                    }
                }
            }
        }

        /**
         * 标题行「展开后显示的磁贴」（span 全行）。
         * 用 ReorderableItem 包裹并仅在空框时 enabled：
         * 空框时标题行作为拖拽落点（拖磁贴到这里 = 追加到框内末尾，库的落点只能是
         * ReorderableItem，见 findTargetItem 的 reorderableKeys 过滤）；
         * 非空框时不注册 reorderableKeys、不作为落点，不影响正常排序拖拽。
         * 不挂拖拽句柄，标题行自身不可拖动。框顶线由网格 overlay 绘制。
         */
        @Composable
        fun LazyGridItemScope.SectionHeaderItem() {
            ReorderableItem(
                reorderableState,
                key = headerKey,
                enabled = innerTiles.isEmpty(),
            ) {
                // 标题行：文字 + 说明图标。图标点击弹出 RichTooltip。
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 文本距离虚线框的边距
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.config_section_expanded),
                        style = MiuixTheme.textStyles.subtitle,
                        color = frameColor,
                    )
                    // Info 图标，点击显示说明
                    val infoTooltipState = rememberTooltipState(isPersistent = true)
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            positioning = TooltipAnchorPosition.Below
                        ),
                        tooltip = {
                            RichTooltip(
                                title = {
                                    Text(
                                        text = stringResource(R.string.config_tip_title),
                                        style = MiuixTheme.textStyles.subtitle
                                    )
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.config_tip_content),
                                    style = MiuixTheme.textStyles.body2,
                                )
                            }
                        },
                        state = infoTooltipState,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Icon(
                            MiuixIcons.Info,
                            contentDescription = stringResource(R.string.config_tip_desc),
                            tint = frameColor,
                            modifier = Modifier
                                .size(18.dp)
                                // indication = null：去掉点击压暗/涟漪特效
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch { infoTooltipState.show() }
                                }
                        )
                    }
                }
            }
        }

        // 系统 overscroll effect（与 LazyVerticalGrid 默认一致，Miuix 定制同样生效）。
        // 通过拆分「渲染/事件」让虚线框进入系统拉伸变换内部：
        // - LazyVerticalGrid 接收 withoutVisualEffect（事件照旧进系统 effect，手感不变，不渲染）
        // - modifier 最外层挂 withoutEventHandling（系统拉伸渲染节点），包住下面的 drawBehind 虚线框，
        //   回弹时框与磁贴被同一个变换拉伸，实现跟随
        val systemOverscrollEffect = rememberOverscrollEffect()

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = lazyGridState,
            overscrollEffect = systemOverscrollEffect?.withoutVisualEffect(),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // 系统 overscroll 渲染节点（null 时不渲染），置于虚线框外层使其同被拉伸
                .overscroll(systemOverscrollEffect?.withoutEventHandling())
                // 虚线框覆盖层：画在网格内容上层，不随磁贴拖动/缩放移动（修复拖拽带线）；
                // 底线按末行实际行底绘制（修复磁贴文本行数不同导致底线不齐）。
                // 框位于 overscroll 渲染变换内部，回弹时与磁贴一起被系统拉伸跟随。
                .drawBehind {
                    // 「展开后」区域（虚线框）
                    if (hasInner) {
                        val layoutInfo = lazyGridState.layoutInfo
                        val visible = layoutInfo.visibleItemsInfo
                        val strokeWidth = 1.5.dp.toPx()
                        val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                        // 圆角半径 16dp
                        val cornerRadius = CornerRadius(16.dp.toPx())
                        // 框左右边界：撑满宽度（不随磁贴数量收窄），
                        // 距屏幕左右边缘各留 10dp 水平边距，磁贴(contentPadding 10dp)完整在框内
                        val left = 10.dp.toPx()
                        val right = size.width - 10.dp.toPx()
                        // 标题行 item（画顶线的锚点）
                        val headerItem = visible.find { it.key == headerKey }
                        // 可见的框内磁贴 item
                        val innerVisible = visible.filter { it.key in innerKeys }
                        // LazyGrid 的 item offset 不含 contentPadding（contentPadding 在
                        // place 阶段作为 visualOffset 叠加），这里补回 contentPadding.top，
                        // 否则虚线框会比磁贴/标题行整体高出顶栏高度
                        val contentTopOffset = gridContentTopPadding.toPx()
                        val top: Float
                        val bottom: Float
                        if (innerVisible.isEmpty()) {
                            // 框内无磁贴：空框只包住标题行
                            if (headerItem == null) return@drawBehind
                            top = headerItem.offset.y.toFloat() + contentTopOffset
                            bottom = (headerItem.offset.y + headerItem.size.height).toFloat() + contentTopOffset
                        } else {
                            // 有框内磁贴：框顶用标题行，框底用末行实际行底
                            val lastRowStart = ((innerTiles.size - 1) / 4) * 4
                            val lastRowKeys = innerTiles.subList(lastRowStart, innerTiles.size).toSet()
                            val lastRowVisible = innerVisible.filter { it.key in lastRowKeys }
                            val lastRowComplete = lastRowVisible.size == lastRowKeys.size
                            top = if (headerItem != null) headerItem.offset.y.toFloat() + contentTopOffset
                                  else innerVisible.minOf { it.offset.y }.toFloat() + contentTopOffset
                            bottom = if (lastRowComplete) lastRowVisible.maxOf { it.offset.y + it.size.height }.toFloat() + contentTopOffset
                                     else innerVisible.maxOf { it.offset.y + it.size.height }.toFloat() + contentTopOffset
                        }
                        // 绘制裁剪到网格视口内，滚出屏幕的部分被裁掉
                        clipRect {
                            // 圆角虚线框
                            drawRoundRect(
                                color = frameColor,
                                topLeft = Offset(left, top),
                                size = Size(right - left, (bottom - top).coerceAtLeast(0f)),
                                cornerRadius = cornerRadius,
                                style = Stroke(width = strokeWidth, pathEffect = dash),
                            )
                        }
                    }
                }
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
            contentPadding = PaddingValues(
                // 磁贴与屏幕边缘 10dp：与虚线框的 10dp 水平边距对应，
                // 使框贴边撑满时磁贴距框线正好 10dp。
                // 加上 Scaffold 已算好的挖孔/导航条 insets：竖屏水平为 0 保持原间距，
                // 横屏/反向横屏自动避让左右摄像头挖孔；宽屏起始侧已消费，自动为 0
                top = gridContentTopPadding,
                start = contentPadding.calculateStartPadding(LocalLayoutDirection.current) + 10.dp,
                end = contentPadding.calculateEndPadding(LocalLayoutDirection.current) + 10.dp,
                bottom = contentBottomPadding() + 96.dp
            )
        ) {
            // 固定卡片（小米特化，不参与拖拽）
            if (viewModel.profile.isXiaomi && viewModel.fixedTileValues.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FixedTilesRow()
                }
            }

            // 统一渲染列表：框外磁贴 / 标题行 / 框内磁贴。所有磁贴在同一 items 块内渲染，
            // 跨框拖拽时 item 不跨组合块、key 不变，避免 ReorderableItem 拖拽句柄丢失。
            gridItems(
                renderList,
                key = { itemKeyOf(it) },
                // 磁贴占 1 格；标题行「展开后显示的磁贴」占满整行
                span = { item -> if (item is String) GridItemSpan(1) else GridItemSpan(maxLineSpan) }
            ) { item ->
                if (item is String) {
                    TileGridItem(tile = item)
                } else {
                    SectionHeaderItem()
                }
            }

            // 窄屏 + 玻璃风格时，底栏已有「添加磁贴」跟随按钮，
            // 这里就不再重复给网格末尾的按钮（避免两个入口同时出现）
            if (!addTileOnNavBar) {
                // 磁贴区域 / 按钮区域之间的间隔
                item { Spacer(modifier = Modifier.height(24.dp)) }

                // 添加按钮区域，占满整行
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AddTilesSection()
                }
            }
        }
    }
}
