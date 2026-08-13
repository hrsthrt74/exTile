# exTile 项目上下文

## 概述

**exTile** 是一个 Android 应用，通过 **Shizuku** 框架获取系统级权限，实现对 Android 快速设置面板（Quick Settings）磁贴的动态切换与配置管理。基于 **Jetpack Compose + MIUIX (HyperOS 设计)** 构建。

| 属性 | 值 |
|------|------|
| 包名 | `com.hrsthrt74.qstile` |
| 语言 | Kotlin 2.4.0 |
| UI | Jetpack Compose + MIUIX (HyperOS 设计) |
| 构建系统 | Gradle (Kotlin DSL) + AGP 9.3.0 |
| 最低 / 目标 / 编译 SDK | Android 13 (33) / 16 (36) / 17 (37) |
| Java 版本 | Java 11 |

> 架构图、完整目录结构、依赖版本见 [README.md](README.md)，此处不再重复。

## 目录要点

关键代码位于 `app/src/main/java/com/hrsthrt74/qstile/`：

- `ui/screens/` 页面（Home / TileConfig / Settings / Oobe / Licenses / DebugTools），另有三个独立 Activity（Licenses / DebugTools / TileLongClick）
- `ui/components/` `AppBottomSheet` / `AppDialog` / `PermissionStatusCard`（主页与 OOBE 复用）
- `data/` TileConfig / TileCatalog / TileRequirement / DeviceProfile / ConfigRepository / StatsRepository / ThemeSettings
- `shizuku/` ShizukuHelper / SecureSettingsHelper / CommandService
- `tile/ExTileService.kt` QS 磁贴服务

## 核心功能

1. **QS 磁贴一键切换**：`ExTileService` 在展开/收起两套布局间切换
2. **磁贴编辑**：4 列网格拖拽排序（Calvin-LL/Reorderable）；一页式布局，exTile 磁贴为「分界锚点」（框外 = 收起可见，框内 = 仅展开可见，虚线框 overlay 绘制）；收起列表由框外自动派生
3. **磁贴操作菜单**：`WindowListPopup` 移动到顶端/底端、删除
4. **自定义磁贴图标**：通过 PackageManager 获取其他应用 TileService 图标
5. **权限架构**：核心为 `WRITE_SECURE_SETTINGS`（写 sysui_qs_tiles）；Shizuku 仅用于 `pm grant` 自动授权，授权后可脱离 Shizuku；`PermissionStatus` 状态机驱动主页权限卡片引导；未装 Shizuku 时可 adb 手动授权
6. **配置备份/恢复**：JSON 导出/导入，支持从系统当前配置导入
7. **主题定制**：MIUIX 动态取色引擎，Monet 取色、深色模式
8. **小米设备特化**：DeviceCompat 检测 MIUI/HyperOS，固定卡片 WLAN+移动数据（平板 WLAN+蓝牙），编辑磁贴固定末尾
9. **横滑切换**：HorizontalPager + MainPagerState 联动；**自适应导航**——窄屏（竖屏手机）底部 NavigationBar（带毛玻璃），宽屏（横屏/平板，`shouldShowSplitPane()`：width≥840dp 或 ≥600dp 且高/宽<1.2）左侧固定展开 NavigationRail（`state=null` 经典形态），两布局共享同一份 Pager 状态与 `Screen.allPages`，旋转不丢选中页；宽屏标志经 `LocalIsWideScreen` 注入，页面底部预留高度用 `contentBottomPadding()` 计算；横屏/反向横屏时左右摄像头挖孔的避让由各 screen 内容区取 Scaffold 传下的 `contentPadding` 的 start/end（`calculateStartPadding/calculateEndPadding` + 固定间距）实现——Scaffold 背景（Surface）保持铺满，仅内容避开挖孔、不露出黑边；宽屏起始侧已由 NavigationRail 避让，`WideScreenContent` 内容区对其 `consumeWindowInsets`，Scaffold 的 `contentWindowInsets`（默认 systemBars∪displayCutout）会自动 exclude 已消费部分，避免重复避让；宽屏内容区背景铺满（surface）并限宽水平居中（`MaxContentWidth`=840dp，定义于 `AdaptiveUtils.kt`，参照 M3 自适应导航标准），平板横屏内容不再摊得过宽；限宽实现在 `AppPager` 每个 page 的页面容器内（Pager 保持全宽 → 翻页动画全屏滑动，页面容器内 surface 背景全宽铺满 + 内容 `widthIn(max)`+`align(Center)` 居中），避免把 Pager 本身限宽导致翻页只在中间一段滑动、超出部分被裁切
10. **开源许可页**：LicensesActivity，点击跳转浏览器
11. **设备感知磁贴**：`TileRequirement` flag 集合（sealed interface）+ `DeviceProfile` 统一求值（AND 语义）；已接入 prop：实时字幕/对话翻译/单手模式；已接入 feature：NFC/自动亮度/振动/手电筒/移动数据；`TileCapabilityFlags` 调试覆盖优先（内存态）
12. **触觉反馈**：LocalHapticFeedback + scrollEndHaptic + pressable/SinkFeedback
13. **调试工具**：DebugToolsActivity +「能力开关（调试）」sheet 模拟 prop/feature 开关
14. **长按 exTile 行为**：幽灵桥接 Activity（`TileLongClickActivity`）读取「长按 exTile 磁贴行为」配置（跳转 exTile/系统设置/自定义应用）后分发并 finish
15. **使用统计**：StatsRepository 记录展开/收起次数（仅统计成功切换），主页 `StatsCard` 实时展示
16. **首次使用引导 OOBE**：DataStore 存 `oobe_completed` 标志；壳层 `MainApp` 按标志分流——未完成渲染 `OobeScreen`（不渲染底部导航栏），完成切回主页；数据流 initial=null 防闪烁。OOBE 流程：请求权限 → 一键 `requestAddTileService` 添加 exTile 磁贴（回调返回 TILE_ADDED/ALREADY_ADDED 后按钮置灰）→ `importSystemTiles` 从系统导入配置 → 预览 → 完成
17. **系统配置导入**：`ConfigRepository.importSystemTiles` 复用 `SecureSettingsHelper.getCurrentTiles` 读 `sysui_qs_tiles`（读 secure settings 免权限）；展开=完整列表原样保存，收起=[0..exTileIndex]+补 `edit`，`isExpanded=true`；返回 `SystemImportResult`（Success/NoExtile/ReadFailed）。配置合法性以 `Success.hideCount>0` 判定：无收纳磁贴时弹窗提示、不显示导入预览卡片、按钮停留「我已配置完成」

