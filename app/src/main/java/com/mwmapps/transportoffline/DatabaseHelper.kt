package com.mwmapps.transportoffline

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "bus_schedule.db"
        private const val DATABASE_VERSION = 2 // Increment this if you make schema changes
        private const val DATABASE_PATH = "/databases/"
    }

    private val dbPath: String = context.applicationInfo.dataDir + DATABASE_PATH + DATABASE_NAME

    init {
        if (!checkDatabase()) {
            copyDatabaseFromAssets()
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
        }
    }

    private fun checkDatabase(): Boolean {
        val dbFile = File(dbPath)
        return dbFile.exists()
    }

    fun copyDatabaseFromAssets() {
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
    }

    override fun getReadableDatabase(): SQLiteDatabase {
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
    }

    override fun getWritableDatabase(): SQLiteDatabase {
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE)
    }
}
