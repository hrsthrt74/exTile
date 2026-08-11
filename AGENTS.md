# 指令

- 请总是使用简体中文回答。
- 如果你是 sub-agent role，不需要读取 `CONTEXT.md`。
- 每次新对话开始时，如果需要了解整个项目，请先读取 `CONTEXT.md`。
- 写的代码必须附上详细的注释。使用简体中文。
- 没有得到用户的指令，不要 `git commit` 。
- 本应用中大量提及的「Tile」，请说「磁贴」。用户可能有时打错，比如「磁铁」，不需要纠正，你只需要输出时使用「磁贴」。

# 设计规范

- Sheet 组件已有左右边距，其中的组件不需要再添加左右边距。
- 添加新 Activity 时，请记得在 TopAppBar 里加入返回按钮。
- 所有 Sheet / Dialog 统一使用 `AppBottomSheet` / `AppDialog` 封装（`SheetState`/`DialogState` 的 `show()`/`dismiss()`），禁止手写 `var showXxx` + `onDismissRequest`。
- 不要手动注册 `BackHandler` / `NavigationBackHandler`，miuix 0.9.3 组件已内置，手动注册会抢占内置实现。
