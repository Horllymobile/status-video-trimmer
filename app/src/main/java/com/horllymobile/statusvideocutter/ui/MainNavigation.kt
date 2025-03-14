package com.horllymobile.statusvideocutter.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.horllymobile.statusvideocutter.ui.viewmodel.SettingsViewModel

@Composable
fun MainNavigation(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
) {
    val systemUiController = rememberSystemUiController()
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
                settingsViewModel = settingsViewModel
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