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
            │           └── LicensesScreen.kt # 开源许可页
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
```

---

## 核心功能

1. **QS 磁贴一键切换**：通过 `ExTileService` 实现展开/收起两套布局间切换
2. **磁贴编辑**：4 列网格拖拽排序（Calvin-LL/Reorderable），支持添加系统/自定义磁贴；小米设备特化固定卡片 + 编辑磁贴 badge
3. **Shizuku 权限**：通过 Shizuku UserService + AIDL 获取 WRITE_SECURE_SETTINGS
4. **配置备份/恢复**：JSON 格式导出/导入，支持从系统当前配置导入
5. **主题定制**：MIUIX 动态取色引擎，支持 Monet 取色、深色模式等
6. **小米设备特化**：通过 DeviceCompat 检测 MIUI/HyperOS，手机显示 WLAN+移动数据固定卡片，平板显示 WLAN+蓝牙；编辑磁贴固定在参数末尾
7. **横滑切换页面**：使用 `HorizontalPager` + `MainPagerState` 实现主页/编辑/设置之间的横滑切换，配合底部导航栏联动
8. **开源许可页面**：独立 `LicensesActivity`，展示所有开源库信息，点击直接跳转浏览器查看项目地址
9. **设备感知磁贴**：通过 `DeviceType` 枚举（UNIVERSAL/XIAOMI_ONLY/AOSP_ONLY）控制磁贴可用性
   - **小米专属**：`batterysaver`（省电）、`aisubtitles`（实时字幕）、`aitranslate`（翻译）、`carsickness`（晕车缓解）、`gps`（GPS）、`autobrightness`（自动亮度）、`settings`（设置）、`voicetrans`（对话翻译）、`papermode`（护眼模式）、`dolbyatomssound`（杜比全景声）、`quietmode`（勿扰模式）、`freeformhang`（迷你小窗）、`scanner`（扫一扫）、`taskmanager`（运行中的应用）、`night`（深色模式）
   - **原生专属**：`saver`（省电模式）、`dnd`（勿扰模式）、`location`（位置信息）

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
- **子页面导航**：开源许可等子页面使用独立 Activity，通过 `Intent` 跳转，有系统默认转场动画
- **设置页**：使用 MIUIX Preference 组件（`WindowSpinnerPreference`、`SwitchPreference`、`ArrowPreference`）统一入口
- **弹窗/Sheet**：使用 Window 级别组件（`WindowBottomSheet`、`WindowDialog`），不依赖 Scaffold；返回事件需在内容内部添加 `BackHandler`；Sheet 底部间距为 `WindowInsets.navigationBars + 8.dp`
- **主题模型**：`ThemeSettings` 包含 `dayNightMode`（0=跟随/1=浅/2=深）+ `isDynamicColorMode`（动态取色开关），`ExTileTheme` 中通过 `SideEffect` 处理状态栏颜色反色
- **磁贴编辑页**：4 列 `LazyVerticalGrid`，Calvin-LL Reorderable 库实现长按拖拽排序；`key(selectedTabIndex, isXiaomi, fixedTileValues)` 确保状态正确重建；拖拽的 `from.index`/`to.index` 需减去前面固定卡片的偏移量（`indexOffset`）
- **图标映射**：`TileMapping.iconRes(value)` 集中管理磁贴 → drawable 映射，新增图标只需加一行 `when` 分支

---

## 待办事项 (TODO)

- **i18n 国际化**：目前所有字符串硬编码为中文，需抽离为字符串资源，支持多语言
- **自定义图标**：当前使用 MIUIX 内置图标，可通过 Android Vector Drawable 添加自定义图标
- **错误处理增强**：各场景下（权限拒绝、Shizuku 未启动、命令执行失败等）的错误提示和恢复机制不够完善
- **无障碍适配**：缺少 TalkBack 等无障碍支持
- **单元测试覆盖**：当前测试覆盖不足，需补充核心逻辑的单元测试
- **Github Actions 自动构建**: 公开后构建测试版

---

## 调试

执行 `./debug.ps1` 进行一键构建→安装→启动调试。
