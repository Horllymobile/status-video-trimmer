package com.horllymobile.statusvideocutter.ui

//import com.arthenica.ffmpegkit.FFmpegKit
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFprobeKit
import com.google.firebase.analytics.FirebaseAnalytics
import com.horllymobile.statusvideocutter.data.VideoTrimmerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

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

    fun shareToWhatsApp(context: Context, file: File, firebaseAnalytics: FirebaseAnalytics) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.whatsapp")
            }
            context.startActivity(Intent.createChooser(intent, "Share to WhatsApp"))
//            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
//                param
//            }
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
            path ?: uri.path
        } catch (e: Exception) {
            updateError("Error resolving path: ${e.message}")
            Log.e("getRealPathFromUri", "Error resolving path: ${e.message}")
            null
        }
    }

    fun trimVideo(context: Context, videoUri: Uri) {
        updateLoading(true)
        viewModelScope.launch(Dispatchers.IO) {
            updateTrimmedChunks()
            updateError()
            try {

                val chunks = trimVideoIntoChunks(context, videoUri)
                updateTrimmedChunks(chunks)
                if (chunks.isEmpty()) {
                    updateError("No chunks were created.")
                }
                updateLoading(false)
            } catch (e: Exception) {
                updateError("Error trimming video: ${e.message}")
            } finally {
                Log.d("finally", "Done")
            }
        }
    }




     private fun trimVideoIntoChunks(context: Context, videoUri: Uri): List<File> {
         val outputDir = context.getExternalFilesDir(null) ?: run {
             Log.e("trimVideoIntoChunks", "Output directory is null")
             updateError("Output directory is null")
             return emptyList()
         }
         val chunkList = mutableListOf<File>()
         val chunkDuration = 30 // 30 seconds in seconds

         // Simplified logic: You'd use FFmpeg or MediaCodec here
         // Example FFmpeg command: ffmpeg -i input.mp4 -t 30 -c copy output1.mp4
         try {
             val tempFile = File(outputDir, "temp_video_${System.currentTimeMillis()}.mp4")
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


             val inputPath = getRealPathFromUri(context, videoUri) ?: run {
                 Log.e("trimVideoIntoChunks", "Failed to resolve input path from URI: $videoUri")
                 updateError("Failed to resolve input path from URI: $videoUri")
                 return emptyList()
             }
             Log.d("trimVideoIntoChunks", "Input path: $inputPath")

             val ffprobeCommand = "-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 \"$inputPath\""
             Log.d("trimVideoIntoChunks", "Executing FFprobe command: $ffprobeCommand")

             val ffprobeSession =
                 FFprobeKit.execute("-v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 $inputPath")
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
                 val ss = FFmpegKit.execute("-encoders")
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
         } catch (e: Exception) {
             Log.d("trimVideoIntoChunksError", "${e.message}")
         }
        Log.d("trimVideoIntoChunks", "Final chunk list: $chunkList")
        return chunkList
    }

}