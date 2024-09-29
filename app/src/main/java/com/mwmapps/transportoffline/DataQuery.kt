package com.mwmapps.transportoffline

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.content.Context

class DataQuery(private val db: SQLiteDatabase, private val context: Context) {

    fun searchBusStops(query: String): List<BusStop> {
        Log.d("DataQuery", "Searching for bus stops with query: $query")
        LoggingActivity.logMessage(context, "Searching for bus stops with query: $query")
        val cursor = db.rawQuery("SELECT stop_id, stop_name FROM stops WHERE stop_name LIKE ? OR stop_code LIKE ?", arrayOf("%$query%", "%$query%"))
        val busStops = mutableListOf<BusStop>()
        while (cursor.moveToNext()) {
            val stopId = cursor.getString(cursor.getColumnIndexOrThrow("stop_id"))
            val stopName = cursor.getString(cursor.getColumnIndexOrThrow("stop_name"))
            busStops.add(BusStop(stopId, stopName))
        }
        cursor.close()
        return busStops
    }

    fun getBusSchedules(stopId: String): List<BusSchedule> {
        Log.d("DataQuery", "Querying bus schedules for stop_id: $stopId")
        LoggingActivity.logMessage(context, "Querying bus schedules for stop_id: $stopId")
        val cursor = db.rawQuery("""
            SELECT 
                stop_sequence, 
                arrival_time 
            FROM 
                stop_times 
            WHERE 
                stop_id = ? 
            ORDER BY 
                arrival_time
        """, arrayOf(stopId))

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
        Log.d("DataQuery", "Logging stop_times for stop_id: $stopId")
        LoggingActivity.logMessage(context, "Logging stop_times for stop_id: $stopId")
        val cursor = db.rawQuery("SELECT * FROM stop_times WHERE stop_id = ?", arrayOf(stopId))
        var count = 0
        while (cursor.moveToNext() && count < 100) { // Limit to 100 entries
            val tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id"))
            val arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow("arrival_time"))
            val departureTime = cursor.getString(cursor.getColumnIndexOrThrow("departure_time"))
            val stopSequence = cursor.getInt(cursor.getColumnIndexOrThrow("stop_sequence"))
            Log.d("DataQuery", "Row: tripId=$tripId, arrivalTime=$arrivalTime, departureTime=$departureTime, stopSequence=$stopSequence")
            LoggingActivity.logMessage(context, "Row: tripId=$tripId, arrivalTime=$arrivalTime, departureTime=$departureTime, stopSequence=$stopSequence")
            count++
        }
        cursor.close()
    }

    fun logTripsForTripIds(tripIds: List<String>) {
        Log.d("DataQuery", "Logging trips for trip_ids: $tripIds")
        LoggingActivity.logMessage(context, "Logging trips for trip_ids: $tripIds")
        val cursor = db.rawQuery("SELECT * FROM trips WHERE trip_id IN (${tripIds.joinToString(",") { "?" }})", tripIds.toTypedArray())
        while (cursor.moveToNext()) {
            val tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id"))
            val routeId = cursor.getString(cursor.getColumnIndexOrThrow("route_id"))
            Log.d("DataQuery", "Row: tripId=$tripId, routeId=$routeId")
            LoggingActivity.logMessage(context, "Row: tripId=$tripId, routeId=$routeId")
        }
        cursor.close()
    }

    fun logStopTimes() {
        Log.d("DataQuery", "Logging all stop_times")
        LoggingActivity.logMessage(context, "Logging all stop_times")
        val cursor = db.rawQuery("SELECT * FROM stop_times", null)
        var count = 0
        while (cursor.moveToNext() && count < 100) { // Limit to 100 entries
            val tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id"))
            val stopId = cursor.getString(cursor.getColumnIndexOrThrow("stop_id"))
            Log.d("DataQuery", "Row: tripId=$tripId, stopId=$stopId")
            LoggingActivity.logMessage(context, "Row: tripId=$tripId, stopId=$stopId")
            count++
        }
        cursor.close()
    }

    fun logTrips() {
        Log.d("DataQuery", "Logging all trips")
        LoggingActivity.logMessage(context, "Logging all trips")
        val cursor = db.rawQuery("SELECT * FROM trips", null)
        var count = 0
        while (cursor.moveToNext() && count < 100) { // Limit to 100 entries
            val tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id"))
            val routeId = cursor.getString(cursor.getColumnIndexOrThrow("route_id"))
            Log.d("DataQuery", "Row: tripId=$tripId, routeId=$routeId")
            LoggingActivity.logMessage(context, "Row: tripId=$tripId, routeId=$routeId")
            count++
        }
        cursor.close()
    }

    fun logSpecificTrips(tripIds: List<String>) {
        Log.d("DataQuery", "Logging specific trips for trip_ids: $tripIds")
        LoggingActivity.logMessage(context, "Logging specific trips for trip_ids: $tripIds")
        val cursor = db.rawQuery("SELECT * FROM trips WHERE trip_id IN (${tripIds.joinToString(",") { "?" }})", tripIds.toTypedArray())
        while (cursor.moveToNext()) {
            val tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id"))
            val routeId = cursor.getString(cursor.getColumnIndexOrThrow("route_id"))
            Log.d("DataQuery", "Row: tripId=$tripId, routeId=$routeId")
            LoggingActivity.logMessage(context, "Row: tripId=$tripId, routeId=$routeId")
        }
        cursor.close()
    }
}
