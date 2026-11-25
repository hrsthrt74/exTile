package com.hrsthrt74.qstile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.Color
import com.hrsthrt74.qstile.ui.screens.DebugToolsScreen
import com.hrsthrt74.qstile.ui.theme.ExTileTheme
import rikka.shizuku.Shizuku
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

class DebugToolsActivity : ComponentActivity() {
    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化 Shizuku
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        setContent {
            // 导航事件 dispatcher 由 ComponentActivity 自动提供（activity 1.13+），
            // 无需手动注入；弹窗组件的预测式返回（NavigationBackHandler）依赖它
            MiuixTheme {
                ExTileTheme {
                    // 根 Scaffold（与 App.kt 同一套写法，缺了它本页的 Sheet 会「点了没反应」）：
                    //
                    // miuix 的玻璃 Sheet / Dialog 不是独立窗口，而是走 DialogLayout 把自身注册进
                    // 一个「弹窗宿主列表」，再由 Scaffold 内的 MiuixPopupHost 统一渲染。
                    // DialogLayout 取列表的顺序是 LocalRootDialogStates ?: LocalDialogStates，
                    // 而 Scaffold 是通过 CompositionLocalProvider 把列表下发给**自己子树**的——
                    // DebugToolsScreen 里的三个 AppBottomSheet 全部声明在页面 Scaffold **之外**
                    // （函数体里 Scaffold{...} 之后的同级节点），既拿不到页面 Scaffold 的列表，
                    // 也没有根 Scaffold 提供 LocalRootDialogStates，于是落到 staticCompositionLocalOf
                    // 的默认空列表上；那份列表无人渲染，Sheet 就永远不出现，也不会有任何报错。
                    // （经典风格走 WindowBottomSheet 的真实 Dialog 窗口，因此不受影响。）
                    //
                    // containerColor 透明 + contentWindowInsets = 0：不铺底色、不重复避让 insets，
                    // 布局与滚动行为完全交给各页面自己的 Scaffold。
                    MiuixScaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0),
                    ) {
                        DebugToolsScreen()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }
}
