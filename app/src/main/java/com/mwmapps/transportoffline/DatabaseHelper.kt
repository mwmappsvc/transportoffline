// Begin DatabaseHelper.kt (rev 1.0)
// Manages database creation and version management.
// Externally Referenced Classes: DatabaseUpdater, DataImporter, DataQuery
package com.mwmapps.transportoffline

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.IOException
import android.util.Log

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "bus_schedule.db"
        private const val DATABASE_VERSION = 2 // Increment this if you make schema changes
        private const val DATABASE_PATH = "/databases/"
        private const val IMPORT_COMPLETE_FLAG = "import_complete"
    }

    private val dbPath: String = context.applicationInfo.dataDir + DATABASE_PATH + DATABASE_NAME

    init {
        if (!checkDatabase()) {
            copyDatabaseFromAssets()
            setImportComplete(false)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // No need to create tables here as we are copying the pre-populated database
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrade
        if (oldVersion < newVersion) {
            context.deleteDatabase(DATABASE_NAME)
            copyDatabaseFromAssets()
            setImportComplete(false)
        }
    }

    private fun checkDatabase(): Boolean {
        val dbFile = File(dbPath)
        return dbFile.exists()
    }

    fun copyDatabaseFromAssets() {
        Log.d("DatabaseHelper", "Copying database from assets")
        val dbDir = File(context.applicationInfo.dataDir + DATABASE_PATH)
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }

        val inputStream: InputStream = context.assets.open(DATABASE_NAME)
        val outputStream: OutputStream = FileOutputStream(dbPath)

        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }

        outputStream.flush()
        outputStream.close()
        inputStream.close()
        Log.d("DatabaseHelper", "Database copy completed")
    }

    fun copyDatabase(context: Context): Boolean {
        return try {
            val inputStream: InputStream = context.assets.open(DATABASE_NAME)
            val outputFile = File(context.getDatabasePath(DATABASE_NAME).path)
            val outputStream: OutputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            Log.d("DatabaseHelper", "Database copy completed successfully")
            true
        } catch (e: IOException) {
            Log.e("DatabaseHelper", "Database copy failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun isDatabaseCopied(context: Context): Boolean {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val exists = dbFile.exists()
        Log.d("DatabaseHelper", "Database copy verification: $exists")
        return exists
    }

    fun deleteJournalFile() {
        val journalFile = File(dbPath + "-journal")
        if (journalFile.exists()) {
            journalFile.delete()
            Log.d("DatabaseHelper", "Journal file deleted")
        } else {
            Log.d("DatabaseHelper", "No journal file to delete")
        }
    }

    override fun getReadableDatabase(): SQLiteDatabase {
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
    }

    override fun getWritableDatabase(): SQLiteDatabase {
        var db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
        if (db.isReadOnly) {
            copyDatabaseFromAssets()
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
        }
        return db
    }

    fun isImportComplete(): Boolean {
        val db = writableDatabase // Ensure the database is opened in writable mode
        val cursor = db.rawQuery("SELECT value FROM flags WHERE key = ?", arrayOf(IMPORT_COMPLETE_FLAG))
        val isComplete = cursor.moveToFirst() && cursor.getInt(0) == 1
        cursor.close()
        Log.d("DatabaseHelper", "Import complete status: $isComplete")
        return isComplete
    }

    fun setImportComplete(complete: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("value", if (complete) 1 else 0)
        }
        db.update("flags", values, "key = ?", arrayOf(IMPORT_COMPLETE_FLAG))
        Log.d("DatabaseHelper", "Import flag set to ${if (complete) "true" else "false"}")
        LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "Import flag set to ${if (complete) "true" else "false"}")
    }
}
// End DatabaseHelper.kt