package com.hadley.receiptbackup.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class ThemeMode(val value: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromValue(value: String?): ThemeMode {
            return entries.firstOrNull { it.value == value } ?: SYSTEM
        }
    }
}

object SettingsDataStore {
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

    fun themeModeFlow(context: Context): Flow<ThemeMode> {
        return context.settingsDataStore.data.map { prefs ->
            ThemeMode.fromValue(prefs[THEME_MODE_KEY])
        }
    }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.value
        }
    }
}

