package com.mwmapps.transportoffline

import android.database.sqlite.SQLiteDatabase
import android.util.Log

class DataQuery(private val db: SQLiteDatabase) {

    fun searchBusStops(query: String): List<BusStop> {
        Log.d("DataQuery", "Searching for bus stops with query: $query")
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
        Log.d("DataQuery", "Querying bus schedules for stopId: $stopId")
        val cursor = db.rawQuery("SELECT stop_sequence, arrival_time FROM stop_times WHERE stop_id = ?", arrayOf(stopId))

        val busSchedules = mutableListOf<BusSchedule>()
        while (cursor.moveToNext()) {
            val stopSequence = cursor.getInt(cursor.getColumnIndexOrThrow("stop_sequence"))
            val arrivalTime = cursor.getString(cursor.getColumnIndexOrThrow("arrival_time"))

            Log.d("DataQuery", "Row: stopSequence=$stopSequence, arrivalTime=$arrivalTime")

            busSchedules.add(BusSchedule(stopSequence, arrivalTime, "", "", ""))
        }
        cursor.close()
        Log.d("DataQuery", "Found ${busSchedules.size} bus schedules for stopId: $stopId")
        return busSchedules
    }

}
