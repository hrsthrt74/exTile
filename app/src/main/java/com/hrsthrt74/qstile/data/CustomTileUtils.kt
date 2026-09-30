package com.hrsthrt74.qstile.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.service.quicksettings.TileService
import com.hrsthrt74.qstile.R

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
     * icon 保证非空：Service 未声明图标时以空白占位图标（tile_blank）兜底。
     */
    data class QSTileServiceInfo(
        val packageName: String,
        val className: String,
        val label: String,
        val appName: String,
        val icon: Drawable
    )

    /**
     * 空白磁贴占位图标（虚线方框轮廓，本身为白色填充，渲染时统一染色）。
     *
     * 渲染处会对第三方图标统一染色（tint）：应用图标染色后会变成实心圆角矩形，观感差，
     * 因此凡拿不到第三方 Service 自身声明的图标时，一律回退到本占位图标，不再使用应用图标。
     */
    private fun blankIcon(context: Context): Drawable =
        context.getDrawable(R.drawable.tile_blank)!!

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
                // 只取 Service 自身声明的图标；拿不到时回退空白占位图标
                // （不回退应用图标：渲染时统一染色，应用图标染色后是实心圆角矩形）
                var icon: Drawable? = null
                if (serviceInfo.icon != 0) {
                    try {
                        val resources = pm.getResourcesForApplication(serviceInfo.applicationInfo)
                        icon = resources.getDrawable(serviceInfo.icon, null)
                    } catch (_: Exception) {}
                }
                result.add(
                    QSTileServiceInfo(
                        packageName = serviceInfo.packageName,
                        className = serviceInfo.name,
                        label = label,
                        appName = appName,
                        icon = icon ?: blankIcon(context)
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    /**
     * 获取 custom 磁贴的图标 Drawable。
     *
     * 只取第三方 TileService 自身声明的图标（[android.content.pm.ServiceInfo.icon]）：
     * 拿不到时不再回退应用图标（染色后会变成实心圆角矩形），改为回退到空白占位图标
     * [R.drawable.tile_blank]。getServiceInfo 不受组件启用状态限制（含已禁用的组件），
     * 已覆盖 queryIntentServices 的查询范围，无需再按 Intent 兜底遍历。
     *
     * @param context Context
     * @param value 磁贴值，格式为 "custom(包名/类名)"
     * @return 图标 Drawable；非 custom 格式返回 null，custom 格式保证非空（blank 兜底）
     */
    fun getCustomTileIcon(context: Context, value: String): Drawable? {
        val component = parseCustomComponent(value) ?: return null
        val pm = context.packageManager
        try {
            val serviceInfo = pm.getServiceInfo(component, PackageManager.GET_META_DATA)
            if (serviceInfo.icon != 0) {
                try {
                    val resources = pm.getResourcesForApplication(serviceInfo.applicationInfo)
                    val drawable = resources.getDrawable(serviceInfo.icon, null)
                    if (drawable != null) return drawable
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        // Service 未声明图标或已卸载：回退空白占位图标（不回退应用图标，原因见方法注释）
        return blankIcon(context)
    }
}
