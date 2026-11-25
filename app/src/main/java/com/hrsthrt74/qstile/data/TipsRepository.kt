package com.hrsthrt74.qstile.data

import android.content.Context
import android.util.Log
import com.hrsthrt74.qstile.R

/**
 * 主页「你知道吗？」卡片的文案仓库。
 *
 * 文案有意不放进 `strings.xml`：条数会持续增长，混在字符串表里既难维护、也不好一眼看出整体语气；
 * 独立成纯文本后，增删一条只需要改一行，不用碰任何代码。
 *
 * 文件按语言放在同名资源目录下，由 Android 资源系统自动挑选（无需手写语言判断）：
 * - `res/raw/tips.txt`    英文（默认，同时是所有未翻译语言的兜底）
 * - `res/raw-zh/tips.txt` 简体中文
 *
 * 两个文件的**文件名必须完全一致**（都是 `tips.txt`），否则 `R.raw.tips` 无法对应到中文版本。
 *
 * 格式约定（两个文件保持一致）：
 * - UTF-8 编码，一行一条文案；
 * - 空行忽略；
 * - 以 `#` 开头的行视为注释；
 * - 一条文案不能换行，需要分段请拆成两条。
 */
object TipsRepository {

    private const val TAG = "TipsRepository"

    /** 注释行前缀（方便在文案文件里写说明、临时注释掉某条） */
    private const val COMMENT_PREFIX = "#"

    /**
     * 读取当前语言下的全部文案。
     *
     * 这里刻意不做进程级缓存：切换应用语言后，同一个 `R.raw.tips` 会解析到另一个文件，
     * 缓存反而容易把旧语言的文案留在内存里。文件本身只有几 KB，重复读取的开销可以忽略。
     *
     * @param context 任意 Context（只用来访问 resources）
     * @return 文案列表；文件缺失或读取失败时返回空列表，调用方据此不渲染卡片
     */
    fun load(context: Context): List<String> = try {
        context.resources.openRawResource(R.raw.tips).bufferedReader().use { reader ->
            reader.readLines()
                // 去掉行首尾空白（编辑器保存时可能带缩进或行尾空格）
                .map { it.trim() }
                // 过滤空行与注释行，剩下的就是可展示的文案
                .filter { it.isNotEmpty() && !it.startsWith(COMMENT_PREFIX) }
        }
    } catch (e: Exception) {
        // 读不到文案属于「功能降级」而不是崩溃点：记一条日志，让卡片整体消失即可
        Log.w(TAG, "读取内置 Tips 文案失败", e)
        emptyList()
    }
}
