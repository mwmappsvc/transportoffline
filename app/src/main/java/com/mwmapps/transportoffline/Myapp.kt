// Begin Myapp.kt (rev 1.0)
// Application class for initializing global settings.
// Externally Referenced Classes: LoggingControl

package com.mwmapps.transportoffline

import android.app.Application

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize LoggingControl with application context
        LoggingControl.initialize(this)
    }
}
// End Myapp.kt