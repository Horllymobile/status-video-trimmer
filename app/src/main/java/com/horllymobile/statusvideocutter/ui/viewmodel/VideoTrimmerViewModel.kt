package com.horllymobile.statusvideocutter.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.FFprobeSession
import com.horllymobile.statusvideocutter.data.VideoTrimmerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Date


class VideoTrimmerViewModel(private val context: Context) : ViewModel() {
    private val _videoTrimmerUiState = MutableStateFlow(VideoTrimmerUiState())

    val videoTrimmerUiState get() = _videoTrimmerUiState.asStateFlow()


    init {
        loadSavedChunks()
    }

    private fun loadSavedChunks() {
        val outputDir = File(context.getExternalFilesDir(null), "VideoChunker")
        if (outputDir.exists()) {
            updateSavedChunks()
            updateSavedChunks(outputDir.listFiles()?.filter { it.extension == "mp4" }?.sortedDescending() ?: emptyList())
        }
    }

    fun updateSavedChunks(files: List<File> = emptyList()) {
        _videoTrimmerUiState.update { state ->
            state.copy(
                savedChunks = files
            )
        }
    }

    fun updateSelectedTab(tab: Int) {
        _videoTrimmerUiState.update { state ->
            state.copy(
                selectedTab = tab
            )
        }
    }


    fun updateTrimmedChunks(files: List<File> = emptyList()) {
        _videoTrimmerUiState.update { state ->
            state.copy(
                trimmedChunks = files
            )
        }
    }

    fun updateVideoUri(videoUri: Uri? = null) {
        _videoTrimmerUiState.update { state ->
            state.copy(
                videoUri = videoUri
            )
        }
    }

    fun updateError(message: String? = null) {
        _videoTrimmerUiState.update { state ->
            state.copy(
                errorMessage = message
            )
        }
    }

    fun updateLoading(status: Boolean) {
        _videoTrimmerUiState.update { state ->
            state.copy(
                isLoading = status
            )
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


    fun trimVideo(context: Context, videoUri: Uri, chunkDuration: Int) {
        updateLoading(true)
        viewModelScope.launch(Dispatchers.IO) {
            updateTrimmedChunks()
            updateError()
            try {

                val chunks = trimVideoIntoChunks(context, videoUri, chunkDuration)
                updateTrimmedChunks(chunks)
                if (chunks.isEmpty()) {
                    updateError("No chunks were created.")
                }
                updateLoading(false)
                loadSavedChunks()
            } catch (e: Exception) {
                updateError("Error trimming video: ${e.message}")
            } finally {
                Log.d("finally", "Done")
            }
        }
    }


    private fun ffmpegExecute(command: String): FFprobeSession {
        return FFprobeKit.execute(command)
    }

     private fun trimVideoIntoChunks(context: Context, videoUri: Uri, chunkDuration: Int): List<File> {
         val outputDir = File(context.getExternalFilesDir(null), "VideoChunker").apply {
             if (!exists()) mkdirs()
         }
         val chunkList = mutableListOf<File>()


         // Simplified logic: You'd use FFmpeg or MediaCodec here
         // Example FFmpeg command: ffmpeg -i input.mp4 -t 30 -c copy output1.mp4
         try {
             val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
             Log.d("trimVideoIntoChunks", "Creating temporary file at: ${tempFile.absolutePath}")

             context.contentResolver.openInputStream(videoUri)?.use { input ->
                 FileOutputStream(tempFile).use { output ->
                     val bytesCopied = input.copyTo(output)
                     Log.d("trimVideoIntoChunks", "Copied $bytesCopied bytes to temporary file")
                 }
             } ?: run {
                 Log.e("trimVideoIntoChunks", "Failed to open input stream for URI: $videoUri")
                 return emptyList()
             }

             // Verify the temporary file exists and is readable
             if (!tempFile.exists() || tempFile.length() == 0L) {
                 Log.e("trimVideoIntoChunks", "Temporary file does not exist or is empty: ${tempFile.absolutePath}")
                 return emptyList()
             }

             val inputPath = tempFile.absolutePath
             Log.d("trimVideoIntoChunks", "Input path for FFmpeg: $inputPath")


             val ffprobeCommand = "-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 \"$inputPath\""

             val ffprobeSession = ffmpegExecute(ffprobeCommand)
             val videoDuration = ffprobeSession.output.toFloatOrNull()?.toInt() ?: run {
                 Log.e(
                     "trimVideoIntoChunks",
                     "Failed to get video duration: ${ffprobeSession.output}"
                 )
                 updateError("Failed to get video duration: ${ffprobeSession.output}")
                 return emptyList()
             }
             Log.d("trimVideoIntoChunks", "Video duration: $videoDuration seconds")

             // Ensure output directory exists
             if (!outputDir.exists()) outputDir.mkdirs()

             for (startTime in 0 until videoDuration step chunkDuration) {
                 val ss = ffmpegExecute("-encoders")
                 Log.d("FFmpegEncoders", "Available encoders: ${ss.output}")
                 val outputFile =
                     File(outputDir, "Vide_${Date().time}_${startTime / chunkDuration}.mp4")
                 val endTime = minOf(startTime + chunkDuration, videoDuration)
                 val command = "-y -i \"$inputPath\" -ss $startTime -t ${endTime - startTime} " +
                         "-c:v h264_mediacodec -c:a aac -r 30 -async 1 -vf setpts=PTS-STARTPTS -af asetpts=PTS-STARTPTS " +
                         "\"${outputFile.absolutePath}\""
                 Log.d("trimVideoIntoChunks", "Executing FFmpeg command: $command")

                 val session: FFmpegSession = FFmpegKit.execute(command)
                 when (session.returnCode.isValueSuccess) {
                     true -> {
                         if (outputFile.exists()) {
                             chunkList.add(outputFile)
                             Log.d("trimVideoIntoChunks", "Chunk saved: ${outputFile.absolutePath}")
                         } else {
                                 updateError("Chunk file not found after FFmpeg: ${outputFile.absolutePath}")
                                 Log.e(
                                     "trimVideoIntoChunks",
                                     "Chunk file not found after FFmpeg: ${outputFile.absolutePath}"
                                 )
                         }
                     }
                     false -> {
                         Log.e("trimVideoIntoChunks", "FFmpeg failed: ${session.output}")
                         updateError("Unable to complete action")
                     }
                 }
             }
             tempFile.delete()
         } catch (e: Exception) {
             Log.d("trimVideoIntoChunksError", "${e.message}")
         }
        Log.d("trimVideoIntoChunks", "Final chunk list: $chunkList")
        return chunkList
    }

}

class VideoTrimmerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoTrimmerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoTrimmerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}