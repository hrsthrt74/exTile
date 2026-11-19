package com.hrsthrt74.qstile.ui.screens.tileconfig

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.AppTopBar
import com.hrsthrt74.qstile.ui.LocalIsWideScreen
import com.hrsthrt74.qstile.ui.TopBarIconButton
import com.hrsthrt74.qstile.ui.TopLayer
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassIconDropdownMenu
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.menu.WindowIconDropdownMenu

/**
 * 磁贴编辑页顶栏。
 *
 * 由原来的页内局部函数 [TileConfigScreen] 的 `TileTopAppBar` 拆分而来，职责保持不变：
 * - 标题：宽屏（侧边 NavigationRail）用紧凑形态，窄屏保留大标题形态；
 * - 右侧动作：撤销上一步（仅在存在可撤销快照时显示）+「更多」菜单（清除配置 / 从系统导入）；
 * - 玻璃风格下，「更多」菜单用 [GlassIconDropdownMenu] 并经 [TopLayer] 提升到应用根部，
 *   避免被顶栏 / 玻璃侧栏遮挡或被 Pager 页面裁剪；
 * - 经典风格下直接用 [WindowIconDropdownMenu]（内部自管显隐状态）。
 *
 * 顶栏形态（官方玻璃渐进模糊 / 原版顶栏）由 [AppTopBar] 按「应用风格」开关统一处理，
 * 本组件只负责往 actions 槽里塞按钮。
 *
 * 之所以把玻璃菜单也放在本组件内：`GlassIconDropdownMenu` 需要与触发按钮共用同一个
 * `GlassPopupAnchor`（按钮上报窗口坐标、菜单据此定位与形变），两者放一起可以把这个
 * anchor 完全内聚在本文件，不向调用方暴露 miuix 的 anchor 类型。
 *
 * @param canUndo 是否存在可撤销快照（false 时撤销按钮整体不显示，连占位也没有）
 * @param onUndo 点击「撤销上一步」
 * @param onClearClick 点击「清除配置」（由调用方弹确认对话框，本组件不直接操作数据）
 * @param onImportClick 点击「从系统导入磁贴」（同上）
 * @param scrollBehavior 顶栏滚动行为（大标题折叠等），由页面与网格共享同一个实例
 * @param backdrop 顶栏模糊采样源（只覆盖滚动内容区）
 * @param overlayBackdrop 玻璃「更多」菜单的模糊采样源（覆盖整页、含顶栏）
 */
