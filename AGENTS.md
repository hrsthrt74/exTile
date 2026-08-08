# 指令

- 请总是使用简体中文回答用户。
- 每次新对话开始时，请先读取 `CONTEXT.md` 以获取项目上下文。
- 写的代码必须附上详细的注释。使用简体中文编写。
- 没有得到用户的指令，不要擅自 `git commit` 。
- 本应用中大量提及的「Tile」，请说「磁贴」。用户可能有时打错字，比如「磁铁」，不需要纠正，你只需要记住你输出时总是使用「磁贴」。

# 设计规范

- Sheet 组件已有左右边距，其中的组件不需要再添加左右边距。
- 添加新 Activity 时，请记得在 TopAppBar 里加入返回按钮。
- 所有 Sheet / Dialog 统一使用 `AppBottomSheet` / `AppDialog` 封装（`SheetState`/`DialogState` 的 `show()`/`dismiss()`），禁止手写 `var showXxx` + `onDismissRequest`。
- 不要手动注册 `BackHandler` / `NavigationBackHandler`，miuix 0.9.3 组件已内置，手动注册会抢占内置实现。

# 关于调试

如果用户需要【构建->安装至手机->启动】来进行调试的话，请执行 `./debug.ps1` 。