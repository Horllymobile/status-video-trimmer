package com.horllymobile.statusvideocutter.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import com.horllymobile.statusvideocutter.R
import com.horllymobile.statusvideocutter.ui.screens.SavedScreen
import com.horllymobile.statusvideocutter.ui.screens.TrimScreen
import com.horllymobile.statusvideocutter.ui.viewmodel.SavedTrimmedViewModel
import com.horllymobile.statusvideocutter.ui.viewmodel.SettingsViewModel
import com.horllymobile.statusvideocutter.ui.viewmodel.VideoTrimmerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimmerApp(
    modifier: Modifier = Modifier,
    onNavigateToSetting: () -> Unit,
    settingsViewModel: SettingsViewModel,
    videoTrimmerViewModel: VideoTrimmerViewModel,
    savedTrimmedViewModel: SavedTrimmedViewModel
) {
    val context = LocalContext.current
    val videoTrimmerUiState by videoTrimmerViewModel.videoTrimmerUiState.collectAsState()
    val tabs = listOf(stringResource(R.string.trim), stringResource(R.string.saved))

    val permissionFail = stringResource(R.string.storage_permission_denied)
    var showPermissionRationale by rememberSaveable { mutableStateOf(false) }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            videoTrimmerViewModel.updateVideoUri(result.data?.data)
        }
    }
    val settingsUiState by settingsViewModel.settingsUiState.collectAsState()
    val chunkDuration = settingsUiState.chunkDuration

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
                if (videoTrimmerUiState.trimmedChunks.isEmpty() && videoTrimmerUiState.videoUri == null) {
                    FloatingActionButton(
                        onClick = {
                            onSelectVideo()
                        }
                    ) {
                        Icon(Icons.Filled.OndemandVideo, contentDescription = stringResource(R.string.on_demand_Video))
                    }
                }

                if (videoTrimmerUiState.trimmedChunks.isNotEmpty()) {
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
                        TrimScreen(
                            videoTrimmerViewModel = videoTrimmerViewModel,
                            context = context,
                            onSelectVideo = { onSelectVideo() },
                            chunkDuration = chunkDuration.value as Int,
                            savedTrimmedViewModel = savedTrimmedViewModel
                        )
                    }
                }
                1 -> {
                    SavedScreen(
                        savedTrimmedViewModel = savedTrimmedViewModel,
                        context = context
                    )
                }
            }
        }
    }
}

