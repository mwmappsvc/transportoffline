package com.mwmapps.transportoffline

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var searchBar: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BusScheduleAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var dataQuery: DataQuery

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        databaseHelper = DatabaseHelper(this)
        dataQuery = DataQuery(databaseHelper.readableDatabase)

        val settingsIcon: ImageView = findViewById(R.id.settings_icon)
        settingsIcon.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        searchBar = findViewById(R.id.search_bar)
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BusScheduleAdapter { busStop ->
            Log.d("HomeActivity", "Bus stop clicked: ${busStop.stopId}")
            displayBusSchedules(busStop)
        }
        recyclerView.adapter = adapter

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                Log.d("HomeActivity", "Search query: $query")
                searchBusStops(query)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        searchBar.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                val query = searchBar.text.toString()
                Log.d("HomeActivity", "Search query submitted: $query")
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
        CoroutineScope(Dispatchers.IO).launch {
            val busStops = dataQuery.searchBusStops(query)
            withContext(Dispatchers.Main) {
                adapter.updateBusStops(busStops)
            }
        }
    }

    private fun displayBusSchedules(busStop: BusStop) {
        Log.d("HomeActivity", "Displaying bus schedules for stop: ${busStop.stopId}")
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("HomeActivity", "Calling getBusSchedules for stopId: ${busStop.stopId}")
            val busSchedules = dataQuery.getBusSchedules(busStop.stopId)
            withContext(Dispatchers.Main) {
                Log.d("HomeActivity", "Received ${busSchedules.size} bus schedules for stopId: ${busStop.stopId}")
                busSchedules.forEach { schedule ->
                    Log.d("HomeActivity", "Bus schedule: ${schedule.stopSequence}, ${schedule.arrivalTime}, ${schedule.routeId}, ${schedule.routeShortName}, ${schedule.routeLongName}")
                }
                adapter.updateBusSchedules(busSchedules)
            }
        }
    }
}
