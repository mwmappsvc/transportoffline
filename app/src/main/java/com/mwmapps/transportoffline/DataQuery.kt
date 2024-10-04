// Begin DataQuery.kt
// Provides methods to query the database.
// Externally Referenced Classes: DatabaseHelper, LoggingControl, BusStop, BusSchedule
package com.mwmapps.transportoffline

import android.content.Context
import android.database.sqlite.SQLiteDatabase

class DataQuery(private val db: SQLiteDatabase, private val context: Context) {

    fun searchBusStops(query: String): List<BusStop> {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Searching for bus stops with query: $query")
        val cursor = db.rawQuery("SELECT stop_id, stop_name, stop_desc FROM stops WHERE stop_name LIKE ? OR stop_code LIKE ? OR stop_desc LIKE ?", arrayOf("%$query%", "%$query%", "%$query%"))
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
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Querying bus schedules for stop_id: $stopId")
        val cursor = db.rawQuery("SELECT stop_sequence, arrival_time FROM stop_times WHERE stop_id = ?", arrayOf(stopId))

        val busSchedules = mutableListOf<BusSchedule>()
        while (cursor.moveToNext()) {
            val stopSequence = cursor.getInt(cursor.getColumnIndexOrThrow("stop_sequence"))
            val arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow("arrival_time"))

            busSchedules.add(BusSchedule(stopSequence, arrivalTime, "", "", ""))
        }
        cursor.close()
        return busSchedules
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