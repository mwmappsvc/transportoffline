package com.mwmapps.transportoffline

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class DataImporter(private val context: Context, private val db: SQLiteDatabase) {

    companion object {
        private const val BATCH_SIZE = 1000 // Adjust this value as needed
    }

    val progressChannel = Channel<Int>()

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
            Log.d("DataImporter", "All tables imported successfully")
            LoggingActivity.logMessage(context, "All tables imported successfully")

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
            Log.d("DataImporter", "Starting import for table: $tableName")
            LoggingActivity.logMessage(context, "Starting import for table: $tableName")

            val file = File(context.filesDir, fileName)
            if (!file.exists()) {
                Log.e("DataImporter", "File not found: $fileName")
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
                    Log.e("DataImporter", "Missing values for table: $tableName. Expected ${columns.size}, but got ${values.size}")
                    continue
                }

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

                if (rowCount % BATCH_SIZE == 0) {
                    db.execSQL("$sql ${valuesList.joinToString(",")}")
                    valuesList.clear()
                    batchCount++
                    Log.d("DataImporter", "Batch $batchCount inserted for table: $tableName")
                    LoggingActivity.logMessage(context, "Batch $batchCount inserted for table: $tableName")
                    CoroutineScope(Dispatchers.Main).launch {
                        progressChannel.send((rowCount.toFloat() / totalRows * 100).toInt())
                    }
                }
            }

            if (valuesList.isNotEmpty()) {
                db.execSQL("$sql ${valuesList.joinToString(",")}")
                Log.d("DataImporter", "Final batch inserted for table: $tableName")
                LoggingActivity.logMessage(context, "Final batch inserted for table: $tableName")
                CoroutineScope(Dispatchers.Main).launch {
                    progressChannel.send(100)
                }
            }

            reader2.close()
            true
        } catch (e: Exception) {
            Log.e("DataImporter", "Error importing data for table: $tableName", e)
            LoggingActivity.logMessage(context, "Error importing data for table: $tableName. ${e.message}")
            false
        }
    }

    private fun logSpecificStop(stopId: Int) {
        val cursor = db.rawQuery("SELECT * FROM stops WHERE stop_id = ?", arrayOf(stopId.toString()))
        if (cursor.moveToFirst()) {
            val stopName = cursor.getString(cursor.getColumnIndexOrThrow("stop_name"))
            Log.d("DataImporter", "Stop ID: $stopId, Stop Name: $stopName")
            LoggingActivity.logMessage(context, "Stop ID: $stopId, Stop Name: $stopName")
        }
        cursor.close()
    }

    private fun logAllStopIds() {
        val cursor = db.rawQuery("SELECT stop_id FROM stops", null)
        while (cursor.moveToNext()) {
            val stopId = cursor.getInt(cursor.getColumnIndexOrThrow("stop_id"))
            Log.d("DataImporter", "Stop ID: $stopId")
            LoggingActivity.logMessage(context, "Stop ID: $stopId")
        }
        cursor.close()
    }
}
