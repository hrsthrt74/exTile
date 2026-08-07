# exTile 项目上下文

## 概述

**exTile** 是一个 Android 应用，通过 **Shizuku** 框架获取系统级权限，实现对 Android 快速设置面板（Quick Settings）磁贴的动态切换与配置管理。

| 属性 | 值 |
|------|-----|
| 包名 | `com.hrsthrt74.qstile` |
| 语言 | Kotlin 2.4.0 |
| UI | Jetpack Compose + MIUIX (HyperOS 设计) |
| 构建系统 | Gradle (Kotlin DSL) + AGP 9.3.0 |
| 最低 SDK | Android 13 (API 33) |
| 目标 SDK | Android 15 (API 36) |
| 编译 SDK | Android 17 (API 37) |
| Java 版本 | Java 11 |

---

## 目录结构

```
exTile/
├── AGENTS.md                    # AI 助手指令
├── debug.ps1                    # 一键构建→安装→启动脚本
├── CONTEXT.md                   # 本文件（持久上下文）
├── TODO.md                      # 待办事项
├── settings.gradle.kts          # 项目设置（含 JitPack 仓库）
├── build.gradle.kts             # 顶级构建脚本
├── gradle.properties            # Gradle 配置
├── local.properties             # 本地 SDK 路径
├── gradlew / gradlew.bat        # Gradle Wrapper
├── gradle/
│   └── libs.versions.toml       # 版本目录
└── app/
    ├── build.gradle.kts         # 模块构建脚本
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── aidl/
            │   └── .../ICommandService.aidl
            ├── java/com/hrsthrt74/qstile/
            │   ├── MainActivity.kt          # 主 Activity，横滑导航 + 模糊
            │   ├── LicensesActivity.kt      # 开源许可 Activity（独立页面）
            │   ├── DebugToolsActivity.kt    # 调试工具 Activity（独立页面）
            │   ├── TileLongClickActivity.kt # 幽灵桥接 Activity（长按磁贴行为分发）
            │   ├── tile/
            │   │   └── ExTileService.kt     # QS Tile Service
            │   ├── data/
            │   │   ├── TileConfig.kt        # 磁贴配置数据模型
            │   │   ├── TileCatalog.kt       # 磁贴目录（TileInfo 静态声明 + 查询）
            │   │   ├── TileRequirement.kt   # 磁贴可用性需求（sealed flag 约束 + prop 门控注册表）
            │   │   ├── DeviceProfile.kt     # 设备能力快照（可用性统一求值）
            │   │   ├── TileCapabilityFlags.kt # 能力 flag 调试开关（内存态，调试工具页模拟）
            │   │   ├── CustomTileUtils.kt   # 第三方磁贴工具（解析/查询/图标）
            │   │   ├── ConfigRepository.kt  # 配置持久化（DataStore）
            │   │   └── ThemeSettings.kt     # 主题设置
            │   ├── shizuku/
            │   │   ├── ShizukuHelper.kt     # Shizuku 状态/权限辅助
            │   │   ├── SecureSettingsHelper.kt # Secure Settings 读写
            │   │   └── CommandService.kt    # Shizuku UserService (AIDL)
│               └── ui/
│                   ├── components/
│                   │   ├── AppBottomSheet.kt  # 统一底部 Sheet（SheetState + AppBottomSheet）
│                   │   └── AppDialog.kt       # 统一对话框（DialogState + AppDialog + ConfirmDialog）
│                   ├── theme/
            │       │   ├── Color.kt         # 预定义颜色
            │       │   └── Theme.kt         # 动态主题控制器
            │       ├── navigation/
            │       │   └── MainPagerState.kt # 横滑翻页状态管理
            │       └── screens/
            │           ├── HomeScreen.kt     # 主页
            │           ├── TileConfigScreen.kt # 磁贴编辑页（拖拽网格）
            │           ├── SettingsScreen.kt # 设置页
            │           ├── LicensesScreen.kt # 开源许可页
            │           └── DebugToolsScreen.kt # 调试工具页
            └── res/
                └── drawable/                # 磁贴图标（tile_*.xml）
```

---

## 架构

