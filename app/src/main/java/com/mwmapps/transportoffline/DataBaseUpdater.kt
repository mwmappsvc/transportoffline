package com.mwmapps.transportoffline

import android.content.Context
import kotlinx.coroutines.Dispatchers
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
            _updateStage.emit(UpdateStage.Downloading)
            val downloader = GtfsDownloader(context)
            val downloadSuccess = downloader.downloadGtfsData()
            downloader.downloadProgress.collect { progress ->
                _updateProgress.emit(progress)
            }
            if (downloadSuccess) {
                _updateStage.emit(UpdateStage.Extracting)
                val extractor = GtfsExtractor(context)
                extractor.extractionProgress.collect { progress ->
                    _updateProgress.emit(progress)
                }
                if (extractor.extractData()) {
                    _updateStage.emit(UpdateStage.Importing)
                    val db = dbHelper.writableDatabase
                    val importer = DataImporter(context, db)
                    importer.importProgress.collect { progress ->
                        _updateProgress.emit(progress)
                    }
                    if (importer.importData()) {
                        _updateStage.emit(UpdateStage.Completed)
                        return@withContext true
                    } else {
                        _updateStage.emit(UpdateStage.Failed)
                        return@withContext false
                    }
                } else {
                    _updateStage.emit(UpdateStage.Failed)
                    return@withContext false
                }
            } else {
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
