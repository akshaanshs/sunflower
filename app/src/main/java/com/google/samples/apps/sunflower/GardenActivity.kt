package com.google.samples.apps.sunflower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.samples.apps.sunflower.compose.SunflowerApp
import com.google.samples.apps.sunflower.ui.SunflowerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GardenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Firebase.analytics.setUserId("sunflower_user_001")

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                android.util.Log.d("FCM_TOKEN", "Token: $token")
            } else {
                android.util.Log.d("FCM_TOKEN", "Failed: ${task.exception?.message}")
            }
        }

        setContent {
            SunflowerTheme {
                SunflowerApp()
            }
        }
    }
}