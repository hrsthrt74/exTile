package com.hrsthrt74.qstile.ui.screens.settings

import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.data.ChangelogEntry
import com.hrsthrt74.qstile.data.ChangelogProvider
import com.hrsthrt74.qstile.data.ConfigRepository
import com.hrsthrt74.qstile.data.UpdateCheckState
import com.hrsthrt74.qstile.data.UpdateChecker
import com.hrsthrt74.qstile.ui.components.AppBottomSheet
import com.hrsthrt74.qstile.ui.components.AppCard
import com.hrsthrt74.qstile.ui.components.SheetState
import com.hrsthrt74.qstile.ui.sheetBottomPadding
import io.noties.markwon.Markwon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import android.graphics.Color as AndroidColor

/**
 * 检查更新 Sheet：自动检查开关 / 手动检查 / 当前版本的内置更新日志。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `UpdateSheet` 拆分而来，行为保持不变：
 * 打开不联网，只有点「手动检查」才请求，结果用 Toast 反馈；更新日志从打包进 assets 的
 * CHANGELOG 里按当前版本切一节，离线渲染。
 *
 * **注意**：下面这几个状态（自动检查开关、检查状态、版本号、内置日志）必须留在函数体顶层、
 * `AppBottomSheet` 之外——它们原先就在 Sheet 调用之前。塞进 content lambda 会导致
 * 每次打开都重读 assets（日志卡片先空一下）、关闭时丢状态。
 *
 * @param state Sheet 显示状态（由页面 `rememberSheetState()` 创建）
 * @param scope 页面级协程作用域（手动检查是 suspend 请求；本 Sheet 关闭后内容离开组合，
 *        用组件内作用域会把请求取消）
 */
