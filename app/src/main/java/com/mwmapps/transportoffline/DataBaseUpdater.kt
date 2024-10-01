package com.mwmapps.transportoffline

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DatabaseUpdater(private val context: Context, private val db: DatabaseHelper, private val scope: CoroutineScope) {

    private val gtfsDownloader = GtfsDownloader(context)
    private val gtfsExtractor = GtfsExtractor(context)
    private val gtfsCompare = GtfsCompare(context)
    private val dataImporter = DataImporter(context, db.writableDatabase)

    private val _updateProgress = MutableSharedFlow<Int>()
    val updateProgress = _updateProgress.asSharedFlow()

    private val _updateStage = MutableSharedFlow<UpdateStage>()
    val updateStage = _updateStage.asSharedFlow()

    suspend fun startUpdate(gtfsUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            var success: Boolean
            try {
                success = gtfsDownloader.downloadGtfsData(gtfsUrl)
                if (success) {
                    _updateStage.emit(UpdateStage.Downloading)
                    _updateProgress.emit(25)
                } else {
                    _updateStage.emit(UpdateStage.DownloadError)
                    return@withContext false
                }

                success = gtfsExtractor.extractData()
                if (success) {
                    _updateStage.emit(UpdateStage.Extracting)
                    _updateProgress.emit(50)
                } else {
                    _updateStage.emit(UpdateStage.ExtractionError)
                    return@withContext false
                }

                success = gtfsCompare.isUpdateNeeded()
                if (success) {
                    _updateStage.emit(UpdateStage.Comparing)
                    _updateProgress.emit(75)
                } else {
                    _updateStage.emit(UpdateStage.ComparisonError)
                    return@withContext false
                }

                success = dataImporter.importData()
                if (success) {
                    _updateStage.emit(UpdateStage.Importing)
                    _updateProgress.emit(100)
                    _updateStage.emit(UpdateStage.Completed)
                    return@withContext true
                } else {
                    _updateStage.emit(UpdateStage.ImportError)
                    return@withContext false
                }
            } catch (e: Exception) {
                _updateStage.emit(UpdateStage.Error)
                return@withContext false
            }
        }
    }
}
