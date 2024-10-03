// Begin DatabaseUtils.kt
// Provides utility methods for database operations.
// Externally Referenced Classes: DatabaseHelper
package com.mwmapps.transportoffline

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseLockedException

object DatabaseUtils {
    fun getDatabaseWithRetry(context: Context): SQLiteDatabase {
        val dbHelper = DatabaseHelper(context)
        var db: SQLiteDatabase? = null
        var attempts = 0
        val maxAttempts = 5

        while (db == null && attempts < maxAttempts) {
            try {
                db = dbHelper.readableDatabase
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
}
// End DatabaseUtils.kt