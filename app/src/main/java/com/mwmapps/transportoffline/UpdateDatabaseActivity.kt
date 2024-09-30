package com.mwmapps.transportoffline

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
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
    private lateinit var gtfsCompare: GtfsCompare

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_database)

        currentTaskDescription = findViewById(R.id.current_task_description)
        progressBar = findViewById(R.id.progress_bar)
        progressPercentage = findViewById(R.id.progress_percentage)
        startUpdateButton = findViewById(R.id.start_update_button)

        databaseUpdater = DatabaseUpdater(this, DatabaseHelper(this))
        gtfsDownloader = GtfsDownloader(this)
        gtfsExtractor = GtfsExtractor(this)
        gtfsCompare = GtfsCompare(this)

        // Initially hide the progress bar, task description, and percentage
        progressBar.visibility = View.GONE
        currentTaskDescription.visibility = View.GONE
        progressPercentage.visibility = View.GONE

        startUpdateButton.setOnClickListener {
            if (startUpdateButton.text == "Start Update") {
                startUpdateProcess()
                startUpdateButton.text = "Please Wait"
                startUpdateButton.isEnabled = false
                progressBar.visibility = View.VISIBLE
                currentTaskDescription.visibility = View.VISIBLE
                progressPercentage.visibility = View.VISIBLE
            } else if (startUpdateButton.text == "Bus Schedules") {
                navigateToHomePage()
            }
        }
    }

    private fun startUpdateProcess() {
        lifecycleScope.launch {
            val isUpdateNeeded = gtfsCompare.isUpdateNeeded()
            if (isUpdateNeeded) {
                overwriteDatabase()
                observeProgress()
                val updateSuccess = databaseUpdater.startUpdate()
                if (updateSuccess) {
                    notifyUserUpdateComplete()
                } else {
                    notifyUserDownloadFailed()
                }
            } else {
                notifyUserNoUpdateNeeded()
            }
        }
    }

    private fun overwriteDatabase() {
        val dbHelper = DatabaseHelper(this)
        dbHelper.copyDatabaseFromAssets()
    }

    private fun notifyUserDownloadFailed() {
        currentTaskDescription.text = "Download failed. Please check the URL and try again."
        startUpdateButton.text = "Retry"
        startUpdateButton.isEnabled = true
        progressBar.visibility = View.GONE
        progressPercentage.visibility = View.GONE
    }

    private fun notifyUserNoUpdateNeeded() {
        currentTaskDescription.text = "No update needed."
        startUpdateButton.text = "Bus Schedules"
        startUpdateButton.isEnabled = true
        progressBar.visibility = View.GONE
        progressPercentage.visibility = View.GONE
    }

    private fun notifyUserUpdateComplete() {
        currentTaskDescription.text = "Update complete."
        startUpdateButton.text = "Bus Schedules"
        startUpdateButton.isEnabled = true
        progressBar.visibility = View.GONE
        progressPercentage.visibility = View.GONE
    }

    private fun navigateToHomePage() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }

    private fun observeProgress() {
        lifecycleScope.launch {
            databaseUpdater.updateStage.collect { stage ->
                when (stage) {
                    UpdateStage.Downloading -> currentTaskDescription.text = "Downloading GTFS data..."
                    UpdateStage.Extracting -> currentTaskDescription.text = "Extracting GTFS data..."
                    UpdateStage.Importing -> currentTaskDescription.text = "Importing GTFS data..."
                    UpdateStage.Completed -> currentTaskDescription.text = "Update complete."
                    UpdateStage.Failed -> currentTaskDescription.text = "Update failed."
                    else -> currentTaskDescription.text = ""
                }
            }
        }

        lifecycleScope.launch {
            databaseUpdater.updateProgress.collect { progress ->
                progressBar.progress = progress
                progressPercentage.text = "$progress%"
            }
        }
    }

    override fun onBackPressed() {
        // Handle back press during update
        if (startUpdateButton.text == "Please Wait") {
            // Show a message to the user
            currentTaskDescription.text = "Update in progress. Please wait..."
        } else {
            super.onBackPressed()
        }
    }
}
