package com.sopa.viva_automotive.feature.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sopa.viva_automotive.core.ui.theme.VivaDimens
import com.sopa.viva_automotive.feature.media.domain.MediaSource
import com.sopa.viva_automotive.feature.media.domain.PlaybackUiState
import com.sopa.viva_automotive.feature.media.domain.RadioStation
import com.sopa.viva_automotive.feature.media.ui.AlbumArtwork
import com.sopa.viva_automotive.feature.media.ui.formatDurationMs

@Composable
fun NowPlayingMediaCard(
    playback: PlaybackUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenMedia: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = playback.track
    val isRadio = playback.source == MediaSource.RADIO
    val notAvailable = stringResource(R.string.media_not_available)
    val title = when {
        track == null && isRadio -> stringResource(R.string.media_radio_idle)
        track == null -> stringResource(R.string.media_idle)
        else -> playback.display.title ?: notAvailable
    }
    val subtitle = when {
        track?.frequencyMhz != null ->
            stringResource(R.string.media_radio_frequency, track.frequencyMhz)
        track == null && isRadio -> stringResource(R.string.media_source_radio)
        track == null -> stringResource(R.string.media_source_library)
        else -> playback.display.artist ?: notAvailable
    }
    val durationLabel = formatDurationMs(playback.display.durationMs) ?: notAvailable

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenMedia),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.media_card_now_playing),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArtwork(
                    artworkBytes = playback.display.artworkBytes,
                    size = 88.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = if (track?.frequencyMhz != null) {
                            MaterialTheme.typography.headlineSmall
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = if (track?.frequencyMhz != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (track != null) {
                        Text(
                            text = durationLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPrevious,
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
                    )
                }
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(56.dp),
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
                            if (playback.isPlaying) R.string.media_pause else R.string.media_play,
                        ),
                    )
                }
                IconButton(
                    onClick = onNext,
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
                    )
                }
            }
        }
    }
}

@Composable
fun RadioMediaCard(
    playback: PlaybackUiState,
    stations: List<RadioStation>,
    onTuneStation: (String) -> Unit,
    onOpenRadio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeId = playback.track?.id.takeIf { playback.source == MediaSource.RADIO }
    val preview = stations.take(3)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenRadio),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.Radio,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = stringResource(R.string.media_card_radio),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = if (activeId != null && playback.track != null) {
                    stringResource(
                        R.string.media_card_radio_tuned,
                        playback.track.title,
                        playback.track.frequencyMhz ?: 0f,
                    )
                } else {
                    stringResource(R.string.media_card_radio_hint)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                preview.forEach { station ->
                    val selected = station.id == activeId
                    Card(
                        onClick = { onTuneStation(station.id) },
                        modifier = Modifier
                            .weight(1f)
                            .height(VivaDimens.TouchTarget),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = String.format("%.1f", station.frequencyMhz),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
