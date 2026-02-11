package com.hadley.receiptbackup.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.navigationDataStore by preferencesDataStore(name = "navigation_prefs")

object NavigationPreferences {
    private val LAST_ROUTE_KEY = stringPreferencesKey("last_route")

    fun lastRoute(context: Context): Flow<String?> {
        return context.navigationDataStore.data.map { prefs ->
            prefs[LAST_ROUTE_KEY]
        }
    }

    suspend fun saveLastRoute(context: Context, route: String) {
        context.navigationDataStore.edit { prefs ->
            prefs[LAST_ROUTE_KEY] = route
        }
    }
}

