package com.mwmapps.transportoffline

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class GtfsDownloader(private val context: Context) {

    fun downloadGtfsData(url: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val downloadSuccess = performDownload(url)
                withContext(Dispatchers.Main) {
                    callback(downloadSuccess)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }

    internal fun performDownload(url: String): Boolean {
        return try {
            val gtfsDataDir = File(context.filesDir, "gtfs_data")
            if (!gtfsDataDir.exists()) {
                gtfsDataDir.mkdirs()
            }

            val outputFile = File(gtfsDataDir, "google_transit.zip")
            val urlConnection = URL(url).openConnection()
            urlConnection.connect()

            val inputStream = urlConnection.getInputStream()
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(1024)
            var len: Int
            while (inputStream.read(buffer).also { len = it } != -1) {
                outputStream.write(buffer, 0, len)
            }

            outputStream.close()
            inputStream.close()

            Log.d("GtfsDownloader", "Download successful, file saved to: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("GtfsDownloader", "Download failed", e)
            false
        }
    }
}