@Composable
fun TileConfigTopBar(
    canUndo: Boolean,
    onUndo: () -> Unit,
    onClearClick: () -> Unit,
    onImportClick: () -> Unit,
    scrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop?,
    overlayBackdrop: LayerBackdrop?,
) {
    val themeSettings = LocalThemeSettings.current
    // 应用风格：开 = Glass 全套组件，关 = 经典组件（与模糊总开关解耦）
    val isGlassStyle = themeSettings.progressiveBlur
    val haptic = LocalHapticFeedback.current

    // 菜单项回调：entry 用 remember 只构造一次以保证弹窗内条目身份稳定，
    // 而回调本身取最新值（remember 里闭包捕获的 lambda 可能已过期）
    val currentOnClear by rememberUpdatedState(onClearClick)
    val currentOnImport by rememberUpdatedState(onImportClick)

    // 菜单项列表：两种形态（玻璃 / 经典）共用同一份定义
    val clearMenuText = stringResource(R.string.config_menu_clear)
    val importSystemMenuText = stringResource(R.string.settings_import_system_title)
    val moreMenuEntry = remember(clearMenuText, importSystemMenuText) {
        DropdownEntry(
            items = listOf(
                DropdownItem(text = clearMenuText, onClick = { currentOnClear() }),
                DropdownItem(text = importSystemMenuText, onClick = { currentOnImport() }),
            )
        )
    }

    // 玻璃形态「更多」菜单的显隐状态与锚点（锚点由触发按钮在布局时上报窗口坐标）
    var showGlassMoreMenu by remember { mutableStateOf(false) }
    val glassMoreAnchor = rememberGlassPopupAnchor()

    // 共享的 actions 内容：撤销上一步按钮 + 三点菜单。
    // 由 AppTopBar 统一下发 LocalAppTopBarGlass，故 TopBarIconButton 会自动跟随顶栏形态。
    val topBarActions: @Composable RowScope.() -> Unit = {
        // 撤销上一步：仅在存在可撤销快照时显示（一次完整拖拽或一次增删移动算一步）
        AnimatedVisibility(
            visible = canUndo,
            enter = fadeIn(animationSpec = tween(durationMillis = 200)) +
                scaleIn(initialScale = 0.5f, animationSpec = tween(durationMillis = 200)),
            exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                scaleOut(targetScale = 0.5f, animationSpec = tween(durationMillis = 150))
        ) {
            // 模糊半径随进入 / 退出过渡：出现时 6dp → 0dp，消失时反向。
            // 与「设置-外观-模糊效果」联动：enableBlur 关闭时恒定 0dp，总是不模糊。
            val blurRadius by if (themeSettings.enableBlur) {
                transition.animateFloat(
                    transitionSpec = {
                        if (targetState == EnterExitState.Visible) {
                            tween(durationMillis = 200)
                        } else {
                            tween(durationMillis = 150)
                        }
                    },
                    label = "undoBlur"
                ) { state ->
                    // 模糊半径 0 <=> 6
                    if (state == EnterExitState.Visible) 0f else 6f
                }
            } else {
                remember { mutableFloatStateOf(0f) }
            }
            TopBarIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUndo()
                },
                // 与右侧三点菜单保持间距
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    MiuixIcons.Undo,
                    contentDescription = stringResource(R.string.config_undo_desc),
                    modifier = Modifier.blur(blurRadius.dp)
                )
            }
        }

        if (isGlassStyle) {
            // 玻璃形态：GlassIconButton 作 anchor，GlassIconDropdownMenu 在下方 TopLayer 中渲染
            GlassIconButton(
                onClick = { showGlassMoreMenu = true },
                modifier = Modifier.glassPopupAnchor(
                    anchor = glassMoreAnchor,
                    cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2,
                    floating = true,
                ),
            ) {
                Icon(
                    MiuixIcons.More,
                    contentDescription = stringResource(R.string.config_more_desc),
                    modifier = Modifier.glassPopupAnchorContent(glassMoreAnchor)
                )
            }
        } else {
            // 原版形态：内部自带普通 IconButton 触发器
            WindowIconDropdownMenu(entry = moreMenuEntry) {
                Icon(MiuixIcons.More, contentDescription = stringResource(R.string.config_more_desc))
            }
        }
    }

    // 统一自适应顶栏：应用风格开 → 官方 GlassTopAppBar，关 → 原版顶栏（普通模糊）
    AppTopBar(
        title = stringResource(R.string.config_title),
        largeTitle = stringResource(R.string.config_title),
        // 宽屏（侧边 NavigationRail）用无大标题形态，窄屏保留大标题形态
        useLargeTitle = !LocalIsWideScreen.current,
        scrollBehavior = scrollBehavior,
        backdrop = backdrop,
        progressive = themeSettings.progressiveBlur,
        actions = topBarActions
    )

    // 玻璃形态的「更多」菜单：提升到 TopLayer，避免被顶栏 / 玻璃侧栏遮挡或被 Pager 页面裁剪。
    // 与官方 demo 一致：anchorContent 只画图标，不重复挂 glassPopupAnchorContent
    // （那会回写 anchor 坐标干扰形变）。
    if (isGlassStyle) {
        TopLayer {
            GlassIconDropdownMenu(
                show = showGlassMoreMenu,
                onDismissRequest = { showGlassMoreMenu = false },
                anchor = glassMoreAnchor,
                anchorContent = {
                    Icon(
                        MiuixIcons.More,
                        contentDescription = stringResource(R.string.config_more_desc),
                        modifier = Modifier.size(28.dp),
                    )
                },
                entries = listOf(moreMenuEntry),
                backdrop = overlayBackdrop,
                collapseOnSelection = true,
            )
        }
    }
}
