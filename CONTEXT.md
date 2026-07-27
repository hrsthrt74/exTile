# exTile 项目上下文

## 概述

**exTile** 是一个 Android 应用，通过 **Shizuku** 框架获取系统级权限，实现对 Android 快速设置面板（Quick Settings）磁贴的动态切换与配置管理。

| 属性 | 值 |
|------|-----|
| 包名 | `com.hrsthrt74.qstile` |
| 语言 | Kotlin 2.4.0 |
| UI | Jetpack Compose + Material 3 + MIUIX |
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
├── settings.gradle.kts          # 项目设置
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
            │   ├── MainActivity.kt          # 主 Activity，底部导航
            │   ├── tile/
            │   │   └── ExTileService.kt     # QS Tile Service
            │   ├── data/
            │   │   ├── TileConfig.kt        # 磁贴配置数据模型
            │   │   ├── TileMapping.kt       # 系统磁贴映射表（26种）
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
            │       └── screens/
            │           ├── HomeScreen.kt     # 主页
            │           ├── TileConfigScreen.kt # 磁贴编辑页
            │           └── SettingsScreen.kt # 设置页
            └── res/                         # 资源文件
```

---

## 架构

```
                    ┌─────────────────────────────┐
                    │        MainActivity          │
                    │  (Shizuku 权限监听 + 导航)    │
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
```

---

## 核心功能

1. **QS 磁贴一键切换**：通过 `ExTileService` 实现展开/收起两套布局间切换
2. **磁贴编辑**：支持拖拽排序、添加系统/自定义磁贴
3. **Shizuku 权限**：通过 Shizuku UserService + AIDL 获取 WRITE_SECURE_SETTINGS
4. **配置备份/恢复**：JSON 格式导出导入，支持从系统当前配置导入
5. **主题定制**：MIUIX 动态取色引擎，支持 Monet 取色、深色模式等

---

## 关键依赖

| 依赖 | 版本 |
|------|------|
| Android Gradle Plugin | 9.3.0 |
| Kotlin Compose Plugin | 2.4.0 |
| Compose BOM | 2026.02.01 |
| Navigation Compose | 2.8.5 |
| DataStore Preferences | 1.0.0 |
| Shizuku API | 13.1.5 |
| Shizuku Provider | 13.1.5 |
| MIUIX UI | 0.9.3 |

---

## 待办事项 (TODO)

- **UX 易用性改进**：当前界面和交互流程仍有优化空间，需更直观的引导和操作反馈
- **i18n 国际化**：目前所有字符串硬编码为中文，需抽离为字符串资源，支持多语言
- **错误处理增强**：各场景下（权限拒绝、Shizuku 未启动、命令执行失败等）的错误提示和恢复机制不够完善
- **无障碍适配**：缺少 TalkBack 等无障碍支持
- **单元测试覆盖**：当前测试覆盖不足，需补充核心逻辑的单元测试

---

## 调试

执行 `./debug.ps1` 进行一键构建→安装→启动调试。
