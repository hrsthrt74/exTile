package com.hrsthrt74.qstile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class ThemeSettings(
    val dayNightMode: Int = 0,
    val isDynamicColorMode: Boolean = false,
    val paletteStyle: Int = 0,
    val colorSpec: Int = 1,
    val enableBlur: Boolean = true,
    // 渐进模糊：开启后顶部栏使用渐变模糊（顶部最强、向下过渡到清晰），
    // 需在 enableBlur 开启的前提下才生效（否则整体回退纯色）
    val progressiveBlur: Boolean = false
)

object ThemeRepository {
    private val DAY_NIGHT_MODE = intPreferencesKey("day_night_mode")
    private val IS_DYNAMIC_COLOR_MODE = booleanPreferencesKey("is_dynamic_color_mode")
    private val PALETTE_STYLE = intPreferencesKey("palette_style")
    private val COLOR_SPEC = intPreferencesKey("color_spec")
    private val ENABLE_BLUR = booleanPreferencesKey("enable_blur")
    private val PROGRESSIVE_BLUR = booleanPreferencesKey("progressive_blur")

    fun getThemeSettingsFlow(context: Context): Flow<ThemeSettings> {
        return context.dataStore.data.map { prefs ->
            ThemeSettings(
                dayNightMode = prefs[DAY_NIGHT_MODE] ?: 0,
                isDynamicColorMode = prefs[IS_DYNAMIC_COLOR_MODE] ?: false,
                paletteStyle = prefs[PALETTE_STYLE] ?: 0,
                colorSpec = prefs[COLOR_SPEC] ?: 1,
                enableBlur = prefs[ENABLE_BLUR] ?: true,
                progressiveBlur = prefs[PROGRESSIVE_BLUR] ?: false
            )
        }
    }

    suspend fun getThemeSettings(context: Context): ThemeSettings {
        return getThemeSettingsFlow(context).first()
    }

    suspend fun saveDayNightMode(context: Context, mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[DAY_NIGHT_MODE] = mode
        }
    }

    suspend fun saveIsDynamicColorMode(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_DYNAMIC_COLOR_MODE] = enabled
        }
    }

    suspend fun savePaletteStyle(context: Context, style: Int) {
        context.dataStore.edit { prefs ->
            prefs[PALETTE_STYLE] = style
        }
    }

    suspend fun saveColorSpec(context: Context, spec: Int) {
        context.dataStore.edit { prefs ->
            prefs[COLOR_SPEC] = spec
        }
    }

    suspend fun saveEnableBlur(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ENABLE_BLUR] = enabled
        }
    }

    suspend fun saveProgressiveBlur(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PROGRESSIVE_BLUR] = enabled
        }
    }
}
