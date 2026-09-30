package com.hrsthrt74.qstile.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 检查更新的结果状态。
 *
 * 状态机：Idle（未检查）→ Checking（检查中）→ 以下四种终态之一：
 * UpToDate（已是最新）/ NoRelease（仓库暂无发布）/ Failed（检查失败）/ Available（发现新版本）。
 * 再次检查会重新走一遍 Checking → 终态。
 */
sealed interface UpdateCheckState {
    /** 尚未检查过（初始状态） */
    data object Idle : UpdateCheckState

    /** 正在检查 */
    data object Checking : UpdateCheckState

    /** 远端版本不高于当前版本，已是最新 */
    data object UpToDate : UpdateCheckState

    /** 仓库还没有发布任何 Release（HTTP 404），不算失败 */
    data object NoRelease : UpdateCheckState

    /** 网络请求失败 / 响应异常 */
    data object Failed : UpdateCheckState

    /** 发现新版本：携带版本号、更新日志原文（Markdown 纯文本展示）与 Release 页面链接 */
    data class Available(
        val latestVersion: String,
        val changelog: String,
        val releaseUrl: String
    ) : UpdateCheckState
}

/**
 * GitHub Release 更新检查器。
 *
 * 只做一件事：请求本仓库最新的 Release（`releases/latest`），与当前安装版本比较。
 * 不引入任何第三方网络/JSON 库，直接用内置的 HttpURLConnection + org.json。
 *
 * 状态通过 [state]（StateFlow）暴露：
 * - 版本 Sheet（设置页）读取它渲染「上次检查结果」；
 * - 壳层（MainApp）观察它，仅当变为 [UpdateCheckState.Available] 时弹「发现新版本」对话框；
 *   无更新 / 暂无发布 / 失败均静默，不打扰用户。
 *
 * 自动检查的触发条件（见 [autoCheckIfNeeded]）：用户开启「自动检查更新」且已完成 OOBE，
 * 每次进程冷启动最多一次。
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"

    /** GitHub Releases API：取最新一次正式发布（不含 prerelease/draft） */
    private const val RELEASE_API_URL = "https://api.github.com/repos/hrsthrt74/exTile/releases/latest"

    /** Release 页面兜底链接（响应缺少 html_url 时使用） */
    private const val RELEASES_PAGE_URL = "https://github.com/hrsthrt74/exTile/releases"

    /** 连接 / 读取超时（毫秒）：检查更新不应阻塞太久，失败就静默放弃 */
    private const val TIMEOUT_MS = 10_000

    /** 最近一次检查结果（内存态，进程内共享；进程重启后回到 Idle） */
    private val _state = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val state: StateFlow<UpdateCheckState> = _state.asStateFlow()

    /** 「发现新版本」对话框在本进程内是否已弹过（防止 OOBE 重跑等场景重复弹窗） */
    @Volatile
    var updatePromptShown: Boolean = false

    /** 自动检查在本进程内是否已执行过（含开关关闭的情况），保证每次冷启动最多检查一次 */
    @Volatile
    private var autoCheckDone = false

    /**
     * 自动检查入口：进程冷启动时由 MainActivity 调用。
     * 满足以下全部条件才真正发起检查，否则直接返回：
     * - 本进程尚未自动检查过（旋转屏幕等 Activity 重建不会重复请求）；
     * - 用户开启了「自动检查更新」（默认关闭，未经同意不联网）；
     * - OOBE 已完成（引导期间不打扰用户）。
     *
     * 检查结果只更新 [state]：发现新版本时由壳层弹窗提示；
     * 无更新 / 暂无发布 / 失败一律静默（仅写日志）。
     */
    suspend fun autoCheckIfNeeded(context: Context) {
        if (autoCheckDone) return
        autoCheckDone = true

        // 开关默认关闭；关闭时连网络请求都不发起
        if (!ConfigRepository.getAutoCheckUpdate(context)) return
        // OOBE 未完成时不检查（首次引导期间用户注意力在配置流程上）
        if (!ConfigRepository.getOobeCompletedFlow(context).first()) return

        val result = checkForUpdate(context)
        when (result) {
            is UpdateCheckState.Available ->
                Log.i(TAG, "自动检查发现新版本：${result.latestVersion}")
            // 静默：无更新 / 暂无发布 / 失败都不弹任何提示
            else -> Log.i(TAG, "自动检查完成（静默）：$result")
        }
    }

    /**
     * 手动检查入口：版本 Sheet 的「手动检查」按钮调用。
     * 无论如何都会真正发起请求，并把过程与结果写入 [state] 供界面渲染。
     *
     * @return 本次检查结果（与 [state] 当前值一致）
     */
    suspend fun checkForUpdate(context: Context): UpdateCheckState {
        _state.value = UpdateCheckState.Checking
        val result = try {
            // 网络请求放 IO 线程；当前版本号读取也一并放在 IO 线程，避免主线程碰 PackageManager
            withContext(Dispatchers.IO) { fetchLatestRelease(context) }
        } catch (e: Exception) {
            // 检查更新是尽力而为的能力，任何异常（断网 / 解析失败等）都归为失败，不向外抛
            Log.w(TAG, "检查更新失败", e)
            UpdateCheckState.Failed
        }
        _state.value = result
        return result
    }

    /**
     * 请求 GitHub API 并解析最新 Release，与当前版本比较。
     * 只在 IO 线程调用；HTTP 层异常向上抛给 [checkForUpdate] 统一兜底。
     */
    private fun fetchLatestRelease(context: Context): UpdateCheckState {
        val connection = (URL(RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            // GitHub API 要求请求必须携带 User-Agent，否则直接 403
            setRequestProperty("User-Agent", "exTile-Android")
            // 固定 API 响应格式，避免 GitHub 修改默认版本导致解析失效
            setRequestProperty("Accept", "application/vnd.github+json")
        }

        try {
            when (connection.responseCode) {
                // 仓库还没有发布任何 Release：语义上是「没得更新」，不算失败
                HttpURLConnection.HTTP_NOT_FOUND -> return UpdateCheckState.NoRelease
                // 其他非 200 的响应码（限流 / 服务端错误等）按失败处理
                HttpURLConnection.HTTP_OK -> Unit
                else -> return UpdateCheckState.Failed
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            // tag_name 是判断版本的唯一依据，缺失时视为响应异常
            val tagName = json.optString("tag_name")
            if (tagName.isBlank()) return UpdateCheckState.Failed

            // 更新日志原文：非字符串值（含 JSON null）一律按空串处理
            // （不同 org.json 实现的 optString 对 null 返回不一致，显式判断避免拿到字面量 "null"）
            val rawChangelog = json.opt("body")
            val changelog = if (rawChangelog is String) rawChangelog else ""
            // Release 页面链接：正常都会返回，缺了就退回仓库 Releases 列表页
            val releaseUrl = json.optString("html_url").ifBlank { RELEASES_PAGE_URL }

            val currentVersion = getCurrentVersionName(context)
            // 当前版本号读取失败（几乎不会发生）时保守起见视为「已是最新」，避免误报更新
            if (currentVersion == null) return UpdateCheckState.UpToDate

            // 远端版本号更大才提示更新；相等或更小都视为已是最新
            return if (compareVersions(currentVersion, tagName) < 0) {
                UpdateCheckState.Available(
                    latestVersion = tagName.removePrefix("v").removePrefix("V"),
                    changelog = changelog,
                    releaseUrl = releaseUrl
                )
            } else {
                UpdateCheckState.UpToDate
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 获取当前安装的版本号（versionName）。
     * 与设置页「关于-版本」条目的读取方式一致（不走 BuildConfig，项目未开启该特性）。
     */
    fun getCurrentVersionName(context: Context): String? = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        null
    }

    /**
     * 比较两个版本号大小，返回值语义同 [Comparator]：负数表示 v1 更小，0 相等，正数表示 v1 更大。
     *
     * 规则：
     * - 忽略开头的 v/V 前缀（兼容 tag 写 v1.2.3 与 versionName 写 1.2.3 的差异）；
     * - 按 `.` 分段逐段转数字比较，段数不足按 0 补齐（1.0 与 1.0.0 等价）；
     * - 任一段无法解析为整数（如 1.2.3-beta）时退化为字符串比较，保证总有确定结果。
     */
    fun compareVersions(v1: String, v2: String): Int {
        val s1 = v1.trim().removePrefix("v").removePrefix("V")
        val s2 = v2.trim().removePrefix("v").removePrefix("V")
        val seg1 = s1.split('.')
        val seg2 = s2.split('.')
        val maxSegments = maxOf(seg1.size, seg2.size)

        for (index in 0 until maxSegments) {
            val n1 = seg1.getOrNull(index)?.trim()?.toIntOrNull()
            val n2 = seg2.getOrNull(index)?.trim()?.toIntOrNull()
            // 任一段不是纯数字：无法按数值比较，退化为整串的字典序比较
            if (n1 == null || n2 == null) return s1.compareTo(s2)
            if (n1 != n2) return n1.compareTo(n2)
        }
        return 0
    }
}
