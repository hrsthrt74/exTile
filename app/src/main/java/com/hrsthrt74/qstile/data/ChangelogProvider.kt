package com.hrsthrt74.qstile.data

import android.content.Context
import android.util.Log

/**
 * 一节更新日志（对应 CHANGELOG.md 里的一个 `## 版本号` 小节）。
 *
 * @param version 版本号，如 `1.0.2`
 * @param date 发布日期（`YYYY-MM-DD`），取自节标题括号里的日期；标题没写日期时为 null
 * @param body Markdown 正文（不含标题行）
 */
data class ChangelogEntry(
    val version: String,
    val date: String?,
    val body: String
)

/**
 * 内置更新日志提供者。
 *
 * 更新日志不再依赖「必须先点一次检查更新」：仓库 `docs/CHANGELOG.md`（中文）与
 * `docs/CHANGELOG.en.md`（英文）在构建时被复制进 assets（见 `app/build.gradle.kts` 的
 * `copyDocsAssets` 任务），运行时按当前应用语言选文件、按 versionName 切出对应小节，
 * **完全离线**，打开 Sheet 就能看到。
 *
 * 文档格式约定（发版时在两个文件最上方各新增一节，节结构与顺序保持一致）：
 * ```
 * ## 1.0.2 (2026-10-10)
 * 正文……
 *
 * ## 1.0.1 (2026-10-04)
 * 正文……
 * ```
 * 二级标题由「版本号 + 括号里的发布日期」组成：版本号可带 v 前缀，日期用于在界面上展示
 * 「1.0.2 · 2026-10-10」，缺省也能正常工作（只显示版本号）。
 * 节内容从该标题下一行开始、到下一个二级标题为止；一级标题 `#` 是文档标题，
 * 三级及以下 `###` 是节内小标题，都不参与切分。
 */
object ChangelogProvider {

    private const val TAG = "ChangelogProvider"

    /** 中文更新日志文件名（仓库主文件，构建时从 docs/ 复制而来） */
    const val CHANGELOG_ASSET = "CHANGELOG.md"

    /** 英文更新日志文件名（与中文版同步维护，按当前应用语言选择） */
    const val CHANGELOG_ASSET_EN = "CHANGELOG.en.md"

    /** 节标题里的发布日期：只认 `YYYY-MM-DD` 这种写法，取第一个匹配项 */
    private val DATE_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")

    /**
     * 读取内置更新日志中属于 [versionName] 的那一节。
     *
     * 匹配顺序：完全一致（忽略 v 前缀与大小写）→ 数值等价（`1.0` 与 `1.0.0`）→
     * 兜底取文档第一节（约定为最新版本，避免「刚改了版本号还没补日志」时一片空白）。
     *
     * @return 匹配到的日志小节；assets 缺失、解析失败或文档里没有任何可用小节时返回 null
     */
    fun loadForVersion(context: Context, versionName: String): ChangelogEntry? {
        val markdown = readChangelog(context) ?: return null
        val sections = parseSections(markdown)
        if (sections.isEmpty()) return null

        // 归一化后的目标版本号：去空白与 v/V 前缀，统一小写便于忽略大小写比较
        val target = versionName.trim().removePrefix("v").removePrefix("V")

        // 1) 完全一致
        sections.firstOrNull { it.version.equals(target, ignoreCase = true) }
            ?.let { return it }
        // 2) 数值等价（1.0 与 1.0.0 视为同一版本），复用检查更新里的比较实现避免两套规则
        sections.firstOrNull { UpdateChecker.compareVersions(it.version, target) == 0 }
            ?.let { return it }
        // 3) 兜底：第一节（文档最上方即最新版本）
        return sections.first()
    }

    /**
     * 读取与当前应用语言匹配的更新日志：中文环境读 [CHANGELOG_ASSET]，
     * 其他语言读 [CHANGELOG_ASSET_EN]。
     * 首选文件缺失（例如某个语言还没翻译）时回退到另一个，保证日志不会整块消失。
     */
    private fun readChangelog(context: Context): String? {
        val chinese = isChineseLanguage(context)
        val primary = if (chinese) CHANGELOG_ASSET else CHANGELOG_ASSET_EN
        val fallback = if (chinese) CHANGELOG_ASSET_EN else CHANGELOG_ASSET
        return readAsset(context, primary) ?: readAsset(context, fallback)
    }

    /** 读取单个 assets 文件；失败只记日志并返回 null（不把异常抛给界面） */
    private fun readAsset(context: Context, assetName: String): String? = try {
        context.assets.open(assetName).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        Log.w(TAG, "读取内置更新日志失败：$assetName", e)
        null
    }

    /**
     * 当前应用是否使用中文。
     *
     * 取的是资源里的 `configuration.locales`（会跟随 Android 13+ 的「应用语言」设置，
     * 而不只是系统语言），主语言为 `zh` 即视为中文。
     */
    private fun isChineseLanguage(context: Context): Boolean {
        val locales = context.resources.configuration.locales
        // 极少数情况下 LocaleList 可能为空，此时按非中文处理（读英文日志）
        if (locales.isEmpty) return false
        return locales[0].language == "zh"
    }

    /**
     * 把 Markdown 全文切分为「## 版本号」小节。
     *
     * 逐行扫描而不是用正则粗暴分割：正文里的 `###` 小标题、代码块中的 `##` 都不会被误判
     * （代码块内的 `##` 前面通常有缩进或围栏，这里只认行首恰好两个井号加空格的写法）。
     */
    private fun parseSections(markdown: String): List<ChangelogEntry> {
        val sections = mutableListOf<ChangelogEntry>()
        // 当前正在收集的版本号与日期；version 为 null 表示还没进入任何版本小节（文档标题/维护说明）
        var version: String? = null
        var date: String? = null
        val buffer = StringBuilder()

        // 收尾当前小节：版本号解析失败或正文为空的节直接丢弃
        fun flush() {
            val currentVersion = version
            if (currentVersion == null) {
                buffer.setLength(0)
                return
            }
            val body = buffer.toString().trim()
            buffer.setLength(0)
            if (body.isNotEmpty()) sections += ChangelogEntry(currentVersion, date, body)
        }

        markdown.lineSequence().forEach { line ->
            val trimmed = line.trim()
            // 只认二级标题（"## " 开头且不是 "### "）：一级标题是文档标题，更深的层级是节内小标题
            if (trimmed.startsWith("## ") && !trimmed.startsWith("### ")) {
                flush()
                val heading = trimmed.removePrefix("## ")
                version = parseVersionFromHeading(heading)
                date = DATE_REGEX.find(heading)?.value
            } else if (version != null) {
                buffer.appendLine(line)
            }
        }
        flush()

        return sections
    }

    /**
     * 从二级标题文本里取出版本号：`1.0.2`、`v1.0.2`、`1.0.2 (2026-10-10)` 都能取到 `1.0.2`。
     *
     * 取法是「去掉 v 前缀后，从行首连续截取数字与小数点」，遇到空格/括号即停止；
     * 标题不以数字开头（如「历史版本」）时返回 null，该节不参与版本匹配，其正文也不会串到别的版本。
     */
    private fun parseVersionFromHeading(heading: String): String? {
        val text = heading.trim().removePrefix("v").removePrefix("V")
        val token = text.takeWhile { it.isDigit() || it == '.' }
        // 至少要含一个数字，"..." 这类异常标题不算版本号
        return token.takeIf { it.any(Char::isDigit) }
    }
}
