// Section 1
// Comments with Section Numbers are Added, Removed, and Modified by the Human developer ONLY
// IMPORTANT: Do not change the location of section remarks. Keep them exactly as they are.
package com.mwmapps.transportoffline

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

private const val BATCH_SIZE = 1000 // Adjust this value as needed
// Section 2
class DataImporter(private val context: Context, private val dbHelper: DatabaseHelper) {

    val progressChannel = Channel<Int>()
    private val _importProgress = MutableStateFlow(0)
    val importProgress: StateFlow<Int> = _importProgress.asStateFlow()

    suspend fun importData(): Boolean {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        var success = true

        // Add your table import logic here
        success = importTableData("agency.txt", "agency", listOf("agency_id", "agency_name", "agency_url", "agency_timezone"))

        if (success) {
            db.setTransactionSuccessful()
            dbHelper.setImportComplete(true)
            Log.d("DataImporter", "All tables imported successfully")
            LoggingActivity.logMessage(context, "All tables imported successfully")
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "All tables imported successfully")
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_VERBOSE, "Detailed import log for all tables")

            // Log the specific stop number 16709
            logSpecificStop(16709)
        } else {
            dbHelper.setImportComplete(false)
        }

        db.endTransaction()
        return success
    }
// Section 3
    private suspend fun importTableData(fileName: String, tableName: String, columns: List<String>): Boolean {
        return try {
            Log.d("DataImporter", "Starting import for table: $tableName")
            LoggingActivity.logMessage(context, "Starting import for table: $tableName")
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "Starting import for table: $tableName")
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_VERBOSE, "Detailed import log for table: $tableName")

            val file = File(context.filesDir, fileName)
            if (!file.exists()) {
                Log.e("DataImporter", "File not found: $fileName")
                LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "File not found: $fileName")
                return false
            }

            val reader = file.bufferedReader()
            val valuesList = mutableListOf<String>()
            var rowCount = 0
            var batchCount = 0
            val totalRows = reader.lineSequence().count()
            reader.close()

            val reader2 = file.bufferedReader()
            var line: String?

            while (reader2.readLine().also { line = it } != null) {
                val values = line!!.split(",").map { it.trim().replace("'", "''") }
                if (values.size < columns.size) {
                    Log.e("DataImporter", "Missing values for table: $tableName. Expected ${columns.size}, but got ${values.size}")
                    LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "Missing values for table: $tableName. Expected ${columns.size}, but got ${values.size}")
                    continue
                }
// Section 4
                // Map the values to the correct columns for each table
                val mappedValues = when (tableName) {
                    "stops" -> listOf(
                        values[10], // stop_id
                        values[2],  // stop_code
                        values[8],  // stop_name
                        values[7],  // stop_desc
                        values[0],  // stop_lat
                        values[3],  // stop_lon
                        values[11], // zone_id
                        values[5],  // stop_url
                        values[9],  // location_type
                        values[6],  // parent_station
                        values[4],  // stop_timezone
                        values[1]   // wheelchair_boarding
                    )
                    "stop_times" -> listOf(
                        values[0].trim(),  // trip_id
                        values[1],  // arrival_time
                        values[2],  // departure_time
                        values[3],  // stop_id
                        values[4],  // stop_sequence
                        values[5],  // stop_headsign
                        values[6],  // pickup_type
                        values[7],  // drop_off_type
                        values[8],  // shape_dist_traveled
                        if (values.size > 9) values[9] else null // timepoint
                    )
                    "trips" -> listOf(
                        values[0].trim(),  // trip_id
                        values[1],  // route_id
                        values[2],  // service_id
                        values[3],  // trip_headsign
                        values[4],  // direction_id
                        values[5],  // block_id
                        values[6]   // shape_id
                    )
                    "routes" -> listOf(
                        values[0],  // route_id
                        values[1],  // agency_id
                        values[2],  // route_short_name
                        values[3],  // route_long_name
                        values[4],  // route_desc
                        values[5],  // route_type
                        values[6],  // route_url
                        values[7],  // route_color
                        values[8]   // route_text_color
                    )
                    "calendar" -> listOf(
                        values[0],  // service_id
                        values[1],  // start_date
                        values[2],  // end_date
                        values[3],  // monday
                        values[4],  // tuesday
                        values[5],  // wednesday
                        values[6],  // thursday
                        values[7],  // friday
                        values[8],  // saturday
                        values[9]   // sunday
                    )
                    "calendar_dates" -> listOf(
                        values[0],  // service_id
                        values[1],  // date
                        values[2]   // exception_type
                    )
                    "shapes" -> listOf(
                        values[0],  // shape_id
                        values[1],  // shape_pt_lat
                        values[2],  // shape_pt_lon
                        values[3],  // shape_pt_sequence
                        values[4]   // shape_dist_traveled
                    )
                    "feed_info" -> listOf(
                        values[0],  // feed_publisher_name
                        values[1],  // feed_publisher_url
                        values[2],  // feed_lang
                        values[3],  // feed_start_date
                        values[4],  // feed_end_date
                        values[5]   // feed_version
                    )
                    else -> values
                }
                valuesList.add("(${mappedValues.joinToString(",") { "'$it'" }})")
                rowCount++
// Section 5
                if (rowCount % BATCH_SIZE == 0) {
                    db.execSQL("INSERT INTO $tableName (${columns.joinToString(",")}) VALUES ${valuesList.joinToString(",")}")
                    valuesList.clear()
                    batchCount++
                    Log.d("DataImporter", "Batch $batchCount inserted for table: $tableName")
                    LoggingActivity.logMessage(context, "Batch $batchCount inserted for table: $tableName")
                    LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_VERBOSE, "Batch $batchCount inserted for table: $tableName")
                    CoroutineScope(Dispatchers.Main).launch {
                        progressChannel.send((rowCount.toFloat() / totalRows * 100).toInt())
                        _importProgress.emit((rowCount.toFloat() / totalRows * 100).toInt())
                    }
                }
            }

            if (valuesList.isNotEmpty()) {
                db.execSQL("INSERT INTO $tableName (${columns.joinToString(",")}) VALUES ${valuesList.joinToString(",")}")
                Log.d("DataImporter", "Final batch inserted for table: $tableName")
                LoggingActivity.logMessage(context, "Final batch inserted for table: $tableName")

                CoroutineScope(Dispatchers.Main).launch {
                    progressChannel.send(100)
                    _importProgress.emit(100)
                }
            }

            reader2.close()
            true
        } catch (e: Exception) {
            Log.e("DataImporter", "Error importing data for table: $tableName", e)
            LoggingActivity.logMessage(context, "Error importing data for table: $tableName. ${e.message}")
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "Error importing data for table: $tableName. ${e.message}")
            false
        }
    }

    private fun logSpecificStop(stopId: Int) {
        // Your logic to log the specific stop number 16709
    }
}
// Section 6