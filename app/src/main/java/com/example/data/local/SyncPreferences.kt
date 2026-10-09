package com.example.data.local

import android.content.Context

/** Guarda quais checklists locais já foram enviados ao Firebase (sem mexer no schema do Room). */
class SyncPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("pmpi_cloud_sync", Context.MODE_PRIVATE)

    @Synchronized
    fun isSynced(recordId: Long): Boolean = syncedIds().contains(recordId.toString())

    @Synchronized
    fun markSynced(recordId: Long) {
        prefs.edit().putStringSet(KEY, syncedIds() + recordId.toString()).apply()
    }

    private fun syncedIds(): Set<String> = prefs.getStringSet(KEY, emptySet()) ?: emptySet()

    private companion object {
        const val KEY = "synced_record_ids"
    }
}
