package com.SlightlyEpic.Radio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "slightly_epic_radio")

class PreferencesManager(private val context: Context) {

    companion object {
        private val LAST_STATION_INDEX = intPreferencesKey("last_station_index")
    }

    suspend fun getLastStationIndex(): Int {
        return context.dataStore.data
            .map { prefs -> prefs[LAST_STATION_INDEX] ?: 0 }
            .first()
    }

    suspend fun saveLastStationIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[LAST_STATION_INDEX] = index
        }
    }
}
