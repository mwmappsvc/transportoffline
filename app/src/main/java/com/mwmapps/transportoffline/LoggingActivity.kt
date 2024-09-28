package com.mwmapps.transportoffline

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import java.io.File

class LoggingActivity : AppCompatActivity() {
    private lateinit var logTextView: TextView
    private lateinit var clearLogButton: Button
    private val logFileName = "app_log.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logging)

        logTextView = findViewById(R.id.log_text_view)
        clearLogButton = findViewById(R.id.clear_log_button)

        // Load log messages from file
        loadLogMessages()

        clearLogButton.setOnClickListener {
            clearLog()
        }
    }

    private fun loadLogMessages() {
        val logFile = File(filesDir, logFileName)
        if (logFile.exists()) {
            val logMessages = logFile.readText()
            logTextView.text = logMessages
        }
    }

    private fun clearLog() {
        val logFile = File(filesDir, logFileName)
        if (logFile.exists()) {
            logFile.writeText("")
        }
        logTextView.text = ""
    }

    companion object {
        fun logMessage(context: Context, message: String) {
            val logFile = File(context.filesDir, "app_log.txt")
            logFile.appendText("${System.currentTimeMillis()}: $message\n")
        }
    }
}
