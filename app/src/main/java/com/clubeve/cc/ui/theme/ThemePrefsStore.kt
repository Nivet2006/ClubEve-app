package com.clubeve.cc.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themePrefsDataStore by preferencesDataStore(name = "theme_prefs")

/**
 * Persists all theme flags (dark mode, glass mode) across app restarts.
 * Glass accent color is handled separately in GlassColorStore.
 */
object ThemePrefsStore {
    private val KEY_IS_DARK  = booleanPreferencesKey("is_dark")
    private val KEY_IS_GLASS = booleanPreferencesKey("is_glass")

    fun isDarkFlow(context: Context): Flow<Boolean> =
        context.themePrefsDataStore.data.map { it[KEY_IS_DARK] ?: false }

    fun isGlassFlow(context: Context): Flow<Boolean> =
        context.themePrefsDataStore.data.map { it[KEY_IS_GLASS] ?: false }

    suspend fun saveDark(context: Context, isDark: Boolean) {
        context.themePrefsDataStore.edit { it[KEY_IS_DARK] = isDark }
    }

    suspend fun saveGlass(context: Context, isGlass: Boolean) {
        context.themePrefsDataStore.edit { it[KEY_IS_GLASS] = isGlass }
    }
}
