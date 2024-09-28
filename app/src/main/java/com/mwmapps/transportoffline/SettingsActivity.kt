package com.mwmapps.transportoffline

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val updateDatabaseButton: Button = findViewById(R.id.update_database_button)
        updateDatabaseButton.setOnClickListener {
            val intent = Intent(this, UpdateDatabaseActivity::class.java)
            startActivity(intent)
        }

        val loggingButton: Button = findViewById(R.id.logging_button)
        loggingButton.setOnClickListener {
            val intent = Intent(this, LoggingActivity::class.java)
            startActivity(intent)
        }
    }
}
