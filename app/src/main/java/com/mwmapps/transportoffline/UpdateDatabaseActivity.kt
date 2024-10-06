// Begin UpdateDatabaseActivity.kt (rev 1.0)
// Associated layout file: activity_update_database.xml
// Manages the database update process.
// Externally Referenced Classes: DatabaseHelper, DatabaseUpdater, GtfsCompare, HomeActivity, SettingsActivity
package com.mwmapps.transportoffline

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateDatabaseActivity : AppCompatActivity() {
    private lateinit var currentTaskDescription: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentage: TextView
    private lateinit var startUpdateButton: Button
    private lateinit var retryButton: Button
    private lateinit var returnToSettingsButton: Button
    private lateinit var forceUpdateButton: Button
    private lateinit var databaseUpdater: DatabaseUpdater
    private lateinit var gtfsCompare: GtfsCompare

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_database)

        currentTaskDescription = findViewById(R.id.current_task_description)
        progressBar = findViewById(R.id.progress_bar)
        progressPercentage = findViewById(R.id.progress_percentage)
        startUpdateButton = findViewById(R.id.start_update_button)
        retryButton = findViewById(R.id.retry_button)
        returnToSettingsButton = findViewById(R.id.return_to_settings_button)
        forceUpdateButton = findViewById(R.id.force_update_button)

        val dbHelper = DatabaseHelper(this)
        databaseUpdater = DatabaseUpdater(this, dbHelper, lifecycleScope)
        gtfsCompare = GtfsCompare(this)

        progressBar.visibility = View.GONE
        currentTaskDescription.visibility = View.GONE
        progressPercentage.visibility = View.GONE
        retryButton.visibility = View.GONE
        returnToSettingsButton.visibility = View.GONE
        forceUpdateButton.visibility = View.GONE

        startUpdateButton.setOnClickListener {
            if (startUpdateButton.text == "Start Update") {
                startUpdateProcess()
                startUpdateButton.text = "Please Wait"
                startUpdateButton.isEnabled = false
                progressBar.visibility = View.VISIBLE
                currentTaskDescription.visibility = View.VISIBLE
            } else if (startUpdateButton.text == "Bus Schedules") {
                navigateToHomePage()
            }
        }

        retryButton.setOnClickListener {
            startUpdateProcess()
        }

        returnToSettingsButton.setOnClickListener {
            navigateToSettingsPage()
        }

        forceUpdateButton.setOnClickListener {
            forceUpdateProcess()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (startUpdateButton.text == "Please Wait") {
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
            observeProgress()
            val updateSuccess = withContext(Dispatchers.IO) {
                databaseUpdater.startUpdate("https://www.rtd-denver.com/files/gtfs/google_transit.zip")
            }
            withContext(Dispatchers.Main) {
                Log.d("UpdateDatabaseActivity", "Update process completed with result: $updateSuccess")
                if (updateSuccess) {
                    if (databaseUpdater.updateStage.value == UpdateStage.NoUpdateNeeded) {
                        notifyUserNoUpdateNeeded()
                    } else {
                        notifyUserUpdateComplete()
                    }
                } else {
                    notifyUserDownloadFailed()
                }
            }
        }
    }

    private fun forceUpdateProcess() {
        lifecycleScope.launch {
            observeProgress()
            val updateSuccess = withContext(Dispatchers.IO) {
                databaseUpdater.forceUpdate("https://www.rtd-denver.com/files/gtfs/google_transit.zip")
            }
            withContext(Dispatchers.Main) {
                Log.d("UpdateDatabaseActivity", "Force update process completed with result: $updateSuccess")
                if (updateSuccess) {
                    notifyUserUpdateComplete()
                } else {
                    notifyUserDownloadFailed()
                }
            }
        }
    }

    private fun notifyUserDownloadFailed() {
        currentTaskDescription.text = "Update failed. Please check the logs for more details."
        startUpdateButton.text = "Retry"
        startUpdateButton.isEnabled = true
        progressBar.visibility = View.GONE
        retryButton.visibility = View.VISIBLE
        returnToSettingsButton.visibility = View.VISIBLE
        forceUpdateButton.visibility = View.GONE
    }

    private fun notifyUserNoUpdateNeeded() {
        Log.d("UpdateDatabaseActivity", "notifyUserNoUpdateNeeded called")
        currentTaskDescription.text = "No update needed."
        startUpdateButton.text = "Bus Schedules"
        startUpdateButton.isEnabled = true
        progressBar.visibility = View.GONE
        forceUpdateButton.visibility = View.VISIBLE
        Log.d("UpdateDatabaseActivity", "Force Update button visibility: ${forceUpdateButton.visibility}")
    }

    private fun notifyUserUpdateComplete() {
        currentTaskDescription.text = "Update completed successfully."
        startUpdateButton.text = "Bus Schedules"
        startUpdateButton.isEnabled = true
        progressBar.visibility = View.GONE
        forceUpdateButton.visibility = View.GONE
    }

    private fun observeProgress() {
        lifecycleScope.launch {
            databaseUpdater.updateProgress.collect { progress ->
                progressBar.progress = progress
                progressPercentage.text = "$progress%"
            }
        }

        lifecycleScope.launch {
            databaseUpdater.updateStage.collect { stage ->
                Log.d("UpdateDatabaseActivity", "Current update stage: $stage")
                when (stage) {
                    UpdateStage.Downloading -> currentTaskDescription.text = "Downloading GTFS data..."
                    UpdateStage.Extracting -> currentTaskDescription.text = "Extracting GTFS data..."
                    UpdateStage.Comparing -> currentTaskDescription.text = "Comparing GTFS data..."
                    UpdateStage.Importing -> currentTaskDescription.text = "Importing GTFS data..."
                    UpdateStage.DownloadError,
                    UpdateStage.ExtractionError,
                    UpdateStage.ComparisonError,
                    UpdateStage.ImportError,
                    UpdateStage.Error -> notifyUserDownloadFailed()
                    UpdateStage.Completed -> notifyUserUpdateComplete()
                    UpdateStage.NoUpdateNeeded -> notifyUserNoUpdateNeeded()
                    UpdateStage.Idle -> {}
                }
            }
        }
    }

    private fun navigateToHomePage() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToSettingsPage() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (startUpdateButton.text == "Please Wait") {
            currentTaskDescription.text = "Update in progress. Please wait..."
        } else {
            super.onBackPressed()
        }
    }
}

// End UpdateDatabaseActivity.kt