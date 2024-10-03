// Begin HomeActivity.kt
// Associated layout file: activity_home.xml
// Remains the primary screen for bus route searching and schedule viewing
// Externally Referenced Classes: DatabaseHelper, HashUtils, DatabaseUtils, DataQuery, BusScheduleAdapter
package com.mwmapps.transportoffline

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var searchBar: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BusScheduleAdapter
    private lateinit var dataQuery: DataQuery
    private lateinit var timeRangeSpinner: Spinner

    // Enum class for time ranges
    enum class TimeRange(val hours: Int, val displayName: String) {
        ONE_HOUR(1, "Next Hour"),
        TWO_HOURS(2, "Next 2 Hours"),
        FOUR_HOURS(4, "Next 4 Hours");

        companion object {
            fun fromDisplayName(displayName: String): TimeRange? = values().find { it.displayName == displayName }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Use the utility function to get the database with retry logic
        val dbHelper = DatabaseHelper(this)
        if (!dbHelper.isImportComplete()) {
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        val db = dbHelper.writableDatabase
        try {
            dataQuery = DataQuery(db, this)

            // Log data from stop_times and trips tables
            dataQuery.logStopTimes()
            dataQuery.logTrips()

            val settingsIcon: ImageView = findViewById(R.id.settings_icon)
            settingsIcon.setOnClickListener {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

            searchBar = findViewById(R.id.search_bar)
            recyclerView = findViewById(R.id.recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(this)
            adapter = BusScheduleAdapter(this) { busStop ->
                LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Bus stop clicked: ${busStop.stopId}")
                displayBusSchedules(busStop)
            }
            recyclerView.adapter = adapter

            timeRangeSpinner = findViewById(R.id.time_range_spinner)
            ArrayAdapter.createFromResource(
                this,
                R.array.time_ranges,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                timeRangeSpinner.adapter = adapter
            }

            searchBar.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val query = s.toString()
                    LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Search query: $query")
                    searchBusStops(query)
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            searchBar.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH || event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                    val query = searchBar.text.toString()
                    LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Search query submitted: $query")
                    searchBusStops(query)
                    return@OnEditorActionListener true
                }
                false
            })
        } finally {
            db.close() // Ensure the database is closed
        }
    }

    private fun searchBusStops(query: String) {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Searching for bus stops with query: $query")
        CoroutineScope(Dispatchers.IO).launch {
            val busStops = dataQuery.searchBusStops(query)
            withContext(Dispatchers.Main) {
                adapter.updateBusStops(busStops)
            }
        }
    }

    private fun displayBusSchedules(busStop: BusStop) {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Displaying bus schedules for stop: ${busStop.stopId}")
        lifecycleScope.launch(Dispatchers.IO) {
            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Calling getBusSchedules for stop_id: ${busStop.stopId}")

            // Log the contents of the stop_times table for the given stop_id
            dataQuery.logStopTimesForStopId(busStop.stopId)

            val busSchedules = dataQuery.getBusSchedules(busStop.stopId)
            val filteredSchedules = filterBusSchedules(busSchedules)
            withContext(Dispatchers.Main) {
                LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Received ${filteredSchedules.size} bus schedules for stop_id: ${busStop.stopId}")
                filteredSchedules.forEach { schedule ->
                    LoggingControl.log(LoggingControl.LoggingGroup.QUERY_VERBOSE, "Bus schedule: ${schedule.stopSequence}, ${schedule.arrivalTime}, ${schedule.routeId}, ${schedule.routeShortName}, ${schedule.routeLongName}")
                }
                adapter.updateBusSchedules(filteredSchedules)
            }
        }
    }

    private fun filterBusSchedules(busSchedules: List<BusSchedule>): List<BusSchedule> {
        // Temporarily disable time filtering
        val filteredSchedules = busSchedules.map { schedule ->
            schedule.copy(arrivalTime = convertTo12HourFormat(schedule.arrivalTime))
        }

        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Filtered bus schedules (time filter disabled): ${filteredSchedules.size}")
        return filteredSchedules
    }

    private fun convertTo12HourFormat(time: String): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val date: Date? = dateFormat.parse(time)
        val newFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return newFormat.format(date!!)
    }
}
// End HomeActivity.kt
