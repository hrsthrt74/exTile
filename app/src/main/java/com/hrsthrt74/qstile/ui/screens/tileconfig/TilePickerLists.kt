package com.hrsthrt74.qstile.ui.screens.tileconfig

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.CustomTileUtils
import com.hrsthrt74.qstile.data.TileCapabilityFlags
import com.hrsthrt74.qstile.data.TileCatalog
import com.hrsthrt74.qstile.ui.components.TileGridCell
import com.hrsthrt74.qstile.ui.components.rememberTileIcon
import com.hrsthrt74.qstile.viewmodel.TileConfigViewModel
import com.microsoft.clarity.modifiers.clarityMask
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.foundation.lazy.grid.items as gridItems

/**
 * 判断某个磁贴能否取到真实图标（不含空白占位兜底）。
 *
 * 内置磁贴查静态清单的 `iconResId`；`custom(包名/类名)` 磁贴运行时查第三方 Service
 * 是否声明了图标；其余未知磁贴值（如 `edit`）一律视为无图标。
 * 「添加磁贴」Sheet 据此隐藏拿不到图标的磁贴（调试开关可强制展示）。
 *
 * @param context 用于查询 PackageManager
 * @param tile 磁贴 value（如 `wifi`、`custom(包名/类名)`）
 */
private fun hasTileIcon(context: Context, tile: String): Boolean {
    // 内置磁贴：静态清单有 iconResId 即有图标
    TileCatalog.iconRes(tile)?.let { return true }
    // custom 磁贴：Service 存在且声明了图标才算有
    if (tile.startsWith("custom(")) return CustomTileUtils.hasCustomTileIcon(context, tile)
    // 其他未知磁贴值（如 edit）：无图标
    return false
}

/**
 * 「添加磁贴」Sheet 的系统磁贴选择区。
 *
 * 按分类分组展示可添加的系统磁贴，点击追加到网格末尾（框内末尾，拖出虚线框即可让收起时也显示）。
 * 已添加的磁贴不进列表；无图标的磁贴默认隐藏（调试开关可强制展示并降透明度）。
 *
 * 列表中每个条目都挂了 `animateItem` 做淡出 + 补位动画，注意 **key 必须用磁贴自身的 value**：
 * 被添加后这一格会从数据源里被过滤掉，若沿用「按 index 认身份」，淡出会播到末位的另一格上。
 *
 * @param viewModel 磁贴配置 ViewModel（提供候选清单、搜索词与添加动作）
 * @param topPadding 列表顶部预留高度 = Sheet 下发的头部高度（拖动条 + 标题行 + TabRow/搜索框）。
 *   玻璃风格由 `GlassBottomSheet` 通过 `content` 的 `PaddingValues` 回传（含头部槽位里的 TabRow），
 *   经典风格为 0（头部是堆叠式，列表已在其下方）。作为 `contentPadding.top` 使用：
 *   contentPadding 在视口内部，条目滑上来时会画在该区域，于是「延伸到头部下方」。
 * @param sidePadding 列表内容左右留白。
 *   本列表是「铺满 Sheet」的（Sheet 侧不给容器级外边距，见 `AppBottomSheet(fullBleedContent)`），
 *   左右留白因此由列表自己的 contentPadding 提供，两种风格统一 24dp
 *   （Sheet 的 `insideMargin` 被置 0，留白全交给内容）。**不要**改成给外层加 padding——那会把列表
 *   视口一起缩小，Tab 切换时内容会在面板内侧被硬切出一条边，头部模糊带也会变窄。
 * @param continuousAdd 「连续放置」是否开启：开启时不收起 Sheet，可连续点多个
 * @param onDismissSheet 需要收起 Sheet 时调用（仅在未开启「连续放置」时被调用）
 */
