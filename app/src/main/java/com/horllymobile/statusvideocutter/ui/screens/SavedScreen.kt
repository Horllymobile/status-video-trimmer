package com.horllymobile.statusvideocutter.ui.screens

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.horllymobile.statusvideocutter.R
import com.horllymobile.statusvideocutter.ui.viewmodel.SavedTrimmedViewModel
import java.io.File

@Composable
fun SavedScreen(
    modifier: Modifier = Modifier,
    savedTrimmedViewModel: SavedTrimmedViewModel,
    context: Context,
) {
    val savedTrimmedUiState by savedTrimmedViewModel.savedTrimmedUiState.collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (savedTrimmedUiState.savedChunks.isEmpty()) {
            Text(
                text = "No saved chunks available.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(savedTrimmedUiState.savedChunks) { index, file ->
                    SavedVideoChunkItem(index = index, file = file, context = context,
                        onShare = {
                            savedTrimmedViewModel.shareToWhatsApp(context, file)
                        },
                        onDelete = {
                            savedTrimmedViewModel.deleteFile(file)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun SavedVideoChunkItem(
    index: Int, file: File, context: Context,
    onShare: () -> Unit,
    onDelete: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomEnd),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier // Position at bottom-right
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

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier // Position at bottom-right
                        .padding(8.dp)
                        .size(40.dp) // Larger tap target
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.share_chunk, index + 1),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}