// Begin DataQuery.kt (rev 1.0)
// Provides methods to query the database.
// Externally Referenced Classes: DatabaseHelper, LoggingControl, BusStop, BusSchedule
package com.mwmapps.transportoffline

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class DataQuery(private val db: SQLiteDatabase, private val context: Context) {

    fun searchBusStops(query: String, criteria: String): List<BusStop> {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Searching for bus stops with query: $query and criteria: $criteria")
        val cursor = db.rawQuery("SELECT stop_id, stop_name FROM stops WHERE $criteria LIKE ?", arrayOf("%$query%"))
        val busStops = mutableListOf<BusStop>()
        while (cursor.moveToNext()) {
            val stopId = cursor.getString(cursor.getColumnIndexOrThrow("stop_id"))
            val stopName = cursor.getString(cursor.getColumnIndexOrThrow("stop_name"))
            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Found bus stop: stopId=$stopId, stopName=$stopName")
            busStops.add(BusStop(stopId, stopName))
        }
        cursor.close()
        return busStops
    }

    fun getBusSchedules(stopId: String): List<BusSchedule> {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Fetching bus schedules for stopId: $stopId")
        val cursor = db.rawQuery("""
        SELECT 
            stop_times.arrival_time,
            trips.route_id,
            trips.trip_headsign
        FROM stop_times
        INNER JOIN trips ON stop_times.trip_id = trips.trip_id
        WHERE stop_times.stop_id = ?
        ORDER BY stop_times.arrival_time
    """, arrayOf(stopId))

        val busSchedules = mutableListOf<BusSchedule>()
        val currentTime = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        while (cursor.moveToNext()) {
            val arrivalTime = LocalTime.parse(cursor.getString(cursor.getColumnIndexOrThrow("arrival_time")), formatter)
            if (arrivalTime.isAfter(currentTime)) {
                val routeId = cursor.getString(cursor.getColumnIndexOrThrow("route_id"))
                val tripHeadsign = cursor.getString(cursor.getColumnIndexOrThrow("trip_headsign"))
                LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Found bus schedule: arrivalTime=$arrivalTime, routeId=$routeId, tripHeadsign=$tripHeadsign")
                busSchedules.add(BusSchedule(arrivalTime.toString(), routeId, tripHeadsign))
            }
        }
        cursor.close()
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Total bus schedules found: ${busSchedules.size}")
        return busSchedules
    }

    // Updated method to log the first row from each table
    fun performTestQueries() {
        val tables = listOf("stops", "stop_times", "trips", "routes", "shapes", "calendar", "calendar_dates")

        for (table in tables) {
            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Querying first row from table: $table")
            val cursor = db.rawQuery("SELECT * FROM $table LIMIT 1", null)
            if (cursor.moveToFirst()) {
                val columnNames = cursor.columnNames
                val columnValues = columnNames.map { cursor.getString(cursor.getColumnIndexOrThrow(it)) }

                // Log column names
                LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "$table - Columns: ${columnNames.joinToString(", ")}")
                // Log column values
                LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "$table - Values: ${columnValues.joinToString(", ")}")
            } else {
                LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "No data found in table: $table")
            }
            cursor.close()
        }
    }

    fun logStopTimesForStopId(stopId: String) {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_VERBOSE, "Logging stop_times for stop_id: $stopId")
        val cursor = db.rawQuery("SELECT * FROM stop_times WHERE stop_id = ?", arrayOf(stopId))
        var count = 0
        while (cursor.moveToNext() && count < 100) { // Limit to 100 entries
            val tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id"))
            val arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow("arrival_time"))
            val departureTime = cursor.getString(cursor.getColumnIndexOrThrow("departure_time"))
            val stopSequence = cursor.getInt(cursor.getColumnIndexOrThrow("stop_sequence"))
            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_VERBOSE, "Row: tripId=$tripId, arrivalTime=$arrivalTime, departureTime=$departureTime, stopSequence=$stopSequence")
            count++
        }
        cursor.close()
    }

    fun logTripsForTripIds(tripIds: List<String>) {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_VERBOSE, "Logging trips for trip_ids: $tripIds")
        val cursor = db.rawQuery("SELECT * FROM trips WHERE trip_id IN (${tripIds.joinToString(",") { "?" }})", tripIds.toTypedArray())
        while (cursor.moveToNext()) {
            val tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id"))
            val routeId = cursor.getString(cursor.getColumnIndexOrThrow("route_id"))
            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_VERBOSE, "Row: tripId=$tripId, routeId=$routeId")
        }
        cursor.close()
    }

    fun logStopTimes() {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_VERBOSE, "Logging all stop_times")
        val cursor = db.rawQuery("SELECT * FROM stop_times", null)
        var count = 0
        while (cursor.moveToNext() && count < 100) { // Limit to 100 entries
            val tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id"))
            val stopId = cursor.getString(cursor.getColumnIndexOrThrow("stop_id"))
            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_VERBOSE, "Row: tripId=$tripId, stopId=$stopId")
            count++
        }
        cursor.close()
    }

    fun logTrips() {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_VERBOSE, "Logging all trips")
        val cursor = db.rawQuery("SELECT * FROM trips", null)
        var count = 0
        while (cursor.moveToNext() && count < 100) { // Limit to 100 entries
            val tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id"))
            val routeId = cursor.getString(cursor.getColumnIndexOrThrow("route_id"))
            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_VERBOSE, "Row: tripId=$tripId, routeId=$routeId")
            count++
        }
        cursor.close()
    }

    fun logSpecificTrips(tripIds: List<String>) {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_VERBOSE, "Logging specific trips for trip_ids: $tripIds")
        val cursor = db.rawQuery("SELECT * FROM trips WHERE trip_id IN (${tripIds.joinToString(",") { "?" }})", tripIds.toTypedArray())
        while (cursor.moveToNext()) {
            val tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id"))
            val routeId = cursor.getString(cursor.getColumnIndexOrThrow("route_id"))
            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_VERBOSE, "Row: tripId=$tripId, routeId=$routeId")
        }
        cursor.close()
    }
}
// End DataQuery.kt