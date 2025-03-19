package com.horllymobile.statusvideocutter.ui.screens

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.horllymobile.statusvideocutter.R
import com.horllymobile.statusvideocutter.data.VideoTrimmerUiState
import com.horllymobile.statusvideocutter.ui.viewmodel.SavedTrimmedViewModel
import com.horllymobile.statusvideocutter.ui.viewmodel.VideoTrimmerViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun TrimScreen(
    modifier: Modifier = Modifier,
    videoTrimmerViewModel: VideoTrimmerViewModel,
    savedTrimmedViewModel: SavedTrimmedViewModel,
    context: Context,
    onSelectVideo: () -> Unit,
    chunkDuration: Int,
) {
    val videoTrimmerUiState by videoTrimmerViewModel.videoTrimmerUiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        if (videoTrimmerUiState.trimmedChunks.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(),
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
                                VideoChunkItem(index = index, file = file, context = context,
                                    onShare = {
                                        videoTrimmerViewModel.shareToWhatsApp(context, file)
                                    }
                                )
                            }
                        }
                    }
                }

            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (videoTrimmerUiState.videoUri != null && videoTrimmerUiState.trimmedChunks.isEmpty()) {
                    if (videoTrimmerUiState.errorMessage != null) {
                        Text(videoTrimmerUiState.errorMessage!!)
                    }
                    if (videoTrimmerUiState.isLoading) {
                        Text(stringResource(R.string.please_wait))
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator()
                    }

                    if (videoTrimmerUiState.videoUri != null && !videoTrimmerUiState.isLoading) {
                        BeforeChunkProcess(
                            videoTrimmerViewModel = videoTrimmerViewModel,
                            savedTrimmedViewModel = savedTrimmedViewModel,
                            chunkDuration = chunkDuration,
                            context = context
                        )
                    }
                }

                if (videoTrimmerUiState.videoUri == null && !videoTrimmerUiState.isLoading){
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = onSelectVideo) {
                            Icon(
                                Icons.Filled.OndemandVideo,
                                modifier = Modifier.size(38.dp),
                                contentDescription = stringResource(R.string.on_demand_Video)
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(stringResource(R.string.tap_the_icon))
                    }
                }
            }
        }
    }
}


@Composable
fun BeforeChunkProcess(
    modifier: Modifier = Modifier,
    videoTrimmerViewModel: VideoTrimmerViewModel,
    savedTrimmedViewModel: SavedTrimmedViewModel,
    context: Context,
    chunkDuration: Int
) {
    var duration by rememberSaveable {
        mutableIntStateOf(chunkDuration)
    }
    var chunkDropDownExpand by rememberSaveable {
        mutableStateOf(false)
    }
    val videoTrimmerUiState by videoTrimmerViewModel.videoTrimmerUiState.collectAsState()
    val scrollState = rememberScrollState()

    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,

        ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        videoTrimmerViewModel.trimVideo(context, videoTrimmerUiState.videoUri!!,
                            chunkDuration = duration,
                            savedTrimmedViewModel  = savedTrimmedViewModel
                        )
                    }
                },
            ) {
                Text(stringResource(R.string.trim_to_chunks))
            }

            IconButton(
                onClick = {
                    videoTrimmerViewModel.updateVideoUri()
                },
            ) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.clear_all))
            }

            Box {

                OutlinedButton(onClick = {
                    chunkDropDownExpand = true
                }) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.seconds, duration.toString().toInt()))
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.arrowDropDown))
                    }
                }
                DropdownMenu(
                    expanded = chunkDropDownExpand,
                    onDismissRequest = { chunkDropDownExpand = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.seconds, 60)) },
                        onClick = {
                            duration = 60
                            chunkDropDownExpand = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.seconds, 30)) },
                        onClick = {
                            duration = 30
                            chunkDropDownExpand = false
                        }
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier.height(30.dp)
        )
        VideoPreview(
            uri = videoTrimmerUiState.videoUri!!
        )
    }
}


@OptIn(UnstableApi::class)
@Composable
fun VideoPreview(
    modifier: Modifier = Modifier,
    uri: Uri
) {
    val exoPlayer = ExoPlayer.Builder(LocalContext.current).build().apply {
        setMediaItem(MediaItem.fromUri(uri))
        prepare()
        playWhenReady = false
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp), // Inner padding for grid spacing
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT // Preserve aspect ratio
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f) // Standard video aspect ratio
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoChunkItem(
    index: Int, file: File, context: Context,
    onShare: () -> Unit
) {
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
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT // Preserve aspect ratio
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f) // Standard video aspect ratio
            )

            // Share Button
            IconButton(
                onClick = onShare,
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
                    contentDescription = stringResource(R.string.share_chunk, index + 1),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
