package com.mwmapps.transportoffline

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class GtfsExtractor(private val context: Context) {

    fun extractData(callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val zipFile = File(context.filesDir, "google_transit.zip")
                val outputDir = File(context.filesDir, "gtfs_data")
                if (!outputDir.exists()) {
                    outputDir.mkdir()
                }
                val inputStream = ZipInputStream(zipFile.inputStream())
                var entry = inputStream.nextEntry
                while (entry != null) {
                    val file = File(outputDir, entry.name)
                    val output = FileOutputStream(file)
                    inputStream.copyTo(output)
                    output.close()
                    inputStream.closeEntry()
                    LoggingActivity.logMessage(context, "Extracted file: ${entry.name}")
                    entry = inputStream.nextEntry
                }
                inputStream.close()
                Log.d("GtfsExtractor", "Extraction successful")
                LoggingActivity.logMessage(context, "Extraction successful")
                withContext(Dispatchers.Main) {
                    callback(true)
                }
            } catch (e: Exception) {
                Log.e("GtfsExtractor", "Extraction failed", e)
                LoggingActivity.logMessage(context, "Extraction failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }
}
