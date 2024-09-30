package com.mwmapps.transportoffline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

class UpdateDatabaseActivity : AppCompatActivity() {
    private lateinit var currentTaskDescription: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentage: TextView
    private lateinit var startUpdateButton: Button
    private lateinit var busScheduleSearchButton: Button
    private lateinit var databaseUpdater: DatabaseUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_database)

        currentTaskDescription = findViewById(R.id.current_task_description)
        progressBar = findViewById(R.id.progress_bar)
        progressPercentage = findViewById(R.id.progress_percentage)
        startUpdateButton = findViewById(R.id.start_update_button)
        busScheduleSearchButton = findViewById(R.id.bus_schedule_search_button)

        databaseUpdater = DatabaseUpdater(this, DatabaseHelper(this))

        startUpdateButton.setOnClickListener {
            if (startUpdateButton.text == "Start Update") {
                startUpdateProcess()
                startUpdateButton.text = "Please Wait"
                startUpdateButton.isEnabled = false
            }
        }
    }

    private fun startUpdateProcess() {
        lifecycleScope.launch {
            databaseUpdater.startUpdate()
        }

        lifecycleScope.launch {
            databaseUpdater.updateProgress.collect { progress ->
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

