package com.example.safetyvestinator.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.safetyvestinator.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val RECIPIENT_EMAIL = stringPreferencesKey("recipient_email")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    val recipientEmail: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECIPIENT_EMAIL] ?: ""
    }

    suspend fun setRecipientEmail(email: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RECIPIENT_EMAIL] = email
        }
    }

    val debugMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEBUG_MODE] ?: false
    }

    suspend fun setDebugMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEBUG_MODE] = enabled
        }
    }
}