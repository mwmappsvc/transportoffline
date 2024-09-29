package com.mwmapps.transportoffline

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class UpdateDatabaseActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var currentTaskDescription: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentage: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var dataImporter: DataImporter
    private lateinit var gtfsDownloader: GtfsDownloader
    private lateinit var gtfsExtractor: GtfsExtractor
    private lateinit var busScheduleSearchButton: Button
    private lateinit var startUpdateButton: Button
    private var updateJob: Job? = null

    private var overallProgress = 0
    private val progressMutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_database)

        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        databaseHelper = DatabaseHelper(this)
        dataImporter = DataImporter(this, databaseHelper.writableDatabase)
        gtfsDownloader = GtfsDownloader(this)
        gtfsExtractor = GtfsExtractor(this)

        startUpdateButton = findViewById(R.id.start_update_button)
        currentTaskDescription = findViewById(R.id.current_task_description)
        progressBar = findViewById(R.id.progress_bar)
        progressPercentage = findViewById(R.id.progress_percentage)
        busScheduleSearchButton = findViewById(R.id.bus_schedule_search_button)

        startUpdateButton.setOnClickListener {
            if (startUpdateButton.text == "Start Update") {
                Log.d("UpdateDatabaseActivity", "Start Update button clicked")
                LoggingActivity.logMessage(this, "Start Update button clicked")
                startUpdateProcess()
                startUpdateButton.text = "Please Wait"
                startUpdateButton.isEnabled = false
            }
        }
    }

    private fun startUpdateProcess() {
        // Reset final status
        currentTaskDescription.text = "Starting update process..."
        progressBar.visibility = View.VISIBLE
        progressPercentage.visibility = View.VISIBLE
        updateProgress(0)

        // Log existing tables
        databaseHelper.logExistingTables()

        // Download GTFS data
        val url = sharedPreferences.getString("gtfs_url", "https://www.rtd-denver.com/files/gtfs/google_transit.zip")
        if (url != null) {
            updateJob = CoroutineScope(Dispatchers.IO).launch {
                LoggingActivity.logMessage(this@UpdateDatabaseActivity, "Starting download from URL: $url")
                withContext(Dispatchers.Main) {
                    currentTaskDescription.text = "Downloading GTFS Data..."
                    launch { simulateProgress(0, 25, 10000) } // Simulate download progress (0-25%, 10 seconds)
                }
                gtfsDownloader.downloadGtfsData(url) { success ->
                    CoroutineScope(Dispatchers.Main).launch {
                        currentTaskDescription.text = if (success) "Download GTFS Data [Success]" else "Download GTFS Data [Fail]"
                        if (success) {
                            launch { simulateProgress(25, 40, 5000) } // Simulate extraction progress (25-40%, 5 seconds)
                            gtfsExtractor.extractData { extractSuccess ->
                                onExtractComplete(extractSuccess)
                            }
                        } else {
                            showFailureDialog()
                        }
                    }
                }
            }
        }
    }

    private fun onExtractComplete(success: Boolean) {
        if (success) {
            currentTaskDescription.text = "Extract Data [Success]"
            CoroutineScope(Dispatchers.Main).launch {
                simulateProgress(40, 55, 5000) // Simulate verification progress (40-55%, 5 seconds)
            }
            CoroutineScope(Dispatchers.IO).launch {
                verifyFiles()
            }
        } else {
            currentTaskDescription.text = "Extract Data [Fail]"
            showFailureDialog()
        }
    }

    private suspend fun verifyFiles() {
        // Implement file verification logic here
        LoggingActivity.logMessage(this@UpdateDatabaseActivity, "Starting file verification")
        // Example verification logic
        val files = File(filesDir, "gtfs_data").listFiles()
        if (files != null && files.isNotEmpty()) {
            files.forEach { file: File ->
                LoggingActivity.logMessage(this@UpdateDatabaseActivity, "Verified file: ${file.name}")
            }
            Log.d("UpdateDatabaseActivity", "Verification successful")
            LoggingActivity.logMessage(this@UpdateDatabaseActivity, "Verification successful")
            withContext(Dispatchers.Main) {
                currentTaskDescription.text = "Verify Files [Success]"
                launch { simulateProgress(55, 70, 5000) } // Simulate import progress (55-70%, 5 seconds)
                currentTaskDescription.text = "Importing Data..."
                CoroutineScope(Dispatchers.IO).launch {
                    importData()
                }
            }
        } else {
            Log.e("UpdateDatabaseActivity", "Verification failed: No files found")
            LoggingActivity.logMessage(this@UpdateDatabaseActivity, "Verification failed: No files found")
            withContext(Dispatchers.Main) {
                currentTaskDescription.text = "Verify Files [Fail]"
                showFailureDialog()
            }
        }
    }

    private fun importData() {
        CoroutineScope(Dispatchers.IO).launch {
            val success = dataImporter.importData { newProgress ->
                CoroutineScope(Dispatchers.Main).launch {
                    progressMutex.withLock {
                        overallProgress = newProgress
                        updateProgress(overallProgress)
                    }
                }
            }
            Log.d("UpdateDatabaseActivity", "Import process completed")
            LoggingActivity.logMessage(this@UpdateDatabaseActivity, "Import process completed")
            withContext(Dispatchers.Main) {
                currentTaskDescription.text = if (success) "Import Data [Success]" else "Import Data [Fail]"
                updateProgress(100) // Update progress
                updateFinalStatus()
            }
        }
    }

    private suspend fun simulateProgress(start: Int, end: Int, duration: Long) {
        val totalSteps = end - start
        val delayPerStep = duration / (totalSteps * 2) // Smaller delay
        withContext(Dispatchers.Main) {
            for (i in 1..totalSteps * 2) {
                progressMutex.withLock {
                    overallProgress = start + i / 2 // Smaller increment
                    updateProgress(overallProgress)
                }
                delay(delayPerStep)
            }
        }
    }

    private fun updateProgress(progress: Int) {
        progressBar.progress = progress
        progressPercentage.text = "$progress%"
    }

    private fun updateFinalStatus() {
        if (currentTaskDescription.text.contains("Success")) {
            currentTaskDescription.text = "Database Updated Successfully, Return to Main Page"
            startUpdateButton.text = "Bus Scheduler"
            startUpdateButton.isEnabled = true
            startUpdateButton.setOnClickListener {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish() // Close the update database activity
            }
            busScheduleSearchButton.isEnabled = true
            busScheduleSearchButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.purple_500))
            progressBar.visibility = View.GONE
            progressPercentage.visibility = View.GONE
        } else {
            currentTaskDescription.text = "Failed to Update Database, Please Contact Support"
        }
    }

    private fun showFailureDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Update Failed")
        builder.setMessage("The update process failed. Please choose an option below:")
        builder.setPositiveButton("Home Page") { dialog, _ ->
            // Navigate to Home Page
            dialog.dismiss()
            finish() // Close the current activity
        }
        builder.setNegativeButton("Settings Page") { dialog, _ ->
            // Navigate to Settings Page
            dialog.dismiss()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish() // Close the current activity
        }
        builder.show()
    }
}
