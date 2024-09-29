package com.mwmapps.transportoffline

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope

class HomeActivity : AppCompatActivity() {

    private lateinit var searchBar: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BusScheduleAdapter
    private lateinit var databaseHelper: DatabaseHelper
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

        databaseHelper = DatabaseHelper(this)
        dataQuery = DataQuery(databaseHelper.readableDatabase, this)

        val settingsIcon: ImageView = findViewById(R.id.settings_icon)
        settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        searchBar = findViewById(R.id.search_bar)
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BusScheduleAdapter(this) { busStop ->
            Log.d("HomeActivity", "Bus stop clicked: ${busStop.stopId}")
            LoggingActivity.logMessage(this, "Bus stop clicked: ${busStop.stopId}")
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
                Log.d("HomeActivity", "Search query: $query")
                LoggingActivity.logMessage(this@HomeActivity, "Search query: $query")
                searchBusStops(query)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        searchBar.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                val query = searchBar.text.toString()
                Log.d("HomeActivity", "Search query submitted: $query")
                LoggingActivity.logMessage(this@HomeActivity, "Search query submitted: $query")
                searchBusStops(query)
                return@OnEditorActionListener true
            }
            false
        })

        // Show welcome popup if it's the first launch
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)
        if (isFirstLaunch) {
            showWelcomePopup()
            sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
        }
    }

    private fun showWelcomePopup() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Welcome to Transport Offline!")
        builder.setMessage("Because this is your first time, please open Settings and select 'Update Database'")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun searchBusStops(query: String) {
        Log.d("HomeActivity", "Searching for bus stops with query: $query")
        LoggingActivity.logMessage(this, "Searching for bus stops with query: $query")
        CoroutineScope(Dispatchers.IO).launch {
            val busStops = dataQuery.searchBusStops(query)
            withContext(Dispatchers.Main) {
                adapter.updateBusStops(busStops)
            }
        }
    }

    private fun displayBusSchedules(busStop: BusStop) {
        Log.d("HomeActivity", "Displaying bus schedules for stop: ${busStop.stopId}")
        LoggingActivity.logMessage(this, "Displaying bus schedules for stop: ${busStop.stopId}")
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d("HomeActivity", "Calling getBusSchedules for stop_id: ${busStop.stopId}")
            LoggingActivity.logMessage(this@HomeActivity, "Calling getBusSchedules for stop_id: ${busStop.stopId}")

            // Log the contents of the stop_times table for the given stop_id
            dataQuery.logStopTimesForStopId(busStop.stopId)

            val busSchedules = dataQuery.getBusSchedules(busStop.stopId)
            val filteredSchedules = filterBusSchedules(busSchedules)
            withContext(Dispatchers.Main) {
                Log.d("HomeActivity", "Received ${filteredSchedules.size} bus schedules for stop_id: ${busStop.stopId}")
                LoggingActivity.logMessage(this@HomeActivity, "Received ${filteredSchedules.size} bus schedules for stop_id: ${busStop.stopId}")
                filteredSchedules.forEach { schedule ->
                    Log.d("HomeActivity", "Bus schedule: ${schedule.stopSequence}, ${schedule.arrivalTime}, ${schedule.routeId}, ${schedule.routeShortName}, ${schedule.routeLongName}")
                    LoggingActivity.logMessage(this@HomeActivity, "Bus schedule: ${schedule.stopSequence}, ${schedule.arrivalTime}, ${schedule.routeId}, ${schedule.routeShortName}, ${schedule.routeLongName}")
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

        Log.d("HomeActivity", "Filtered bus schedules (time filter disabled): ${filteredSchedules.size}")
        LoggingActivity.logMessage(this, "Filtered bus schedules (time filter disabled): ${filteredSchedules.size}")
        return filteredSchedules
    }


    private fun convertTo12HourFormat(time: String): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val date = dateFormat.parse(time)
        val newFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return newFormat.format(date)
    }
}
