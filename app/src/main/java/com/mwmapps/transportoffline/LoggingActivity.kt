// Section 1
// Comments with Section Numbers are Added, Removed, and Modified by the Human developer ONLY
package com.mwmapps.transportoffline

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
// Section 2
class LoggingActivity : AppCompatActivity() {
    companion object {
        private lateinit var logTextView: TextView

        fun logMessage(tag: String, message: String) {
            if (::logTextView.isInitialized) {
                logTextView.append("$tag: $message\n")
            }
        }
    }
// Section 3
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logging)

        logTextView = findViewById(R.id.log_text_view)
    }
}
// Section 4