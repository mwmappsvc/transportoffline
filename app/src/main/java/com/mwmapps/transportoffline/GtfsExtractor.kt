package com.mwmapps.transportoffline

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

class GtfsExtractor(private val context: Context) {

    fun extractData(callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val extractSuccess = performExtraction()
                withContext(Dispatchers.Main) {
                    callback(extractSuccess)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }

    private fun performExtraction(): Boolean {
        return try {
            val gtfsDataDir = File(context.filesDir, "gtfs_data")
            val zipFile = File(gtfsDataDir, "google_transit.zip")

            if (!zipFile.exists()) {
                Log.e("GtfsExtractor", "Zip file not found: ${zipFile.absolutePath}")
                return false
            }

            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val outputFile = File(gtfsDataDir, entry.name)
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        zip.getInputStream(entry).use { input ->
                            outputFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }

            Log.d("GtfsExtractor", "Extraction successful, files extracted to: ${gtfsDataDir.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("GtfsExtractor", "Extraction failed", e)
            false
        }
    }
}
