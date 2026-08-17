package com.example.meandgpt2

import android.app.Application
import android.content.res.Configuration

class MeAndGpt2Application : Application() {

    override fun onCreate() {
        super.onCreate()

        val configuration = resources.configuration

        if (configuration.fontScale != 1.0f) {
            configuration.fontScale = 1.0f

            @Suppress("DEPRECATION")
            resources.updateConfiguration(
                configuration,
                resources.displayMetrics
            )
        }
    }
}