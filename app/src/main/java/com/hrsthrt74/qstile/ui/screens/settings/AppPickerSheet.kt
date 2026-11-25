package com.hrsthrt74.qstile.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.AppCard
import com.hrsthrt74.qstile.ui.components.GlassSearchField
import com.hrsthrt74.qstile.ui.components.SheetState
import com.hrsthrt74.qstile.ui.rememberBlurBackdrop
import com.hrsthrt74.qstile.ui.sheetBottomPadding
import com.hrsthrt74.qstile.ui.theme.LocalThemeSettings
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import com.microsoft.clarity.modifiers.clarityMask
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 自定义应用选择器 Sheet：搜索 + 列表选择「长按 exTile 磁贴跳转的目标应用」。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `AppPickerSheet` 拆分而来，行为保持不变：
 * 搜索框放在 Sheet 的头部槽位（玻璃风格浮在列表之上、采样本 Sheet 的列表层，
 * 不会「穿透」到后面的设置页），列表可滚动到头部下方，关闭后清空搜索词。
 *
 * **注意两处位置约束**：
 * 1. `apps` / `isLoading` / `hasApps` / `headerBackdrop` 必须留在函数体顶层、`AppBottomSheet` 之外
 *    ——它们原先就在 Sheet 调用之前。塞进 content lambda 会导致每次打开都重建 backdrop
 *    （首次打开出现模糊跳变），且 `isLoading` 的判空语义会被打乱。
 * 2. `isLoading` 用 `apps == null` 判断，**不要**改成 `isNullOrEmpty()`：
 *    `null` = 还在加载（转圈）、`emptyList` = 确实没有可启动的应用（提示文案），两者 UI 不同。
 *
 * @param state Sheet 显示状态（由页面 `rememberSheetState()` 创建）
 * @param viewModel 设置页 ViewModel（应用列表、搜索词、选中结果都在它上面）
 */
