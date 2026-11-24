package com.hrsthrt74.qstile.ui.screens.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.ContainedIcon
import com.hrsthrt74.qstile.ui.components.GITHUB_ISSUES_URL
import com.hrsthrt74.qstile.ui.components.QQ_GROUP_NUMBER
import com.hrsthrt74.qstile.ui.components.QQ_GROUP_URL
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.FeedbackFill
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 「第一个版本」提示卡片组件。
 * 常驻显示在主页顶部，说明当前是第一个版本、恳请多多关照，并引导用户反馈问题。
 * 反馈渠道随语言分流：简体中文环境走 QQ 群（群聊仅面向简中用户），
 * 其他语言走 GitHub Issues。点击卡片即跳转对应渠道。
 */
@Composable
internal fun FirstVersionCard() {
    val context = LocalContext.current
    // 简体中文判断：语言为 zh 且文字为 Hans（或地区为 CN/SG），可覆盖 zh-CN/zh-Hans 等变体，
    // 排除繁体环境（zh-TW/zh-HK 的 script 为 Hant）。
    // 使用 LocalConfiguration 而非 context.resources.configuration：前者是配置感知的，
    // 语言变化时会触发重组，避免读到过期的旧配置（与设置页交流群条目的判断保持一致）
    val locale = LocalConfiguration.current.locales[0]
    val isSimplifiedChinese = locale?.language == "zh" &&
        (locale.script == "Hans" || locale.country == "CN" || locale.country == "SG")

    Card(
        modifier = Modifier.fillMaxWidth(),
        // 点击反馈：miuix 的 Card 默认 pressFeedbackType = None 且 showIndication = false（按下毫无变化），
        // 这里两套都打开——MiuixIndication 叠一层主题色蒙版 + Sink 按下内凹回弹
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {
            if (isSimplifiedChinese) {
                // 简中环境：群号复制到剪贴板，方便直接在 QQ 里搜索加群（与设置页行为一致）
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
                // runCatching 兜底：设备无浏览器/无 QQ 处理应用时避免崩溃
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(QQ_GROUP_URL)))
                }
            } else {
                // 其他语言：打开 GitHub Issues 反馈问题
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_ISSUES_URL)))
                }
            }
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 统一卡片布局：左「有底图标」+ 右内容
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 反馈渠道提示图标（对话/社区语义，与设置页交流群条目一致）
                // 底色用中性灰（与「关于」分类同一档灰），Monet 下由组件压平为主题 primary
                ContainedIcon(
                    icon = MiuixIcons.Glass.FeedbackFill,
                    containerColor = Color(0xFF8C9DAF)
                )
                Spacer(modifier = Modifier.width(12.dp))
                // 右侧文字（字号与其它卡片正文统一）
                Text(
                    text = stringResource(R.string.home_first_version_summary),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        }
    }
}
