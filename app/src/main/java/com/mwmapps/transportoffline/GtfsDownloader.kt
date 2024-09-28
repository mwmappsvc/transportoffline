package com.mwmapps.transportoffline

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class GtfsDownloader(private val context: Context) {

    fun downloadGtfsData(url: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val input = URL(url).openStream()
                val output = FileOutputStream(File(context.filesDir, "google_transit.zip"))
                input.copyTo(output)
                input.close()
                output.close()
                Log.d("GtfsDownloader", "Download successful")
                LoggingActivity.logMessage(context, "Download successful")
                callback(true)
            } catch (e: Exception) {
                Log.e("GtfsDownloader", "Download failed", e)
                LoggingActivity.logMessage(context, "Download failed: ${e.message}")
                callback(false)
            }
        }
    }
}
