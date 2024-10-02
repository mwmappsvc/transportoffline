package com.mwmapps.transportoffline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import android.util.Log

class GtfsExtractor(private val context: Context) {

    private val _extractionProgress = MutableSharedFlow<Int>()
    val extractionProgress = _extractionProgress.asSharedFlow()

    suspend fun extractGtfsData(zipFilePath: String, outputDir: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val zipFile = File(zipFilePath)
                val outputDirectory = File(outputDir)

                if (!outputDirectory.exists()) {
                    outputDirectory.mkdirs()
                }

                val fileInputStream = FileInputStream(zipFile)
                val zipInputStream = ZipInputStream(fileInputStream)
                var zipEntry: ZipEntry?
                var totalSize: Long = 0
                var extractedSize: Long = 0

                // Calculate total size of the zip file
                while (zipInputStream.nextEntry.also { zipEntry = it } != null) {
                    totalSize += zipEntry!!.size
                }

                // Reset the stream to the beginning
                zipInputStream.close()
                fileInputStream.close()

                val fileInputStream2 = FileInputStream(zipFile)
                val zipInputStream2 = ZipInputStream(fileInputStream2)

                // Extract files and update progress
                while (zipInputStream2.nextEntry.also { zipEntry = it } != null) {
                    val outputFile = File(outputDirectory, zipEntry!!.name)
                    val outputStream = outputFile.outputStream()

                    val buffer = ByteArray(1024)
                    var length: Int
                    while (zipInputStream2.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                        extractedSize += length.toLong()
                        val progress = (extractedSize * 100 / totalSize).toInt()
                        _extractionProgress.emit(progress)
                    }

                    outputStream.close()
                    zipInputStream2.closeEntry()
                }

                zipInputStream2.close()
                fileInputStream2.close()

                Log.d("GtfsExtractor", "Extraction completed successfully")
                true
            } catch (e: Exception) {
                Log.e("GtfsExtractor", "Extraction failed: ${e.message}")
                false
            }
        }
    }
}
