package com.google.samples.apps.sunflower

import android.app.Application
import android.os.Bundle
import androidx.work.Configuration
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {

  override fun onCreate() {
    super.onCreate()
    // Enable Firebase Analytics debug mode
    val analytics = FirebaseAnalytics.getInstance(this)
    analytics.setAnalyticsCollectionEnabled(true)
  }

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder()
      .setMinimumLoggingLevel(
        if (BuildConfig.DEBUG) android.util.Log.DEBUG
        else android.util.Log.ERROR
      )
      .build()
}