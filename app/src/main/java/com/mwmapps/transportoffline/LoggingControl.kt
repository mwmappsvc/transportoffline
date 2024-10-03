// Section 1
// Comments with Section Numbers are Added, Removed, and Modified by the Human developer ONLY
// IMPORTANT: Do not change the location of section remarks. Keep them exactly as they are.
package com.mwmapps.transportoffline

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
// Section 2
object LoggingControl {
    enum class LoggingGroup {
        IMPORT_SIMPLE, IMPORT_VERBOSE,
        DOWNLOAD_SIMPLE, EXTRACTOR_SIMPLE, COMPARE_SIMPLE,
        QUERY_SIMPLE, QUERY_VERBOSE
    }

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("LoggingPreferences", Context.MODE_PRIVATE)
    }
// Section 3
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
// Section 4