package com.mwmapps.transportoffline

import android.content.Context
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
                    LoggingControl.log(LoggingControl.LoggingGroup.EXTRACTOR_SIMPLE, "Zip file not found: ${zipFile.absolutePath}")
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
                        withContext(Dispatchers.Main) {
                            _extractionProgress.emit(progress)
                        }
                    }
                }

                LoggingControl.log(LoggingControl.LoggingGroup.EXTRACTOR_SIMPLE, "Extraction successful, files extracted to: ${gtfsDataDir.absolutePath}")
                return@withContext verifyData(gtfsDataDir)
            } catch (e: Exception) {
                LoggingControl.log(LoggingControl.LoggingGroup.EXTRACTOR_SIMPLE, "Extraction failed. ${e.message}")
                return@withContext false
            }
        }
    }

    private fun verifyData(gtfsDataDir: File): Boolean {
        val requiredFiles = listOf("agency.txt", "stops.txt", "routes.txt", "trips.txt", "stop_times.txt")
        for (fileName in requiredFiles) {
            val file = File(gtfsDataDir, fileName)
            if (!file.exists()) {
                LoggingControl.log(LoggingControl.LoggingGroup.EXTRACTOR_SIMPLE, "Verification failed: Missing file $fileName")
                return false
            }
        }
        LoggingControl.log(LoggingControl.LoggingGroup.EXTRACTOR_SIMPLE, "Verification successful")
        return true
    }
}
