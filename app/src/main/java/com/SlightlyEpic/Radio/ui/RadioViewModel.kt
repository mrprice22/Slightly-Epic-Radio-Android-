package com.SlightlyEpic.Radio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.SlightlyEpic.Radio.data.NowPlaying
import com.SlightlyEpic.Radio.data.PreferencesManager
import com.SlightlyEpic.Radio.data.Station
import com.SlightlyEpic.Radio.data.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RadioUiState(
    val allStations: List<Station> = StationRepository.stations,
    val hiddenStationIds: Set<Int> = emptySet(),
    val selectedStationId: Int = StationRepository.stations.first().id,
    val isPlaying: Boolean = false,
    val nowPlaying: NowPlaying = NowPlaying(),
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false
) {
    val stations: List<Station>
        get() = allStations.filter { it.id !in hiddenStationIds }

    val selectedStationIndex: Int
        get() {
            val i = stations.indexOfFirst { it.id == selectedStationId }
            return if (i >= 0) i else 0
        }

    val selectedStation: Station
        get() = stations.getOrNull(selectedStationIndex) ?: allStations.first()
}

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    // Player control callbacks — set by MainActivity when controller connects
    var onPlayStation: ((Station) -> Unit)? = null
    var onPause: (() -> Unit)? = null
    var onResume: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            val base = StationRepository.stations
            val savedOrder = preferencesManager.getStationOrder()
            val ordered = if (savedOrder != null) {
                val byId = base.associateBy { it.id }
                val inOrder = savedOrder.mapNotNull { byId[it] }
                val missing = base.filter { it.id !in savedOrder.toSet() }
                inOrder + missing
            } else {
                base
            }
            val hidden = preferencesManager.getHiddenStations()
                .filter { id -> base.any { it.id == id } }
                .toSet()
            // Prefer saved station ID; fall back to legacy index for older installs.
            val lastId = preferencesManager.getLastStationId()
                ?.takeIf { id -> ordered.any { it.id == id } }
                ?: run {
                    val lastIndex = preferencesManager.getLastStationIndex()
                        .coerceIn(0, base.size - 1)
                    base.getOrNull(lastIndex)?.id ?: ordered.first().id
                }
            val selectedId = if (lastId in hidden) {
                ordered.firstOrNull { it.id !in hidden }?.id ?: ordered.first().id
            } else {
                lastId
            }
            _uiState.value = _uiState.value.copy(
                allStations = ordered,
                hiddenStationIds = hidden,
                selectedStationId = selectedId,
                isLoading = false
            )
        }
    }

    fun selectStation(index: Int) {
        val visible = _uiState.value.stations
        if (visible.isEmpty()) return
        val clamped = index.coerceIn(0, visible.size - 1)
        val station = visible[clamped]
        _uiState.value = _uiState.value.copy(
            selectedStationId = station.id,
            nowPlaying = NowPlaying()
        )

        viewModelScope.launch {
            preferencesManager.saveLastStationId(station.id)
        }

        onPlayStation?.invoke(station)
        _uiState.value = _uiState.value.copy(isPlaying = true)
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            onPause?.invoke()
            _uiState.value = _uiState.value.copy(isPlaying = false)
        } else {
            if (onResume != null) {
                onResume?.invoke()
            } else {
                onPlayStation?.invoke(_uiState.value.selectedStation)
            }
            _uiState.value = _uiState.value.copy(isPlaying = true)
        }
    }

    fun updatePlayingState(isPlaying: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
    }

    fun enterEditMode() {
        _uiState.value = _uiState.value.copy(isEditMode = true)
    }

    fun exitEditMode() {
        _uiState.value = _uiState.value.copy(isEditMode = false)
    }

    fun toggleStationVisibility(stationId: Int) {
        val current = _uiState.value
        val newHidden = current.hiddenStationIds.toMutableSet()
        val hiding: Boolean
        if (stationId in newHidden) {
            newHidden.remove(stationId)
            hiding = false
        } else {
            // Block hiding the last visible station
            if (current.stations.size <= 1) return
            newHidden.add(stationId)
            hiding = true
        }
        val updated = current.copy(hiddenStationIds = newHidden)
        _uiState.value = updated
        viewModelScope.launch {
            preferencesManager.saveHiddenStations(newHidden)
        }

        // If the now-playing station was just hidden, switch to the first visible one.
        if (hiding && stationId == current.selectedStationId) {
            val firstVisible = updated.stations.firstOrNull() ?: return
            selectStation(updated.stations.indexOf(firstVisible))
        }
    }

    fun moveStation(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value
        val list = current.allStations.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _uiState.value = current.copy(allStations = list)
        viewModelScope.launch {
            preferencesManager.saveStationOrder(list.map { it.id })
        }
    }

    /** Called when the underlying player switches stations on its own (e.g. via Bluetooth next/prev). */
    fun onExternalStationChange(stationId: Int) {
        val current = _uiState.value
        if (current.allStations.none { it.id == stationId }) return
        if (stationId == current.selectedStationId) return

        _uiState.value = current.copy(
            selectedStationId = stationId,
            nowPlaying = NowPlaying()
        )

        viewModelScope.launch {
            preferencesManager.saveLastStationId(stationId)
        }
    }

    fun updateNowPlayingFromMetadata(title: String?, artist: String?) {
        val t = title?.trim().orEmpty()
        val a = artist?.trim().orEmpty()
        val current = _uiState.value
        val stationName = current.selectedStation.title

        val looksLikeStationPlaceholder = t.isBlank() || t == a || t == stationName
        val nowPlaying = if (looksLikeStationPlaceholder) {
            NowPlaying()
        } else {
            parseDisplayText(t)
        }
        _uiState.value = current.copy(nowPlaying = nowPlaying)
    }

    private fun parseDisplayText(text: String): NowPlaying {
        val dash = text.indexOf(" - ")
        return if (dash > 0) {
            NowPlaying(
                artist = text.substring(0, dash).trim(),
                title = text.substring(dash + 3).trim()
            )
        } else {
            NowPlaying(title = text)
        }
    }
}
