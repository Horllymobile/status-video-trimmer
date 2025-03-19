package com.horllymobile.statusvideocutter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.horllymobile.statusvideocutter.ui.LanguagePreferences
import com.horllymobile.statusvideocutter.ui.MainNavigation
import com.horllymobile.statusvideocutter.ui.theme.StatusVideoCutterTheme
import com.horllymobile.statusvideocutter.ui.viewmodel.SettingsViewModel
import com.horllymobile.statusvideocutter.ui.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
//    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "MainActivity"

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
//        val adRequest = AdRequest.Builder().build()
        firebaseAnalytics = Firebase.analytics
        super.onCreate(savedInstanceState)
        GlobalScope.launch {
            val language = LanguagePreferences.getLanguage(this@MainActivity)
            applyLocale(language)
        }
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(LocalContext.current)
            )
            val settingsUiState by settingsViewModel.settingsUiState.collectAsState()
            StatusVideoCutterTheme(
                darkTheme = settingsUiState.theme?.value == "Dark"
            ) {
                MainNavigation(
                    settingsViewModel = settingsViewModel
                )
            }
        }

//        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712",
//            adRequest, object : InterstitialAdLoadCallback() {
//            override fun onAdFailedToLoad(adError: LoadAdError) {
//                adError.toString().let { Log.d(TAG, it) }
//                mInterstitialAd = null
//            }
//                override fun onAdLoaded(interstitialAd: InterstitialAd) {
//                    Log.d(TAG, "Ad was loaded.")
//                    mInterstitialAd = interstitialAd
//                }
//        })
    }

    private fun applyLocale(languageCode: String) {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

//    override fun onResume() {
//        super.onResume()
//        Firebase.appDistribution.showFeedbackNotification(
//            // Text providing notice to your testers about collection and
//            // processing of their feedback data
//            R.string.additionalFormText,
//            // The level of interruption for the notification
//            InterruptionLevel.HIGH)
//        mInterstitialAd?.show(this)
//    }
}