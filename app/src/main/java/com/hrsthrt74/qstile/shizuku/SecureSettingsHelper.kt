package com.hrsthrt74.qstile.shizuku

import android.content.Context
import android.provider.Settings
import android.util.Log

object SecureSettingsHelper {
    private const val TAG = "SecureSettingsHelper"
    private const val SYSUI_QS_TILES = "sysui_qs_tiles"

    fun getSysuiQsTiles(context: Context): String? {
        return try {
            Settings.Secure.getString(context.contentResolver, SYSUI_QS_TILES)
        } catch (e: Exception) {
            Log.e(TAG, "getSysuiQsTiles failed", e)
            null
        }
    }

    fun setSysuiQsTiles(context: Context, value: String): Boolean {
        return try {
            Settings.Secure.putString(context.contentResolver, SYSUI_QS_TILES, value)
        } catch (e: Exception) {
            Log.e(TAG, "setSysuiQsTiles failed", e)
            false
        }
    }

    fun getCurrentTiles(context: Context): List<String> {
        val tilesString = getSysuiQsTiles(context) ?: return emptyList()
        return tilesString.split(",").filter { it.isNotBlank() }
    }

    fun setCurrentTiles(context: Context, tiles: List<String>): Boolean {
        val tilesString = tiles.joinToString(",")
        return setSysuiQsTiles(context, tilesString)
    }
}
