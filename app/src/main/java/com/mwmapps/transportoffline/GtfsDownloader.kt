package com.mwmapps.transportoffline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.URL

class GtfsDownloader(private val context: Context) {

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    suspend fun downloadGtfsData(): Boolean {
        val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val url = sharedPreferences.getString("gtfs_url", "https://www.rtd-denver.com/files/gtfs/google_transit.zip") ?: return false

        return withContext(Dispatchers.IO) {
            try {
                val gtfsDataDir = File(context.filesDir, "gtfs_data")
                if (!gtfsDataDir.exists()) {
                    gtfsDataDir.mkdirs()
                }

                val outputFile = File(gtfsDataDir, "google_transit.zip")
                val urlConnection = URL(url).openConnection()
                urlConnection.connect()

                val fileLength = urlConnection.contentLength
                val inputStream = urlConnection.getInputStream()
                val outputStream = FileOutputStream(outputFile)

                val buffer = ByteArray(1024)
                var totalBytesRead = 0
                var len: Int

                while (inputStream.read(buffer).also { len = it } != -1) {
                    totalBytesRead += len
                    val progress = (totalBytesRead * 100 / fileLength).toInt()
                    _downloadProgress.emit(progress)
                    outputStream.write(buffer, 0, len)
                }

                outputStream.close()
                inputStream.close()

                LoggingControl.log(LoggingControl.LoggingGroup.DOWNLOAD_SIMPLE, "Download successful, file saved to: ${outputFile.absolutePath}")
                true
            } catch (e: FileNotFoundException) {
                LoggingControl.log(LoggingControl.LoggingGroup.DOWNLOAD_SIMPLE, "Download failed: File not found. ${e.message}")
                false
            } catch (e: Exception) {
                LoggingControl.log(LoggingControl.LoggingGroup.DOWNLOAD_SIMPLE, "Download failed. ${e.message}")
                false
            }
        }
    }
}
