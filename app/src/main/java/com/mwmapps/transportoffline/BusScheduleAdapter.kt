package com.mwmapps.transportoffline

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BusScheduleAdapter(
    private val busStops: MutableList<BusStop>,
    private val busSchedules: MutableList<BusSchedule>,
    private var isDisplayingBusStops: Boolean,
    private val onBusStopClick: (BusStop) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_BUS_STOP = 0
        private const val VIEW_TYPE_BUS_SCHEDULE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (isDisplayingBusStops) VIEW_TYPE_BUS_STOP else VIEW_TYPE_BUS_SCHEDULE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_BUS_STOP -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bus_stop, parent, false)
                BusStopViewHolder(view, onBusStopClick)
            }
            VIEW_TYPE_BUS_SCHEDULE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bus_schedule, parent, false)
                BusScheduleViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (isDisplayingBusStops) {
            (holder as BusStopViewHolder).bind(busStops[position])
        } else {
            (holder as BusScheduleViewHolder).bind(busSchedules[position])
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
            stopSequenceTextView.text = busSchedule.stopSequence.toString()
            arrivalTimeTextView.text = busSchedule.arrivalTime
            routeIdTextView.text = busSchedule.routeId
            routeNameTextView.text = busSchedule.routeLongName // Assuming routeLongName is the full route name
        }
    }

    fun updateBusStops(newBusStops: List<BusStop>) {
        busStops.clear()
        busStops.addAll(newBusStops)
        isDisplayingBusStops = true
        notifyDataSetChanged()
    }

    fun updateBusSchedules(newBusSchedules: List<BusSchedule>) {
        busSchedules.clear()
        busSchedules.addAll(newBusSchedules)
        isDisplayingBusStops = false
        notifyDataSetChanged()
    }
}