@Composable
internal fun SystemTilePicker(
    viewModel: TileConfigViewModel,
    topPadding: Dp,
    sidePadding: Dp,
    continuousAdd: Boolean,
    onDismissSheet: () -> Unit,
) {
    val context = LocalContext.current
    // 当前已添加的磁贴（含固定卡片、edit、exTile，用于过滤重复项）。
    // 必须用 currentTileSet 而不是 config.expandedTiles：创造模式下编辑的是某一层，
    // 用普通配置去重会把固定磁贴当成「可添加」，也会错判另一层的磁贴。
    val currentTiles = viewModel.currentTileSet

    // 读取调试 flag（订阅变化），切换后刷新可用磁贴列表
    // showUnavailableTiles：调试开关，开启后额外显示预设内因设备能力不满足而不可用的磁贴
    val capabilityKey = listOf(
        TileCapabilityFlags.satelliteOverride,
        TileCapabilityFlags.coolingFanOverride,
        TileCapabilityFlags.propOverrides.toMap(),
        TileCapabilityFlags.featureOverrides.toMap(),
        TileCapabilityFlags.showUnavailableTiles,
        TileCapabilityFlags.showNoIconTiles,
    )
    val availableTiles = remember(currentTiles, viewModel.profile, capabilityKey) {
        // 基础来源：默认仅设备可用磁贴；调试开关开启时放宽为全量静态清单（不可用磁贴降透明度展示）
        val catalog = if (TileCapabilityFlags.showUnavailableTiles) {
            TileCatalog.systemTiles
        } else {
            TileCatalog.getAvailableTiles(viewModel.profile)
        }
        // 过滤已添加磁贴；无图标磁贴默认隐藏（空白占位不展示），调试开关开启时保留（降透明度展示）
        catalog.filter {
            it.value !in currentTiles && (hasTileIcon(context, it.value) || TileCapabilityFlags.showNoIconTiles)
        }
    }
    // 调试开关开启时，用于区分「不可用」磁贴的能力快照（渲染时降透明度）：
    // 设备能力不满足，或因无图标而本应隐藏（现被调试开关保留展示）的磁贴
    val availableValues = remember(availableTiles, viewModel.profile, capabilityKey) {
        availableTiles.filter {
            it.isAvailable(viewModel.profile) && hasTileIcon(context, it.value)
        }.map { it.value }.toSet()
    }

    // 根据搜索关键字过滤：同时匹配磁贴显示名和 value（不区分大小写）
    val query = viewModel.addSearchQuery.trim()
    val filteredTiles = remember(availableTiles, query) {
        if (query.isEmpty()) {
            availableTiles
        } else {
            availableTiles.filter { tile ->
                tile.displayName.contains(query, ignoreCase = true) ||
                    tile.value.contains(query, ignoreCase = true)
            }
        }
    }

    // 按分类分组
    val tilesByCategory = remember(filteredTiles) { filteredTiles.groupBy { it.category } }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (filteredTiles.isEmpty()) {
            // 无可添加磁贴 / 搜索无结果提示（区分文案）
            Text(
                text = if (query.isEmpty()) stringResource(R.string.config_no_tiles)
                       else stringResource(R.string.config_no_matching_tiles),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    // 顶部让出头部浮层高度，避免提示文字被 TabRow / 搜索框挡住
                    .padding(
                        start = sidePadding,
                        end = sidePadding,
                        top = topPadding,
                        bottom = 24.dp,
                    )
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
                // 列表本身铺满面板，左右留白走 contentPadding（含义见 sidePadding 注释）
                contentPadding = PaddingValues(
                    start = sidePadding,
                    end = sidePadding,
                    top = topPadding + 8.dp,
                    bottom = 64.dp,
                )
            ) {
                tilesByCategory.forEach { (category, tiles) ->
                    // 分类标题
                    item(key = "category:$category", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = category,
                            style = MiuixTheme.textStyles.subtitle,
                            modifier = Modifier
                                // 分类被整组加完时跟着淡出，不要「啪」地消失
                                // （animateItem 要挂在 item 根节点上，故放在 padding 之前）
                                .animateItem(fadeInSpec = null, fadeOutSpec = tween(160))
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }

                    // 磁贴网格
                    //
                    // key 必须用磁贴自身的 value（唯一且稳定），原因见函数注释。
                    gridItems(tiles, key = { it.value }) { tile ->
                        // 调试开关保留展示的「不可用/无图标」磁贴整体降透明度区分
                        val isUnavailable = tile.value !in availableValues
                        TileGridCell(
                            icon = rememberTileIcon(tile.value),
                            // 标题来源：系统磁贴用静态清单里的显示名，副标题直接显示原始 value
                            title = tile.displayName,
                            subtitle = tile.value,
                            contentDescription = tile.displayName,
                            onClick = {
                                // 添加到网格末尾（框内末尾），拖出框外即可让收起时也显示
                                viewModel.addTile(tile.value, context)
                                // 「连续放置」开启时不收起 Sheet，可以接着点下一个
                                if (!continuousAdd) onDismissSheet()
                            },
                            modifier = Modifier
                                // 消失动画：淡出这一格，同时让后面的格子滑过来补位
                                // （placement 用 animateItem 的默认 spring，不用另配）
                                .animateItem(fadeInSpec = null, fadeOutSpec = tween(160))
                                .alpha(if (isUnavailable) 0.4f else 1f),
                        )
                    }

                    // 分类结束分割线
                    item(key = "divider:$category", span = { GridItemSpan(maxLineSpan) }) {
                        HorizontalDivider(
                            modifier = Modifier
                                .animateItem(fadeInSpec = null, fadeOutSpec = tween(160))
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 「添加磁贴」Sheet 的第三方磁贴选择区。
 *
 * 展示其他应用导出的 QS TileService（需要 `QUERY_ALL_PACKAGES` 权限才能枚举）。
 * 已添加的磁贴与 exTile 预设清单内的磁贴会被过滤掉，避免重复。
 *
 * 该列表的图标已在后台线程批量预取（见 ViewModel.loadCustomTileServices），
 * 渲染时不再触发跨进程查询，滚动才不至于卡顿。
 *
 * 由于涉及第三方应用的名称与包名，整块列表挂了 `clarityMask()`，避免被埋点截屏采集。
 *
 * @param viewModel 磁贴配置 ViewModel（提供 Service 列表、搜索词与添加动作）
 * @param topPadding 列表顶部预留高度（= Sheet 下发的头部高度，含义同 [SystemTilePicker]）
 * @param sidePadding 列表内容左右留白（含义同 [SystemTilePicker]，两种风格统一 24dp）
 * @param continuousAdd 「连续放置」是否开启
 * @param onDismissSheet 需要收起 Sheet 时调用（仅在未开启「连续放置」时被调用）
 */
@Composable
internal fun ThirdPartyTilePicker(
    viewModel: TileConfigViewModel,
    topPadding: Dp,
    sidePadding: Dp,
    continuousAdd: Boolean,
    onDismissSheet: () -> Unit,
) {
    val context = LocalContext.current

    // 检查是否有 QUERY_ALL_PACKAGES 权限（没有就无法枚举其他应用的 Service）
    val hasQueryPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.QUERY_ALL_PACKAGES
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 获取所有 QS Tile 服务（后台线程已加载，由 AddTileSheet 在首次切到本 Tab 时触发）
    val services = viewModel.allTileServices

    // 获取当前已添加的磁贴（与系统磁贴 Tab 同一口径：普通模式=展开布局，创造模式=当前编辑层）
    val currentTiles = viewModel.currentTileSet

    // 获取系统预定义的磁贴 ComponentName 集合（用于过滤：这些已在「系统磁贴」Tab 里出现）
    val systemTileComponents = remember {
        TileCatalog.getAllValues()
            .filter { it.startsWith("custom(") }
            .mapNotNull { CustomTileUtils.parseCustomComponent(it) }
            .toSet()
    }

    // 获取已添加的磁贴 ComponentName 集合（用于过滤）
    val currentTileComponents = remember(currentTiles) {
        currentTiles
            .filter { it.startsWith("custom(") }
            .mapNotNull { CustomTileUtils.parseCustomComponent(it) }
            .toSet()
    }

    // 过滤掉已添加的磁贴和系统预定义的磁贴
    val availableTileServices = remember(services, currentTileComponents, systemTileComponents) {
        services
            ?.filter { service ->
                val component = ComponentName(service.packageName, service.className)
                // 过滤掉已添加的磁贴
                if (component in currentTileComponents) return@filter false
                // 过滤掉系统预定义的磁贴（当前变体的 exTile 已在这里面）
                if (component in systemTileComponents) return@filter false
                // 过滤掉相反版本的 exTile 磁贴：反向磁贴由菜单里的「切换到 Release / Debug」
                // 直接改系统配置来互换，不应该在添加 Sheet 里被当第三方磁贴手动添加
                if (component.packageName == CustomTileUtils.oppositePackage &&
                    component.className == CustomTileUtils.EX_TILE_SERVICE_CLASS
                ) return@filter false
                true
            }
            ?: emptyList()
    }

    when {
        // 无权限时显示引导页面
        !hasQueryPermission -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 顶部让出头部浮层（TabRow / 搜索框）的高度
                    // 左右各自再留 24dp：这两种「提示/引导」块比列表更窄，观感更稳
                    .padding(
                        start = sidePadding + 24.dp,
                        end = sidePadding + 24.dp,
                        top = topPadding + 24.dp,
                        bottom = 24.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.config_permission_needed),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // 跳转到应用详情设置页面
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.config_go_grant))
                }
            }
        }
        // 加载中：显示 Miuix 无限进度指示器
        services == null -> {
            // 用 fillMaxHeight 撑满，与加载完成后的 sheet 高度一致，避免高度突变
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                InfiniteProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.common_loading),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
        // 有权限但过滤后没有可用的磁贴
        availableTileServices.isEmpty() -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 顶部让出头部浮层（TabRow / 搜索框）的高度
                    .padding(
                        start = sidePadding + 24.dp,
                        end = sidePadding + 24.dp,
                        top = topPadding + 24.dp,
                        bottom = 24.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.config_no_custom_tiles),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center
                )
            }
        }
        // 显示磁贴网格
        else -> {
            // 根据搜索关键字过滤：同时匹配磁贴 label、应用名和包名（不区分大小写）
            val query = viewModel.customSearchQuery.trim()
            val filteredServices = remember(availableTileServices, query) {
                if (query.isEmpty()) {
                    availableTileServices
                } else {
                    availableTileServices.filter { service ->
                        service.label.contains(query, ignoreCase = true) ||
                            service.appName.contains(query, ignoreCase = true) ||
                            service.packageName.contains(query, ignoreCase = true)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                if (filteredServices.isEmpty()) {
                    // 搜索无结果提示
                    Text(
                        text = stringResource(R.string.config_no_matching_tiles),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .fillMaxWidth()
                            // 顶部让出头部浮层高度
                            .padding(
                                start = sidePadding,
                                end = sidePadding,
                                top = topPadding,
                                bottom = 24.dp,
                            )
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scrollEndHaptic(HapticFeedbackType.TextHandleMove)
                            .clarityMask(),
                        // 列表本身铺满面板，左右留白走 contentPadding（含义见 sidePadding 注释）
                        contentPadding = PaddingValues(
                            start = sidePadding,
                            end = sidePadding,
                            top = topPadding + 8.dp,
                            bottom = 64.dp,
                        )
                    ) {
                        gridItems(filteredServices, key = { it.packageName + "/" + it.className }) { service ->
                            val tileValue = "custom(${service.packageName}/${service.className})"
                            TileGridCell(
                                // 图标已在后台批量预取（Service 图标或 blank 占位），渲染时不再触发 IPC。
                                // 但**必须**走 rememberTileIcon(drawable) 而不是就地 `service.icon.toBitmap()`：
                                // 预取结果是 PackageManager 跨包 Resources 缓存里的共享 Drawable，
                                // 直接 toBitmap() 不拷贝、多个列表项共用同一张位图；本列表又挂着
                                // clarityMask() 的离屏层，共享纹理会错位——表现就是某个磁贴的图标
                                // 被画成一整张与磁贴同尺寸的大图。helper 内部会复制出独立软件位图。
                                icon = rememberTileIcon(service.icon),
                                // 标题来源：第三方磁贴用 Service 自己声明的 label，副标题显示所属应用名
                                title = service.label,
                                subtitle = service.appName,
                                contentDescription = service.label,
                                onClick = {
                                    viewModel.addTile(tileValue, context)
                                    // 「连续放置」开启时不收起 Sheet
                                    if (!continuousAdd) onDismissSheet()
                                },
                                // 消失动画：添加后被过滤出列表时淡出，后面的格子滑过来补位
                                // （key 已在 gridItems 上按「包名/类名」声明，身份稳定）
                                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = tween(160)),
                            )
                        }
                    }
                }
            }
        }
    }
}
