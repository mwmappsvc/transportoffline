package com.mwmapps.transportoffline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class GtfsCompare(private val context: Context) {

    suspend fun isUpdateNeeded(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val gtfsDataDir = File(context.filesDir, "gtfs_data")
                val existingFile = File(gtfsDataDir, "google_transit.zip")
                val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                val url = sharedPreferences.getString("gtfs_url", "https://www.rtd-denver.com/files/gtfs/google_transit.zip") ?: return@withContext false

                val urlConnection = URL(url).openConnection()
                urlConnection.connect()
                val newFileSize = urlConnection.contentLength

                if (existingFile.exists()) {
                    val existingFileSize = existingFile.length().toInt()
                    LoggingControl.log(LoggingControl.LoggingGroup.COMPARE_SIMPLE, "Comparing file sizes: newFileSize=$newFileSize, existingFileSize=$existingFileSize")
                    return@withContext newFileSize != existingFileSize
                } else {
                    LoggingControl.log(LoggingControl.LoggingGroup.COMPARE_SIMPLE, "Existing file not found, update needed")
                    return@withContext true
                }
            } catch (e: Exception) {
                LoggingControl.log(LoggingControl.LoggingGroup.COMPARE_SIMPLE, "Error comparing GTFS data. ${e.message}")
                return@withContext false
            }
        }
    }
}
