package com.mwmapps.transportoffline

import android.app.Application

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize LoggingControl with application context
        LoggingControl.initialize(this)
    }
}
