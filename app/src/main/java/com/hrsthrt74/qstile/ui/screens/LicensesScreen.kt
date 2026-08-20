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
 * 开源许可列表内容区（不含 Scaffold / TopAppBar）。
 *
 * 供 DocsScreen 的「开源许可」Tab 复用：点击条目跳转浏览器查看对应许可证。
 * 滚动通过 [scrollBehavior] 与页面的可收起 TopAppBar 联动。
 *
 * @param scrollBehavior 页面的 TopAppBar 滚动行为，用于嵌套滚动联动收起。
 */
@Composable
fun LicensesContent(scrollBehavior: ScrollBehavior) {
    val context = LocalContext.current

    val licenses = remember {
        listOf(
            LicenseItem(
                name = "AndroidX Core KTX",
                author = "Google",
                license = "Apache License 2.0",
                url = "https://developer.android.com/jetpack/androidx"
            ),
            LicenseItem(
                name = "AndroidX Compose",
                author = "Google",
                license = "Apache License 2.0",
                url = "https://developer.android.com/jetpack/compose"
            ),
            LicenseItem(
                name = "AndroidX Navigation Compose",
                author = "Google",
                license = "Apache License 2.0",
                url = "https://developer.android.com/jetpack/compose/navigation"
            ),
            LicenseItem(
                name = "AndroidX DataStore Preferences",
                author = "Google",
                license = "Apache License 2.0",
                url = "https://developer.android.com/topic/libraries/architecture/datastore"
            ),
            LicenseItem(
                name = "Shizuku",
                author = "Rikka",
                license = "Apache License 2.0",
                url = "https://github.com/RikkaApps/Shizuku"
            ),
            LicenseItem(
                name = "Miuix",
                author = "Yukonga",
                license = "Apache License 2.0",
                url = "https://github.com/compose-miuix-ui/miuix/"
            ),
            LicenseItem(
                name = "Calvin-LL Reorderable",
                author = "Calvin-LL",
                license = "Apache License 2.0",
                url = "https://github.com/Calvin-LL/Reorderable"
            ),
            LicenseItem(
                name = "DeviceCompat",
                author = "getActivity",
                license = "Apache License 2.0",
                url = "https://github.com/getActivity/DeviceCompat"
            ),
            LicenseItem(
                name = "InstallerX Revived",
                author = "wxxsfxyzm",
                license = "GPL-3.0",
                url = "https://github.com/wxxsfxyzm/InstallerX-Revived"
            ),
            LicenseItem(
                name = "miuix-skill",
                author = "limczhh",
                license = "MIT",
                url = "https://github.com/limczhh/miuix-skill"
            )
        )
    }

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