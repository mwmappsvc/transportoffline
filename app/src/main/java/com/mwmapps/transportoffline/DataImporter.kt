package com.mwmapps.transportoffline

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class DataImporter(private val context: Context, private val db: SQLiteDatabase) {

    companion object {
        private const val BATCH_SIZE = 1000 // Adjust this value as needed
    }

    private val _importProgress = MutableStateFlow(0)
    val importProgress: StateFlow<Int> = _importProgress.asStateFlow()

    private val _currentTable = MutableStateFlow("")
    val currentTable: StateFlow<String> = _currentTable.asStateFlow()

    suspend fun importData(): Boolean {
        var success = true
        db.beginTransaction()
        runBlocking {
            val jobs = listOf(
                async { success = success && importTableData("gtfs_data/agency.txt", "agency", listOf("agency_id", "agency_name", "agency_url", "agency_timezone", "agency_lang")) },
                async { success = success && importTableData("gtfs_data/calendar.txt", "calendar", listOf("service_id", "start_date", "end_date", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")) },
                async { success = success && importTableData("gtfs_data/calendar_dates.txt", "calendar_dates", listOf("service_id", "date", "exception_type")) },
                async { success = success && importTableData("gtfs_data/feed_info.txt", "feed_info", listOf("feed_publisher_name", "feed_publisher_url", "feed_lang", "feed_start_date", "feed_end_date", "feed_version")) },
                async { success = success && importTableData("gtfs_data/routes.txt", "routes", listOf("route_id", "agency_id", "route_short_name", "route_long_name", "route_desc", "route_type", "route_url", "route_color", "route_text_color")) },
                async { success = success && importTableData("gtfs_data/shapes.txt", "shapes", listOf("shape_id", "shape_pt_lat", "shape_pt_lon", "shape_pt_sequence", "shape_dist_traveled")) },
                async { success = success && importTableData("gtfs_data/stops.txt", "stops", listOf("stop_id", "stop_code", "stop_name", "stop_desc", "stop_lat", "stop_lon", "zone_id", "stop_url", "location_type", "parent_station", "stop_timezone", "wheelchair_boarding")) },
                async { success = success && importTableData("gtfs_data/stop_times.txt", "stop_times", listOf("trip_id", "arrival_time", "departure_time", "stop_id", "stop_sequence", "stop_headsign", "pickup_type", "drop_off_type", "shape_dist_traveled", "timepoint")) },
                async { success = success && importTableData("gtfs_data/trips.txt", "trips", listOf("trip_id", "route_id", "service_id", "trip_headsign", "direction_id", "block_id", "shape_id")) }
            )
            jobs.awaitAll()
        }
        if (success) {
            db.setTransactionSuccessful()
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "All tables imported successfully")
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_VERBOSE, "Detailed import log for all tables")

            // Log the specific stop number 16709
            logSpecificStop(16709)

            // Log all stop IDs
            logAllStopIds()
        }
        db.endTransaction()

        return success
    }

    private suspend fun importTableData(fileName: String, tableName: String, columns: List<String>): Boolean {
        return try {
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "Starting import for table: $tableName")
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_VERBOSE, "Detailed import log for table: $tableName")

            _currentTable.value = tableName

            val file = File(context.filesDir, fileName)
            if (!file.exists()) {
                LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "File not found: $fileName")
                return false
            }

            val reader = BufferedReader(FileReader(file))
            reader.readLine() // Skip header line

            val totalRows = reader.lineSequence().count()
            reader.close()

            val reader2 = BufferedReader(FileReader(file))
            reader2.readLine() // Skip header line

            val sql = StringBuilder("INSERT OR REPLACE INTO $tableName (${columns.joinToString(",")}) VALUES ")
            val valuesList = mutableListOf<String>()
            var rowCount = 0
            var batchCount = 0
            var line: String?

            while (reader2.readLine().also { line = it } != null) {
                val values = line!!.split(",").map { it.trim().replace("'", "''") }
                if (values.size < columns.size) {
                    LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "Missing values for table: $tableName. Expected ${columns.size}, but got ${values.size}")
                    continue
                }

                valuesList.add("(${values.joinToString(",") { "'$it'" }})")
                rowCount++

                if (rowCount % BATCH_SIZE == 0) {
                    db.execSQL("$sql ${valuesList.joinToString(",")}")
                    valuesList.clear()
                    batchCount++
                    LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_VERBOSE, "Batch $batchCount inserted for table: $tableName")
                    CoroutineScope(Dispatchers.Main).launch {
                        _importProgress.emit((rowCount.toFloat() / totalRows * 100).toInt())
                    }
                }
            }

            if (valuesList.isNotEmpty()) {
                db.execSQL("$sql ${valuesList.joinToString(",")}")
                LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_VERBOSE, "Final batch inserted for table: $tableName")
                CoroutineScope(Dispatchers.Main).launch {
                    _importProgress.emit(100)
                }
            }

            reader2.close()
            true
        } catch (e: Exception) {
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "Error importing data for table: $tableName. ${e.message}")
            false
        }
    }

    private fun logSpecificStop(stopId: Int) {
        val cursor = db.rawQuery("SELECT * FROM stops WHERE stop_id = ?", arrayOf(stopId.toString()))
        if (cursor.moveToFirst()) {
            val stopName = cursor.getString(cursor.getColumnIndexOrThrow("stop_name"))
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "Stop ID: $stopId, Stop Name: $stopName")
        }
        cursor.close()
    }

    private fun logAllStopIds() {
        val cursor = db.rawQuery("SELECT stop_id FROM stops", null)
        while (cursor.moveToNext()) {
            val stopId = cursor.getInt(cursor.getColumnIndexOrThrow("stop_id"))
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_VERBOSE, "Stop ID: $stopId")
        }
        cursor.close()
    }
}
