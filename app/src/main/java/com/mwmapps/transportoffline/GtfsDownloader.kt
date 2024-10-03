// Section 1
// Comments with Section Numbers are Added, Removed, and Modified by the Human developer ONLY
package com.mwmapps.transportoffline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import android.util.Log
// Section 2
class GtfsDownloader(private val context: Context) {

    private val _downloadProgress = MutableSharedFlow<Int>()
    val downloadProgress = _downloadProgress.asSharedFlow()

    suspend fun downloadGtfsData(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connect()

                val fileLength = connection.contentLength
                val inputStream = BufferedInputStream(connection.getInputStream())

                // Ensure the gtfs_data directory exists
                val gtfsDataDir = File(context.filesDir, "gtfs_data")
                if (!gtfsDataDir.exists()) {
                    gtfsDataDir.mkdirs()
                }

                val outputFile = File(gtfsDataDir, "google_transit.zip")
                val outputStream = FileOutputStream(outputFile)
                val data = ByteArray(1024)
                var total: Long = 0
// Section 3
                while (true) {
                    val count = inputStream.read(data)
                    if (count == -1) break

                    total += count.toLong()
                    val progress = (total * 100 / fileLength).toInt()
                    _downloadProgress.emit(progress)

                    outputStream.write(data, 0, count)
                }
// Section 4
                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Log.d("GtfsDownloader", "Download completed successfully")
                true
            } catch (e: Exception) {
                Log.e("GtfsDownloader", "Download failed: ${e.message}")
                false
            }
        }
    }
}
// Section 5