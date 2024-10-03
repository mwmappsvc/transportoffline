// Begin BusSchedule.kt
// Data class representing a bus schedule.
// Externally Referenced Classes: BusScheduleAdapter, DataQuery
package com.mwmapps.transportoffline

data class BusSchedule(
    val stopSequence: Int,
    val arrivalTime: String,
    val routeId: String,
    val routeShortName: String,
    val routeLongName: String
)
// End BusSchedule.kt