@Composable
fun AppPickerSheet(
    state: SheetState,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    // 应用风格开关 / 模糊开关：玻璃搜索框的采样源按两者共同决定（任一关闭都回退普通输入框）
    val themeSettings = LocalThemeSettings.current

    // 打开选择器时，在后台线程加载应用列表（查询所有应用 + 加载图标较耗时）。
    // 放在本组件顶层而不是页面里：本组件由页面无条件调用，注册时机与拆分前等价；
    // ViewModel 内部对「已加载」做了短路，重复打开不会重复查询。
    LaunchedEffect(state.show) {
        if (state.show) {
            viewModel.loadLaunchableApps(context)
        }
    }

    // 列表为空时是加载中，需要区分"加载中"和"确实没有应用"
    // （在 AppBottomSheet 之外读：搜索框要作为 Sheet 的头部内容，只有真拿到应用才显示）
    val apps = viewModel.launchableApps
    val isLoading = apps == null
    val hasApps = !isLoading && !apps.isNullOrEmpty()

    // 玻璃搜索框的采样源：只录列表层（不含头部），否则会采样到 Sheet 后面的设置页，
    // 看起来像「穿透了 Sheet」。模糊关闭 / 经典风格时返回 null，搜索框自动回退普通底色。
    val headerBackdrop = rememberBlurBackdrop(
        enabled = themeSettings.progressiveBlur && themeSettings.enableBlur
    )

    AppBottomSheet(
        state = state,
        title = stringResource(R.string.settings_sheet_app_picker_title),
        // 搜索框放进 Sheet 的头部槽位：玻璃风格它浮在列表之上（随头部一起、不会被模糊带模糊），
        // 经典风格退化为列表上方普通一行——与「添加磁贴」Sheet 完全同一套结构。
        // 加载中 / 空列表时不显示搜索框（没有内容可搜）。
        headerBottomContent = if (hasApps) {
            {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 搜索框会显示用户输入的应用名 / 包名，同样属于敏感信息，
                        // 跟着内容一起挂 clarityMask（它已经不在下层 Column 的遮罩范围里了）
                        .clarityMask()
                        // 与列表内容之间留 8dp（列表的 contentPadding.top 从头部下缘算起）
                        .padding(bottom = 8.dp)
                ) {
                    // 搜索框（支持按应用名或包名搜索）
                    SearchBar(
                        expanded = false,
                        onExpandedChange = { expanded ->
                            if (!expanded) {
                                state.dismiss()
                            }
                        },
                        insideMargin = DpSize(0.dp, 4.dp),
                        inputField = {
                            // 玻璃风格：胶囊底下垫真实 backdrop 模糊（与 GlassSegmentedTabRow 同一套材质），
                            // 采样源是本 Sheet 的列表层；没有 backdrop 时自动回退普通输入框
                            GlassSearchField(
                                query = viewModel.appSearchQuery,
                                onQueryChange = { viewModel.appSearchQuery = it },
                                onSearch = {},
                                // 避免打开 sheet 时自动聚焦弹键盘
                                expanded = false,
                                onExpandedChange = {},
                                label = stringResource(R.string.common_search),
                                backdrop = headerBackdrop,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { }
                }
            }
        } else {
            null
        },
        // 列表可滚动：条目滑到标题栏 / 搜索框下方（「延伸到顶部」）
        contentExtendsUnderHeader = true,
        // 关闭动画完成后清空搜索关键字，避免下次打开残留
        onDismissed = { viewModel.clearAppSearchQuery() },
    ) { innerPadding ->
        // 头部让出的高度交给内容自己消费（搜索框已进头部槽位，含它的高度）
        val headerHeight = innerPadding.calculateTopPadding()

        Column(
            modifier = Modifier
                // 列表层同时是玻璃搜索框的采样源：头部搜索框在本节点之外，不会采到自己
                .then(
                    if (headerBackdrop != null) Modifier.layerBackdrop(headerBackdrop)
                    else Modifier
                )
                .clarityMask()
        ) {
            when {
                isLoading -> {
                    // 加载状态：使用 Miuix 无限进度指示器 + 加载文本，居中显示
                    // 用 fillMaxHeight 撑满，与列表加载完成后的 sheet 高度一致，避免高度突变
                    // （顶部先让出头部高度：内容是从面板顶端开始铺的，不让位会中心偏下）
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = headerHeight)
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
                apps.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.settings_no_apps),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = headerHeight + 24.dp, bottom = 24.dp)
                    )
                }
                else -> {
                    // 根据搜索关键字过滤：同时匹配应用名和包名（不区分大小写）
                    val filteredApps = remember(apps, viewModel.appSearchQuery) {
                        val query = viewModel.appSearchQuery.trim()
                        if (query.isEmpty()) {
                            apps
                        } else {
                            apps.filter {
                                it.label.contains(query, ignoreCase = true) ||
                                    it.packageName.contains(query, ignoreCase = true)
                            }
                        }
                    }
                    // 搜索框已移到 Sheet 的头部槽位（见上方 AppBottomSheet 的 headerBottomContent），
                    // 这里只剩列表；顶部让位交给 contentPadding，条目滚动时会从头部下方滑过
                    if (filteredApps.isEmpty()) {
                        // 搜索无结果提示
                        Text(
                            text = stringResource(R.string.settings_no_matching_apps),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = headerHeight + 24.dp, bottom = 24.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
                            contentPadding = PaddingValues(
                                top = headerHeight,
                                bottom = sheetBottomPadding(),
                            )
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                            // 每个应用项用卡片包裹，提供背景（无阴影）+ 圆角；
                            // 浮层内走 AppCard：玻璃风格换成 GlassCard（半透明），经典风格保持 secondaryContainer
                            AppCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.saveCustomAppPackage(context, app.packageName)
                                        state.dismiss()
                                    },
                                insideMargin = PaddingValues(16.dp),
                                color = MiuixTheme.colorScheme.secondaryContainer,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 应用图标
                                    val icon = app.icon
                                    val bitmap = remember(icon) { icon.toBitmap() }
                                    Icon(
                                        painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) },
                                        contentDescription = app.label,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(42.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = app.label,
                                            style = MiuixTheme.textStyles.body1
                                        )
                                        Text(
                                            text = app.packageName,
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
            }
        }
    }
}
