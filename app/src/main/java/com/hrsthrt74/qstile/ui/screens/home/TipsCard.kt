package com.hrsthrt74.qstile.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.ui.components.ContainedIcon
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.UsageHelp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import kotlin.random.Random

/**
 * 「你知道吗？」卡片的图标底色：紫色（与设置页部分功能图标同一色系）。
 * 动态取色（Monet）开启时，[ContainedIcon] 会忽略这个颜色、统一压平为主题色。
 */
private val TipsIconColor = Color(0xFF7667F8)

/**
 * 主页「你知道吗？」卡片。
 *
 * 展示一条使用小知识。每次冷启动随机挑一条开始；点击后按「一轮之内不重复」的随机顺序前进，
 * 一轮全部看完再重新洗牌（伪随机轮播，而不是纯随机——保证每一条都会被用户看到）。
 * 文案由 [com.hrsthrt74.qstile.data.TipsRepository]
 * 从 `res/raw(-zh)/tips.txt` 读取，标题与正文都按当前应用语言自动切换。
 *
 * 布局照抄主页的 `PermissionStatusCard`：左「有底图标」（竖直居中）+ 右内容（标题 + 正文），
 * 标题 `headline1`、正文 `body2`，两行之间只留 2dp（`Arrangement.spacedBy`）。
 * 按用户要求不加序号、不加右侧箭头——整卡可点这件事
 * 靠 miuix 的按压反馈表达（[Card] 默认两套反馈都关着，必须显式打开）：
 * - `showIndication = true`：按下时叠一层主题色蒙版；
 * - `pressFeedbackType = Sink`：按下内凹 + 回弹，与主页其它卡片手感一致。
 *
 * @param tips 全部文案（调用方保证非空；为空时本组件不渲染任何内容）
 */
