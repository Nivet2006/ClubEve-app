package com.clubeve.cc.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.glassColorDataStore by preferencesDataStore(name = "glass_color_prefs")

object GlassColorStore {
    private val KEY_ACCENT = longPreferencesKey("glass_accent_color")

    // Default: soft violet 0xFF9B8FFF
    private const val DEFAULT_ACCENT = 0xFF9B8FFFu

    fun accentColorFlow(context: Context): Flow<Long> =
        context.glassColorDataStore.data.map { prefs ->
            prefs[KEY_ACCENT] ?: DEFAULT_ACCENT.toLong()
        }

    suspend fun saveAccentColor(context: Context, colorArgb: Long) {
        context.glassColorDataStore.edit { prefs ->
            prefs[KEY_ACCENT] = colorArgb
        }
    }
}
