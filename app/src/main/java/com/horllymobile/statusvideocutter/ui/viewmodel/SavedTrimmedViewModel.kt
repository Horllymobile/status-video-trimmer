package com.horllymobile.statusvideocutter.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.horllymobile.statusvideocutter.data.SavedTrimmedUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class SavedTrimmedViewModel(private val context: Context) : ViewModel() {
    private val _savedTrimmedUiState = MutableStateFlow(SavedTrimmedUiState())

    val savedTrimmedUiState get() = _savedTrimmedUiState.asStateFlow()

    init {
        loadSavedChunks()
    }

    fun loadSavedChunks() {
        Log.d("loadSavedChunks", "loadSavedChunks entered")
        val outputDir = File(context.getExternalFilesDir(null), "VideoChunker")
        if (outputDir.exists()) {
            updateSavedChunks()
            updateSavedChunks(outputDir.listFiles()?.filter { it.extension == "mp4" }?.sortedDescending() ?: emptyList())
            Log.d("loadSavedChunks", "Saved Chunks Loaded")
        }
    }

    fun deleteFile(file: File) {
        Log.d("deleteFile", "deleteFile function")
        val outputDir = File(context.getExternalFilesDir(null), "VideoChunker")
        if (outputDir.exists()) {
            val files = outputDir.listFiles()

            val found = files?.find { file.absolutePath == it.absolutePath }
            if (found != null) {
                file.delete()
                Log.d("", "File deleted")
                loadSavedChunks()
            }
        }
    }

    fun shareToWhatsApp(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            context.startActivity(Intent.createChooser(intent, "Share to WhatsApp"))
        }
        catch (e: Exception) {
            Log.d("shareToWhatsApp Error", "$e")
        }
    }

    private fun updateSavedChunks(files: List<File> = emptyList()) {
        _savedTrimmedUiState.update { state ->
            state.copy(
                savedChunks = files
            )
        }
    }
}

class SavedTrimmedViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SavedTrimmedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SavedTrimmedViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}