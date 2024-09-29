package com.mwmapps.transportoffline

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the user has already seen the Welcome screen
        val sharedPreferences: SharedPreferences = getSharedPreferences("TransportOfflinePrefs", MODE_PRIVATE)
        val hasSeenWelcome = sharedPreferences.getBoolean("hasSeenWelcome", false)

        if (hasSeenWelcome) {
            // If the user has already seen the Welcome screen, go directly to HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // If the user has not seen the Welcome screen, show it
            setContentView(R.layout.activity_welcome)

            val nextButton: Button = findViewById(R.id.next_button)
            nextButton.setOnClickListener {
                // Mark that the user has seen the Welcome screen
                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                editor.putBoolean("hasSeenWelcome", true)
                editor.apply()

                val intent = Intent(this, UpdateDatabaseActivity::class.java)
                startActivity(intent)
                finish() // Close the welcome activity
            }
        }
    }
}
