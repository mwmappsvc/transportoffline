// Section 1
// Comments with Section Numbers are Added, Removed, and Modified by the Human developer ONLY

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