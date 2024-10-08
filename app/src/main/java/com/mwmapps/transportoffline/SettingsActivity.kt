// Begin SettingsActivity.kt (rev 1.0)
// Associated layout file: activity_settings.xml
// Manages app settings.
// Externally Referenced Classes: UpdateDatabaseActivity, LoggingActivity, ConfigureLoggingActivity
package com.mwmapps.transportoffline

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val updateDatabaseButton: Button = findViewById(R.id.update_database_button)
        updateDatabaseButton.setOnClickListener {
            val intent = Intent(this, UpdateDatabaseActivity::class.java)
            startActivity(intent)
        }

        val configureUrlButton: Button = findViewById(R.id.configure_url_button)
        configureUrlButton.setOnClickListener {
            showConfigureUrlDialog()
        }

        val loggingButton: Button = findViewById(R.id.logging_button)
        loggingButton.setOnClickListener {
            val intent = Intent(this, LoggingActivity::class.java)
            startActivity(intent)
        }

        val configureLoggingButton: Button = findViewById(R.id.button_configure_logging)
        configureLoggingButton.setOnClickListener {
            val intent = Intent(this, ConfigureLoggingActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showConfigureUrlDialog() {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentUrl = sharedPreferences.getString("gtfs_url", "https://www.rtd-denver.com/files/gtfs/google_transit.zip")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_configure_url, null)
        val urlEditText: EditText = dialogView.findViewById(R.id.url_edit_text)
        urlEditText.setText(currentUrl)

        AlertDialog.Builder(this)
            .setTitle("Configure URL")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val url = urlEditText.text.toString()
                saveUrlToPreferences(url)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun saveUrlToPreferences(url: String) {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("gtfs_url", url)
            apply()
        }
    }
}
// End SettingsActivity.kt