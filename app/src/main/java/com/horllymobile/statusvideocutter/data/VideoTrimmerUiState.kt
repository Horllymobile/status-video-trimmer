package com.horllymobile.statusvideocutter.data

import android.net.Uri
import java.io.File

data class VideoTrimmerUiState(
    var isLoading: Boolean = false,
    var trimmedChunks: List<File> = emptyList(),
    var videoUri: Uri? = null,
    var errorMessage: String? = null
)
