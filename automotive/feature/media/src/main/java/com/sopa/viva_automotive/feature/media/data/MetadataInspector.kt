package com.sopa.viva_automotive.feature.media.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.inspector.MetadataRetriever
import com.sopa.viva_automotive.feature.media.domain.TrackDisplayInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class MetadataInspector @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val cache = ConcurrentHashMap<String, TrackDisplayInfo>()

    suspend fun inspect(mediaId: String, file: File): TrackDisplayInfo =
        withContext(Dispatchers.IO) {
            cache[mediaId] ?: extract(mediaId, file).also { cache[mediaId] = it }
        }

    fun cached(mediaId: String): TrackDisplayInfo? = cache[mediaId]

    private fun extract(mediaId: String, file: File): TrackDisplayInfo {
        val mediaItem = MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(Uri.fromFile(file))
            .build()

        val durationMs = retrieveDurationMs(mediaItem)
            ?: retrieveDurationMsPlatform(file)
        val tags = retrieveId3Tags(file)

        return TrackDisplayInfo(
            mediaId = mediaId,
            title = tags.title,
            artist = tags.artist,
            durationMs = durationMs,
            artworkBytes = tags.artworkBytes,
        )
    }

    private fun retrieveDurationMs(mediaItem: MediaItem): Long? =
        runCatching {
            MetadataRetriever.Builder(context, mediaItem).build().use { retriever ->
                val durationUs = retriever.retrieveDurationUs()
                    .get(TIMEOUT_SEC, TimeUnit.SECONDS)
                if (durationUs == null || durationUs == C.TIME_UNSET || durationUs < 0L) {
                    null
                } else {
                    durationUs / 1_000L
                }
            }
        }.onFailure { error ->
            Log.w(TAG, "MetadataRetriever duration failed", error)
        }.getOrNull()

    private fun retrieveDurationMsPlatform(file: File): Long? =
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?.takeIf { it > 0L }
            }
        }.getOrNull()

    private fun retrieveId3Tags(file: File): Id3Tags {
        return runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(file.absolutePath)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    .takeIfMeaningful()
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    .takeIfMeaningful()
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                        .takeIfMeaningful()
                val artwork = if (file.extension.equals("wav", ignoreCase = true)) {
                    null
                } else {
                    runCatching { retriever.embeddedPicture }.getOrNull()
                }
                Id3Tags(title = title, artist = artist, artworkBytes = artwork)
            }
        }.onFailure { error ->
            Log.w(TAG, "ID3/tag retrieve failed for ${file.name}", error)
        }.getOrDefault(Id3Tags())
    }

    private fun String?.takeIfMeaningful(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }

    private data class Id3Tags(
        val title: String? = null,
        val artist: String? = null,
        val artworkBytes: ByteArray? = null,
    )

    private companion object {
        const val TAG = "MetadataInspector"
        const val TIMEOUT_SEC = 8L
    }
}
