package com.mwmapps.transportoffline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class DatabaseUpdater(private val context: Context, private val dbHelper: DatabaseHelper) {

    private val _updateProgress = MutableStateFlow(0)
    val updateProgress: StateFlow<Int> = _updateProgress.asStateFlow()

    private val _updateStage = MutableStateFlow<UpdateStage?>(null)
    val updateStage: StateFlow<UpdateStage?> = _updateStage.asStateFlow()

    suspend fun startUpdate(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                _updateStage.emit(UpdateStage.Downloading)
                val downloader = GtfsDownloader(context)
                val downloadSuccess = downloader.downloadGtfsData()
                downloader.downloadProgress.collect { progress ->
                    _updateProgress.emit(progress)
                }
                if (!downloadSuccess) {
                    _updateStage.emit(UpdateStage.Failed)
                    return@withContext false
                }

                _updateStage.emit(UpdateStage.Extracting)
                val extractor = GtfsExtractor(context)
                val extractionSuccess = extractor.extractData()
                extractor.extractionProgress.collect { progress ->
                    _updateProgress.emit(progress)
                }
                if (!extractionSuccess) {
                    _updateStage.emit(UpdateStage.Failed)
                    return@withContext false
                }

                _updateStage.emit(UpdateStage.Importing)
                val db = dbHelper.writableDatabase
                val importer = DataImporter(context, db)
                val importSuccess = importer.importData()
                importer.importProgress.collect { progress ->
                    _updateProgress.emit(progress)
                }
                if (!importSuccess) {
                    _updateStage.emit(UpdateStage.Failed)
                    return@withContext false
                }

                _updateStage.emit(UpdateStage.Completed)
                return@withContext true

            } catch (e: Exception) {
                _updateStage.emit(UpdateStage.Failed)
                return@withContext false
            }
        }
    }
}

enum class UpdateStage {
    Downloading,
    Extracting,
    Importing,
    Completed,
    Failed
}
