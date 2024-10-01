package com.mwmapps.transportoffline

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
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
    private lateinit var databaseUpdater: DatabaseUpdater
    private lateinit var gtfsCompare: GtfsCompare

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_database)

        currentTaskDescription = findViewById(R.id.current_task_description)
        progressBar = findViewById(R.id.progress_bar)
        progressPercentage = findViewById(R.id.progress_percentage)
        startUpdateButton = findViewById(R.id.start_update_button)

        // Initialize DatabaseUpdater and GtfsCompare with context, DatabaseHelper, and lifecycleScope
        val dbHelper = DatabaseHelper(this)
        databaseUpdater = DatabaseUpdater(this, dbHelper, lifecycleScope)
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

        // Handle back press during update
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (startUpdateButton.text == "Please Wait") {
                    // Show a message to the user
                    currentTaskDescription.text = "Update in progress. Please wait..."
                } else {
                    isEnabled = false
                    onBackPressed()
                }
            }
        })
    }

    private fun startUpdateProcess() {
        lifecycleScope.launch {
            val isUpdateNeeded = withContext(Dispatchers.IO) { gtfsCompare.isUpdateNeeded() }
            if (isUpdateNeeded) {
                overwriteDatabase()
                observeProgress()
                val updateSuccess = withContext(Dispatchers.IO) {
                    databaseUpdater.startUpdate("https://www.rtd-denver.com/files/gtfs/google_transit.zip")
                }
                withContext(Dispatchers.Main) {
                    if (updateSuccess) {
                        notifyUserUpdateComplete()
                    } else {
                        notifyUserDownloadFailed()
                    }
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
                    UpdateStage.Comparing -> currentTaskDescription.text = "Comparing GTFS data..."
                    UpdateStage.Importing -> currentTaskDescription.text = "Importing GTFS data..."
                    UpdateStage.Completed -> currentTaskDescription.text = "Update complete."
                    UpdateStage.DownloadError -> currentTaskDescription.text = "Download failed."
                    UpdateStage.ExtractionError -> currentTaskDescription.text = "Extraction failed."
                    UpdateStage.ComparisonError -> currentTaskDescription.text = "Comparison failed."
                    UpdateStage.ImportError -> currentTaskDescription.text = "Import failed."
                    UpdateStage.Error -> currentTaskDescription.text = "Update failed."
                }
            }
        }

        lifecycleScope.launch {
            databaseUpdater.updateProgress.collect { progress ->
                withContext(Dispatchers.Main) {
                    updateProgressBar(progress)
                }
            }
        }
    }

    private fun updateProgressBar(progress: Int) {
        progressBar.progress = progress
        progressPercentage.text = "$progress%" // Update percentage text
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
