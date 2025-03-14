package com.horllymobile.statusvideocutter.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.horllymobile.statusvideocutter.R
import com.horllymobile.statusvideocutter.ui.viewmodel.SettingsViewModel
import com.horllymobile.statusvideocutter.ui.viewmodel.VideoTrimmerViewModel
import com.horllymobile.statusvideocutter.ui.viewmodel.VideoTrimmerViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimmerApp(
    modifier: Modifier = Modifier,
    onNavigateToSetting: () -> Unit,
    settingsViewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    val videoTrimmerViewModel: VideoTrimmerViewModel = viewModel(
        factory = VideoTrimmerViewModelFactory(context)
    )
    val videoTrimmerUiState by videoTrimmerViewModel.videoTrimmerUiState.collectAsState()
    val tabs = listOf(stringResource(R.string.trim), stringResource(R.string.saved))

    val permissionFail = stringResource(R.string.storage_permission_denied)
    var showPermissionRationale by remember { mutableStateOf(false) }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            videoTrimmerViewModel.updateVideoUri(result.data?.data)
        }
    }
    val settingsUiState by settingsViewModel.settingsUiState.collectAsState()
    Log.d("settingsUiState", "$settingsUiState")
    val chunkDuration = settingsUiState.chunkDuration
    Log.d("chunkDuration", "$chunkDuration")

    val activity = LocalActivity.current

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "video/*"
            }
            pickVideoLauncher.launch(intent)
        } else {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val shouldShowRationale = activity
                ?.shouldShowRequestPermissionRationale(permission) == true

            if (shouldShowRationale) {
                // Show rationale and allow re-request
                showPermissionRationale = true
            } else {
                // Permission permanently denied, guide to settings
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = permissionFail,
                        actionLabel = "Settings",
                        duration = SnackbarDuration.Long,
                    ).apply {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    }

    fun onSelectVideo() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        permissionLauncher.launch(permission)
    }

    LaunchedEffect(videoTrimmerUiState.errorMessage) {
        if (videoTrimmerUiState.errorMessage != null) {
            delay(5000)
            videoTrimmerViewModel.updateError()
            videoTrimmerViewModel.updateVideoUri()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {},
                title = {
                    Text(stringResource(R.string.app_name))
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSetting
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    if (videoTrimmerUiState.selectedTab == 0 && videoTrimmerUiState.trimmedChunks.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                videoTrimmerViewModel.updateTrimmedChunks()
                                videoTrimmerViewModel.updateVideoUri()
                            }
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.clear_all))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (videoTrimmerUiState.selectedTab == 0) {
                if (videoTrimmerUiState.trimmedChunks.isEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            onSelectVideo()
                        }
                    ) {
                        Icon(Icons.Filled.OndemandVideo, contentDescription = stringResource(R.string.on_demand_Video))
                    }
                } else {
                    FloatingActionButton(
                        onClick = {
                            videoTrimmerViewModel.updateTrimmedChunks()
                            videoTrimmerViewModel.updateVideoUri()
                        }
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.clear_all))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = videoTrimmerUiState.selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = videoTrimmerUiState.selectedTab == index,
                        onClick = { videoTrimmerViewModel.updateSelectedTab(index) }
                    )
                }
            }

            when(videoTrimmerUiState.selectedTab) {
                0 -> {
                    if (chunkDuration != null) {
                        TrimmerScreen(
                            videoTrimmerViewModel = videoTrimmerViewModel,
                            context = context,
                            onSelectVideo = { onSelectVideo() },
                            chunkDuration = chunkDuration.value as Int
                        )
                    }
                }
                1 -> {
                    SavedScreen(
                        videoTrimmerViewModel = videoTrimmerViewModel,
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
fun TrimmerScreen(
    modifier: Modifier = Modifier,
    videoTrimmerViewModel: VideoTrimmerViewModel,
    context: Context,
    onSelectVideo: () -> Unit,
    chunkDuration: Int,
) {
    val videoTrimmerUiState by videoTrimmerViewModel.videoTrimmerUiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
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
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    videoTrimmerViewModel.trimVideo(context, videoTrimmerUiState.videoUri!!, chunkDuration = chunkDuration)
                                }
                            },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(stringResource(R.string.trim_to_chunks))
                        }
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
fun SavedScreen(
    modifier: Modifier = Modifier,
    videoTrimmerViewModel: VideoTrimmerViewModel,
    context: Context,
) {
    val videoTrimmerUiState by videoTrimmerViewModel.videoTrimmerUiState.collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (videoTrimmerUiState.savedChunks.isEmpty()) {
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
                itemsIndexed(videoTrimmerUiState.savedChunks) { index, file ->
                    VideoChunkItem(index = index, file = file, context = context,
                        onShare = {
                            videoTrimmerViewModel.shareToWhatsApp(context, file)
                        })
                }
            }
        }
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
