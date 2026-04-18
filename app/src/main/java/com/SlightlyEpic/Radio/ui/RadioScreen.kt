package com.SlightlyEpic.Radio.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.SlightlyEpic.Radio.data.Station

@Composable
fun RadioScreen(viewModel: RadioViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { viewModel.enterEditMode() })
                }
        ) {
            // Top area: Station artwork and now playing
            NowPlayingSection(
                station = uiState.selectedStation,
                isPlaying = uiState.isPlaying,
                nowPlayingText = uiState.nowPlaying.displayText,
                onPlayPauseClick = viewModel::togglePlayPause,
                onSeekBack30 = viewModel::seekBack30,
                onSeekForward30 = viewModel::seekForward30,
                onSeekToBufferStart = viewModel::seekToBufferStart,
                onSeekToLive = viewModel::seekToLive,
                modifier = Modifier.weight(1f)
            )

            // Bottom: Station selector
            StationSelector(
                stations = uiState.stations,
                selectedIndex = uiState.selectedStationIndex,
                onStationSelected = viewModel::selectStation
            )
        }

        if (uiState.isEditMode) {
            StationEditOverlay(
                stations = uiState.allStations,
                hiddenIds = uiState.hiddenStationIds,
                onToggleVisibility = viewModel::toggleStationVisibility,
                onMove = viewModel::moveStation,
                onDismiss = viewModel::exitEditMode
            )
        }
    }
}

private const val SeekControlsCooldownMs = 4000L

@Composable
private fun NowPlayingSection(
    station: Station,
    isPlaying: Boolean,
    nowPlayingText: String,
    onPlayPauseClick: () -> Unit,
    onSeekBack30: () -> Unit,
    onSeekForward30: () -> Unit,
    onSeekToBufferStart: () -> Unit,
    onSeekToLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Seek controls show while paused, and linger a few seconds after any seek click.
    // Each click bumps the token, restarting the auto-hide timer.
    var seekClickToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(seekClickToken, isPlaying) {
        if (isPlaying && seekClickToken > 0) {
            delay(SeekControlsCooldownMs)
            seekClickToken = 0
        }
    }
    val showSeekControls = !isPlaying || seekClickToken > 0
    val bumpSeek = { seekClickToken++ }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Station logo
            val logoResId = context.resources.getIdentifier(
                station.logoResName, "drawable", context.packageName
            )

            if (logoResId != 0) {
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = station.title,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Fallback icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Station name
            Text(
                text = station.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Now playing metadata
            AnimatedVisibility(
                visible = nowPlayingText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = nowPlayingText,
                        color = Color(0xFFCCCCCC),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Play/Pause button
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seek controls are disabled for now. The underlying Icecast/Shoutcast
            // streams are endless progressive HTTP feeds that ExoPlayer treats as
            // non-seekable live sources, so only "jump to live" actually does
            // anything — the other three have nothing to seek within. To make them
            // work we'd need to build a client-side rolling cache (what VLC does):
            //   1. Wrap the HttpDataSource in a CacheDataSource backed by a
            //      SimpleCache (capped size, per-session or rolling).
            //   2. Enable DefaultLoadControl.setBackBuffer(retainMs, false) so
            //      past audio is retained instead of discarded.
            //   3. ProgressiveMediaSource then reports the cached range as
            //      seekable, currentPosition grows, and ±30s / jump-to-start work.
            //      "Jump to live" becomes seekToDefaultPosition() (end of cache)
            //      or re-opening the live URL.
            // Until that's wired up, leave the buttons hidden to avoid dead UI.
            @Suppress("ControlFlowWithEmptyBody")
            if (false) {
                AnimatedVisibility(
                    visible = showSeekControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SeekButton(
                            icon = Icons.Default.SkipPrevious,
                            description = "Jump to start of buffer",
                            onClick = { bumpSeek(); onSeekToBufferStart() }
                        )
                        SeekButton(
                            icon = Icons.Default.FastRewind,
                            description = "Skip back 30 seconds",
                            onClick = { bumpSeek(); onSeekBack30() }
                        )
                        SeekButton(
                            icon = Icons.Default.FastForward,
                            description = "Skip forward 30 seconds",
                            onClick = { bumpSeek(); onSeekForward30() }
                        )
                        SeekButton(
                            icon = Icons.Default.SkipNext,
                            description = "Jump to live",
                            onClick = { bumpSeek(); onSeekToLive() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeekButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .background(
                color = Color(0xFF222222),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun StationSelector(
    stations: List<Station>,
    selectedIndex: Int,
    onStationSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll to selected station
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(
            index = selectedIndex,
            scrollOffset = -200
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xCC000000), Color(0xFF1A1A1A))
                )
            )
            .padding(top = 16.dp, bottom = 16.dp)
    ) {
        Text(
            text = "STATIONS",
            color = Color(0xFF888888),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(stations) { index, station ->
                StationCard(
                    station = station,
                    isSelected = index == selectedIndex,
                    onClick = { onStationSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun StationCard(
    station: Station,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            val logoResId = context.resources.getIdentifier(
                station.logoResName, "drawable", context.packageName
            )

            if (logoResId != 0) {
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = station.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = station.title,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}

private val EditRowHeightDp = 64

@Composable
private fun StationEditOverlay(
    stations: List<Station>,
    hiddenIds: Set<Int>,
    onToggleVisibility: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val rowHeightPx = with(density) { EditRowHeightDp.dp.toPx() }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE000000))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(Unit) { detectTapGestures { /* swallow taps */ } }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EDIT STATIONS",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = "Tap checkbox to hide/show. Drag the handle to reorder.",
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                stations.forEachIndexed { index, station ->
                    key(station.id) {
                        val isDragging = draggingIndex == index
                        val visible = station.id !in hiddenIds
                        EditRow(
                            station = station,
                            visible = visible,
                            isDragging = isDragging,
                            dragOffsetY = if (isDragging) dragOffsetY else 0f,
                            onToggle = { onToggleVisibility(station.id) },
                            dragModifier = Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val current = draggingIndex ?: return@detectDragGestures
                                        dragOffsetY += dragAmount.y
                                        if (dragOffsetY > rowHeightPx / 2f && current < stations.lastIndex) {
                                            onMove(current, current + 1)
                                            draggingIndex = current + 1
                                            dragOffsetY -= rowHeightPx
                                        } else if (dragOffsetY < -rowHeightPx / 2f && current > 0) {
                                            onMove(current, current - 1)
                                            draggingIndex = current - 1
                                            dragOffsetY += rowHeightPx
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun EditRow(
    station: Station,
    visible: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    onToggle: () -> Unit,
    dragModifier: Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(EditRowHeightDp.dp)
            .graphicsLayer {
                translationY = dragOffsetY
                if (isDragging) {
                    shadowElevation = 12f
                    alpha = 0.95f
                }
            }
            .background(if (isDragging) Color(0xFF222222) else Color.Transparent)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox area
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (visible) MaterialTheme.colorScheme.primary else Color(0xFF333333))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (visible) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Visible",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Logo
        val logoResId = context.resources.getIdentifier(
            station.logoResName, "drawable", context.packageName
        )
        if (logoResId != 0) {
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF333333)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title
        Text(
            text = station.title,
            color = if (visible) Color.White else Color(0xFF888888),
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Drag handle
        Box(
            modifier = Modifier
                .size(48.dp)
                .then(dragModifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = Color(0xFFBBBBBB)
            )
        }
    }
}
