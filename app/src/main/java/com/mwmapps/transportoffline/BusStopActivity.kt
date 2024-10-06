// Begin BusStopActivity.kt (rev 1.0)
package com.mwmapps.transportoffline

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BusStopActivity : AppCompatActivity() {

    private lateinit var stopNameTextView: TextView
    private lateinit var stopIdTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BusScheduleAdapter
    private lateinit var dataQuery: DataQuery
    private lateinit var timeFrameSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_stop)

        val stopId = intent.getStringExtra("stop_id") ?: return
        val stopName = intent.getStringExtra("stop_name") ?: return

        stopNameTextView = findViewById(R.id.stop_name)
        stopIdTextView = findViewById(R.id.stop_id)
        recyclerView = findViewById(R.id.recycler_view)
        timeFrameSpinner = findViewById(R.id.time_frame_spinner)

        stopNameTextView.text = stopName
        stopIdTextView.text = stopId

        val searchIcon: ImageView = findViewById(R.id.search_icon)
        searchIcon.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        val dbHelper = DatabaseHelper(this)
        dataQuery = DataQuery(dbHelper.writableDatabase, this)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BusScheduleAdapter(this) { busStop ->
            // Future feature: Handle click on bus schedule item
        }
        recyclerView.adapter = adapter

        // Set up the time frame spinner
        val timeFrameOptions = resources.getStringArray(R.array.time_ranges)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeFrameOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeFrameSpinner.adapter = spinnerAdapter

        // Load bus schedules and apply the default filter (Next Hour)
        displayBusSchedules(stopId)
        applyTimeFrameFilter("Next Hour")

        timeFrameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedTimeFrame = timeFrameOptions[position]
                applyTimeFrameFilter(selectedTimeFrame)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun displayBusSchedules(stopId: String) {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Displaying bus schedules for stop: $stopId")
        lifecycleScope.launch(Dispatchers.IO) {
            val busSchedules = dataQuery.getBusSchedules(stopId)
            withContext(Dispatchers.Main) {
                LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Updating bus schedules in adapter, size: ${busSchedules.size}")
                adapter.updateBusSchedules(busSchedules)
            }
        }
    }

    private fun applyTimeFrameFilter(timeFrame: String) {
        val currentTime = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getDefault() // Ensure the date format uses the device's time zone

        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Current time: ${dateFormat.format(currentTime.time)}")

        val filteredBusSchedules = adapter.getBusSchedules().filter { busSchedule ->
            val arrivalTime = dateFormat.parse(busSchedule.arrivalTime)
            val arrivalCalendar = Calendar.getInstance().apply { time = arrivalTime }
            arrivalCalendar.timeZone = TimeZone.getDefault() // Ensure the arrival time uses the device's time zone

            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Arrival time: ${dateFormat.format(arrivalCalendar.time)}")

            val startTime = currentTime.clone() as Calendar
            val endTime = currentTime.clone() as Calendar

            when (timeFrame) {
                "Next Hour" -> {
                    startTime.add(Calendar.MINUTE, -15)
                    endTime.add(Calendar.HOUR, 1)
                    endTime.add(Calendar.MINUTE, 15)
                }
                "Next 2 Hours" -> {
                    startTime.add(Calendar.MINUTE, -15)
                    endTime.add(Calendar.HOUR, 2)
                    endTime.add(Calendar.MINUTE, 15)
                }
                "Next 4 Hours" -> {
                    startTime.add(Calendar.MINUTE, -15)
                    endTime.add(Calendar.HOUR, 4)
                    endTime.add(Calendar.MINUTE, 15)
                }
                else -> {
                    LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Showing all schedules")
                    return@filter true // Show all
                }
            }

            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Start time: ${dateFormat.format(startTime.time)}, End time: ${dateFormat.format(endTime.time)}")
            arrivalCalendar.after(startTime) && arrivalCalendar.before(endTime)
        }

        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Filtered bus schedules size: ${filteredBusSchedules.size}")
        adapter.updateBusSchedules(filteredBusSchedules)
    }
}
// End BusStopActivity.kt
