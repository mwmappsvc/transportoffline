package com.mwmapps.transportoffline

import android.content.Context
import java.io.File
import android.util.Log

class GtfsCompare(private val context: Context) {

    fun isNewData(): Boolean {
        val newHash = calculateHash(File(context.filesDir, "gtfs_data/google_transit.zip"))
        val oldHash = getStoredHash(context)
        Log.d("GtfsCompare", "New hash: $newHash")
        Log.d("GtfsCompare", "Old hash: $oldHash")
        return newHash != oldHash
    }

    fun storeHash(hash: String) {
        storeHash(context, hash)
        Log.d("GtfsCompare", "Stored hash: $hash")
    }
}
