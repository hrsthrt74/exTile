package com.hrsthrt74.qstile.ui.screens.tileconfig

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.CreativeLayer
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRowDefaults
import top.yukonga.miuix.kmp.glass.glassShadow
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.material.GlassShadows
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * 创造模式的「层」切换 Tab（层 1 / 层 2）。
 *
 * 挂载位置：磁贴编辑页的**顶栏 `bottomContent`** 槽位（见 `TileConfigTopBar` 的 layerTabs 参数）。
 * 这样放下有三个好处：
 * 1. 常驻可见——不会随网格滚动消失，编辑层 2 时不会忘记自己在哪一层；
 * 2. Scaffold 的 contentPadding 会自动加上这一行的高度，网格首行与虚线框偏移无需任何手工补偿；
 * 3. Tab 位于顶栏内，天然处在「内容区 backdrop」的**子树之外**，玻璃采样不会自采样成环
 *    （miuix 明确要求 backdrop 必须在被采样子树之外，违反会绘制期无限递归、native 栈溢出）。
 *
 * 玻璃形态直接复用「添加磁贴」Sheet 里的写法：
 * - `GlassSegmentedTabRow` 内部写死正圆胶囊形状，这里在外层再裁一次保持一致；
 * - **阴影必须画在裁剪之前**（`glassShadow` 挂在外层 Box、`clip` 在其后），否则会被裁成一圈硬边；
 * - 阴影色只用库自带的 `GlassShadows.Float`（纯黑低透明），不要贴主题色——浅色模式下会变成
 *   一圈亮色，看着像「反色的阴影」。
 *
 * @param selectedLayer 当前正在编辑的层（1 基）
 * @param onSelect 选中某一层（参数为 1 基层号）
 * @param backdrop 顶栏的采样源（录在内容区 Box 上，本组件在其子树之外）
 * @param isGlassStyle 应用风格开关：true = Glass，false = 经典
 * @param enabled 是否允许切换；拖拽排序期间传 false，避免列表整体替换打断拖拽
 */
@Composable
fun CreativeLayerTabBar(
    selectedLayer: Int,
    onSelect: (Int) -> Unit,
    backdrop: LayerBackdrop?,
    isGlassStyle: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    // Tab 文案走资源（层 1 / 层 2），不硬编码
    val tabLabels = listOf(
        stringResource(R.string.config_creative_tab_layer, 1),
        stringResource(R.string.config_creative_tab_layer, 2),
    )
    // 只在真正换层时给触感反馈；拖拽期间直接吞掉点击
    val selectLayer: (Int) -> Unit = { index ->
        val layer = index + 1
        if (enabled && layer != selectedLayer) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onSelect(layer)
        }
    }

    if (isGlassStyle && backdrop != null) {
        // 轨道形状 = 高度一半的正圆胶囊（与 miuix 内部一致），只为给外层的阴影用
        val tabTrackShape = remember {
            GlassShape(GlassSegmentedTabRowDefaults.Height / 2, smoothing = 0f)
        }
        Box(
            modifier = modifier
                .fillMaxWidth()
                // 阴影先画（会溢出节点边界）
                .glassShadow(tabTrackShape, GlassShadows.Float)
                // 再裁剪：轨道 / 指示器 / 标签同尺寸嵌套，一起裁不会互相裁坏
                .clip(Capsule())
        ) {
            GlassSegmentedTabRow(
                tabs = tabLabels,
                selectedIndex = selectedLayer - 1,
                onSelect = selectLayer,
                backdrop = backdrop,
                // 阴影已在外层画好，组件内部再画会被外层 clip 裁成硬边
                shadow = null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        // 经典风格：Miuix 描边分段控件
        TabRowWithContour(
            tabs = tabLabels,
            selectedTabIndex = selectedLayer - 1,
            onTabSelected = selectLayer,
            modifier = modifier.fillMaxWidth(),
        )
    }
}

/**
 * 每层配置页底部的「当切换到此层时……」区块。
 *
 * 版式：标题（[SmallTitle]，用默认内边距）在卡片**外侧**，卡片里只有两个开关。
 * 标题放在卡片内会把圆角背景的上方留白顶开，与设置页其它分节的观感不一致。
 *
 * 两个开关都**只在点 QS 磁贴真正切到本层时**生效（编辑页里切换它们不写系统），
 * 与标题文案的语义一致；具体写入逻辑见 `ExTileService.applyCreativeLayer`。
 *
 * @param layer 当前编辑层的配置（提供两个开关的勾选状态）
 * @param isXiaomi 是否小米机型：「显示磁贴名」写的是 HyperOS 专属的 wordless_mode，非小米不显示该项
 * @param onShowLabelsChange 切换「显示磁贴名」
 * @param onShowDeviceCenterChange 切换「显示融合设备中心」
 */
@Composable
fun CreativeLayerOptionsCard(
    layer: CreativeLayer,
    isXiaomi: Boolean,
    onShowLabelsChange: (Boolean) -> Unit,
    onShowDeviceCenterChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 标题在卡片外，作为这一段的分节标题（与调试页 / 设置页的分节写法一致）；
    // 卡片因此只承载两个开关，圆角背景不会被标题撑出多余的上方留白
    Column(
        modifier = modifier
            .fillMaxWidth()
            // 与网格里的磁贴 / 添加按钮保持同样的左右边距
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        SmallTitle(
            text = stringResource(R.string.config_creative_layer_options_title),
            insideMargin = PaddingValues(16.dp, 8.dp)
            )

        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                // 无字模式是 HyperOS 专属 Secure Setting，仅小米设备显示（与设置页保持一致）
                if (isXiaomi) {
                    SwitchPreference(
                        title = stringResource(R.string.config_creative_show_label_title),
                        summary = stringResource(R.string.config_creative_show_label_summary),
                        checked = layer.showTileLabels,
                        onCheckedChange = onShowLabelsChange,
                    )
                }

                SwitchPreference(
                    title = stringResource(R.string.config_creative_device_center_title),
                    summary = stringResource(R.string.config_creative_device_center_summary),
                    checked = layer.showDeviceCenter,
                    onCheckedChange = onShowDeviceCenterChange,
                )
            }
        }
    }
}
