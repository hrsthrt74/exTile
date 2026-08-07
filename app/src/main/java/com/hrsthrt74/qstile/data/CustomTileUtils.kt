package com.hrsthrt74.qstile.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.service.quicksettings.TileService

/**
 * 第三方（custom）磁贴工具类。
 *
 * 从原 `TileMapping` 拆分而来：这里只处理「custom(包名/类名) 格式的解析、第三方 QS Tile 服务查询与图标获取」，
 * 与磁贴静态清单/图标映射彻底解耦。
 *
 * custom 磁贴格式：`custom(com.package/.Class)`，见 [parseCustomComponent]。
 */
object CustomTileUtils {

    /**
     * 解析 custom 磁贴的 ComponentName。
     * @param value 磁贴值，格式为 "custom(包名/类名)"
     * @return ComponentName 或 null（如果不是 custom 格式）
     */
    fun parseCustomComponent(value: String): ComponentName? {
        if (!value.startsWith("custom(")) return null
        val componentStr = value.removePrefix("custom(").removeSuffix(")")
        return ComponentName.unflattenFromString(componentStr)
    }

    /**
     * 获取 custom 磁贴的显示名与应用名。
     * 第一行应显示磁贴显示名（label），第二行应显示应用名（appName），
     * 而不是原始的 "custom(包名/类名)" 字符串。
     * @param context Context
     * @param value 磁贴值，格式为 "custom(包名/类名)"
     * @return Pair(显示名, 应用名)，无法解析时返回 null
     */
    fun getCustomTileNames(context: Context, value: String): Pair<String, String>? {
        val component = parseCustomComponent(value) ?: return null
        val pm = context.packageManager
        return try {
            val serviceInfo = pm.getServiceInfo(component, PackageManager.GET_META_DATA)
            val label = try {
                serviceInfo.loadLabel(pm).toString()
            } catch (_: Exception) {
                null
            }
            val appName = try {
                pm.getApplicationLabel(serviceInfo.applicationInfo).toString()
            } catch (_: Exception) {
                null
            }
            // 任一字段成功即返回，失败的字段用包名/类名兜底
            if (label != null || appName != null) {
                Pair(
                    label ?: component.className.substringAfterLast('.'),
                    appName ?: component.packageName.substringAfterLast('.')
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 获取所有可用的 Quick Settings Tile 服务。
     * @param context Context
     * @return List<Triple<包名, 类名, 显示名称>>
     */
    fun getAllQSTileServices(context: Context): List<Triple<String, String, String>> {
        val pm = context.packageManager
        val result = mutableListOf<Triple<String, String, String>>()
        // 通过 queryIntentServices 查询已启用的 QS_TILE 服务
        try {
            val intent = Intent(TileService.ACTION_QS_TILE)
            val resolveInfos = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
            for (info in resolveInfos) {
                val serviceInfo = info.serviceInfo ?: continue
                val componentName = "${serviceInfo.packageName}/${serviceInfo.name}"
                val label = serviceInfo.loadLabel(pm).toString()
                result.add(Triple(serviceInfo.packageName, serviceInfo.name, label))
            }
        } catch (_: Exception) {}
        return result
    }

    /**
     * 第三方磁贴服务信息（含图标）。
     * 用于「添加第三方磁贴」列表：一次批量查询，避免渲染时逐个 IPC。
     */
    data class QSTileServiceInfo(
        val packageName: String,
        val className: String,
        val label: String,
        val appName: String,
        val icon: Drawable?
    )

    /**
     * 获取所有可用的 Quick Settings Tile 服务及其图标。
     * 一次 [queryIntentServices] 批量取回全部信息，避免每个磁贴单独 IPC 查询；
     * 应放到后台线程执行。
     * @param context Context
     * @return 磁贴服务信息列表（含图标 Drawable）
     */
    fun getAllQSTileServicesWithIcon(context: Context): List<QSTileServiceInfo> {
        val pm = context.packageManager
        val result = mutableListOf<QSTileServiceInfo>()
        try {
            val intent = Intent(TileService.ACTION_QS_TILE)
            val resolveInfos = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
            for (info in resolveInfos) {
                val serviceInfo = info.serviceInfo ?: continue
                val label = try {
                    serviceInfo.loadLabel(pm).toString()
                } catch (e: Exception) {
                    serviceInfo.name.substringAfterLast('.')
                }
                val appName = try {
                    pm.getApplicationLabel(serviceInfo.applicationInfo).toString()
                } catch (e: Exception) {
                    serviceInfo.packageName.substringAfterLast('.')
                }
                // 优先 Service 图标，回退到应用图标
                var icon: Drawable? = null
                if (serviceInfo.icon != 0) {
                    try {
                        val resources = pm.getResourcesForApplication(serviceInfo.applicationInfo)
                        icon = resources.getDrawable(serviceInfo.icon, null)
                    } catch (_: Exception) {}
                }
                if (icon == null) {
                    try {
                        icon = serviceInfo.applicationInfo.loadIcon(pm)
                    } catch (_: Exception) {}
                }
                result.add(
                    QSTileServiceInfo(
                        packageName = serviceInfo.packageName,
                        className = serviceInfo.name,
                        label = label,
                        appName = appName,
                        icon = icon
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    /**
     * 获取 custom 磁贴的图标 Drawable。
     * @param context Context
     * @param value 磁贴值，格式为 "custom(包名/类名)"
     * @return 图标 Drawable 或 null（如果无法获取）
     */
    fun getCustomTileIcon(context: Context, value: String): Drawable? {
        val component = parseCustomComponent(value) ?: return null
        val pm = context.packageManager
        // 优先通过 getServiceInfo 获取（包括禁用的组件）
        try {
            val serviceInfo = pm.getServiceInfo(component, PackageManager.GET_META_DATA)
            // 尝试通过 Resources 获取 Service 的图标
            if (serviceInfo.icon != 0) {
                try {
                    val resources = pm.getResourcesForApplication(serviceInfo.applicationInfo)
                    val drawable = resources.getDrawable(serviceInfo.icon, null)
                    if (drawable != null) return drawable
                } catch (_: Exception) {}
            }
            // 回退到应用图标
            try {
                val icon = serviceInfo.applicationInfo.loadIcon(pm)
                if (icon != null) return icon
            } catch (_: Exception) {}
        } catch (_: Exception) {}
        // 通过 queryIntentServices 查询已启用的 QS_TILE 服务
        try {
            val intent = Intent(TileService.ACTION_QS_TILE)
            val resolveInfos = pm.queryIntentServices(intent, PackageManager.GET_META_DATA)
            for (info in resolveInfos) {
                val serviceInfo = info.serviceInfo ?: continue
                if (serviceInfo.packageName == component.packageName &&
                    serviceInfo.name == component.className) {
                    // 尝试通过 Resources 获取 Service 的图标
                    if (serviceInfo.icon != 0) {
                        try {
                            val resources = pm.getResourcesForApplication(serviceInfo.applicationInfo)
                            val drawable = resources.getDrawable(serviceInfo.icon, null)
                            if (drawable != null) return drawable
                        } catch (_: Exception) {}
                    }
                    // 回退到应用图标
                    try {
                        val icon = serviceInfo.applicationInfo.loadIcon(pm)
                        if (icon != null) return icon
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
        // 最后尝试获取应用图标
        try {
            val appInfo = pm.getApplicationInfo(component.packageName, 0)
            val icon = appInfo.loadIcon(pm)
            if (icon != null) return icon
        } catch (_: Exception) {}
        return null
    }
}
