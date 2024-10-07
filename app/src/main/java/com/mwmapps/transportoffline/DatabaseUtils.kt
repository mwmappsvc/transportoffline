// Begin DatabaseUtils.kt (rev 1.1)
// Provides utility methods for database operations.
// Externally Referenced Classes: DatabaseHelper
package com.mwmapps.transportoffline

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log

object DatabaseUtils {
    fun initializeDatabase(context: Context): Boolean {
        val dbHelper = DatabaseHelper(context)
        if (!dbHelper.isDatabaseCopied(context)) {
            dbHelper.copyDatabaseFromAssets()
            setImportCompleteFlag(context, false)
        }
        return isImportComplete(context)
    }

    fun checkImportComplete(context: Context): Boolean {
        return isImportComplete(context)
    }

    fun getDatabaseWithRetry(context: Context): SQLiteDatabase {
        val dbHelper = DatabaseHelper(context)
        var db: SQLiteDatabase? = null
        var attempts = 0
        val maxAttempts = 5

        while (db == null && attempts < maxAttempts) {
            try {
                db = dbHelper.writableDatabase // Ensure the database is opened in writable mode
            } catch (e: SQLiteDatabaseLockedException) {
                attempts++
                Thread.sleep(100) // Wait for 100ms before retrying
            }
        }

        if (db == null) {
            throw SQLiteDatabaseLockedException("Failed to open database after $maxAttempts attempts")
        }

        return db
    }

    private fun setImportCompleteFlag(context: Context, complete: Boolean) {
        val sharedPreferences = context.getSharedPreferences("TransportOfflinePrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("import_flag", complete).apply()
        Log.d("DatabaseUtils", "Import flag set to ${if (complete) "true" else "false"} in SharedPreferences")
    }

    private fun isImportComplete(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("TransportOfflinePrefs", Context.MODE_PRIVATE)
        val isComplete = sharedPreferences.getBoolean("import_flag", false)
        Log.d("DatabaseUtils", "Import complete status: $isComplete")
        return isComplete
    }
}
// End DatabaseUtils.kt