@Composable
internal fun TipsCard(tips: List<String>) {
    // 防御性判空：正常调用方已过滤空列表，这里再兜一层，避免下面取模时除零
    if (tips.isEmpty()) return

    // 本轮排列的洗牌种子：冷启动时随机（每次打开应用看到的第一条都不同）；
    // 旋转屏幕 / 进程重建时由 rememberSaveable 恢复，保证当前这条不会突然变掉。
    // 只存种子而不存整个排列：种子 + 条数就能唯一还原顺序，不必序列化一个 List。
    var seed by rememberSaveable { mutableIntStateOf(Random.nextInt()) }

    // 当前走到本轮的第几条（一轮 = 一次完整洗牌，一轮之内不会重复）
    var position by rememberSaveable { mutableIntStateOf(0) }

    // 本轮展示顺序：由「种子 + 条数」派生，两者不变则顺序稳定；切换语言导致条数变化时自动重算
    val order = remember(seed, tips.size) { shuffledOrder(tips.size, seed) }

    // 语言切换后 position 可能超出新排列长度，取模兜底
    val safePosition = position % order.size
    // 当前这条在 tips 里的真实下标
    val tipIndex = order[safePosition]

    Card(
        modifier = Modifier.fillMaxWidth(),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = {
            if (safePosition >= order.lastIndex) {
                // 一轮已经看完：换种子重新洗牌，从新一轮的第一条重新开始。
                // avoidFirst 保证新一轮的第一条不是刚刚看过的那条，避免「点了没反应」的错觉
                seed = nextSeed(tips.size, avoidFirst = tipIndex)
                position = 0
            } else {
                position = safePosition + 1
            }
        }
    ) {
        // 统一卡片布局（照抄权限卡）：左「有底图标」（竖直居中）+ 右内容（标题 + 正文）。
        // 图标用 Row 的 CenterVertically：正文变长时图标始终停在整块内容的垂直中线上。
        Row(
            // fillMaxWidth 保证 Row 吃满卡片宽度，右侧 weight(1f) 的内容列才能拿到正确的剩余宽度
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContainedIcon(
                icon = MiuixIcons.Glass.UsageHelp,
                containerColor = TipsIconColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            // 右侧内容列：weight(1f) 占满剩余宽度，两行间距固定 2dp（与权限卡一致）
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_tips_title),
                    // 卡片标题统一 headline1（与权限卡、统计卡同级）
                    style = MiuixTheme.textStyles.headline1,
                    color = MiuixTheme.colorScheme.onSurface
                )

                // 正文：切换时淡入 + 轻微上移，旧文案先淡出。
                // 高度变化交给 AnimatedContent 自带的 SizeTransform 平滑过渡（不再叠 animateContentSize，
                // 否则两套尺寸动画会互相打架）。
                AnimatedContent(
                    // 用真实下标做 targetState：换到下一条时一定触发动画（一轮之内下标不会重复）
                    targetState = tipIndex,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) +
                            slideInVertically(animationSpec = tween(220)) { height -> height / 10 })
                            .togetherWith(fadeOut(animationSpec = tween(150)))
                            // 关键：默认 SizeTransform(clip = true) 会把滑出容器边界的文字直接裁掉，
                            // 动画看起来像被「切」了一刀。clip = false 只影响绘制、不影响测量，
                            // 所以高度动画照常，滑出部分也能完整显示（外层 Card 有圆角裁剪兜底，不会溢出卡片）。
                            .using(SizeTransform(clip = false))
                    },
                    label = "TipsContent",
                    // 撑满右侧内容列：短文案与长文案的测量宽度一致，动画期间不会左右跳
                    modifier = Modifier.fillMaxWidth()
                ) { targetIndex ->
                    Text(
                        // 动画期间 tips 可能因语言切换而变短，取模保证索引始终合法
                        text = tips[targetIndex % tips.size],
                        style = MiuixTheme.textStyles.body2,
                        // 与权限卡的引导文案同色，比标题弱一档，形成阅读层级
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        // 文本自身也撑满整行：换行宽度稳定，不会因为某条特别短而缩成一团
                        modifier = Modifier.fillMaxWidth(),
                        // 兜底：极端长的文案最多 8 行，避免把首页其它卡片挤出屏幕
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 生成一轮的展示顺序：把 `0 until size` 这 [size] 个下标用 [seed] 打乱。
 *
 * 同一个 seed + size 永远得到同一个排列，所以卡片只需要把 seed 存进 `rememberSaveable`，
 * 旋转屏幕后顺序完全一致，也不用序列化整个列表。
 *
 * @param size 文案条数
 * @param seed 洗牌种子（可为负数，[Random] 支持）
 * @return 长度为 [size] 的下标排列，每个下标恰好出现一次
 */
private fun shuffledOrder(size: Int, seed: Int): List<Int> =
    (0 until size).shuffled(Random(seed))

/**
 * 为下一轮挑一个洗牌种子，并尽量保证新一轮的第一条不是 [avoidFirst]。
 *
 * 一轮看完重新洗牌时，如果新排列的第一个元素恰好就是刚才最后看的那条，
 * 用户点下去会「文案没变化」，看起来像卡住了。这里先小范围试几个种子，
 * 拿到首元素不同的排列就用；极端情况下（条数很少、连续撞车）直接返回随机种子，
 * 此时最多只是偶尔重复一次，不值得再循环等待。
 *
 * @param size 文案条数
 * @param avoidFirst 上一轮最后一条的真实下标；为 null 时不限制首元素
 * @return 新的洗牌种子
 */
private fun nextSeed(size: Int, avoidFirst: Int?): Int {
    // 只有一条文案时没有「换一条」可言，直接给随机种子即可
    if (size <= 1 || avoidFirst == null) return Random.nextInt()

    // repeat 是 inline 函数，这里的 return 是直接返回给调用方
    repeat(16) {
        val candidate = Random.nextInt()
        if (shuffledOrder(size, candidate).first() != avoidFirst) return candidate
    }
    return Random.nextInt()
}
