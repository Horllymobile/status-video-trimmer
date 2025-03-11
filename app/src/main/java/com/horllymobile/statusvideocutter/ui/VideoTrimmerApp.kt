package com.horllymobile.statusvideocutter.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFprobeKit
import com.horllymobile.statusvideocutter.data.VideoTrimmerUiState
import kotlinx.coroutines.launch
import java.io.File


@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun VideoTrimmerApp(
    modifier: Modifier = Modifier
) {
    val videoTrimmerViewModel: VideoTrimmerViewModel = viewModel()
    val videoTrimmerUiState by videoTrimmerViewModel.videoTrimmerUiState.collectAsState()
    val context = LocalContext.current
//    var videoUri by remember { mutableStateOf<Uri?>(null) }
//    var trimmedChunks by remember { mutableStateOf<List<File>>(emptyList()) }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            videoTrimmerViewModel.updateVideoUri(result.data?.data)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "video/*"
            }
            pickVideoLauncher.launch(intent)
        } else {
            Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (videoTrimmerUiState.videoUri == null) {
                FloatingActionButton(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                    }
                ) {
                    Icon(Icons.Filled.OndemandVideo, contentDescription = "OndemandVideo")
                }
            }
            if (videoTrimmerUiState.videoUri != null) {
                FloatingActionButton(
                    onClick = {
                        videoTrimmerViewModel.updateTrimmedChunks()
                        videoTrimmerViewModel.updateVideoUri()
                    }
                ) {
                    Icon(Icons.Filled.ClearAll, contentDescription = "ClearAll")
                }
            }
        }
    ) { paddding ->

        if (videoTrimmerUiState.trimmedChunks.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                if (videoTrimmerUiState.trimmedChunks.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {

                        // Staggered Grid for video chunks
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2), // 2 columns for consistency
                            verticalItemSpacing = 8.dp,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(videoTrimmerUiState.trimmedChunks) { index, file ->
                                VideoChunkItem(index = index, file = file, context = context, videoTrimmerViewModel = videoTrimmerViewModel)
                            }
                        }
                    }
                }

            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (videoTrimmerUiState.videoUri != null && videoTrimmerUiState.trimmedChunks.isEmpty()) {
                    if (videoTrimmerUiState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    videoTrimmerViewModel.trimVideoIntoChunks(context, videoTrimmerUiState.videoUri!!)
                                }
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Trim into 30s Chunks")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tap the icon")
                        Spacer(modifier = Modifier.width(5.dp))
                        IconButton(onClick = {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                        }) {
                            Icon(Icons.Filled.OndemandVideo, contentDescription = "OndemandVideo")
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("to choose a video for trimming")
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoChunkItem(index: Int, file: File, context: Context, videoTrimmerViewModel: VideoTrimmerViewModel) {
    // ExoPlayer setup
    val exoPlayer = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Card to wrap the video and share button for elevation and separation
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp), // Inner padding for grid spacing
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Video Preview
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT // Preserve aspect ratio
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f) // Standard video aspect ratio
            )

            // Share Button
            IconButton(
                onClick = {
                    videoTrimmerViewModel.shareToWhatsApp(context, file)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd) // Position at bottom-right
                    .padding(8.dp)
                    .size(40.dp) // Larger tap target
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share chunk ${index + 1}",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
