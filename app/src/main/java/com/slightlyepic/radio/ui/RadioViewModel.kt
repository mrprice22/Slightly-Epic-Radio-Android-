package com.slightlyepic.radio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.slightlyepic.radio.data.MetadataFetcher
import com.slightlyepic.radio.data.NowPlaying
import com.slightlyepic.radio.data.PreferencesManager
import com.slightlyepic.radio.data.Station
import com.slightlyepic.radio.data.StationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val metadataFetcher = MetadataFetcher()

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    private var metadataJob: Job? = null

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
        startMetadataPolling()
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            onPause?.invoke()
            _uiState.value = _uiState.value.copy(isPlaying = false)
            stopMetadataPolling()
        } else {
            if (onResume != null) {
                onResume?.invoke()
            } else {
                onPlayStation?.invoke(_uiState.value.selectedStation)
            }
            _uiState.value = _uiState.value.copy(isPlaying = true)
            startMetadataPolling()
        }
    }

    fun updatePlayingState(isPlaying: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
    }

    private fun startMetadataPolling() {
        stopMetadataPolling()
        metadataJob = viewModelScope.launch {
            while (true) {
                val station = _uiState.value.selectedStation
                val nowPlaying = metadataFetcher.fetch(station)
                _uiState.value = _uiState.value.copy(nowPlaying = nowPlaying)
                delay(20_000) // Poll every 20 seconds, matching Roku app
            }
        }
    }

    private fun stopMetadataPolling() {
        metadataJob?.cancel()
        metadataJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopMetadataPolling()
    }
}
