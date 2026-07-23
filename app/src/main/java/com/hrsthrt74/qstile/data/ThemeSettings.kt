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
    val colorSchemeMode: Int = 0,
    val isDark: Boolean? = null,
    val paletteStyle: Int = 0,
    val colorSpec: Int = 0
)

object ThemeRepository {
    private val COLOR_SCHEME_MODE = intPreferencesKey("color_scheme_mode")
    private val IS_DARK = booleanPreferencesKey("is_dark")
    private val HAS_IS_DARK = booleanPreferencesKey("has_is_dark")
    private val PALETTE_STYLE = intPreferencesKey("palette_style")
    private val COLOR_SPEC = intPreferencesKey("color_spec")

    fun getThemeSettingsFlow(context: Context): Flow<ThemeSettings> {
        return context.dataStore.data.map { prefs ->
            ThemeSettings(
                colorSchemeMode = prefs[COLOR_SCHEME_MODE] ?: 0,
                isDark = if (prefs[HAS_IS_DARK] == true) prefs[IS_DARK] else null,
                paletteStyle = prefs[PALETTE_STYLE] ?: 0,
                colorSpec = prefs[COLOR_SPEC] ?: 0
            )
        }
    }

    suspend fun getThemeSettings(context: Context): ThemeSettings {
        return getThemeSettingsFlow(context).first()
    }

    suspend fun saveColorSchemeMode(context: Context, mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[COLOR_SCHEME_MODE] = mode
        }
    }

    suspend fun saveIsDark(context: Context, isDark: Boolean?) {
        context.dataStore.edit { prefs ->
            if (isDark != null) {
                prefs[HAS_IS_DARK] = true
                prefs[IS_DARK] = isDark
            } else {
                prefs[HAS_IS_DARK] = false
            }
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
}
