package com.SlightlyEpic.Radio.ui

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.SlightlyEpic.Radio.data.Station
import com.SlightlyEpic.Radio.service.RadioPlaybackService
import com.SlightlyEpic.Radio.ui.theme.SlightlyEpicRadioTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RadioViewModel by viewModels()
    private var mediaController: MediaController? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> /* proceed regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        setContent {
            SlightlyEpicRadioTheme {
                RadioScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connectToService()
    }

    override fun onStop() {
        super.onStop()
        mediaController?.release()
        mediaController = null
    }

    private fun connectToService() {
        val sessionToken = SessionToken(this, ComponentName(this, RadioPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller

            viewModel.onPlayStation = { station -> playStation(controller, station) }
            viewModel.onPause = { controller.pause() }
            viewModel.onResume = {
                if (controller.currentMediaItem == null) {
                    playStation(controller, viewModel.uiState.value.selectedStation)
                } else {
                    controller.play()
                }
            }

            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    viewModel.updatePlayingState(isPlaying)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val stationId = mediaItem?.mediaId?.toIntOrNull() ?: return
                    viewModel.onExternalStationChange(stationId)
                    // Seed the UI from whatever metadata the new item carries.
                    val md = mediaItem.mediaMetadata
                    viewModel.updateNowPlayingFromMetadata(md.title?.toString(), md.artist?.toString())
                }

                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    viewModel.updateNowPlayingFromMetadata(
                        mediaMetadata.title?.toString(),
                        mediaMetadata.artist?.toString()
                    )
                }
            })

            if (controller.isPlaying) {
                viewModel.updatePlayingState(true)
            }
            // Pick up any metadata already on the session (e.g. app reopened while playing).
            controller.currentMediaItem?.mediaMetadata?.let { md ->
                viewModel.updateNowPlayingFromMetadata(md.title?.toString(), md.artist?.toString())
            }
        }, MoreExecutors.directExecutor())
    }

    private fun playStation(controller: MediaController, station: Station) {
        controller.setMediaItem(RadioPlaybackService.buildStationMediaItem(station))
        controller.prepare()
        controller.play()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
