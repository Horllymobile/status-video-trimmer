package com.horllymobile.statusvideocutter.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFprobeKit
import com.horllymobile.statusvideocutter.data.VideoTrimmerUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class VideoTrimmerViewModel : ViewModel() {
    private val _videoTrimmerUiState = MutableStateFlow(VideoTrimmerUiState())

    val videoTrimmerUiState get() = _videoTrimmerUiState.asStateFlow()


    fun updateTrimmedChunks(trimmedChunks: List<File> = emptyList()) {
        _videoTrimmerUiState.update { state ->
            state.copy(
                trimmedChunks = trimmedChunks
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
                setPackage("com.whatsapp")
            }
            context.startActivity(Intent.createChooser(intent, "Share to WhatsApp"))
        } catch (e: Exception) {
            Log.d("shareToWhatsApp Error", "$e")
        }
    }

    // Helper function to get real path from URI (simplified, improve as needed)
    private fun getRealPathFromUri(context: Context, uri: Uri): String? {
        return try {
            val path = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val columnIndex = cursor.getColumnIndexOrThrow("_data")
                cursor.moveToFirst()
                cursor.getString(columnIndex)
            }
            path ?: uri.path // Fallback to uri.path if query fails
        } catch (e: Exception) {
            Log.e("getRealPathFromUri", "Error resolving path: ${e.message}")
            null
        }
    }



    suspend fun trimVideoIntoChunks(context: Context, videoUri: Uri): List<File> {
        updateLoading(true)
        delay(500)
        val outputDir = context.getExternalFilesDir(null) ?: run {
            Log.e("trimVideoIntoChunks", "Output directory is null")
            return emptyList()
        }
        val chunkList = mutableListOf<File>()
        val chunkDuration = 30 // 30 seconds in seconds

        // Simplified logic: You'd use FFmpeg or MediaCodec here
        // Example FFmpeg command: ffmpeg -i input.mp4 -t 30 -c copy output1.mp4
        try {
            val inputPath = getRealPathFromUri(context, videoUri) ?: run {
                Log.e("trimVideoIntoChunks", "Failed to resolve input path from URI: $videoUri")
                return emptyList()
            }
            Log.d("trimVideoIntoChunks", "Input path: $inputPath")

            val ffprobeSession = FFprobeKit.execute("-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 $inputPath")
            val videoDuration = ffprobeSession.output.toFloatOrNull()?.toInt() ?: run {
                Log.e("trimVideoIntoChunks", "Failed to get video duration: ${ffprobeSession.output}")
                return emptyList()
            }
            Log.d("trimVideoIntoChunks", "Video duration: $videoDuration seconds")

            // Ensure output directory exists
            if (!outputDir.exists()) outputDir.mkdirs()

            for (startTime in 0 until videoDuration step chunkDuration) {
                val outputFile = File(outputDir, "chunk_${startTime / chunkDuration}.mp4")
                val command = "-y -i \"$inputPath\" -ss $startTime -t $chunkDuration -c copy \"${outputFile.absolutePath}\""
                Log.d("trimVideoIntoChunks", "Executing FFmpeg command: $command")

                val session: FFmpegSession = FFmpegKit.execute(command)
                when (session.returnCode.isValueSuccess) {
                    true -> {
                        if (outputFile.exists()) {
                            Log.d("trimVideoIntoChunks", "Chunk saved: ${outputFile.absolutePath}")
                            chunkList.add(outputFile)
                        } else {
                            Log.e("trimVideoIntoChunks", "Chunk file not found after FFmpeg: ${outputFile.absolutePath}")
                        }
                    }
                    false -> {
                        Log.e("trimVideoIntoChunks", "FFmpeg failed: ${session.output}")
                    }
                }
            }
            updateLoading(false)
        } catch (e: Exception) {
            Log.d("trimVideoIntoChunksError", "${e.message}")
        }
        Log.d("trimVideoIntoChunks", "Final chunk list: $chunkList")
        updateTrimmedChunks(chunkList)
        return chunkList
    }

}