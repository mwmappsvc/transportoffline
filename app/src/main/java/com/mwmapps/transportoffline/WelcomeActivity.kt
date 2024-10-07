// Begin WelcomeActivity.kt (rev 1.0)
// Associated layout file: activity_welcome.xml
// Screen shown once at launch, initial setup
// Externally Referenced Classes: Potentially checks for database updates or guides users to UpdateDatabaseActivity
package com.mwmapps.transportoffline

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences
import java.io.File

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LoggingControl.log(LoggingControl.LoggingGroup.QUERY_SIMPLE, "WelcomeActivity onCreate called")

        if (DatabaseUtils.initializeDatabase(this)) {
            val sharedPreferences: SharedPreferences = getSharedPreferences("TransportOfflinePrefs", MODE_PRIVATE)
            val hasSeenWelcome = sharedPreferences.getBoolean("hasSeenWelcome", false)

            if (hasSeenWelcome) {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                setContentView(R.layout.activity_welcome)
                val nextButton: Button = findViewById(R.id.next_button)
                nextButton.setOnClickListener {
                    val editor: SharedPreferences.Editor = sharedPreferences.edit()
                    editor.putBoolean("hasSeenWelcome", true)
                    editor.apply()
                    val intent = Intent(this, UpdateDatabaseActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        } else {
            setContentView(R.layout.activity_welcome)
            val nextButton: Button = findViewById(R.id.next_button)
            nextButton.setOnClickListener {
                val intent = Intent(this, UpdateDatabaseActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}
// End WelcomeActivity.kt