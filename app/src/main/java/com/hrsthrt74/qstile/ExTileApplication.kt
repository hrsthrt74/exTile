package com.hrsthrt74.qstile

import android.app.Application
import android.util.Log
import com.hrsthrt74.qstile.data.ConfigRepository
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 自定义 Application 类。
 *
 * 主要职责：
 * - 管理 MS Clarity 的生命周期初始化
 * - 根据用户隐私同意状态决定是否启用 Clarity
 *
 * Clarity 初始化策略：
 * - 默认不初始化（保护用户隐私）
 * - 仅在用户明确同意隐私政策后才初始化
 * - 仅在前台启动时初始化，后台 TileService 启动时不初始化
 * - 用户撤销同意后，需要重启应用才能停止数据收集（Clarity SDK 限制）
 */
class ExTileApplication : Application() {
    /** Application 级别的协程作用域，用于 DataStore 读写 */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Clarity 是否已初始化的标志，避免重复初始化 */
    private var clarityInitialized = false

    companion object {
        private const val TAG = "ExTileApplication"
        /** MS Clarity 项目 ID */
        private const val CLARITY_PROJECT_ID = "y2ktagi3yy"
    }

    override fun onCreate() {
        super.onCreate()
        // 不自动初始化 Clarity，由 MainActivity 负责前台初始化
    }

    /**
     * 检查用户隐私同意状态，如果已同意则初始化 Clarity。
     * 此方法是幂等的，多次调用不会重复初始化。
     * 使用协程读取 DataStore，不阻塞主线程。
     */
    fun initClarityIfNeeded() {
        if (clarityInitialized) return

        applicationScope.launch {
            val consent = ConfigRepository.getClarityConsent(this@ExTileApplication)
            if (consent) {
                initClarity()
            }
        }
    }

    /**
     * 初始化 MS Clarity SDK。
     * 此方法是幂等的，多次调用不会重复初始化。
     */
    fun initClarity() {
        if (clarityInitialized) return

        try {
            val config = ClarityConfig(
                projectId = CLARITY_PROJECT_ID
            )
            Clarity.initialize(applicationContext, config)
            clarityInitialized = true
            Log.d(TAG, "MS Clarity 初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "MS Clarity 初始化失败", e)
        }
    }

    /**
     * 上传自定义统计数据到 Clarity。
     * 只在前台时调用，后台不调用。
     */
    fun sendStatsToClarity(expandCount: Long, collapseCount: Long) {
        if (!clarityInitialized) return

        try {
            Clarity.setCustomTag("total_expand", expandCount.toString())
            Clarity.setCustomTag("total_collapse", collapseCount.toString())
            Log.d(TAG, "统计数据已上传到 Clarity: expand=$expandCount, collapse=$collapseCount")
        } catch (e: Exception) {
            Log.e(TAG, "上传统计数据到 Clarity 失败", e)
        }
    }

    /**
     * 停止 MS Clarity 数据收集。
     * 注意：Clarity SDK 不支持运行时停止，此方法主要用于状态标记。
     * 用户撤销同意后，需要重启应用才能完全停止数据收集。
     */
    fun shutdownClarity() {
        // Clarity SDK 目前没有提供运行时停止的 API
        // 标记状态，提醒用户重启应用
        clarityInitialized = false
        Log.d(TAG, "MS Clarity 已标记为停止状态，需要重启应用才能完全停止数据收集")
    }
}
