// Section 1
// Comments with Section Numbers are Added, Removed, and Modified by the Human developer ONLY
// IMPORTANT: Do not change the location of section remarks. Keep them exactly as they are.
package com.mwmapps.transportoffline

data class BusSchedule(
    val stopSequence: Int,
    val arrivalTime: String,
    val routeId: String,
    val routeShortName: String,
    val routeLongName: String
)
// Section 2