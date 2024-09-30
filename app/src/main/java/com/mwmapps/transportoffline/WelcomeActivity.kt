package com.mwmapps.transportoffline

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "WelcomeActivity onCreate called")

        // Copy the database from assets
        val dbHelper = DatabaseHelper(this)
        dbHelper.copyDatabaseFromAssets()

        // Log database path for debugging
        val db = dbHelper.readableDatabase
        Log.d("DATABASE_PATH", db.path)

        // Check if the user has already seen the Welcome screen
        val sharedPreferences: SharedPreferences = getSharedPreferences("TransportOfflinePrefs", MODE_PRIVATE)
        val hasSeenWelcome = sharedPreferences.getBoolean("hasSeenWelcome", false)
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "hasSeenWelcome: $hasSeenWelcome")

        if (hasSeenWelcome) {
            // If the user has already seen the Welcome screen, go directly to HomeActivity
            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "User has seen the Welcome screen, navigating to HomeActivity")
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // If the user has not seen the Welcome screen, show it
            LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "User has not seen the Welcome screen, showing Welcome screen")
            setContentView(R.layout.activity_welcome)

            val nextButton: Button = findViewById(R.id.next_button)
            nextButton.setOnClickListener {
                LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Next button clicked")

                // Mark that the user has seen the Welcome screen
                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                editor.putBoolean("hasSeenWelcome", true)
                editor.apply()
                LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "hasSeenWelcome set to true")

                val intent = Intent(this, UpdateDatabaseActivity::class.java)
                startActivity(intent)
                finish() // Close the welcome activity
                LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "Navigating to UpdateDatabaseActivity")
            }
        }
    }
}
