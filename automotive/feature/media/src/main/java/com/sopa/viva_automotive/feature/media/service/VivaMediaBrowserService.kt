package com.sopa.viva_automotive.feature.media.service

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import com.sopa.viva_automotive.feature.media.data.DemoToneFactory
import com.sopa.viva_automotive.feature.media.data.VivaMediaRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VivaMediaBrowserService : MediaBrowserServiceCompat() {

    @Inject lateinit var mediaRepository: VivaMediaRepository

    override fun onCreate() {
        super.onCreate()
        runCatching {
            sessionToken = mediaRepository.sessionToken()
        }.onFailure { error ->
            Log.e(TAG, "Unable to attach MediaSession token", error)
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?,
    ): BrowserRoot = BrowserRoot(ROOT_ID, null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>,
    ) {
        if (parentId != ROOT_ID) {
            result.sendResult(mutableListOf())
            return
        }
        val tracks = DemoToneFactory.ensureTracks(cacheDir)
        val items = tracks.map { track ->
            val description = android.support.v4.media.MediaDescriptionCompat.Builder()
                .setMediaId(track.id)
                .setTitle(track.title)
                .setSubtitle(track.artist)
                .build()
            MediaBrowserCompat.MediaItem(
                description,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE,
            )
        }.toMutableList()
        result.sendResult(items)
    }

    private companion object {
        const val TAG = "VivaMediaBrowser"
        const val ROOT_ID = "viva_media_root"
    }
}
