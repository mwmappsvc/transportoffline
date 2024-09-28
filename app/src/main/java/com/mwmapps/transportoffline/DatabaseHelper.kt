package com.mwmapps.transportoffline

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.FileOutputStream
import java.io.InputStream

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "bus_schedule.db"
        private const val DATABASE_VERSION = 1
    }

    init {
        copyDatabase()
    }

    private fun copyDatabase() {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) {
            try {
                val inputStream: InputStream = context.assets.open(DATABASE_NAME)
                val outputStream = FileOutputStream(dbFile)

                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Log.d("DatabaseHelper", "Database copied from assets")
                LoggingActivity.logMessage(context, "Database copied from assets")
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error copying database", e)
                LoggingActivity.logMessage(context, "Error copying database: ${e.message}")
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Tables are already created in the pre-populated database
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrade if needed
    }

    fun logExistingTables() {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
        if (cursor.moveToFirst()) {
            do {
                val tableName = cursor.getString(0)
                Log.d("DatabaseHelper", "Table: $tableName")
                LoggingActivity.logMessage(context, "Table: $tableName")
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
}
