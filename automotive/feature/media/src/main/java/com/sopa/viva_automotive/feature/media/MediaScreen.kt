package com.sopa.viva_automotive.feature.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sopa.viva_automotive.core.ui.components.SectionCard
import com.sopa.viva_automotive.core.ui.theme.VivaDimens
import com.sopa.viva_automotive.feature.media.cast.CastRouteButton
import com.sopa.viva_automotive.feature.media.domain.MediaSource
import com.sopa.viva_automotive.feature.media.domain.PlaybackRoute
import com.sopa.viva_automotive.feature.media.ui.AlbumArtwork
import com.sopa.viva_automotive.feature.media.ui.formatDurationMs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaScreen(
    modifier: Modifier = Modifier,
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isRadio = playback.source == MediaSource.RADIO

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(R.string.media_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isRadio,
                    onClick = { viewModel.selectSource(MediaSource.LIBRARY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.height(VivaDimens.ButtonHeight),
                ) {
                    Text(stringResource(R.string.media_source_library))
                }
                SegmentedButton(
                    selected = isRadio,
                    onClick = { viewModel.selectSource(MediaSource.RADIO) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.height(VivaDimens.ButtonHeight),
                ) {
                    Text(stringResource(R.string.media_source_radio))
                }
            }

            SectionCard(title = stringResource(R.string.media_cast_route)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                !playback.castAvailable ->
                                    stringResource(R.string.media_cast_unavailable)
                                playback.route == PlaybackRoute.CAST ->
                                    stringResource(R.string.media_route_cast)
                                else -> stringResource(R.string.media_route_local)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (playback.castAvailable) {
                        CastRouteButton()
                    }
                }
            }

            SectionCard(
                title = if (isRadio) {
                    stringResource(R.string.media_source_radio)
                } else {
                    stringResource(R.string.media_now_playing)
                },
            ) {
                val track = playback.track
                val display = playback.display
                val notAvailable = stringResource(R.string.media_not_available)
                val titleText = when {
                    track == null && isRadio -> stringResource(R.string.media_radio_idle)
                    track == null -> stringResource(R.string.media_idle)
                    else -> display.title ?: notAvailable
                }
                val artistText = if (track == null) "" else display.artist ?: notAvailable
                val durationText = formatDurationMs(display.durationMs) ?: notAvailable

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AlbumArtwork(
                        artworkBytes = display.artworkBytes,
                        size = 260.dp,
                    )
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    if (track?.frequencyMhz != null) {
                        Text(
                            text = stringResource(
                                R.string.media_radio_frequency,
                                track.frequencyMhz,
                            ),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        text = artistText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "${stringResource(R.string.media_duration_label)}: $durationText",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (playback.queueSize > 0) {
                        Text(
                            text = stringResource(
                                R.string.media_track_format,
                                playback.queueIndex + 1,
                                playback.queueSize,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = viewModel::previous,
                        modifier = Modifier.size(VivaDimens.TouchTarget),
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = stringResource(
                                if (isRadio) {
                                    R.string.media_radio_seek_previous
                                } else {
                                    R.string.media_previous
                                },
                            ),
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    FilledIconButton(
                        onClick = viewModel::togglePlayPause,
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = if (playback.isPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = stringResource(
                                if (playback.isPlaying) {
                                    R.string.media_pause
                                } else {
                                    R.string.media_play
                                },
                            ),
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    IconButton(
                        onClick = viewModel::next,
                        modifier = Modifier.size(VivaDimens.TouchTarget),
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = stringResource(
                                if (isRadio) {
                                    R.string.media_radio_seek_next
                                } else {
                                    R.string.media_next
                                },
                            ),
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }

            if (isRadio) {
                SectionCard(title = stringResource(R.string.media_radio_presets)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        playback.stations.forEach { station ->
                            val selected = playback.track?.id == station.id
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectStation(station.id) },
                                label = {
                                    Text(
                                        text = String.format(
                                            "%.1f  %s",
                                            station.frequencyMhz,
                                            station.name,
                                        ),
                                    )
                                },
                                modifier = Modifier.height(VivaDimens.TouchTargetMin),
                            )
                        }
                    }
                }
            }

            SectionCard(title = stringResource(R.string.media_volume)) {
                var sliderValue by remember { mutableFloatStateOf(playback.mediaVolume) }
                LaunchedEffect(playback.mediaVolume) {
                    sliderValue = playback.mediaVolume
                }
                val sliderScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(
                            available: Offset,
                            source: NestedScrollSource,
                        ): Offset = Offset(0f, available.y)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .nestedScroll(sliderScrollConnection),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeDown,
                        contentDescription = stringResource(R.string.media_volume_down),
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { value ->
                            sliderValue = value
                            viewModel.setMediaVolume(value)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .height(VivaDimens.TouchTarget),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.media_volume_up),
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${(playback.mediaVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}
