package com.hrsthrt74.qstile.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.DocsActivity
import com.hrsthrt74.qstile.R
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/** Docs 页面的三个 Tab。标题持有 @StringRes 资源 id,支持多语言,使用处在 Composable 上下文解析。 */
enum class DocsTab(@StringRes val titleRes: Int, val assetName: String?) {
    /** 隐私政策：渲染 docs/PRIVACY_POLICY.md。 */
    PRIVACY_POLICY(R.string.docs_tab_privacy, "PRIVACY_POLICY.md"),

    /** 用户协议：渲染 docs/TERMS_OF_SERVICE.md。 */
    TERMS_OF_SERVICE(R.string.docs_tab_terms, "TERMS_OF_SERVICE.md"),

    /** 开源许可：展示开源库列表，无对应 Markdown 文档。 */
    LICENSES(R.string.docs_tab_licenses, null);

    companion object {
        /** 按 Intent extra 解析 Tab，未传入或非法时兜底为隐私政策，避免页面为空。 */
        fun fromValue(value: String?): DocsTab {
            return entries.firstOrNull { it.name == value } ?: PRIVACY_POLICY
        }
    }
}

/**
 * Docs 聚合页：用 TabRow 在「隐私政策 / 用户协议 / 开源许可」间切换。
 *
 * 三个 Tab 共用同一个 Scaffold 与可收起的 TopAppBar，滚动联动通过
 * [ScrollBehavior] 传递给当前可见的内容组件；[rememberSaveable] 保存选中 Tab，
 * 旋转屏幕不会丢失当前页。
 */
@Composable
fun DocsScreen(initialTab: DocsTab) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = MiuixScrollBehavior(state = topAppBarState)
    // Tab 标题随语言解析（stringResource 需 Composable 上下文,故不在 remember 内调用）
    val tabs = DocsTab.entries.map { stringResource(it.titleRes) }

    // 记住初始 Tab；rememberSaveable 保证配置变更（旋转）后仍停留在原 Tab。
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(initialTab.ordinal) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.docs_screen_title),
                largeTitle = stringResource(R.string.docs_screen_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { (context as? DocsActivity)?.finish() }) {
                        Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            // Tab 切换：点击触发长按震动反馈，与项目内其它触觉反馈风格一致
            TabRow(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { index ->
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedTabIndex = index
                },
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (val currentTab = DocsTab.entries[selectedTabIndex]) {
                    DocsTab.PRIVACY_POLICY,
                    DocsTab.TERMS_OF_SERVICE,
                    -> MarkdownDocumentContent(
                        assetName = checkNotNull(currentTab.assetName),
                        scrollBehavior = scrollBehavior
                    )

                    DocsTab.LICENSES -> LicensesContent(scrollBehavior = scrollBehavior)
                }
            }
        }
    }
}