```
                    ┌─────────────────────────────┐
                    │        MainActivity          │
                    │  (Shizuku 权限监听 + 横滑导航) │
                    │  HorizontalPager + NavigationBar │
                    │  NavigationBar 毛玻璃模糊     │
                    └──────┬──────────────────────┘
            ┌──────────────┼──────────────┐
            v              v              v
       HomeScreen    TileConfigScreen  SettingsScreen
             │              │              │
             └──────────────┼──────────────┘
                            │
                 ┌──────────▼──────────┐
                 │   ConfigRepository   │  ← DataStore Preferences
                 └─────────────────────┘
                            │
                 ┌──────────▼──────────┐
                 │ SecureSettingsHelper │  ← 读写 sysui_qs_tiles
                 └──────────┬──────────┘
                            │
                 ┌──────────▼──────────┐
                 │   CommandService     │  ← Shizuku UserService (AIDL)
                 └──────────┬──────────┘
                            │
                 ┌──────────▼──────────┐
                 │   Runtime.exec()     │  ← shell 命令执行
                 └─────────────────────┘

    ┌────────────────────────────────────┐
    │         LicensesActivity           │
    │  (独立 Activity，开源许可页面)       │
    │  点击项目直接跳转浏览器              │
    └────────────────────────────────────┘

    ┌────────────────────────────────────┐
    │        DebugToolsActivity          │
    │  (独立 Activity，调试工具页面)       │
    │  查看状态/调试信息/添加磁贴          │
    └────────────────────────────────────┘
```

---

## 核心功能

1. **QS 磁贴一键切换**：通过 `ExTileService` 实现展开/收起两套布局间切换
2. **磁贴编辑**：4 列网格拖拽排序（Calvin-LL/Reorderable），支持添加系统/自定义磁贴；长按放大 1.1 倍反馈；文本居中显示；小米设备特化固定卡片 + 编辑磁贴 badge；添加磁贴使用网格布局按分类分组
3. **磁贴操作菜单**：点击普通磁贴弹出 `WindowListPopup` 菜单，支持移动到顶端/底端、删除操作；菜单分三组（磁贴名、移动、删除），使用 `DropdownImpl` + `HorizontalDivider` 实现
4. **自定义磁贴图标**：通过 `PackageManager` 获取其他应用的 TileService 图标，支持 `QUERY_ALL_PACKAGES` 权限引导
5. **权限架构（Shizuku 可选）**：核心权限是 `WRITE_SECURE_SETTINGS`（写入 sysui_qs_tiles 切换展开/收起）；Shizuku 仅用于首次/失效时通过 `pm grant` 自动授权，一旦应用持有该权限即可完全脱离 Shizuku。`ShizukuHelper.PermissionStatus` 状态机（GRANTED / SHIZUKU_NOT_INSTALLED / SHIZUKU_NOT_RUNNING / SHIZUKU_NOT_GRANTED / NEEDS_PM_GRANT / GRANT_FAILED）驱动主页权限卡片引导；`SecureSettingsHelper` 读写**直接 API 优先、Shizuku 兜底**；不装 Shizuku 时可通过 adb 手动授权（主页提供命令复制弹窗）；`grantWriteSecureSettings` 成功判据为「非 ERROR 前缀」（pm grant 成功时输出为空串）
6. **配置备份/恢复**：JSON 格式导出/导入，支持从系统当前配置导入
7. **主题定制**：MIUIX 动态取色引擎，支持 Monet 取色、深色模式等；动态取色启用时禁用移动数据绿色特殊取色
8. **小米设备特化**：通过 DeviceCompat 检测 MIUI/HyperOS，手机显示 WLAN+移动数据固定卡片，平板显示 WLAN+蓝牙；编辑磁贴固定在参数末尾；固定磁贴和编辑磁贴支持 Tooltip 提示
9. **横滑切换页面**：使用 `HorizontalPager` + `MainPagerState` 实现主页/编辑/设置之间的横滑切换，配合底部导航栏联动
10. **开源许可页面**：独立 `LicensesActivity`，展示所有开源库信息，点击直接跳转浏览器查看项目地址
11. **设备感知磁贴**：通过 flag 式需求集合 `TileRequirement`（sealed interface：XIAOMI_ONLY / AOSP_ONLY / MAX_SDK_36 / SATELLITE / COOLING_FAN / `RequiresProp(key)` / `RequiresFeature(feature)`）声明每个磁贴的可用性约束；`DeviceProfile`（isXiaomi / isTablet / sdkInt / 卫星 / 风扇 / gatedProps / features）一次性采集设备能力并统一求值（`satisfies`，AND 语义）；`internet` 磁贴声明为 AOSP_ONLY + MAX_SDK_36（替代原硬编码特判）；SATELLITE / COOLING_FAN 为 TODO 占位（恒满足，保持现状可见性），待 prop 检测实现后收紧，求值时优先采用 `TileCapabilityFlags` 调试覆盖（卫星/风扇为 nullable 开关，prop/feature 为 `propOverrides`/`featureOverrides` 表，覆盖优先于设备检测能力）；`RequiresProp` 通过反射读 `android.os.SystemProperties`，语义为「prop 明确为 0/false/no 才隐藏，true/未定义视为支持」（避免误伤未定义 prop 的 AOSP 设备，如单手模式），prop key 集中在 `TileRequirement.gatedPropKeys` 注册表（`gatedPropLabels` 提供显示名），`DeviceProfile.from` 一次性批量读取缓存；`RequiresFeature` 通过 `PackageManager.hasSystemFeature` 检测硬件能力（false 才隐藏，检测异常视为支持），feature 集中在 `gatedFeatureKeys`/`gatedFeatureLabels` 注册表，`DeviceProfile.from` 一次性批量检测缓存；已接入 prop：实时字幕 / 对话翻译 / 单手模式；已接入 feature：NFC / 自动亮度（环境光）/ 振动（马达，用 `VibratorManager.hasVibrator()` 运行时检测而非 `hasSystemFeature`，因不少 ROM 未声明 vibrator feature 会误判）/ 手电筒（闪光灯）/ 移动数据（蜂窝网）/ 相机（bt、自动旋转、麦克风、GPS、WLAN、各类 sensor 因所有目标设备都支持而未接入）；原 `DeviceType` 枚举已废弃
12. **触觉反馈**：各处点击添加震动反馈（LongPress/TextHandleMove）；滚动到边界触觉反馈（`scrollEndHaptic`）
13. **按压特效**：磁贴配置页面磁贴使用 `pressable` + `SinkFeedback` 实现按压缩放效果；添加磁贴 sheet 的磁贴点击特效限定在圆圈内
14. **加载状态**：首页权限卡片和调试工具页面支持加载状态显示，避免权限状态闪烁
15. **调试工具**：独立 `DebugToolsActivity`，提供详细调试信息（权限状态、磁贴配置、系统信息）、复制调试信息、添加磁贴到末尾等功能；调试开关（能力 flag / prop 门控 / 硬件特性）集中在「能力开关（调试）」`AppBottomSheet` 中（入口为操作卡片内的「能力开关（调试）」按钮，内容用 `LazyColumn` 可滚动；能力 flag 为 `SwitchPreference` 开关 `TileCapabilityFlags` 模拟卫星通讯/散热风扇；prop 门控遍历 `TileRequirement.gatedPropKeys` 自动生成开关模拟各 prop；硬件特性遍历 `gatedFeatureKeys` 自动生成开关模拟 NFC/自动亮度/振动/手电筒/移动数据/相机特性），均为内存态（重启恢复），用于测试对应磁贴可见性
16. **长按 exTile 磁贴行为**：通过幽灵桥接 Activity（`TileLongClickActivity`）实现；系统长按第三方磁贴时会启动 `ACTION_QS_TILE_PREFERENCES` 对应的 Activity，该 Activity 不显示界面，读取「长按 exTile 磁贴行为」配置（跳转 exTile / 跳转系统设置 / 跳转自定义应用）分发后立即 `finish()`；`ConfigRepository` 提供 `LongPressBehavior` 常量 + 行为/自定义应用包名存储；应用选择器使用后台线程加载应用列表 + `InfiniteProgressIndicator` 加载状态避免卡顿

