package com.slightlyepic.radio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.slightlyepic.radio.data.NowPlaying
import com.slightlyepic.radio.data.PreferencesManager
import com.slightlyepic.radio.data.Station
import com.slightlyepic.radio.data.StationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RadioUiState(
    val stations: List<Station> = StationRepository.stations,
    val selectedStationIndex: Int = 0,
    val isPlaying: Boolean = false,
    val nowPlaying: NowPlaying = NowPlaying(),
    val isLoading: Boolean = true
) {
    val selectedStation: Station
        get() = stations[selectedStationIndex]
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
            val lastIndex = preferencesManager.getLastStationIndex()
                .coerceIn(0, StationRepository.stations.size - 1)
            _uiState.value = _uiState.value.copy(
                selectedStationIndex = lastIndex,
                isLoading = false
            )
        }
    }

    fun selectStation(index: Int) {
        val clamped = index.coerceIn(0, _uiState.value.stations.size - 1)
        _uiState.value = _uiState.value.copy(
            selectedStationIndex = clamped,
            nowPlaying = NowPlaying()
        )

        viewModelScope.launch {
            preferencesManager.saveLastStationIndex(clamped)
        }

        val station = _uiState.value.selectedStation
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

    /** Called when the underlying player switches stations on its own (e.g. via Bluetooth next/prev). */
    fun onExternalStationChange(stationId: Int) {
        val stations = _uiState.value.stations
        val newIndex = stations.indexOfFirst { it.id == stationId }
        if (newIndex < 0 || newIndex == _uiState.value.selectedStationIndex) return

        _uiState.value = _uiState.value.copy(
            selectedStationIndex = newIndex,
            nowPlaying = NowPlaying()
        )

        viewModelScope.launch {
            preferencesManager.saveLastStationIndex(newIndex)
        }
    }

    /**
     * Called when the media session's metadata changes — either because the service's polling
     * pushed fresh now-playing info or because a new station was loaded. We only surface it to the
     * UI when it's real track info, not the station-name placeholder.
     */
    fun updateNowPlayingFromMetadata(title: String?, artist: String?) {
        val t = title?.trim().orEmpty()
        val a = artist?.trim().orEmpty()
        val current = _uiState.value
        val stationName = current.selectedStation.title

        // Placeholder items have title == artist == station name — treat as "no track info yet".
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
