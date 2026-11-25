package com.hrsthrt74.qstile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.mikepenz.aboutlibraries.Libs
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 单个开源库的许可条目。 */
data class LicenseItem(
    val name: String,
    val author: String,
    val license: String,
    val url: String
)

/**
 * 从 AboutLibraries 插件生成的数据中解析许可列表。
 *
 * 插件在构建期收集所有依赖的许可信息，生成 raw 资源 aboutlibraries.json；
 * 这里读取并解析为许可页使用的 [LicenseItem] 列表。
 * 卡片摘要沿用「groupId • 许可证名」格式，点击跳转库的官网地址。
 */
private fun loadLicenseItems(context: android.content.Context): List<LicenseItem> {
    return runCatching {
        // 读取插件生成的原始 JSON 数据
        val json = context.resources.openRawResource(R.raw.aboutlibraries)
            .bufferedReader().use { it.readText() }
        val libs: Libs = Libs.Builder().withJson(json).build()

        libs.libraries
            .filter { it.name != null } // 过滤掉没有名称的条目，无法展示
            .map { library ->
                LicenseItem(
                    name = library.name!!,
                    // aboutlibraries 没有「作者」字段，用 maven groupId 代替
                    // （uniqueId 格式为 "groupId:artifactId"，去掉 artifactId 即得 groupId）
                    author = library.uniqueId.removeSuffix(":${library.artifactId}"),
                    // 一个库可能带多个许可证，这里取第一个展示
                    license = library.licenses.firstOrNull()?.name ?: "",
                    // 优先用库的官网地址，缺失时退回第一个许可证的地址
                    url = library.website
                        ?: library.licenses.firstOrNull()?.url
                        ?: ""
                )
            }
            .filter { it.url.isNotBlank() } // 无处可跳的条目没有意义
            .sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList()) // 解析失败时返回空列表，避免许可页崩溃
}

/**
 * 开源许可列表内容区（不含 Scaffold / TopAppBar）。
 *
 * 供 DocsScreen 的「开源许可」Tab 复用：点击条目跳转浏览器查看对应许可证。
 * 数据由 AboutLibraries 构建期自动收集，无需手动维护列表。
 * 滚动通过 [scrollBehavior] 与页面的可收起 TopAppBar 联动。
 *
 * @param scrollBehavior 页面的 TopAppBar 滚动行为，用于嵌套滚动联动收起。
 */
@Composable
fun LicensesContent(scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current

    // 数据在构建期已生成，运行时只需解析一次
    val licenses = remember { loadLicenseItems(context) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .scrollEndHaptic(HapticFeedbackType.TextHandleMove),
        contentPadding = PaddingValues(
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(licenses) { license ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                ArrowPreference(
                    title = license.name,
                    summary = "${license.author} • ${license.license}",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(license.url)))
                    }
                )
            }
        }
    }
}
