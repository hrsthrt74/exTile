package com.hrsthrt74.qstile.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.DocsActivity
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.ExpandableSettingsCard
import com.hrsthrt74.qstile.ui.components.PreferenceIconEndPadding
import com.hrsthrt74.qstile.ui.components.PreferenceIconSize
import com.hrsthrt74.qstile.ui.components.PreferenceIconStartPadding
import com.hrsthrt74.qstile.ui.components.PreferenceLeadingIcon
import com.hrsthrt74.qstile.ui.components.QQ_GROUP_NUMBER
import com.hrsthrt74.qstile.ui.components.QQ_GROUP_URL
import com.hrsthrt74.qstile.ui.components.SheetState
import com.hrsthrt74.qstile.viewmodel.SettingsViewModel
import com.microsoft.clarity.modifiers.clarityUnmask
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.squircle.squircleClip

/**
 * 关于分类卡片：作者 / 版本 / GitHub / 社区 / 文档。
 *
 * 由原 `SettingsScreen` 的页内局部函数 `AboutCategoryCard` 拆分而来，行为保持不变：
 * 头像保留原图配色（不走行首图标的染色逻辑）、版本号挂 `clarityUnmask`、
 * QQ 群条目仅在简体中文环境显示（群聊仅面向简中用户）。
 *
 * @param viewModel 设置页 ViewModel（分类卡片的展开/收起状态在它上面；读取留在组件内部，
 *        与拆分前的失效范围一致）
 * @param authorSheetState 作者信息 Sheet 状态
 * @param updateSheetState 检查更新 Sheet 状态
 * @param donateSheetState 打赏 Sheet 状态（界面本体是 ui/components/DonateSheet，与主页共用）
 */
@Composable
fun AboutCategoryCard(
    viewModel: SettingsViewModel,
    authorSheetState: SheetState,
    updateSheetState: SheetState,
    donateSheetState: SheetState,
) {
    val context = LocalContext.current
    // 版本号读不到时（PackageManager 异常）的兜底文案
    val unknownText = stringResource(R.string.common_unknown)

    ExpandableSettingsCard(
        title = stringResource(R.string.settings_cat_about),
        // 关于分类使用应用自身的 ExTile 图标（vector drawable），
        // 通过 vectorResource 转成 ImageVector，交由通用组件「有底图标」统一染色
        icon = ImageVector.vectorResource(R.drawable.ic_settings_extile),
        // 该图标内容（4~44 / 48 视口）比 miuix 图标更「满」，同尺寸渲染反而显大，
        // 因此比其它分类图标小一档（24dp vs 26dp）做视觉对齐
        iconSize = 24.dp,
        // 关于分类品牌色：深灰；Monet 动态取色下会被自动压平为主题 primary。
        // 深底会让 ContainedIcon 自动把前景图标切成白色，保证对比度
        containerColor = Color(0xFF616161),
        expanded = viewModel.expandedCategory == SettingsViewModel.SettingsCategory.ABOUT,
        onToggle = { viewModel.toggleCategory(SettingsViewModel.SettingsCategory.ABOUT) }
    ) {
        // 作者：头像 + 昵称 + 身份，点击弹出作者信息 Sheet 展示各社交平台入口
        ArrowPreference(
            title = "hrsthrt74",
            summary = stringResource(R.string.settings_author_role),
            onClick = { authorSheetState.show() },
            // 头像为彩色图片，需用 Color.Unspecified 保留原图颜色，不能走 PreferenceLeadingIcon 的染色逻辑
            startAction = {
                Icon(
                    painter = painterResource(R.drawable.avatar),
                    contentDescription = stringResource(R.string.settings_author_avatar),
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .padding(start = PreferenceIconStartPadding - 4.dp, end = PreferenceIconEndPadding - 4.dp)
                        .size(PreferenceIconSize + 8.dp)
                        .squircleClip(8.dp)
                )
            }
        )

        // 版本号：点击弹出「检查更新」Sheet（自动检查开关 / 检查状态 / 手动检查）
        ArrowPreference(
            title = stringResource(R.string.settings_version_title),
            summary = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: unknownText
            } catch (e: Exception) {
                unknownText
            },
            onClick = { updateSheetState.show() },
            startAction = { PreferenceLeadingIcon(R.drawable.ic_settings_extile) },
            // 版本号 -> Clarity
            modifier = Modifier.clarityUnmask()
        )

        // Github Repo
        ArrowPreference(
            title = "GitHub",
            summary = "hrsthrt74/exTile",
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/hrsthrt74/exTile")))
            },
            startAction = { PreferenceLeadingIcon(R.drawable.ic_github) }
        )

        // 分割线：把上面「关于本应用」类条目（作者 / 版本 / GitHub）与下面
        // 「社区、支持、法律文档」这组入口在视觉上分开，形成第二个分组。
        // 放在 if 之外，保证非简体中文环境（无 QQ 群条目）下这组依然有分隔
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // 交流群（QQ 群）：仅当系统语言为简体中文时显示（群聊仅面向简中用户）
        // 简体中文判断：语言为 zh 且文字为 Hans（或地区为 CN/SG），可覆盖 zh-CN/zh-Hans 等变体，
        // 排除繁体环境（zh-TW/zh-HK 的 script 为 Hant）。
        // 使用 LocalConfiguration 而非 context.resources.configuration：前者是配置感知的，
        // 语言变化时会触发重组，避免读到过期的旧配置
        val locale = LocalConfiguration.current.locales[0]
        val isSimplifiedChinese = locale?.language == "zh" &&
            (locale.script == "Hans" || locale.country == "CN" || locale.country == "SG")
        if (isSimplifiedChinese) {
            // 条目不再显示 summary：单行标题更清爽，点击行为本身已足够说明用途
            ArrowPreference(
                title = stringResource(R.string.settings_qq_group_title),
                onClick = {
                    // 群号复制到剪贴板，方便直接在 QQ 里搜索加群
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("QQ群号", QQ_GROUP_NUMBER)
                    )
                    // 提示已复制；用 applicationContext 避免 Android 12+ 的 Toast 限制问题
                    Toast.makeText(
                        context.applicationContext,
                        context.getString(R.string.settings_qq_group_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                    // runCatching 兜底：设备无浏览器应用时避免崩溃
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, android.net.Uri.parse(QQ_GROUP_URL))
                        )
                    }
                },
                startAction = { PreferenceLeadingIcon(MiuixIcons.Community) }
            )
        }

        // 支持作者：不显示 summary，保持与上方 QQ 群、下方文档条目一致的「单行标题」形态
        ArrowPreference(
            title = stringResource(R.string.settings_donate_title),
            onClick = { donateSheetState.show() },
            startAction = { PreferenceLeadingIcon(MiuixIcons.Favorites) }
        )

        // 文档：隐私政策 / 用户协议 / 开源许可 统一入口，进入 Docs 页用 Tab 切换。
        // 原来的 summary 文案已上移为标题（三个文档名直接写进标题，比「查看文档」更明确），
        // 故此处不再传 summary，标题即完整的入口说明
        ArrowPreference(
            title = stringResource(R.string.settings_docs_title),
            onClick = {
                context.startActivity(Intent(context, DocsActivity::class.java))
            },
            startAction = { PreferenceLeadingIcon(MiuixIcons.File) }
        )

        // 「其他」分组（匿名统计 / 重新运行引导）已迁到「高级」分类。
    }
}
