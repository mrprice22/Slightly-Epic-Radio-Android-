package com.SlightlyEpic.Radio.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "slightly_epic_radio")

class PreferencesManager(private val context: Context) {

    companion object {
        private val LAST_STATION_INDEX = intPreferencesKey("last_station_index")
        private val LAST_STATION_ID = intPreferencesKey("last_station_id")
        private val STATION_ORDER = stringPreferencesKey("station_order")
        private val HIDDEN_STATIONS = stringPreferencesKey("hidden_stations")
    }

    suspend fun getLastStationId(): Int? {
        return context.dataStore.data
            .map { prefs -> prefs[LAST_STATION_ID] }
            .first()
    }

    suspend fun saveLastStationId(id: Int) {
        context.dataStore.edit { prefs ->
            prefs[LAST_STATION_ID] = id
        }
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

    suspend fun getStationOrder(): List<Int>? {
        val raw = context.dataStore.data
            .map { prefs -> prefs[STATION_ORDER] }
            .first() ?: return null
        if (raw.isBlank()) return null
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    suspend fun saveStationOrder(order: List<Int>) {
        context.dataStore.edit { prefs ->
            prefs[STATION_ORDER] = order.joinToString(",")
        }
    }

    suspend fun getHiddenStations(): Set<Int> {
        val raw = context.dataStore.data
            .map { prefs -> prefs[HIDDEN_STATIONS] }
            .first() ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    suspend fun saveHiddenStations(hidden: Set<Int>) {
        context.dataStore.edit { prefs ->
            prefs[HIDDEN_STATIONS] = hidden.joinToString(",")
        }
    }
}
