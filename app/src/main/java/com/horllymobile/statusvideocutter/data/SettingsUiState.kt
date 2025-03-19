package com.horllymobile.statusvideocutter.data

import com.horllymobile.statusvideocutter.Language
import com.horllymobile.statusvideocutter.ui.Setting

data class SettingsUiState(
    val chunkDuration: Setting? = null,
    var language: Language? = null,
    val theme: Setting? = null,
)