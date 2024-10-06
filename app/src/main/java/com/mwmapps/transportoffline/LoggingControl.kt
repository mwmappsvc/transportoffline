// Begin LoggingControl.kt (rev 1.0)
// Manages logging settings and logs messages.
// Externally Referenced Classes: LoggingActivity
package com.mwmapps.transportoffline

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object LoggingControl {
    enum class LoggingGroup {
        IMPORT_SIMPLE, IMPORT_VERBOSE,
        DOWNLOAD_SIMPLE, EXTRACTOR_SIMPLE, COMPARE_SIMPLE,
        QUERY_SIMPLE, QUERY_VERBOSE,
        ERROR // Add this line
    }

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("LoggingPreferences", Context.MODE_PRIVATE)
    }

    fun setLoggingState(group: LoggingGroup, isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean(group.name, isEnabled).apply()
    }

    fun isLoggingEnabled(group: LoggingGroup): Boolean {
        return sharedPreferences.getBoolean(group.name, true) // Default to true
    }

    fun log(group: LoggingGroup, message: String) {
        if (isLoggingEnabled(group)) {
            Log.d(group.name, message)
            LoggingActivity.logMessage(group.name, message)
        }
    }
}
// End LoggingControl.kt