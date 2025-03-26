package com.example.senderos.utils

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

object PreferencesKeys {
    val AUTH_TOKEN = stringPreferencesKey("auth_token")
}

// Función para obtener el token
suspend fun getAuthToken(context: Context): String? {
    return context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.AUTH_TOKEN] }
        .first()  // Obtiene el primer valor disponible del Flow
}
