package com.mwmapps.transportoffline

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

class GtfsExtractor(private val context: Context) {

    private val _extractionProgress = MutableStateFlow(0)
    val extractionProgress: StateFlow<Int> = _extractionProgress.asStateFlow()

    suspend fun extractData(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val gtfsDataDir = File(context.filesDir, "gtfs_data")
                val zipFile = File(gtfsDataDir, "google_transit.zip")

                if (!zipFile.exists()) {
                    Log.e("GtfsExtractor", "Zip file not found: ${zipFile.absolutePath}")
                    return@withContext false
                }

                ZipFile(zipFile).use { zip ->
                    val totalFiles = zip.size()
                    var filesExtracted = 0

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

                        filesExtracted++
                        val progress = (filesExtracted * 100 / totalFiles).toInt()
                        _extractionProgress.emit(progress)
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
}
