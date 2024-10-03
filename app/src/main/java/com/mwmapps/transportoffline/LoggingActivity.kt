// Begin LoggingActivity.kt
// Associated layout file: activity_logging.xml
// Displays log information.
// Externally Referenced Classes:
package com.mwmapps.transportoffline

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoggingActivity : AppCompatActivity() {
    companion object {
        private lateinit var logTextView: TextView

        fun logMessage(tag: String, message: String) {
            if (::logTextView.isInitialized) {
                logTextView.append("$tag: $message\n")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logging)

        logTextView = findViewById(R.id.log_text_view)
    }
}
// End LoggingActivity.kt