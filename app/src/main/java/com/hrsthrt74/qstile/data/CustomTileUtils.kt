package com.hrsthrt74.qstile.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.service.quicksettings.TileService
import com.hrsthrt74.qstile.BuildConfig
import com.hrsthrt74.qstile.R
import com.hrsthrt74.qstile.tile.ExTileService

/**
 * 第三方（custom）磁贴工具类。
 *
 * 从原 `TileMapping` 拆分而来：这里只处理「custom(包名/类名) 格式的解析、第三方 QS Tile 服务查询与图标获取」，
 * 与磁贴静态清单/图标映射彻底解耦。
 *
 * custom 磁贴格式：`custom(com.package/.Class)`，见 [parseCustomComponent]。
 */
object CustomTileUtils {

    /** release 变体的 applicationId */
    const val RELEASE_PACKAGE = "com.hrsthrt74.qstile"

    /** debug 变体的 applicationId（build.gradle.kts 里通过 applicationIdSuffix 追加） */
    const val DEBUG_PACKAGE = "com.hrsthrt74.qstile.debug"

    /** 两个变体共用的磁贴服务类名（代码包名不随 applicationId 变化） */
    val EX_TILE_SERVICE_CLASS: String = ExTileService::class.java.name

    /** 当前变体的「相反版本」包名：release 里是 debug，debug 里是 release */
    val oppositePackage: String =
        if (BuildConfig.APPLICATION_ID == DEBUG_PACKAGE) RELEASE_PACKAGE else DEBUG_PACKAGE

    /**
     * 生成指定变体的 exTile 磁贴 spec。
     *
     * 类名必须用完整限定名：debug 变体的 applicationId 带 `.debug` 后缀，
     * 若沿用 `custom(pkg/.tile.ExTileService)` 的相对写法，ComponentName 会拼出不存在的类。
     */
    fun exTileSpec(packageName: String): String = "custom($packageName/$EX_TILE_SERVICE_CLASS)"

    /**
     * 判断 ComponentName 是否指向任一变体的 exTile 磁贴服务。
     *
     * 用于「切换 Release / Debug」与「添加磁贴 Sheet 排除相反版本」：
     * 这两个场景都需要同时认识两个变体，不能只用 [isExTileSpec]（只认当前变体）。
     */
    fun isExTileComponent(component: ComponentName): Boolean {
        val pkg = component.packageName
        if (pkg != RELEASE_PACKAGE && pkg != DEBUG_PACKAGE) return false
        // 除完整类名外，也接受 `custom(pkg/.tile.ExTileService)` 的相对写法：
        // 解析后类名会变成 `pkg.tile.ExTileService`，在 debug 上它指向的是不存在的类，
        // 但仍应被识别为「exTile 磁贴」，以便在切换时替换成正确的完整类名 spec。
        return component.className == EX_TILE_SERVICE_CLASS ||
            component.className == "$pkg.tile.ExTileService"
    }

    /** 判断 spec 是否指向任一变体的 exTile 磁贴（兼容相对/完整类名两种写法） */
    fun isAnyExTileSpec(value: String): Boolean {
        val component = parseCustomComponent(value) ?: return false
        return isExTileComponent(component)
    }

    /** 检测指定包名是否已安装（两个版本据此决定是否显示「切换到相对版本」） */
    fun isPackageInstalled(context: Context, packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)) != null
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

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
     * 判断一个磁贴 spec 是否指向 exTile 自身的磁贴服务。
     *
     * 用 ComponentName 规范化后比较，而不是字符串相等，原因有两点：
     * 1. 兼容 `custom(包名/.相对类名)` 与 `custom(包名/完整类名)` 两种写法 —— 老版本写入系统配置的
     *    是相对写法，必须继续识别，否则旧用户升级后锚点丢失；
     * 2. 用 [BuildConfig.APPLICATION_ID] 而非硬编码包名 —— debug 变体带 `.debug` 后缀，
     *    其系统磁贴 spec 的包名部分也带后缀，硬编码会把自己的磁贴当成普通第三方磁贴。
     *
     * @param value 磁贴 value，custom 格式为 "custom(包名/类名)"
     * @return 是否为本应用的 exTile 磁贴
     */
    fun isExTileSpec(value: String): Boolean {
        val component = parseCustomComponent(value) ?: return false
        return component.packageName == BuildConfig.APPLICATION_ID && isExTileComponent(component)
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
     * 判断 custom 磁贴能否取到真实图标（不含空白占位兜底）。
     *
     * 与 [getCustomTileIcon] 不同：本方法只做「有无图标」判断，不返回兜底占位图，
     * 供「添加磁贴」列表过滤掉拿不到图标的磁贴（Service 已卸载或未声明图标）。
     *
     * @param context Context
     * @param value 磁贴值，格式为 "custom(包名/类名)"
     * @return true = Service 存在且自身声明了图标；否则 false
     */
    fun hasCustomTileIcon(context: Context, value: String): Boolean {
        val component = parseCustomComponent(value) ?: return false
        val pm = context.packageManager
        return try {
            val serviceInfo = pm.getServiceInfo(component, PackageManager.GET_META_DATA)
            serviceInfo.icon != 0
        } catch (_: Exception) {
            false
        }
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
