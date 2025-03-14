package com.horllymobile.statusvideocutter.ui.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.horllymobile.statusvideocutter.AppLocaleManager
import com.horllymobile.statusvideocutter.Language
import com.horllymobile.statusvideocutter.appLanguages
import com.horllymobile.statusvideocutter.data.SettingsUiState
import com.horllymobile.statusvideocutter.ui.Setting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val CHUNK_DURATION = intPreferencesKey("Chunk Duration")
val THEME = stringPreferencesKey("theme")

class SettingsViewModel(private val context: Context) : ViewModel() {

    private val appLocaleManager = AppLocaleManager()
    private val _settingsUiState = MutableStateFlow(SettingsUiState())

    val settingsUiState get() = _settingsUiState.asStateFlow()

    init {
        initSettings()
        loadInitialLanguage()
    }

    private  fun loadInitialLanguage() {
        val currentLanguage = appLocaleManager.getLanguage(context)
        val lang = appLanguages.find { it.code == currentLanguage }

        lang.let {
            if (it != null) {
                updateLanguage(it)
            }
        }

    }

    fun updateChunkDuration(duration: Int) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[CHUNK_DURATION] = duration
                _settingsUiState.update { state ->
                    state.copy(
                        chunkDuration = state.chunkDuration?.copy(
                            value = duration
                        ) ?: Setting(CHUNK_DURATION.name, value = duration),
                    )
                }

            }
        }
    }

    fun updateLanguage(language: Language) {
        viewModelScope.launch {
            appLocaleManager.setLocale(context, language.code)
            _settingsUiState.update { state ->
                state.copy(
                    language = language
                )
            }
        }
    }

    fun updateTheme(mode: String) {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                settings[THEME] = mode
                _settingsUiState.update { state ->
                    state.copy(
                        theme = state.theme?.copy(
                            value = mode
                        )?: Setting(THEME.name, value = mode),
                    )
                }

            }
        }
    }

    private fun initSettings() {
        viewModelScope.launch {
            context.dataStore.edit { settings ->
                val chunkDuration = settings[CHUNK_DURATION] ?: 60
                settings[CHUNK_DURATION] = chunkDuration



                val theme = settings[THEME] ?: "Dark"
                settings[THEME] = theme

                val language = appLanguages.find { it.code == appLocaleManager.getLanguage(context) }
                if (language != null) {
                    _settingsUiState.update { state ->
                        state.copy(
                            chunkDuration = Setting(CHUNK_DURATION.name, value = chunkDuration),
                            language = language,
                            theme = Setting(THEME.name, value = theme),
                        )
                    }
                }

            }
        }
    }

}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}