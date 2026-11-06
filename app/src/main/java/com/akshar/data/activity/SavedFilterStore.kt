package com.akshar.data.activity

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.activityFiltersDataStore: DataStore<Preferences> by preferencesDataStore(name = "activity_filters")

/** Named Activity filters, kept on the device only. */
interface SavedFilterStore {
    val savedFilters: Flow<List<SavedActivityFilter>>

    /** Saves [filter] as [name]; an existing filter with the same name (any case) is replaced. */
    suspend fun save(name: String, filter: ActivityFilter)

    suspend fun delete(name: String)
}

/**
 * [SavedFilterStore] backed by Preferences DataStore. The list is stored as one JSON string, so
 * it needs no Room table or schema migration. DataStore does its file IO on its own dispatcher.
 */
class DataStoreSavedFilterStore(private val dataStore: DataStore<Preferences>) : SavedFilterStore {

    constructor(context: Context) : this(context.applicationContext.activityFiltersDataStore)

    override val savedFilters: Flow<List<SavedActivityFilter>> =
        dataStore.data.map { ActivityFilterCodec.decodeSaved(it[KEY]) }

    override suspend fun save(name: String, filter: ActivityFilter) {
        val cleanName = name.trim().take(SavedActivityFilter.MAX_NAME_LENGTH)
        require(cleanName.isNotEmpty()) { "A saved filter needs a name" }
        dataStore.edit { prefs ->
            val current = ActivityFilterCodec.decodeSaved(prefs[KEY])
            val index = current.indexOfFirst { it.name.equals(cleanName, ignoreCase = true) }
            val updated = current.toMutableList()
            val entry = SavedActivityFilter(cleanName, filter)
            if (index >= 0) updated[index] = entry else updated += entry
            prefs[KEY] = ActivityFilterCodec.encodeSaved(updated)
        }
    }

    override suspend fun delete(name: String) {
        dataStore.edit { prefs ->
            val updated = ActivityFilterCodec.decodeSaved(prefs[KEY]).filterNot { it.name.equals(name, ignoreCase = true) }
            prefs[KEY] = ActivityFilterCodec.encodeSaved(updated)
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("saved_filters")
    }
}
