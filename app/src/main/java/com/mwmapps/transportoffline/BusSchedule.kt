// Begin BusSchedule.kt
// Data class representing a bus schedule.
// Externally Referenced Classes: BusScheduleAdapter, DataQuery
package com.mwmapps.transportoffline

data class BusSchedule(
    val arrivalTime: String,
    val routeId: String,
    val tripHeadsign: String
)
// End BusSchedule.kt