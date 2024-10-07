// Begin ImportStatusManager.kt
package com.mwmapps.transportoffline

import android.content.Context

class ImportStatusManager(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences("TransportOfflinePrefs", Context.MODE_PRIVATE)

    fun isImportComplete(): Boolean {
        return sharedPreferences.getBoolean("import_flag", false)
    }

    fun setImportComplete(complete: Boolean) {
        sharedPreferences.edit().putBoolean("import_flag", complete).apply()
    }
}
// End ImportStatusManager.kt