package com.zerostress.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.zerostress.data.model.Player
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zs_prefs")

class LocalDataStore(private val context: Context) {
    private val gson = Gson()

    companion object {
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val USER_JSON = stringPreferencesKey("current_user")
        private val ACCENT_COLOR = stringPreferencesKey("accent_color")
    }

    // Theme
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: true }
    val accentColor: Flow<String> = context.dataStore.data.map { it[ACCENT_COLOR] ?: "#38bdf8" }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setAccentColor(color: String) {
        context.dataStore.edit { it[ACCENT_COLOR] = color }
    }

    // Session
    val currentUser: Flow<Player?> = context.dataStore.data.map { prefs ->
        prefs[USER_JSON]?.let { gson.fromJson(it, Player::class.java) }
    }

    suspend fun saveUser(player: Player) {
        context.dataStore.edit { it[USER_JSON] = gson.toJson(player) }
    }

    suspend fun clearUser() {
        context.dataStore.edit { it.remove(USER_JSON) }
    }
}