## UI 设计规范

- **导航**：HorizontalPager + MainPagerState 实现横滑切换；自适应导航——窄屏底部 NavigationBar 联动，宽屏（`shouldShowSplitPane()` 判定）切为左侧 NavigationRail，两布局共享同一份状态；**不要**手动注入 NavigationEventDispatcherOwner（activity 1.13+ 自动提供，手动注入会覆盖导致返回不响应）
- **横滑动画**：`CubicBezierEasing(0.25, 0.1, 0.25, 1.0)` 350ms；`beyondViewportPageCount = Screen.allPages.size` 保留页面状态
- **导航栏模糊**：`layerBackdrop` + `textureBlur`；底部 padding 只留 `calculateTopPadding()` 让内容延伸到导航栏后供模糊捕获
- **子页面**：独立 Activity + Intent 跳转，需提供 `LocalNavigationEventDispatcherOwner`，TopAppBar 加返回按钮
- **设置页**：卡片式分类（通用/外观/数据/关于），`ExpandableSettingsCard` 手风琴互斥展开，标题行含分类图标与旋转箭头
- **弹窗/Sheet**：统一 `AppBottomSheet` / `AppDialog` 封装；用 `SheetState`/`DialogState` 的 `show()`/`dismiss()`，**禁止**手写 `var showXxx` + `onDismissRequest`；关闭后清临时状态用 `onDismissed`
- **返回键**：miuix 0.9.3 组件已内置 NavigationBackHandler，**不要**手动注册 BackHandler（会抢占内置实现）；`enableOnBackInvokedCallback="true"` 已在 Manifest 配置
- **毛玻璃背景暂不实现**（WindowBottomSheet 纯色实现，待 miuix 0.9.4 官方方案）
- **主题模型**：`ThemeSettings`（dayNightMode 0=跟随/1=浅/2=深 + isDynamicColorMode），ExTileTheme 中 SideEffect 处理状态栏颜色反色
- **磁贴编辑页**：统一渲染列表 `gridItems(renderList)`（标题行 span 占满整行，跨框拖拽 key 不变）；虚线框 `drawBehind` overlay 绘制（不随拖拽/回弹移动）；`gridToData` 映射索引，落在标题/卡片/添加按钮上忽略移动；点击磁贴弹 WindowListPopup 菜单；标题行 Info 图标弹 RichTooltip
- **磁贴编辑页结构**：拆分为局部函数（闭包捕获、不显式传参）：TileTopAppBar / FixedTilesRow / AddTilesSection / TileGridPane / AddTileSheet / AddCustomTileSheet / ConfirmationDialogs
- **撤销上一步**：内存态只留一步；一次完整拖拽合并为一步（观察 `reorderableState.isAnyItemDragging`，**不要用** onDragStarted/Stopped 手势回调）；清除配置/恢复默认不可撤销
- **图标映射**：`TileInfo.iconResId` 数据字段，`TileCatalog.iconRes(value)` 查询；custom 图标用 `CustomTileUtils.getCustomTileIcon()`
- **添加磁贴**：WindowBottomSheet + LazyVerticalGrid 按分类分组，分类间 HorizontalDivider 分隔，点击特效限定在圆圈内
- **添加第三方磁贴**：`getAllQSTileServicesWithIcon()` 批量查询缓存（避免逐个全量扫描卡顿）；后台线程加载 + InfiniteProgressIndicator + fillMaxHeight
- **加载状态**：异步操作（Shizuku 绑定、权限检查、应用列表加载）必须加 isLoading 防闪烁
- **长按行为**：设置页 WindowSpinnerPreference 选择；应用选择器后台加载缓存
- **磁贴切换联动**：`wordless_mode_sync` / `smart_device_control_sync` 两个 SwitchPreference，映射**相反**（wordless 展开→0/收起→1；smart 展开→1/收起→0），写入在 ExTileService.onClick 切换成功后
- **主页权限卡片**：PermissionStatus 状态机渲染引导；ON_RESUME 自动刷新权限状态；`autoRequestAfterResume` 标记防重复弹窗；卡片抽为 `PermissionStatusCard` 供 OOBE 复用
- **主页统计卡片**：StatsCard，`getStatsFlow` 实时订阅，数据由 ExTileService.onClick 成功切换时写入
- **设置页「重新运行引导」**：仅重置 `oobe_completed=false`（配置数据保留），壳层监听到 false 自动切回 OOBE 页面，可再次导入覆盖