---

## 关键依赖

| 依赖 | 版本 |
|------|------|
| Android Gradle Plugin | 9.3.0 |
| Kotlin Compose Plugin | 2.4.0 |
| Compose BOM | 2026.02.01 |
| Navigation Compose | 2.8.5 |
| NavigationEvent Compose | 1.1.2 |
| Activity Compose | 1.13.0 |
| DataStore Preferences | 1.0.0 |
| Shizuku API | 13.1.5 |
| Shizuku Provider | 13.1.5 |
| MIUIX UI | 0.9.3 |
| MIUIX Preference | 0.9.3 |
| MIUIX Blur | 0.9.3 |
| Calvin-LL/Reorderable | 3.1.0 |
| DeviceCompat | 2.6 |

---

## UI 设计规范

- **导航**：使用 `HorizontalPager` + `MainPagerState` 实现横滑切换主页/编辑/设置，底部 `NavigationBar` 控制三个页面路由并联动。navigation event dispatcher 由 `ComponentActivity` 自动提供（activity 1.13+，`enableOnBackInvokedCallback="true"`），**不要**再手动 `rememberNavigationEventDispatcherOwner` 注入（root dispatcher 无 input，会覆盖 Activity 自动 dispatcher、导致返回不响应）
- **横滑动画**：使用三次贝塞尔曲线 `CubicBezierEasing(0.25, 0.1, 0.25, 1.0)`，350ms 时长，iOS 风格手感
- **页面保留**：`HorizontalPager` 设置 `beyondViewportPageCount = Screen.allPages.size`，保留所有页面状态，避免重新加载
- **导航栏模糊**：`rememberLayerBackdrop()` + `Modifier.layerBackdrop(backdrop)` 在 HorizontalPager 捕获内容，`Modifier.textureBlur(backdrop, ...)` 在 NavigationBar 上应用毛玻璃效果；底部 padding 只保留 `calculateTopPadding()`，让内容延伸到导航栏背后供模糊捕获
- **子页面导航**：开源许可、调试工具等子页面使用独立 Activity，通过 `Intent` 跳转，有系统默认转场动画；独立 Activity 需要提供 `LocalNavigationEventDispatcherOwner`
- **设置页**：卡片式分类结构（通用 / 外观 / 数据 / 关于），使用 MIUIX Preference 组件（`WindowSpinnerPreference`、`SwitchPreference`、`ArrowPreference`）统一入口。「调试工具」入口为 TopAppBar 右上角的 `IconButton`（`MiuixIcons.Settings` 图标）直接跳转 Activity；4 个分类用 `ExpandableSettingsCard` 实现手风琴互斥展开（`SettingsCategory` 枚举 + `expandedCategory` 状态，点击当前展开的分类则全部折叠），内容用 `AnimatedVisibility` 平滑展开/收起；标题行含 `CategoryIcon`（primary 背景 `squircleClip` 圆角矩形 + onPrimary 图标）、headline1 字号标题、随展开状态旋转 90° 的 `MiuixIcons.Basic.ArrowRight` 箭头（`animateFloatAsState`）；点击标题行有 `HapticFeedbackType.LongPress` 震动；标题行与内容用 `HorizontalDivider` 分隔。分类图标：通用 Tune / 外观 Background / 数据 Backup / 关于 Info；卡片列表整体垂直居中（外层 `Column` + `verticalArrangement = Arrangement.Center` + LazyColumn `weight(1f, fill = false)`，内容超高时自动受限并恢复滚动）
- **弹窗/Sheet**：使用 Window 级别组件（`WindowBottomSheet`、`WindowDialog`），不依赖 Scaffold；Sheet 底部间距为 `WindowInsets.navigationBars + 8.dp`
- **统一底部 Sheet**：所有 `WindowBottomSheet` 统一封装为 `AppBottomSheet`（`ui/components/AppBottomSheet.kt`）。页面使用 `rememberSheetState()` 创建 `SheetState`（`show`/`show()`/`dismiss()`），打开用 `xxxState.show()`；关闭（点击遮罩 / 返回键 / 下拉拖拽）由 `AppBottomSheet` 内部统一调用 `dismiss()`，**禁止**再手写 `var showXxx by remember {...}` + `onDismissRequest = { showXxx = false }`。返回键行为由 `allowDismiss` 参数统一控制（miuix 0.9.3 内置 NavigationBackHandler，无需手动 BackHandler）；需要关闭后清理临时状态（如清空输入框）时用 `onDismissed` 回调（动画完成后触发）。**毛玻璃背景暂不实现**：曾尝试改用 `OverlayBottomSheet` + `textureBlur`（同 window 才能采样模糊，Window 系组件的 Dialog 独立窗口无法采样），但模糊层与面板动画 `graphicsLayer` 不同步、拖动条区无法覆盖，且 miuix 官方将在 0.9.4 优化并可能提供官方写法，故回退为 WindowBottomSheet 纯色实现，待官方方案发布后再跟进
- **统一对话框**：所有 `WindowDialog` 统一封装为 `AppDialog`（`ui/components/AppDialog.kt`）。页面使用 `rememberDialogState()` 创建 `DialogState`，打开用 `xxxDialogState.show()`；关闭（点击遮罩 / 返回键）由 `AppDialog` 内部统一调用 `dismiss()`，**禁止**再手写 `var showXxx by remember {...}` + `onDismissRequest = { showXxx = false }`。**`AppDialog` 默认样式即为「取消 + 确认」按钮**（`cancelText`/`confirmText`/`destructive` 可配，`onConfirm` 执行后自动关闭，`content` 放按钮区上方的额外内容），无独立 ConfirmDialog 组件；磁贴配置页三个确认操作、设置页导入确认、主页 adb 授权均使用该默认样式
- **预测式返回（Predictive Back）**：miuix 0.9.3 的 `BottomSheetContentLayout`/`DialogContentLayout`/`ListPopupLayout`/`SearchBar` 等组件**已内置** `NavigationBackHandler`（跟手下滑 + 遮罩淡出 + 取消回弹），应用层只需 `activity-compose 1.13.0` + Manifest `<application android:enableOnBackInvokedCallback="true">`，**不要**手动注册任何 BackHandler（会抢占内置实现、导致无跟手动画）。Activity 间转场动画由系统自动处理；手写 `PredictiveBackDismiss` 方案已弃用并删除
- **主题模型**：`ThemeSettings` 包含 `dayNightMode`（0=跟随/1=浅/2=深）+ `isDynamicColorMode`（动态取色开关），`ExTileTheme` 中通过 `SideEffect` 处理状态栏颜色反色
- **磁贴编辑页**：4 列 `LazyVerticalGrid`，Calvin-LL Reorderable 库实现长按拖拽排序；`key(selectedTabIndex, isXiaomi, fixedTileValues)` 确保状态正确重建；拖拽的 `from.index`/`to.index` 需减去前面固定卡片的偏移量（`indexOffset`）；点击磁贴弹出 `WindowListPopup` 操作菜单；固定磁贴支持 `TooltipBox` 提示（无箭头）
- **图标映射**：图标资源 id 并入 `TileCatalog.TileInfo.iconResId`（数据字段），`TileCatalog.iconRes(value)` 查询；新增图标只需在清单条目中声明；custom 磁贴通过 `CustomTileUtils.getCustomTileIcon()` 获取其他应用图标
- **触觉反馈**：使用 `LocalHapticFeedback.current` 触发震动；`Modifier.scrollEndHaptic()` 实现滚动到边界触觉反馈；`Modifier.pressable()` + `SinkFeedback()` 实现按压特效
- **添加磁贴**：使用 `WindowBottomSheet` + `LazyVerticalGrid` 按分类分组显示；每个分类结束后添加 `HorizontalDivider` 分割线；点击特效限定在圆圈内
- **添加第三方磁贴**：使用 `getAllQSTileServicesWithIcon()` 一次 `queryIntentServices` 批量取回全部 QS Tile 服务的 label/应用名/图标（避免渲染时逐个 `getCustomTileIcon` 反复全量扫描导致卡顿）；在 `Dispatchers.IO` 后台加载并缓存，加载态显示 `InfiniteProgressIndicator`（默认样式）+「加载中...」并用 `fillMaxHeight` 撑满防高度突变；网格 item 用 `key = 包名/类名` 复用；图标统一 `tint = primary`（第三方图标多为白色单色，浅色模式下不可用 `Color.Unspecified` 保留原色）
- **加载状态**：涉及异步操作（如 Shizuku 服务绑定、权限检查）的 UI 需要添加加载状态，使用 `isLoading` 变量控制显示"加载中..."，避免状态闪烁
- **长按行为**：设置页「通用」分类使用 `WindowSpinnerPreference` 选择长按行为；选择「跳转自定义应用」时显示 `ArrowPreference` 进入应用选择器；应用选择器 Sheet 的应用列表在 `Dispatchers.IO` 后台加载（缓存，避免重复查询），加载中显示 `InfiniteProgressIndicator`（默认样式）+「加载中...」文本并用 `fillMaxHeight` 撑满避免 sheet 高度突变；应用列表行不加左右边距（Sheet 自带边距）
- **磁贴切换联动设置**：设置页「通用」分类两个 `SwitchPreference`「展开收起同时控制无字模式」/「展开收起同时控制融合设备中心」，仅持久化开关状态（DataStore key `wordless_mode_sync` / `smart_device_control_sync`）；实际写入在 `ExTileService.onClick` 切换磁贴展开/收起布局成功后，开启时通过 `SecureSettingsHelper.putSecureSetting` 写入 secure settings。注意两个 key 映射**相反**：wordless_mode 展开→0 / 收起→1；smart_device_control 展开→1 / 收起→0
- **主页权限卡片**：按 `PermissionStatus` 状态机渲染引导文案和按钮（安装 Shizuku / 启动 Shizuku / 授予权限 / 自动授权 / 重试）；未授权标题显示「未获得必须权限」；始终提供「使用 adb 手动授权」入口，弹窗含「复制命令」按钮；通过 `DisposableEffect` + `LifecycleEventObserver` 监听 `ON_RESUME` 自动刷新权限状态，从「启动 Shizuku」跳转返回时自动衔接请求授权（`autoRequestAfterResume` 标记，避免反复弹窗）

---

## 调试

执行 `./debug.ps1` 进行一键构建→安装→启动调试。
