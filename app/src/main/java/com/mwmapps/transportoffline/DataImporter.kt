// Imports GTFS data into the database.
// Externally Referenced Classes: DatabaseHelper, LoggingControl
package com.mwmapps.transportoffline

import kotlinx.coroutines.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class DataImporter(private val context: Context, private val dbHelper: DatabaseHelper) {

    companion object {
        private const val BATCH_SIZE = 1000 // Adjust this value as needed
    }

    fun importData(): Boolean {
        val db = dbHelper.writableDatabase
        var success = true
        db.beginTransaction()
        runBlocking {
            val jobs = listOf(
                async { success = success && importTableData("${context.filesDir.path}/agency.txt", "agency", listOf("agency_id", "agency_name", "agency_url", "agency_timezone", "agency_lang"), db) },
                async { success = success && importTableData("${context.filesDir.path}/calendar.txt", "calendar", listOf("service_id", "start_date", "end_date", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"), db) },
                async { success = success && importTableData("${context.filesDir.path}/calendar_dates.txt", "calendar_dates", listOf("service_id", "date", "exception_type"), db) },
                async { success = success && importTableData("${context.filesDir.path}/feed_info.txt", "feed_info", listOf("feed_publisher_name", "feed_publisher_url", "feed_lang", "feed_start_date", "feed_end_date", "feed_version"), db) },
                async { success = success && importTableData("${context.filesDir.path}/routes.txt", "routes", listOf("route_id", "agency_id", "route_short_name", "route_long_name", "route_desc", "route_type", "route_url", "route_color", "route_text_color"), db) },
                async { success = success && importTableData("${context.filesDir.path}/shapes.txt", "shapes", listOf("shape_id", "shape_pt_lat", "shape_pt_lon", "shape_pt_sequence", "shape_dist_traveled"), db) },
                async { success = success && importTableData("${context.filesDir.path}/stops.txt", "stops", listOf("stop_id", "stop_code", "stop_name", "stop_desc", "stop_lat", "stop_lon", "zone_id", "stop_url", "location_type", "parent_station", "stop_timezone", "wheelchair_boarding"), db) },
                async { success = success && importTableData("${context.filesDir.path}/stop_times.txt", "stop_times", listOf("trip_id", "arrival_time", "departure_time", "stop_id", "stop_sequence", "stop_headsign", "pickup_type", "drop_off_type", "shape_dist_traveled", "timepoint"), db) },
                async { success = success && importTableData("${context.filesDir.path}/trips.txt", "trips", listOf("trip_id", "route_id", "service_id", "trip_headsign", "direction_id", "block_id", "shape_id"), db) }
            )
            jobs.awaitAll()
        }
        if (success) {
            db.setTransactionSuccessful()
            dbHelper.setImportComplete(true)
            Log.d("DataImporter", "All tables imported successfully")
            LoggingActivity.logMessage("Import", "All tables imported successfully")
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "All tables imported successfully")
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_VERBOSE, "Detailed import log for all tables")

            // Log the specific stop number 16709
            logSpecificStop(16709, db)
        } else {
            dbHelper.setImportComplete(false)
        }
        db.endTransaction()
        db.close() // Ensure the database is closed

        return success
    }

    suspend fun importGtfsData(gtfsDir: String): Boolean {
        return withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            db.beginTransaction() // Ensure transaction begins here
            try {
                val jobs = listOf(
                    async { importTableData("$gtfsDir/agency.txt", "agency", listOf("agency_id", "agency_name", "agency_url", "agency_timezone", "agency_lang"), db) },
                    async { importTableData("$gtfsDir/routes.txt", "routes", listOf("route_id", "agency_id", "route_short_name", "route_long_name", "route_desc", "route_type", "route_url", "route_color", "route_text_color"), db) },
                    async { importTableData("$gtfsDir/trips.txt", "trips", listOf("trip_id", "route_id", "service_id", "trip_headsign", "direction_id", "block_id", "shape_id"), db) },
                    async { importTableData("$gtfsDir/stops.txt", "stops", listOf("stop_id", "stop_code", "stop_name", "stop_desc", "stop_lat", "stop_lon", "zone_id", "stop_url", "location_type", "parent_station", "stop_timezone", "wheelchair_boarding"), db) },
                    async { importTableData("$gtfsDir/calendar.txt", "calendar", listOf("service_id", "start_date", "end_date", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"), db) },
                    async { importTableData("$gtfsDir/calendar_dates.txt", "calendar_dates", listOf("service_id", "date", "exception_type"), db) },
                    async { importTableData("$gtfsDir/stop_times.txt", "stop_times", listOf("trip_id", "arrival_time", "departure_time", "stop_id", "stop_sequence", "stop_headsign", "pickup_type", "drop_off_type", "shape_dist_traveled", "timepoint"), db) }
                )
                jobs.awaitAll()
                db.setTransactionSuccessful()
                true
            } catch (e: Exception) {
                Log.e("DataImporter", "Error importing GTFS data", e)
                false
            } finally {
                db.endTransaction()
                db.close() // Ensure the database is closed
            }
        }
    }

    private fun importTableData(fileName: String, tableName: String, columns: List<String>, db: SQLiteDatabase): Boolean {
        return try {
            Log.d("DataImporter", "Starting import for table: $tableName")
            LoggingActivity.logMessage("Import", "Starting import for table: $tableName")
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "Starting import for table: $tableName")
            LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_VERBOSE, "Detailed import log for table: $tableName")

            val file = File(fileName)
            if (!file.exists()) {
                Log.e("DataImporter", "File not found: $fileName")
                LoggingControl.log(LoggingControl.LoggingGroup.IMPORT_SIMPLE, "File not found: $fileName")
                return false
            }

            val reader = BufferedReader(FileReader(file))
            reader.readLine() // Skip header line

            val sql = StringBuilder("INSERT OR REPLACE INTO $tableName (${columns.joinToString(",")}) VALUES ")
            val valuesList = mutableListOf<String>()
            var rowCount = 0
            var batchCount = 0
            var line: String?

            while (reader.readLine().also { line = it } != null) {
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
                        values[0],  // trip_id
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
                        values[0],  // trip_id
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
                    else -> values
                }
                valuesList.add("('${mappedValues.joinToString("','")}')")
                rowCount++
                batchCount++

                if (batchCount >= BATCH_SIZE) {
                    sql.append(valuesList.joinToString(","))
                    db.execSQL(sql.toString())
                    valuesList.clear()
                    sql.clear().append("INSERT OR REPLACE INTO $tableName (${columns.joinToString(",")}) VALUES ")
                    batchCount = 0
                }
            }

            if (valuesList.isNotEmpty()) {
                sql.append(valuesList.joinToString(","))
                db.execSQL(sql.toString())
            }

            reader.close()
            Log.d("DataImporter", "Imported $rowCount rows into $tableName")
            LoggingActivity.logMessage("Import", "Imported $rowCount rows into $tableName")
            true
        } catch (e: Exception) {
            Log.e("DataImporter", "Error importing data into $tableName", e)
            LoggingActivity.logMessage("Import", "Error importing data into $tableName: ${e.message}")
            false
        }
    }

    private fun logSpecificStop(stopNumber: Int, db: SQLiteDatabase) {
        val cursor = db.rawQuery("SELECT * FROM stops WHERE stop_id = ?", arrayOf(stopNumber.toString()))
        if (cursor.moveToFirst()) {
            do {
                val stopId = cursor.getString(cursor.getColumnIndexOrThrow("stop_id"))
                val stopName = cursor.getString(cursor.getColumnIndexOrThrow("stop_name"))
                Log.d("DataImporter", "Stop ID: $stopId, Stop Name: $stopName")
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    private fun logAllStopIds(db: SQLiteDatabase) {
        val cursor = db.rawQuery("SELECT stop_id FROM stops", null)
        val stopIds = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            do {
                stopIds.add(cursor.getString(cursor.getColumnIndexOrThrow("stop_id")))
            } while (cursor.moveToNext())
        }
        cursor.close()
        Log.d("DataImporter", "All stop IDs: ${stopIds.joinToString(", ")}")
    }
}
// End DataImporter.kt