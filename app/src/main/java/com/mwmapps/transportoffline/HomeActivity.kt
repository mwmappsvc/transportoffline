// Begin HomeActivity.kt (rev 1.0)
// Associated layout file: activity_home.xml
// Remains the primary screen for bus route searching and schedule viewing
// Externally Referenced Classes: DatabaseHelper, HashUtils, DatabaseUtils, DataQuery, BusScheduleAdapter
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
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
    private lateinit var dataQuery: DataQuery
    private lateinit var searchCriteriaGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (!ImportStatusManager(this).isImportComplete()) {
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        dataQuery = DataQuery(DatabaseUtils.getDatabaseWithRetry(this), this)

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
            openBusStopActivity(busStop)
        }
        recyclerView.adapter = adapter

        searchCriteriaGroup = findViewById<RadioGroup>(R.id.search_criteria_group)
        searchCriteriaGroup.check(R.id.search_by_name) // Default selection

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

        // Add the test queries and logging
        testQueries()
    }

    private fun searchBusStops(query: String) {
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Searching for bus stops with query: $query")
        CoroutineScope(Dispatchers.IO).launch {
            val selectedCriteria = when (searchCriteriaGroup.checkedRadioButtonId) {
                R.id.search_by_id -> "stop_id"
                else -> "stop_name"
            }
            val busStops = dataQuery.searchBusStops(query, selectedCriteria)
            withContext(Dispatchers.Main) {
                adapter.updateBusStops(busStops, selectedCriteria)
            }
        }
    }

    private fun openBusStopActivity(busStop: BusStop) {
        val intent = Intent(this, BusStopActivity::class.java).apply {
            putExtra("stop_id", busStop.stopId)
            putExtra("stop_name", busStop.stopName)
        }
        startActivity(intent)
    }

    private fun testQueries() {
        lifecycleScope.launch(Dispatchers.IO) {
            dataQuery.performTestQueries()
        }
    }
}
// End HomeActivity.kt