@Composable
fun UpdateSheet(
    state: SheetState,
    scope: CoroutineScope,
) {
    val context = LocalContext.current

    // 自动检查更新开关：直连 DataStore，与 OOBE 隐私页的开关共享同一状态
    val autoCheckUpdate by ConfigRepository.getAutoCheckUpdateFlow(context)
        .collectAsState(initial = false)
    // 检查状态：来自 UpdateChecker 的进程级共享状态（冷启动自动检查的结果也会体现在这里）
    val checkState by UpdateChecker.state.collectAsState()
    // 是否正在检查：检查中要禁用手动按钮，避免并发重复请求
    val checking = checkState is UpdateCheckState.Checking
    // 当前安装版本号：用于在内置更新日志里匹配对应版本小节；
    // PackageManager 读取放 remember 里，避免每次重组都查一遍
    val currentVersionName = remember(context) {
        UpdateChecker.getCurrentVersionName(context).orEmpty()
    }
    // 内置更新日志：从随应用打包的 assets/CHANGELOG（按应用语言选中文/英文）里切出当前安装版本那一节。
    // 与检查更新完全解耦——打开 Sheet 就能看到，无需联网；文件很小，仍在 IO 线程读取
    val builtinChangelog by produceState<ChangelogEntry?>(initialValue = null, context, currentVersionName) {
        value = withContext(Dispatchers.IO) {
            ChangelogProvider.loadForVersion(context, currentVersionName)
        }
    }
    // 内置日志卡片的标题：「版本号 · 发布日期」（如 1.0.2 · 2026-10-10）。
    // 日期取自 CHANGELOG 节标题的括号，没写日期就只显示版本号；
    // 整节都读不到时退化为通用标题「更新日志」，避免标题空着
    val fallbackChangelogTitle = stringResource(R.string.update_changelog_title)
    val builtinChangelogTitle = builtinChangelog
        ?.let { entry -> entry.date?.let { date -> "${entry.version} · $date" } ?: entry.version }
        ?: fallbackChangelogTitle

    AppBottomSheet(
        state = state,
        title = stringResource(R.string.settings_update_sheet_title),
        // 整页滚动的内容，让它在标题栏下方滑过去（「延伸到顶部」）
        contentExtendsUnderHeader = true,
    ) { innerPadding ->
        // 整页滚动：日志不再单独限高内滚，长日志跟随整个 Sheet 内容一起滑动
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // 顶部让位必须放在 verticalScroll **内部**：滚动视口因此从面板顶端开始、
                // 首屏内容仍从标题栏下方开始，滚动时正文滑到标题栏下方被渐进模糊带盖住。
                // 放到 verticalScroll 外面（或交给 AppBottomSheet 的容器）的话，
                // 视口本身就在标题栏下方，正文永远滑不进头部区域。
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // 自动检查更新开关：开启后应用每次冷启动时会静默检查一次
            // 浮层内的卡片走 AppCard：玻璃风格自动换成 GlassCard（半透明，露出 Sheet 材质）
            AppCard(modifier = Modifier.fillMaxWidth()) {
                SwitchPreference(
                    title = stringResource(R.string.settings_update_auto_title),
                    checked = autoCheckUpdate,
                    onCheckedChange = { newValue ->
                        scope.launch {
                            ConfigRepository.saveAutoCheckUpdate(context, newValue)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 手动检查按钮：检查中禁用并用文案提示，防止并发重复请求。
            // 检查结果不再占用卡片，直接用 Toast 反馈（已是最新 / 发现新版本 / 暂无发布 / 失败）
            Button(
                onClick = {
                    scope.launch {
                        val result = UpdateChecker.checkForUpdate(context)
                        // Idle / Checking 是过程态，没有可提示的结论；其余终态各给一句
                        val message = when (result) {
                            is UpdateCheckState.UpToDate ->
                                context.getString(R.string.update_status_up_to_date)
                            is UpdateCheckState.Available ->
                                context.getString(R.string.update_status_available, result.latestVersion)
                            is UpdateCheckState.NoRelease ->
                                context.getString(R.string.update_status_no_release)
                            is UpdateCheckState.Failed ->
                                context.getString(R.string.update_status_failed)
                            else -> null
                        }
                        message?.let {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !checking,
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 检查中把按钮文案换成「正在检查更新…」，让按钮变灰有明确原因
                Text(
                    stringResource(
                        if (checking) R.string.update_status_checking else R.string.update_check_now
                    )
                )
            }

            // 当前版本更新日志：来自打包进 assets 的 CHANGELOG（按应用语言选择），
            // 离线渲染、与检查结果无关，始终显示
            val changelogEntry = builtinChangelog
            if (changelogEntry != null) {
                Spacer(modifier = Modifier.height(16.dp))

                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = builtinChangelogTitle,
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.Bold
                        )
                        // Markdown 渲染（标题/列表/加粗/链接等）：不设限高，跟随 Sheet 整体滚动
                        ChangelogMarkdownText(changelog = changelogEntry.body)
                    }
                }
            }

            // 底部避让导航栏（与其他 Sheet 保持一致）
            Spacer(modifier = Modifier.padding(bottom = sheetBottomPadding()))
        }
    }
}

/**
 * 更新日志 Markdown 渲染组件：用 Markwon 把 CHANGELOG 的 Markdown 源文本
 * 解析为富文本（标题/列表/加粗/行内代码/链接等）显示在 TextView 上。
 *
 * 与法律文档页（MarkdownDocumentContent）共用同一套 Markwon 解析方案，差异点：
 * - 字号更小（跟随设置页 footnote1 字号）、正文用摘要色，视觉上融入 Sheet 卡片；
 * - 高度与滚动由调用方控制（本组件只撑开内容完整高度），便于限高内滚动。
 *
 * 随 `UpdateSheet` 一并从 `SettingsScreen` 搬出，仍为文件内私有组件——它的字号/配色/滚动策略
 * 与法律文档页的实现不同，提升为公共组件只会诱导错误复用。
 *
 * @param changelog Markdown 源文本（CHANGELOG 某一节）。
 * @param modifier 应用到 AndroidView 容器上的修饰符（调用方传入限高 + 滚动）。
 */
@Composable
private fun ChangelogMarkdownText(changelog: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Markwon 实例按 Context 缓存：解析器构建有开销，重组时不重复创建
    val markwon = remember(context) { Markwon.builder(context).build() }
    // 正文/链接颜色随主题切换：正文用摘要色弱化层级，链接用主题色提示可点击
    val textColor = MiuixTheme.colorScheme.onSurfaceVariantSummary.toArgb()
    val linkColor = MiuixTheme.colorScheme.primary.toArgb()
    // 渲染基准字号：跟随 footnote1 的 sp 值（Markwon 的标题按此基准相对缩放），
    // 取不到有效 sp 值时退回 13sp，避免 TextView 字号异常
    val footnoteFontSize = MiuixTheme.textStyles.footnote1.fontSize
    val baseTextSize = if (footnoteFontSize.type == TextUnitType.Sp && footnoteFontSize.value > 0f) {
        footnoteFontSize.value
    } else {
        13f
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            TextView(viewContext).apply {
                setTextColor(textColor)
                setLinkTextColor(linkColor)
                // 透明背景：融入 Sheet 卡片底色
                setBackgroundColor(AndroidColor.TRANSPARENT)
                textSize = baseTextSize
                // 让 Markdown 里的链接可以点击打开
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            // 主题切换 / 字号变化时同步 View 属性，再重新应用 Markdown
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            textView.textSize = baseTextSize
            markwon.setMarkdown(textView, changelog)
        }
    )
}
