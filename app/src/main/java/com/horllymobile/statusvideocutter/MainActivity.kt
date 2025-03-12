package com.horllymobile.statusvideocutter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.appdistribution.InterruptionLevel
import com.google.firebase.appdistribution.appDistribution
import com.horllymobile.statusvideocutter.ui.VideoTrimmerApp
import com.horllymobile.statusvideocutter.ui.theme.StatusVideoCutterTheme

class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        firebaseAnalytics = Firebase.analytics
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StatusVideoCutterTheme {
                VideoTrimmerApp(
                    firebaseAnalytics = firebaseAnalytics
                )
                Firebase.appDistribution.showFeedbackNotification(
                    // Text providing notice to your testers about collection and
                    // processing of their feedback data
                    R.string.additionalFormText,
                    // The level of interruption for the notification
                    InterruptionLevel.HIGH)
            }
        }
    }
}