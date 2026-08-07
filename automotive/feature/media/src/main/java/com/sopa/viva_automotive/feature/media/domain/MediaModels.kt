package com.sopa.viva_automotive.feature.media.domain

import java.io.File

enum class MediaSource {
    LIBRARY,
    RADIO,
}

enum class PlaybackRoute {
    LOCAL,
    CAST,
}

data class MediaTrack(
    val id: String,
    val title: String,
    val artist: String,
    val file: File,
    val frequencyMhz: Float? = null,
)

data class RadioStation(
    val id: String,
    val name: String,
    val frequencyMhz: Float,
    val region: String,
) {
    fun toTrack(file: File): MediaTrack = MediaTrack(
        id = id,
        title = name,
        artist = String.format("%.1f FM · %s", frequencyMhz, region),
        file = file,
        frequencyMhz = frequencyMhz,
    )
}

data class TrackDisplayInfo(
    val mediaId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val durationMs: Long? = null,
    val artworkBytes: ByteArray? = null,
) {
    val hasArtwork: Boolean get() = artworkBytes != null && artworkBytes.isNotEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackDisplayInfo) return false
        return mediaId == other.mediaId &&
            title == other.title &&
            artist == other.artist &&
            durationMs == other.durationMs &&
            artworkBytes.contentEquals(other.artworkBytes)
    }

    override fun hashCode(): Int {
        var result = mediaId?.hashCode() ?: 0
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (artist?.hashCode() ?: 0)
        result = 31 * result + (durationMs?.hashCode() ?: 0)
        result = 31 * result + (artworkBytes?.contentHashCode() ?: 0)
        return result
    }
}

data class PlaybackUiState(
    val source: MediaSource = MediaSource.LIBRARY,
    val track: MediaTrack? = null,
    val isPlaying: Boolean = false,
    val queueIndex: Int = 0,
    val queueSize: Int = 0,
    val stations: List<RadioStation> = emptyList(),
    val route: PlaybackRoute = PlaybackRoute.LOCAL,
    val castAvailable: Boolean = false,
    val display: TrackDisplayInfo = TrackDisplayInfo(),
    val mediaVolume: Float = 1f,
)
