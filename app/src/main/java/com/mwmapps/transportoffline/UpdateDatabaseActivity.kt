package com.mwmapps.transportoffline

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateDatabaseActivity : AppCompatActivity() {
    private lateinit var currentTaskDescription: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentage: TextView
    private lateinit var startUpdateButton: Button
    private lateinit var busScheduleSearchButton: Button
    private lateinit var databaseUpdater: DatabaseUpdater
    private lateinit var gtfsDownloader: GtfsDownloader
    private lateinit var gtfsExtractor: GtfsExtractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_database)

        currentTaskDescription = findViewById(R.id.current_task_description)
        progressBar = findViewById(R.id.progress_bar)
        progressPercentage = findViewById(R.id.progress_percentage)
        startUpdateButton = findViewById(R.id.start_update_button)
        busScheduleSearchButton = findViewById(R.id.bus_schedule_search_button)

        databaseUpdater = DatabaseUpdater(this, DatabaseHelper(this))
        gtfsDownloader = GtfsDownloader(this)
        gtfsExtractor = GtfsExtractor(this)

        startUpdateButton.setOnClickListener {
            if (startUpdateButton.text == "Start Update") {
                startUpdateProcess()
                startUpdateButton.text = "Please Wait"
                startUpdateButton.isEnabled = false
                progressBar.visibility = View.VISIBLE
                progressPercentage.visibility = View.VISIBLE
            }
        }
    }

    private fun startUpdateProcess() {
        lifecycleScope.launch {
            databaseUpdater.startUpdate()
        }

        lifecycleScope.launch {
            gtfsDownloader.downloadProgress
                .debounce(100) // Debounce updates by 100 milliseconds
                .collect { progress ->
                    withContext(Dispatchers.Main) {
                        updateProgressBar(progress)
                    }
                }
        }

        lifecycleScope.launch {
            gtfsExtractor.extractionProgress
                .debounce(100) // Debounce updates by 100 milliseconds
                .collect { progress ->
                    withContext(Dispatchers.Main) {
                        updateProgressBar(progress)
                    }
                }
        }

        lifecycleScope.launch {
            databaseUpdater.updateProgress
                .debounce(100) // Debounce updates by 100 milliseconds
                .collect { progress ->
                    withContext(Dispatchers.Main) {
                        updateProgressBar(progress)
                    }
                }
        }

        lifecycleScope.launch {
            databaseUpdater.updateStage.collect { stage ->
                withContext(Dispatchers.Main) {
                    updateDescription(stage)
                }
            }
        }
    }

    private fun updateProgressBar(progress: Int) {
        progressBar.progress = progress
        progressPercentage.text = "$progress%"
    }

    private fun updateDescription(stage: UpdateStage?) {
        currentTaskDescription.text = when (stage) {
            UpdateStage.Downloading -> "Downloading GTFS Data..."
            UpdateStage.Extracting -> "Extracting GTFS Data..."
            UpdateStage.Verifying -> "Verifying Files..."
            UpdateStage.Importing -> "Importing Data..."
            else -> ""
        }
    }
}
