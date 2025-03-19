package com.horllymobile.statusvideocutter.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.horllymobile.statusvideocutter.ui.viewmodel.SavedTrimmedViewModel
import com.horllymobile.statusvideocutter.ui.viewmodel.SavedTrimmedViewModelFactory
import com.horllymobile.statusvideocutter.ui.viewmodel.SettingsViewModel
import com.horllymobile.statusvideocutter.ui.viewmodel.VideoTrimmerViewModel
import com.horllymobile.statusvideocutter.ui.viewmodel.VideoTrimmerViewModelFactory

@Composable
fun MainNavigation(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
) {
    val systemUiController = rememberSystemUiController()
    val videoTrimmerViewModel: VideoTrimmerViewModel = viewModel(
        factory = VideoTrimmerViewModelFactory(LocalContext.current)
    )

    val savedTrimmedViewModel: SavedTrimmedViewModel = viewModel(
        factory = SavedTrimmedViewModelFactory(LocalContext.current)
    )
    systemUiController.isStatusBarVisible = true
    val mainNavigationControl: NavHostController = rememberNavController()
    NavHost(
        navController = mainNavigationControl,
        startDestination = "home",
        modifier = modifier
    ) {
        composable(route = "home") {
            VideoTrimmerApp(
                onNavigateToSetting = {
                    mainNavigationControl.navigate("settings")
                },
                settingsViewModel = settingsViewModel,
                videoTrimmerViewModel = videoTrimmerViewModel,
                savedTrimmedViewModel = savedTrimmedViewModel
            )
        }
        composable(route = "settings") {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                onNavigateBack = {
                    mainNavigationControl.navigate("home")
                }
            )
        }
    }
}