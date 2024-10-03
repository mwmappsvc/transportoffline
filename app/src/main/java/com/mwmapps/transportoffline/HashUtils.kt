// Begin HashUtils.kt
// Provides utility methods for calculating and storing hashes.
// Externally Referenced Classes:
package com.mwmapps.transportoffline

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

object HashUtils {
    fun calculateHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val inputStream = FileInputStream(file)
        val buffer = ByteArray(1024)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }

        inputStream.close()
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun storeHash(context: Context, hash: String) {
        val hashFile = File(context.filesDir, "gtfs_hash.txt")
        hashFile.writeText(hash)
    }

    fun getStoredHash(context: Context): String? {
        val hashFile = File(context.filesDir, "gtfs_hash.txt")
        return if (hashFile.exists()) {
            hashFile.readText()
        } else {
            null
        }
    }
}
// End HashUtils.kt