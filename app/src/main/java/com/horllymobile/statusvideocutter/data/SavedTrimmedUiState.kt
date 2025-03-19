package com.horllymobile.statusvideocutter.data

import java.io.File

data class SavedTrimmedUiState(
    val savedChunks: List<File> = emptyList(),
)
