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

class GtfsDownloader {

    private val _downloadProgress = MutableSharedFlow<Int>()
    val downloadProgress = _downloadProgress.asSharedFlow()

    suspend fun downloadGtfsData(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connect()

                val fileLength = connection.contentLength
                val inputStream = BufferedInputStream(connection.getInputStream())
                val gtfsDataDir = File(context.filesDir, "gtfs_data")
                if (!gtfsDataDir.exists()) {
                    gtfsDataDir.mkdirs()
                }
                val outputFile = File(gtfsDataDir, "google_transit.zip")
                val outputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(1024)
                var totalBytesRead = 0

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    totalBytesRead += bytesRead
                    val progress = (totalBytesRead * 100 / fileLength).toInt()
                    _downloadProgress.emit(progress) // Emit progress update

                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Log.d("GtfsDownloader", "Download completed successfully")
                true // Return true if download was successful
            } catch (e: Exception) {
                Log.e("GtfsDownloader", "Download failed: ${e.message}")
                false // Return false if download failed
            }
        }
    }
}
