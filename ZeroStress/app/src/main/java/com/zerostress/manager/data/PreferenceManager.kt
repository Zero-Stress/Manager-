package com.zerostress.manager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zero_stress_prefs")

class PreferenceManager(private val context: Context) {

    companion object {
        private val THEME_KEY = booleanPreferencesKey("dark_theme")
        private val SESSION_PHONE = stringPreferencesKey("session_phone")
        private val SESSION_NAME = stringPreferencesKey("session_name")
        private val SESSION_ROLE = stringPreferencesKey("session_role")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[THEME_KEY] ?: true }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    val sessionPhone: Flow<String> = context.dataStore.data.map { it[SESSION_PHONE] ?: "" }
    val sessionName: Flow<String> = context.dataStore.data.map { it[SESSION_NAME] ?: "" }
    val sessionRole: Flow<String> = context.dataStore.data.map { it[SESSION_ROLE] ?: "player" }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[THEME_KEY] = enabled }
    }

    suspend fun saveSession(phone: String, name: String, role: String) {
        context.dataStore.edit {
            it[SESSION_PHONE] = phone
            it[SESSION_NAME] = name
            it[SESSION_ROLE] = role
            it[IS_LOGGED_IN] = true
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it[SESSION_PHONE] = ""
            it[SESSION_NAME] = ""
            it[SESSION_ROLE] = ""
            it[IS_LOGGED_IN] = false
        }
    }
}
