// Section 1
package com.mwmapps.transportoffline

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Log
// Section 2
class DatabaseUpdater(
    private val context: Context,
    private val dbHelper: DatabaseHelper,
    private val lifecycleScope: LifecycleCoroutineScope
) {

    private val _updateProgress = MutableStateFlow(0)
    val updateProgress: StateFlow<Int> = _updateProgress

    private val _updateStage = MutableStateFlow<UpdateStage>(UpdateStage.Idle)
    val updateStage: StateFlow<UpdateStage> = _updateStage

    private val _currentTable = MutableStateFlow("")
    val currentTable: StateFlow<String> = _currentTable
    // Section 3
    suspend fun startUpdate(gtfsUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DatabaseUpdater", "Starting database copy...")
                if (!dbHelper.copyDatabase(context)) {
                    _updateStage.value = UpdateStage.Error
                    Log.e("DatabaseUpdater", "Database copy failed")
                    return@withContext false
                }

                Log.d("DatabaseUpdater", "Verifying database copy...")
                if (!dbHelper.isDatabaseCopied(context)) {
                    _updateStage.value = UpdateStage.Error
                    Log.e("DatabaseUpdater", "Database copy verification failed")
                    return@withContext false
                }
// Section 4
                dbHelper.deleteJournalFile()

                _updateStage.value = UpdateStage.Downloading
                val downloader = GtfsDownloader(context)
                val downloadSuccess = downloader.downloadGtfsData(gtfsUrl)
                if (!downloadSuccess) {
                    _updateStage.value = UpdateStage.DownloadError
                    return@withContext false
                }

                _updateStage.value = UpdateStage.Extracting
                val extractor = GtfsExtractor(context)
                val extractionSuccess = extractor.extractGtfsData(
                    zipFilePath = "${context.filesDir}/gtfs_data/google_transit.zip",
                    outputDir = "${context.filesDir}/gtfs_data"
                )
                if (!extractionSuccess) {
                    _updateStage.value = UpdateStage.ExtractionError
                    return@withContext false
                }

                _updateStage.value = UpdateStage.Comparing
                val gtfsCompare = GtfsCompare(context)
                val isNewData = gtfsCompare.isNewData()
                if (!isNewData) {
                    _updateStage.value = UpdateStage.NoUpdateNeeded
                    return@withContext true
                }

                _updateStage.value = UpdateStage.Importing
                val importer = DataImporter(context, dbHelper)
                val importSuccess = importer.importGtfsData(context.filesDir.path + "/gtfs_data") // Pass directory path
                if (!importSuccess) {
                    _updateStage.value = UpdateStage.ImportError
                    return@withContext false
                }

                val newHash = HashUtils.calculateHash(File(context.filesDir, "gtfs_data/google_transit.zip"))
                HashUtils.storeHash(context, newHash)

                _updateStage.value = UpdateStage.Completed
                true
            } catch (e: Exception) {
                _updateStage.value = UpdateStage.Error
                Log.e("DatabaseUpdater", "Update failed: ${e.message}")
                false
            }
        }
    }
    // Section 5
    suspend fun forceUpdate(gtfsUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DatabaseUpdater", "Starting database copy...")
                if (!dbHelper.copyDatabase(context)) {
                    _updateStage.value = UpdateStage.Error
                    Log.e("DatabaseUpdater", "Database copy failed")
                    return@withContext false
                }

                Log.d("DatabaseUpdater", "Verifying database copy...")
                if (!dbHelper.isDatabaseCopied(context)) {
                    _updateStage.value = UpdateStage.Error
                    Log.e("DatabaseUpdater", "Database copy verification failed")
                    return@withContext false
                }

                dbHelper.deleteJournalFile()

                _updateStage.value = UpdateStage.Downloading
                val downloader = GtfsDownloader(context)
                val downloadSuccess = downloader.downloadGtfsData(gtfsUrl)
                if (!downloadSuccess) {
                    _updateStage.value = UpdateStage.DownloadError
                    return@withContext false
                }

                _updateStage.value = UpdateStage.Importing
                val importer = DataImporter(context, dbHelper)
                val importSuccess = importer.importGtfsData(context.filesDir.path + "/gtfs_data") // Pass directory path
                if (!importSuccess) {
                    _updateStage.value = UpdateStage.ImportError
                    return@withContext false
                }

                val newHash = HashUtils.calculateHash(File(context.filesDir, "gtfs_data/google_transit.zip"))
                HashUtils.storeHash(context, newHash)

                _updateStage.value = UpdateStage.Completed
                true
            } catch (e: Exception) {
                _updateStage.value = UpdateStage.Error
                Log.e("DatabaseUpdater", "Force update failed: ${e.message}")
                false
            }
        }
    }
    // Section 6
    private fun calculateHash(file: File): String {
        // Your logic to calculate the hash of the file
        return "hash_value"
    }

    private fun storeHash(context: Context, hash: String) {
        // Your logic to store the hash
    }
}
// Section 7
