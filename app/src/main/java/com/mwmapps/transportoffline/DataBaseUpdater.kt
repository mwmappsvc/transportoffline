package com.mwmapps.transportoffline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class DatabaseUpdater(private val context: Context, private val dbHelper: DatabaseHelper) {

    private val _updateProgress = MutableStateFlow(0)
    val updateProgress: StateFlow<Int> = _updateProgress.asStateFlow()

    private val _updateStage = MutableStateFlow<UpdateStage?>(null)
    val updateStage: StateFlow<UpdateStage?> = _updateStage.asStateFlow()

    suspend fun startUpdate(url: String): Boolean = coroutineScope {
        try {
            _updateStage.emit(UpdateStage.Downloading)
            val downloader = GtfsDownloader()
            val downloadResult = async { downloader.downloadGtfsData(url) }
            downloader.downloadProgress.collect { progress ->
                _updateProgress.emit(progress)
            }
            if (!downloadResult.await()) {
                _updateStage.emit(UpdateStage.Failed)
                return@coroutineScope false
            }
            _updateProgress.emit(25)
            _updateStage.emit(UpdateStage.Downloading)

            _updateStage.emit(UpdateStage.Extracting)
            val extractor = GtfsExtractor(context)
            val extractionResult = async { extractor.extractData() }
            extractor.extractionProgress.collect { progress ->
                _updateProgress.emit(progress)
            }
            if (!extractionResult.await()) {
                _updateStage.emit(UpdateStage.Failed)
                return@coroutineScope false
            }
            _updateProgress.emit(50)
            _updateStage.emit(UpdateStage.Extracting)

            _updateStage.emit(UpdateStage.Importing)
            val db = dbHelper.writableDatabase
            val importer = DataImporter(context, db)
            val importResult = async { importer.importData() }
            importer.importProgress.collect { progress ->
                _updateProgress.emit(progress)
            }
            if (!importResult.await()) {
                _updateStage.emit(UpdateStage.Failed)
                return@coroutineScope false
            }
            _updateProgress.emit(75)
            _updateStage.emit(UpdateStage.Importing)

            _updateStage.emit(UpdateStage.Completed)
            _updateProgress.emit(100)
            return@coroutineScope true

        } catch (e: Exception) {
            _updateStage.emit(UpdateStage.Failed)
            return@coroutineScope false
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
