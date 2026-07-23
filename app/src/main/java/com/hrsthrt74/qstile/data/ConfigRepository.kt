package com.hrsthrt74.qstile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tile_config")

object ConfigRepository {
    private val EXPANDED_TILES = stringPreferencesKey("expanded_tiles")
    private val COLLAPSED_TILES = stringPreferencesKey("collapsed_tiles")
    private val IS_EXPANDED = booleanPreferencesKey("is_expanded")

    private val DEFAULT_EXPANDED = listOf("wifi", "bt", "cell", "airplane", "flashlight", "hotspot")
    private val DEFAULT_COLLAPSED = listOf("wifi", "bt", "cell")

    fun getConfigFlow(context: Context): Flow<TileConfig> {
        return context.dataStore.data.map { preferences ->
            TileConfig(
                expandedTiles = preferences[EXPANDED_TILES]?.split(",")?.filter { it.isNotBlank() } ?: DEFAULT_EXPANDED,
                collapsedTiles = preferences[COLLAPSED_TILES]?.split(",")?.filter { it.isNotBlank() } ?: DEFAULT_COLLAPSED,
                isExpanded = preferences[IS_EXPANDED] ?: false
            )
        }
    }

    suspend fun getConfig(context: Context): TileConfig {
        return getConfigFlow(context).first()
    }

    suspend fun saveExpandedTiles(context: Context, tiles: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[EXPANDED_TILES] = tiles.joinToString(",")
        }
    }

    suspend fun saveCollapsedTiles(context: Context, tiles: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[COLLAPSED_TILES] = tiles.joinToString(",")
        }
    }

    suspend fun saveIsExpanded(context: Context, isExpanded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_EXPANDED] = isExpanded
        }
    }

    suspend fun toggleExpanded(context: Context): Boolean {
        val config = getConfig(context)
        val newIsExpanded = !config.isExpanded
        saveIsExpanded(context, newIsExpanded)
        return newIsExpanded
    }
}
