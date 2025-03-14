package com.horllymobile.statusvideocutter.ui

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.horllymobile.statusvideocutter.ui.viewmodel.dataStore
import kotlinx.coroutines.flow.first
import java.util.Locale

object LanguagePreferences {
    private val LANGUAGE_KEY = stringPreferencesKey("language")

    suspend fun saveLanguage(context: Context, languageCode: String) {
        Log.d("saveLanguage", languageCode)
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }

    suspend fun getLanguage(context: Context): String {
        val preferences = context.dataStore.data.first()
        Log.d("getLanguage", "${preferences[LANGUAGE_KEY]}")
        return preferences[LANGUAGE_KEY] ?: Locale.getDefault().language
    }
}