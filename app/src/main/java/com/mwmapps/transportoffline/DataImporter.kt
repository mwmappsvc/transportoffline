package com.mwmapps.transportoffline

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class DataImporter(private val context: Context, private val db: SQLiteDatabase) {

    companion object {
        private const val BATCH_SIZE = 1000 // Adjust this value as needed
    }

    fun importData(): Boolean {
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

    private fun importTableData(fileName: String, tableName: String, columns: List<String>): Boolean {
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

            val sql = StringBuilder("INSERT OR REPLACE INTO $tableName (${columns.joinToString(",")}) VALUES ")
            val valuesList = mutableListOf<String>()
            var rowCount = 0
            var batchCount = 0
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val values = line!!.split(",").map { it.trim().replace("'", "''") }
                if (values.size != columns.size) {
                    Log.e("DataImporter", "Column count mismatch for table: $tableName. Expected ${columns.size}, but got ${values.size}")
                    continue
                }
                // Map the values to the correct columns
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
            LoggingActivity.logMessage(context, "Imported $rowCount rows into $tableName")
            true
        } catch (e: Exception) {
            Log.e("DataImporter", "Error importing data into $tableName", e)
            LoggingActivity.logMessage(context, "Error importing data into $tableName: ${e.message}")
            false
        }
    }

    private fun logSpecificStop(stopNumber: Int) {
        val cursor = db.rawQuery("SELECT * FROM stops WHERE stop_id = ?", arrayOf(stopNumber.toString()))
        if (cursor.moveToFirst()) {
            do {
                val stopId = cursor.getString(cursor.getColumnIndexOrThrow("stop_id"))
                val stopName = cursor.getString(cursor.getColumnIndexOrThrow("stop_name"))
                val stopLat = cursor.getDouble(cursor.getColumnIndexOrThrow("stop_lat"))
                val stopLon = cursor.getDouble(cursor.getColumnIndexOrThrow("stop_lon"))
                Log.d("DataImporter", "Specific Stop - Stop ID: $stopId, Stop Name: $stopName, Latitude: $stopLat, Longitude: $stopLon")
            } while (cursor.moveToNext())
        } else {
            Log.d("DataImporter", "No records found for stop number: $stopNumber")
        }
        cursor.close()
    }

    private fun logAllStopIds() {
        val cursor = db.rawQuery("SELECT stop_id FROM stops", null)
        if (cursor.moveToFirst()) {
            do {
                val stopId = cursor.getString(cursor.getColumnIndexOrThrow("stop_id"))
                Log.d("DataImporter", "Stop ID: $stopId")
            } while (cursor.moveToNext())
        } else {
            Log.d("DataImporter", "No records found in stops table")
        }
        cursor.close()
    }
}
