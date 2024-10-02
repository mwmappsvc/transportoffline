package com.mwmapps.transportoffline

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Log

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

    suspend fun startUpdate(gtfsUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Copy the database before starting the update
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

                // Delete the journal file to ensure a clean state
                dbHelper.deleteJournalFile()

                // Proceed with the existing update process
                _updateStage.value = UpdateStage.Downloading
                val downloader = GtfsDownloader(context)
                val downloadSuccess = downloader.downloadGtfsData(gtfsUrl)
                if (!downloadSuccess) {
                    _updateStage.value = UpdateStage.DownloadError
                    return@withContext false
                }

                _updateStage.value = UpdateStage.Extracting
                val extractor = GtfsExtractor(context)
                val extractSuccess = extractor.extractGtfsData(
                    "${context.filesDir}/gtfs_data/google_transit.zip",
                    "${context.filesDir}/gtfs_data"
                )
                if (!extractSuccess) {
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
                val db = dbHelper.writableDatabase
                val importer = DataImporter(context, db)
                val importSuccess = importer.importData()
                if (!importSuccess) {
                    _updateStage.value = UpdateStage.ImportError
                    return@withContext false
                }

                // Store the new hash after a successful update
                val newHash = calculateHash(File(context.filesDir, "gtfs_data/google_transit.zip"))
                storeHash(context, newHash)

                _updateStage.value = UpdateStage.Completed
                true
            } catch (e: Exception) {
                _updateStage.value = UpdateStage.Error
                Log.e("DatabaseUpdater", "Update failed: ${e.message}")
                false
            }
        }
    }

    suspend fun forceUpdate(gtfsUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Copy the database before starting the update
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

                // Delete the journal file to ensure a clean state
                dbHelper.deleteJournalFile()

                // Proceed with the existing update process
                _updateStage.value = UpdateStage.Downloading
                val downloader = GtfsDownloader(context)
                val downloadSuccess = downloader.downloadGtfsData(gtfsUrl)
                if (!downloadSuccess) {
                    _updateStage.value = UpdateStage.DownloadError
                    return@withContext false
                }
                _updateStage.value = UpdateStage.Importing
                val db = dbHelper.writableDatabase
                val importer = DataImporter(context, db)
                val importSuccess = importer.importData()
                if (!importSuccess) {
                    _updateStage.value = UpdateStage.ImportError
                    return@withContext false
                }

                // Store the new hash after a successful update
                val newHash = calculateHash(File(context.filesDir, "gtfs_data/google_transit.zip"))
                storeHash(context, newHash)

                _updateStage.value = UpdateStage.Completed
                true
            } catch (e: Exception) {
                _updateStage.value = UpdateStage.Error
                Log.e("DatabaseUpdater", "Force update failed: ${e.message}")
                false
            }
        }
    }

    fun getImportProgress(): StateFlow<Int> {
        return _updateProgress
    }
}
