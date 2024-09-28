package com.mwmapps.transportoffline

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class BusStop(val stopId: String, val stopName: String)
data class BusSchedule(val stopSequence: Int, val arrivalTime: String, val routeId: String, val routeShortName: String, val routeLongName: String)

class BusScheduleAdapter(private val onBusStopClick: (BusStop) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val busStops = mutableListOf<BusStop>()
    private val busSchedules = mutableListOf<BusSchedule>()
    private var isDisplayingBusStops = true

    override fun getItemViewType(position: Int): Int {
        return if (isDisplayingBusStops) VIEW_TYPE_BUS_STOP else VIEW_TYPE_BUS_SCHEDULE
    }

    fun updateBusStops(newBusStops: List<BusStop>) {
        Log.d("BusScheduleAdapter", "Updating bus stops in adapter")
        busStops.clear()
        busStops.addAll(newBusStops)
        isDisplayingBusStops = true
        notifyDataSetChanged()
    }

    fun updateBusSchedules(newBusSchedules: List<BusSchedule>) {
        Log.d("BusScheduleAdapter", "Updating bus schedules in adapter, size: ${newBusSchedules.size}")
        busSchedules.clear()
        busSchedules.addAll(newBusSchedules)
        isDisplayingBusStops = false
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_BUS_STOP) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bus_stop, parent, false)
            BusStopViewHolder(view, onBusStopClick)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bus_schedule, parent, false)
            BusScheduleViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_BUS_STOP) {
            val busStop = busStops[position]
            (holder as BusStopViewHolder).bind(busStop)
        } else {
            val busSchedule = busSchedules[position]
            (holder as BusScheduleViewHolder).bind(busSchedule)
        }
    }

    override fun getItemCount(): Int {
        return if (isDisplayingBusStops) busStops.size else busSchedules.size
    }

    class BusStopViewHolder(itemView: View, private val onBusStopClick: (BusStop) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val stopNameTextView: TextView = itemView.findViewById(R.id.stop_name)

        fun bind(busStop: BusStop) {
            stopNameTextView.text = busStop.stopName
            itemView.setOnClickListener {
                Log.d("BusScheduleAdapter", "Bus stop clicked: ${busStop.stopId}")
                onBusStopClick(busStop)
            }
        }
    }

    class BusScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val stopSequenceTextView: TextView = itemView.findViewById(R.id.stop_sequence)
        private val arrivalTimeTextView: TextView = itemView.findViewById(R.id.arrival_time)
        private val routeIdTextView: TextView = itemView.findViewById(R.id.route_id)
        private val routeNameTextView: TextView = itemView.findViewById(R.id.route_name)

        fun bind(busSchedule: BusSchedule) {
            stopSequenceTextView.text = "Stop Sequence: ${busSchedule.stopSequence}"
            arrivalTimeTextView.text = "Arrival Time: ${busSchedule.arrivalTime}"
            routeIdTextView.text = "Route ID: ${busSchedule.routeId}"
            routeNameTextView.text = "Route: ${busSchedule.routeShortName} - ${busSchedule.routeLongName}"
        }
    }

    companion object {
        private const val VIEW_TYPE_BUS_STOP = 0
        private const val VIEW_TYPE_BUS_SCHEDULE = 1
    }
}
