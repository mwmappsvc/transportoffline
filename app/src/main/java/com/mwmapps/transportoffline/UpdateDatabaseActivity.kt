package com.mwmapps.transportoffline

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UpdateDatabaseActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var downloadStatus: TextView
    private lateinit var extractStatus: TextView
    private lateinit var verifyStatus: TextView
    private lateinit var importStatus: TextView
    private lateinit var finalStatus: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var dataImporter: DataImporter
    private lateinit var gtfsDownloader: GtfsDownloader
    private lateinit var gtfsExtractor: GtfsExtractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_database)

        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        databaseHelper = DatabaseHelper(this)
        dataImporter = DataImporter(this, databaseHelper.writableDatabase)
        gtfsDownloader = GtfsDownloader(this)
        gtfsExtractor = GtfsExtractor(this)

        val startUpdateButton: Button = findViewById(R.id.start_update_button)
        val configureUrlButton: Button = findViewById(R.id.configure_url_button)
        downloadStatus = findViewById(R.id.download_status)
        extractStatus = findViewById(R.id.extract_status)
        verifyStatus = findViewById(R.id.verify_status)
        importStatus = findViewById(R.id.import_status)
        finalStatus = findViewById(R.id.final_status)

        startUpdateButton.setOnClickListener {
            Log.d("UpdateDatabaseActivity", "Start Update button clicked")
            LoggingActivity.logMessage(this, "Start Update button clicked")
            startUpdateProcess()
        }

        configureUrlButton.setOnClickListener {
            Log.d("UpdateDatabaseActivity", "Configure URL button clicked")
            LoggingActivity.logMessage(this, "Configure URL button clicked")
            showUrlConfigurationDialog()
        }
    }

    private fun startUpdateProcess() {
        // Reset final status
        finalStatus.text = "Update Result [Awaiting Result]"

        // Log existing tables
        databaseHelper.logExistingTables()

        // Download GTFS data
        val url = sharedPreferences.getString("gtfs_url", "https://www.rtd-denver.com/files/gtfs/google_transit.zip")
        if (url != null) {
            CoroutineScope(Dispatchers.IO).launch {
                LoggingActivity.logMessage(this@UpdateDatabaseActivity, "Starting download from URL: $url")
                gtfsDownloader.downloadGtfsData(url, ::onDownloadComplete)
            }
        }
    }

    private fun onDownloadComplete(success: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            if (success) {
                downloadStatus.text = "Download GTFS Data [Success]"
                gtfsExtractor.extractData(::onExtractComplete)
            } else {
                downloadStatus.text = "Download GTFS Data [Fail]"
                updateFinalStatus()
            }
        }
    }

    private fun onExtractComplete(success: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            if (success) {
                extractStatus.text = "Extract Data [Success]"
                verifyFiles()
            } else {
                extractStatus.text = "Extract Data [Fail]"
                updateFinalStatus()
            }
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
                verifyStatus.text = "Verify Files [Success]"
                importData()
            }
        } else {
            Log.e("UpdateDatabaseActivity", "Verification failed: No files found")
            LoggingActivity.logMessage(this@UpdateDatabaseActivity, "Verification failed: No files found")
            withContext(Dispatchers.Main) {
                verifyStatus.text = "Verify Files [Fail]"
                updateFinalStatus()
            }
        }
    }

    private suspend fun importData() {
        withContext(Dispatchers.IO) {
            val success = dataImporter.importData()
            Log.d("UpdateDatabaseActivity", "Import process completed")
            LoggingActivity.logMessage(this@UpdateDatabaseActivity, "Import process completed")
            withContext(Dispatchers.Main) {
                importStatus.text = if (success) "Import Data [Success]" else "Import Data [Fail]"
                updateFinalStatus()
            }
        }
    }

    private fun updateFinalStatus() {
        if (downloadStatus.text.contains("Success") &&
            extractStatus.text.contains("Success") &&
            verifyStatus.text.contains("Success") &&
            importStatus.text.contains("Success")) {
            finalStatus.text = "Database Updated Successfully, Return to Main Page"
        } else {
            finalStatus.text = "Failed to Update Database, Please Contact Support"
        }
    }

    private fun showUrlConfigurationDialog() {
        val builder = AlertDialog.Builder(this)
        val input = android.widget.EditText(this)
        input.setText(sharedPreferences.getString("gtfs_url", "https://www.rtd-denver.com/files/gtfs/google_transit.zip"))
        builder.setTitle("Configure URL")
        builder.setView(input)
        builder.setPositiveButton("OK") { dialog, _ ->
            sharedPreferences.edit().putString("gtfs_url", input.text.toString()).apply()
            LoggingActivity.logMessage(this, "URL configured to: ${input.text}")
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }
}
