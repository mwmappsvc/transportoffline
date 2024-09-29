package com.mwmapps.transportoffline

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.content.Context

class DataQuery(private val db: SQLiteDatabase, private val context: Context) {

    fun searchBusStops(query: String): List<BusStop> {
        Log.d("DataQuery", "Searching for bus stops with query: $query")
        LoggingActivity.logMessage(context, "Searching for bus stops with query: $query")
        val cursor = db.rawQuery("SELECT stop_id, stop_name, stop_desc FROM stops WHERE stop_name LIKE ? OR stop_code LIKE ? OR stop_desc LIKE ?", arrayOf("%$query%", "%$query%", "%$query%"))
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
        Log.d("DataQuery", "SQL Query: SELECT stop_times.stop_sequence, stop_times.arrival_time, routes.route_id, routes.route_short_name, routes.route_long_name FROM stop_times JOIN trips ON stop_times.trip_id = trips.trip_id JOIN routes ON trips.route_id = routes.route_id WHERE stop_times.stop_id = ? ORDER BY stop_times.arrival_time")
        LoggingActivity.logMessage(context, "SQL Query: SELECT stop_times.stop_sequence, stop_times.arrival_time, routes.route_id, routes.route_short_name, routes.route_long_name FROM stop_times JOIN trips ON stop_times.trip_id = trips.trip_id JOIN routes ON trips.route_id = routes.route_id WHERE stop_times.stop_id = ? ORDER BY stop_times.arrival_time")
        Log.d("DataQuery", "Query Parameter: stop_id = $stopId")
        LoggingActivity.logMessage(context, "Query Parameter: stop_id = $stopId")
        val cursor = db.rawQuery("""
            SELECT 
                stop_times.stop_sequence, 
                stop_times.arrival_time, 
                routes.route_id, 
                routes.route_short_name, 
                routes.route_long_name 
            FROM 
                stop_times 
            JOIN 
                trips ON stop_times.trip_id = trips.trip_id 
            JOIN 
                routes ON trips.route_id = routes.route_id 
            WHERE 
                stop_times.stop_id = ?
            ORDER BY 
                stop_times.arrival_time
        """, arrayOf(stopId))

        val busSchedules = mutableListOf<BusSchedule>()
        while (cursor.moveToNext()) {
            val stopSequence = cursor.getInt(cursor.getColumnIndexOrThrow("stop_sequence"))
            val arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow("arrival_time"))
            val routeId = cursor.getString(cursor.getColumnIndexOrThrow("route_id"))
            val routeShortName = cursor.getString(cursor.getColumnIndexOrThrow("route_short_name"))
            val routeLongName = cursor.getString(cursor.getColumnIndexOrThrow("route_long_name"))

            Log.d("DataQuery", "Row: stopSequence=$stopSequence, arrivalTime=$arrivalTime, routeId=$routeId, routeShortName=$routeShortName, routeLongName=$routeLongName")
            LoggingActivity.logMessage(context, "Row: stopSequence=$stopSequence, arrivalTime=$arrivalTime, routeId=$routeId, routeShortName=$routeShortName, routeLongName=$routeLongName")

            busSchedules.add(BusSchedule(stopSequence, arrivalTime, routeId, routeShortName, routeLongName))
        }
        cursor.close()
        Log.d("DataQuery", "Found ${busSchedules.size} bus schedules for stop_id: $stopId")
        LoggingActivity.logMessage(context, "Found ${busSchedules.size} bus schedules for stop_id: $stopId")
        return busSchedules
    }

    fun logStopTimesForStopId(stopId: String) {
        Log.d("DataQuery", "Logging stop_times for stop_id: $stopId")
        LoggingActivity.logMessage(context, "Logging stop_times for stop_id: $stopId")
        val cursor = db.rawQuery("SELECT * FROM stop_times WHERE stop_id = ?", arrayOf(stopId))
        while (cursor.moveToNext()) {
            val tripId = cursor.getString(cursor.getColumnIndexOrThrow("trip_id"))
            val arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow("arrival_time"))
            val departureTime = cursor.getString(cursor.getColumnIndexOrThrow("departure_time"))
            val stopSequence = cursor.getInt(cursor.getColumnIndexOrThrow("stop_sequence"))
            Log.d("DataQuery", "Row: tripId=$tripId, arrivalTime=$arrivalTime, departureTime=$departureTime, stopSequence=$stopSequence")
            LoggingActivity.logMessage(context, "Row: tripId=$tripId, arrivalTime=$arrivalTime, departureTime=$departureTime, stopSequence=$stopSequence")
        }
        cursor.close()
    }
}
