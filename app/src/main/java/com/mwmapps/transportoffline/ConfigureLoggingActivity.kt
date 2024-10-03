// Section 1
// Comments with Section Numbers are Added, Removed, and Modified by the Human developer ONLY
// IMPORTANT: Do not change the location of section remarks. Keep them exactly as they are.
package com.mwmapps.transportoffline

import android.os.Bundle
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
// Section 2
class ConfigureLoggingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure_logging)

        setupToggleButton(R.id.toggle_import_simple, LoggingControl.LoggingGroup.IMPORT_SIMPLE)
        setupToggleButton(R.id.toggle_import_verbose, LoggingControl.LoggingGroup.IMPORT_VERBOSE)
        setupToggleButton(R.id.toggle_download_simple, LoggingControl.LoggingGroup.DOWNLOAD_SIMPLE)
        setupToggleButton(R.id.toggle_extractor_simple, LoggingControl.LoggingGroup.EXTRACTOR_SIMPLE)
        setupToggleButton(R.id.toggle_compare_simple, LoggingControl.LoggingGroup.COMPARE_SIMPLE)
        setupToggleButton(R.id.toggle_query_simple, LoggingControl.LoggingGroup.QUERY_SIMPLE)
        setupToggleButton(R.id.toggle_query_verbose, LoggingControl.LoggingGroup.QUERY_VERBOSE)
    }

    private fun setupToggleButton(buttonId: Int, loggingGroup: LoggingControl.LoggingGroup) {
        val toggleButton: ToggleButton = findViewById(buttonId)
        toggleButton.isChecked = LoggingControl.isLoggingEnabled(loggingGroup)
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            LoggingControl.setLoggingState(loggingGroup, isChecked)
        }
    }
}
// Section 3