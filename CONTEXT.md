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
            │   │   ├── TileMapping.kt       # 系统磁贴映射表 + 图标映射
            │   │   ├── ConfigRepository.kt  # 配置持久化（DataStore）
            │   │   └── ThemeSettings.kt     # 主题设置
            │   ├── shizuku/
            │   │   ├── ShizukuHelper.kt     # Shizuku 状态/权限辅助
            │   │   ├── SecureSettingsHelper.kt # Secure Settings 读写
            │   │   └── CommandService.kt    # Shizuku UserService (AIDL)
            │   └── ui/
            │       ├── theme/
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
5. **Shizuku 权限**：通过 Shizuku UserService + AIDL 获取 WRITE_SECURE_SETTINGS
6. **配置备份/恢复**：JSON 格式导出/导入，支持从系统当前配置导入
7. **主题定制**：MIUIX 动态取色引擎，支持 Monet 取色、深色模式等；动态取色启用时禁用移动数据绿色特殊取色
8. **小米设备特化**：通过 DeviceCompat 检测 MIUI/HyperOS，手机显示 WLAN+移动数据固定卡片，平板显示 WLAN+蓝牙；编辑磁贴固定在参数末尾；固定磁贴和编辑磁贴支持 Tooltip 提示
9. **横滑切换页面**：使用 `HorizontalPager` + `MainPagerState` 实现主页/编辑/设置之间的横滑切换，配合底部导航栏联动
10. **开源许可页面**：独立 `LicensesActivity`，展示所有开源库信息，点击直接跳转浏览器查看项目地址
11. **设备感知磁贴**：通过 `DeviceType` 枚举（UNIVERSAL/XIAOMI_ONLY/AOSP_ONLY）控制磁贴可用性；`internet` 磁贴仅在 SDK < 37 且 AOSP 设备时显示
12. **触觉反馈**：各处点击添加震动反馈（LongPress/TextHandleMove）；滚动到边界触觉反馈（`scrollEndHaptic`）
13. **按压特效**：磁贴配置页面磁贴使用 `pressable` + `SinkFeedback` 实现按压缩放效果；添加磁贴 sheet 的磁贴点击特效限定在圆圈内
14. **加载状态**：首页权限卡片和调试工具页面支持加载状态显示，避免权限状态闪烁
15. **调试工具**：独立 `DebugToolsActivity`，提供详细调试信息（权限状态、磁贴配置、系统信息）、复制调试信息、添加磁贴到末尾等功能
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

- **导航**：使用 `HorizontalPager` + `MainPagerState` 实现横滑切换主页/编辑/设置，底部 `NavigationBar` 控制三个页面路由并联动。通过 `CompositionLocalProvider` + `rememberNavigationEventDispatcherOwner` 注入 navigation event dispatcher，供 MIUIX Overlay 组件使用
- **横滑动画**：使用三次贝塞尔曲线 `CubicBezierEasing(0.25, 0.1, 0.25, 1.0)`，350ms 时长，iOS 风格手感
- **页面保留**：`HorizontalPager` 设置 `beyondViewportPageCount = Screen.allPages.size`，保留所有页面状态，避免重新加载
- **导航栏模糊**：`rememberLayerBackdrop()` + `Modifier.layerBackdrop(backdrop)` 在 HorizontalPager 捕获内容，`Modifier.textureBlur(backdrop, ...)` 在 NavigationBar 上应用毛玻璃效果；底部 padding 只保留 `calculateTopPadding()`，让内容延伸到导航栏背后供模糊捕获
- **子页面导航**：开源许可、调试工具等子页面使用独立 Activity，通过 `Intent` 跳转，有系统默认转场动画；独立 Activity 需要提供 `LocalNavigationEventDispatcherOwner`
- **设置页**：使用 MIUIX Preference 组件（`WindowSpinnerPreference`、`SwitchPreference`、`ArrowPreference`）统一入口；调试工具入口放在最前面
- **弹窗/Sheet**：使用 Window 级别组件（`WindowBottomSheet`、`WindowDialog`），不依赖 Scaffold；返回事件需在内容内部添加 `BackHandler`；Sheet 底部间距为 `WindowInsets.navigationBars + 8.dp`
- **主题模型**：`ThemeSettings` 包含 `dayNightMode`（0=跟随/1=浅/2=深）+ `isDynamicColorMode`（动态取色开关），`ExTileTheme` 中通过 `SideEffect` 处理状态栏颜色反色
- **磁贴编辑页**：4 列 `LazyVerticalGrid`，Calvin-LL Reorderable 库实现长按拖拽排序；`key(selectedTabIndex, isXiaomi, fixedTileValues)` 确保状态正确重建；拖拽的 `from.index`/`to.index` 需减去前面固定卡片的偏移量（`indexOffset`）；点击磁贴弹出 `WindowListPopup` 操作菜单；固定磁贴支持 `TooltipBox` 提示（无箭头）
- **图标映射**：`TileMapping.iconRes(value)` 集中管理磁贴 → drawable 映射，新增图标只需加一行 `when` 分支；custom 磁贴通过 `getCustomTileIcon()` 获取其他应用图标
- **触觉反馈**：使用 `LocalHapticFeedback.current` 触发震动；`Modifier.scrollEndHaptic()` 实现滚动到边界触觉反馈；`Modifier.pressable()` + `SinkFeedback()` 实现按压特效
- **添加磁贴**：使用 `WindowBottomSheet` + `LazyVerticalGrid` 按分类分组显示；每个分类结束后添加 `HorizontalDivider` 分割线；点击特效限定在圆圈内
- **加载状态**：涉及异步操作（如 Shizuku 服务绑定、权限检查）的 UI 需要添加加载状态，使用 `isLoading` 变量控制显示"加载中..."，避免状态闪烁
- **长按行为**：设置页「磁贴行为」板块使用 `WindowSpinnerPreference` 选择长按行为；选择「跳转自定义应用」时显示 `ArrowPreference` 进入应用选择器；应用选择器 Sheet 的应用列表在 `Dispatchers.IO` 后台加载（缓存，避免重复查询），加载中显示 `InfiniteProgressIndicator`（默认样式）+「加载中...」文本并用 `fillMaxHeight` 撑满避免 sheet 高度突变；应用列表行不加左右边距（Sheet 自带边距）

---

## 调试

执行 `./debug.ps1` 进行一键构建→安装→启动调试。
