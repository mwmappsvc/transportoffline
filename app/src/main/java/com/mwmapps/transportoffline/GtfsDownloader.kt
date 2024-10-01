package com.mwmapps.transportoffline

import android.content.Context
import androidx.health.services.client.flush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.URL
import kotlin.concurrent.read

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
                val outputStream = FileOutputStream(Utils.getGtfsZipFile(Utils.getAppContext()))
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