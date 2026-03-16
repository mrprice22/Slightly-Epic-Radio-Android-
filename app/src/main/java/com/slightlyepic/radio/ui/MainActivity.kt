package com.slightlyepic.radio.ui

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.slightlyepic.radio.data.Station
import com.slightlyepic.radio.service.RadioPlaybackService
import com.slightlyepic.radio.ui.theme.SlightlyEpicRadioTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RadioViewModel by viewModels()
    private var mediaController: MediaController? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> /* proceed regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

            // Wire up ViewModel callbacks
            viewModel.onPlayStation = { station -> playStation(controller, station) }
            viewModel.onPause = { controller.pause() }
            viewModel.onResume = { controller.play() }

            // Sync state if player is already active
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    viewModel.updatePlayingState(isPlaying)
                }
            })

            if (controller.isPlaying) {
                viewModel.updatePlayingState(true)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun playStation(controller: MediaController, station: Station) {
        val mediaItem = MediaItem.Builder()
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.title)
                    .setArtist("Slightly Epic Radio")
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
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
