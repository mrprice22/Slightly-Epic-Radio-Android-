package com.SlightlyEpic.Radio.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.SlightlyEpic.Radio.R
import com.SlightlyEpic.Radio.data.MetadataFetcher
import com.SlightlyEpic.Radio.data.NowPlaying
import com.SlightlyEpic.Radio.data.Station
import com.SlightlyEpic.Radio.data.StationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val metadataFetcher = MetadataFetcher()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var metadataJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .build()

        val stationPlayer = StationCyclingPlayer(exoPlayer) { direction ->
            switchStation(direction)
        }

        stationPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // New station loaded — restart polling for its metadata.
                val station = mediaItem?.currentStation() ?: return
                startMetadataPolling(station)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    val station = stationPlayer.currentMediaItem?.currentStation() ?: return
                    startMetadataPolling(station)
                } else {
                    stopMetadataPolling()
                }
            }
        })

        // Tap notification to return to the app
        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.SlightlyEpic.Radio.ui.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Close button for the notification
        val closeButton = CommandButton.Builder()
            .setDisplayName(getString(R.string.action_close))
            .setIconResId(R.drawable.ic_close)
            .setSessionCommand(SessionCommand(ACTION_CLOSE, Bundle.EMPTY))
            .build()

        mediaSession = MediaSession.Builder(this, stationPlayer)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val connectionResult = super.onConnect(session, controller)
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(
                            connectionResult.availableSessionCommands.buildUpon()
                                .add(SessionCommand(ACTION_CLOSE, Bundle.EMPTY))
                                .build()
                        )
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction == ACTION_CLOSE) {
                        session.player.apply {
                            playWhenReady = false
                            stop()
                            clearMediaItems()
                        }
                        stopSelf()
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }
            })
            .setCustomLayout(listOf(closeButton))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Stop playback and tear down the service when the user swipes the app away.
        mediaSession?.player?.apply {
            playWhenReady = false
            stop()
            clearMediaItems()
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopMetadataPolling()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun switchStation(direction: Int) {
        val player = mediaSession?.player ?: return
        val stations = StationRepository.stations
        if (stations.isEmpty()) return

        val currentId = player.currentMediaItem?.mediaId?.toIntOrNull()
        val currentIndex = stations.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
        val newIndex = ((currentIndex + direction) % stations.size + stations.size) % stations.size
        val newStation = stations[newIndex]

        player.setMediaItem(buildStationMediaItem(newStation))
        player.prepare()
        player.play()
    }

    private fun startMetadataPolling(station: Station) {
        stopMetadataPolling()
        if (station.metaType == com.SlightlyEpic.Radio.data.MetaType.NONE) return

        metadataJob = serviceScope.launch {
            while (true) {
                val nowPlaying = metadataFetcher.fetch(station)
                if (nowPlaying.displayText.isNotBlank()) {
                    applyNowPlaying(station, nowPlaying)
                }
                delay(20_000) // Poll every 20 seconds, matching Roku app
            }
        }
    }

    private fun stopMetadataPolling() {
        metadataJob?.cancel()
        metadataJob = null
    }

    private fun applyNowPlaying(station: Station, nowPlaying: NowPlaying) {
        val player = mediaSession?.player ?: return
        val currentItem = player.currentMediaItem ?: return
        // Guard against races: don't overwrite a different station's item with stale metadata.
        if (currentItem.mediaId != station.id.toString()) return

        val updatedItem = currentItem.buildUpon()
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(nowPlaying.displayText)
                    .setArtist(station.title)
                    .build()
            )
            .build()

        player.replaceMediaItem(player.currentMediaItemIndex, updatedItem)
    }

    private fun MediaItem.currentStation(): Station? {
        val id = mediaId.toIntOrNull() ?: return null
        return StationRepository.stations.firstOrNull { it.id == id }
    }

    companion object {
        private const val ACTION_CLOSE = "com.SlightlyEpic.Radio.CLOSE"

        fun buildStationMediaItem(station: Station): MediaItem {
            return MediaItem.Builder()
                .setMediaId(station.id.toString())
                .setUri(station.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(station.title)
                        .setArtist(station.title)
                        .build()
                )
                .build()
        }
    }
}

/**
 * Forwarding player that always exposes next/previous media-item commands and cycles
 * through the radio stations (wrapping at both ends) when Bluetooth remotes invoke them.
 */
@OptIn(UnstableApi::class)
private class StationCyclingPlayer(
    player: Player,
    private val onSwitchStation: (direction: Int) -> Unit
) : ForwardingPlayer(player) {

    override fun getAvailableCommands(): Player.Commands {
        return super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        return when (command) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
            else -> super.isCommandAvailable(command)
        }
    }

    override fun hasNextMediaItem(): Boolean = true
    override fun hasPreviousMediaItem(): Boolean = true

    override fun seekToNext() {
        onSwitchStation(1)
    }

    override fun seekToNextMediaItem() {
        onSwitchStation(1)
    }

    override fun seekToPrevious() {
        onSwitchStation(-1)
    }

    override fun seekToPreviousMediaItem() {
        onSwitchStation(-1)
    }
}
