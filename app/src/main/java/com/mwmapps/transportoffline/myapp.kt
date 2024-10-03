// Section 1
// Comments with Section Numbers are Added, Removed, and Modified by the Human developer ONLY
// IMPORTANT: Do not change the location of section remarks. Keep them exactly as they are.

package com.mwmapps.transportoffline

import android.app.Application

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize LoggingControl with application context
        LoggingControl.initialize(this)
    }
}
// Section 2