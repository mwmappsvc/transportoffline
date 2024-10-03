// Section 1
// Comments with Section Numbers are Added, Removed, and Modified by the Human developer ONLY
package com.mwmapps.transportoffline

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
// Section 2

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
    // Section 3
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
// Section 4