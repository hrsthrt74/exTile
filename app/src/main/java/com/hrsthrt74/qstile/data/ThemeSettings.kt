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
    val enableBlur: Boolean = true
)

object ThemeRepository {
    private val DAY_NIGHT_MODE = intPreferencesKey("day_night_mode")
    private val IS_DYNAMIC_COLOR_MODE = booleanPreferencesKey("is_dynamic_color_mode")
    private val PALETTE_STYLE = intPreferencesKey("palette_style")
    private val COLOR_SPEC = intPreferencesKey("color_spec")
    private val ENABLE_BLUR = booleanPreferencesKey("enable_blur")

    fun getThemeSettingsFlow(context: Context): Flow<ThemeSettings> {
        return context.dataStore.data.map { prefs ->
            ThemeSettings(
                dayNightMode = prefs[DAY_NIGHT_MODE] ?: 0,
                isDynamicColorMode = prefs[IS_DYNAMIC_COLOR_MODE] ?: false,
                paletteStyle = prefs[PALETTE_STYLE] ?: 0,
                colorSpec = prefs[COLOR_SPEC] ?: 1,
                enableBlur = prefs[ENABLE_BLUR] ?: true
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
}
