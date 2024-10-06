// Begin BusScheduleAdapter.kt (rev 1.0)
// Adapter for displaying bus schedules and stops in a RecyclerView.
// Externally Referenced Classes: BusStop, BusSchedule, LoggingControl
package com.mwmapps.transportoffline

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class BusScheduleAdapter(private val context: Context, private val onBusStopClick: (BusStop) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val busStops = mutableListOf<BusStop>()
    private val busSchedules = mutableListOf<BusSchedule>()
    private var isDisplayingBusStops = true
    private var searchCriteria: String = "stop_name"

    override fun getItemViewType(position: Int): Int {
        return if (isDisplayingBusStops) VIEW_TYPE_BUS_STOP else VIEW_TYPE_BUS_SCHEDULE
    }

    fun updateBusStops(newBusStops: List<BusStop>, criteria: String) {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Updating bus stops in adapter")
        busStops.clear()
        busStops.addAll(newBusStops)
        isDisplayingBusStops = true
        searchCriteria = criteria
        notifyDataSetChanged()
    }

    fun updateBusSchedules(newBusSchedules: List<BusSchedule>) {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Updating bus schedules in adapter, size: ${newBusSchedules.size}")
        busSchedules.clear()
        busSchedules.addAll(newBusSchedules)
        isDisplayingBusStops = false
        notifyDataSetChanged()
    }

    fun getBusSchedules(): List<BusSchedule> {
        return busSchedules
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_BUS_STOP) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bus_stop, parent, false)
            BusStopViewHolder(view, onBusStopClick, searchCriteria)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bus_schedule, parent, false)
            BusScheduleViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_BUS_STOP) {
            val busStop = busStops[position]
            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Binding bus stop: stopId=${busStop.stopId}, stopName=${busStop.stopName}")
            (holder as BusStopViewHolder).bind(busStop, searchCriteria)
        } else {
            val busSchedule = busSchedules[position]
            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Binding bus schedule: arrivalTime=${busSchedule.arrivalTime}, routeId=${busSchedule.routeId}, tripHeadsign=${busSchedule.tripHeadsign}")
            (holder as BusScheduleViewHolder).bind(busSchedule)
        }
    }

    override fun getItemCount(): Int {
        return if (isDisplayingBusStops) busStops.size else busSchedules.size
    }

    class BusStopViewHolder(itemView: View, private val onBusStopClick: (BusStop) -> Unit, private val criteria: String) : RecyclerView.ViewHolder(itemView) {
        private val stopNameTextView: TextView = itemView.findViewById(R.id.stop_name)

        fun bind(busStop: BusStop, criteria: String) {
            stopNameTextView.text = if (criteria == "stop_id") busStop.stopId else busStop.stopName
            itemView.setOnClickListener {
                LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Bus stop clicked: ${busStop.stopId}")
                onBusStopClick(busStop)
            }
        }
    }

    class BusScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val arrivalTimeTextView: TextView = itemView.findViewById(R.id.arrival_time)
        private val routeIdTextView: TextView = itemView.findViewById(R.id.route_id)
        private val tripHeadsignTextView: TextView = itemView.findViewById(R.id.trip_headsign)

        fun bind(busSchedule: BusSchedule) {
            val inputDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val outputDateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            try {
                val arrivalTime = inputDateFormat.parse(busSchedule.arrivalTime)
                val formattedArrivalTime = outputDateFormat.format(arrivalTime)

                routeIdTextView.text = "Bus Number: ${busSchedule.routeId}"
                arrivalTimeTextView.text = "Arrival Time: $formattedArrivalTime"
                tripHeadsignTextView.text = "Destination: ${busSchedule.tripHeadsign}"
            } catch (e: ParseException) {
                LoggingControl.log(LoggingControl.LoggingGroup.ERROR, "Failed to parse arrival time: ${busSchedule.arrivalTime}")
                arrivalTimeTextView.text = "Arrival Time: Invalid"
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_BUS_STOP = 0
        private const val VIEW_TYPE_BUS_SCHEDULE = 1
    }
}
// End BusScheduleAdapter.kt