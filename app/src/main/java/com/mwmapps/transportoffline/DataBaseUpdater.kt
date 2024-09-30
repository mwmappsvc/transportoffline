package com.mwmapps.transportoffline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

class DatabaseUpdater(private val context: Context, private val dbHelper: DatabaseHelper) {

    private val _updateProgress = MutableStateFlow(0)
    val updateProgress: StateFlow<Int> = _updateProgress.asStateFlow()

    private val _updateStage = MutableStateFlow<UpdateStage?>(null)
    val updateStage: StateFlow<UpdateStage?> = _updateStage.asStateFlow()

    suspend fun startUpdate(): Boolean {
        return withContext(Dispatchers.IO) {
            var success = true
            try {
                _updateStage.emit(UpdateStage.Downloading)
                val downloader = GtfsDownloader(context)
                val downloadJob = async { downloader.downloadGtfsData() }
                downloader.downloadProgress.collect { progress ->
                    _updateProgress.emit(progress)
                }
                val downloadSuccess = downloadJob.await()
                if (!downloadSuccess) {
                    _updateStage.emit(UpdateStage.Failed)
                    success = false
                }

                if (success) {
                    _updateStage.emit(UpdateStage.Extracting)
                    val extractor = GtfsExtractor(context)
                    val extractionJob = async { extractor.extractData() }
                    extractor.extractionProgress.collect { progress ->
                        _updateProgress.emit(progress)
                    }
                    val extractionSuccess = extractionJob.await()
                    if (!extractionSuccess) {
                        _updateStage.emit(UpdateStage.Failed)
                        success = false
                    }
                }

                if (success) {
                    _updateStage.emit(UpdateStage.Importing)
                    val db = dbHelper.writableDatabase
                    val importer = DataImporter(context, db)
                    val importJob = async { importer.importData() }
                    importer.importProgress.collect { progress ->
                        _updateProgress.emit(progress)
                    }
                    val importSuccess = importJob.await()
                    if (!importSuccess) {
                        _updateStage.emit(UpdateStage.Failed)
                        success = false
                    }
                }

                if (success) {
                    _updateStage.emit(UpdateStage.Completed)
                }

            } catch (e: Exception) {
                _updateStage.emit(UpdateStage.Failed)
                success = false
            }
            return@withContext success
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
