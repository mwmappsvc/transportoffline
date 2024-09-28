package com.mwmapps.transportoffline

data class BusSchedule(
    val stopSequence: Int,
    val arrivalTime: String,
    val routeId: String,
    val routeShortName: String,
    val routeLongName: String
)
