package com.sopa.viva_automotive.feature.media.cast

import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import androidx.mediarouter.media.MediaControlIntent
import androidx.mediarouter.media.MediaRouteSelector
import com.google.android.gms.cast.CastMediaControlIntent
import com.sopa.viva_automotive.core.ui.theme.VivaDimens
import com.sopa.viva_automotive.feature.media.R

@Composable
fun CastRouteButton(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.size(VivaDimens.TouchTarget),
        factory = { context ->
            val themed = ContextThemeWrapper(context, R.style.Theme_Viva_MediaRouteButton)
            MediaRouteButton(themed).apply {
                routeSelector = MediaRouteSelector.Builder()
                    .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                    .addControlCategory(
                        CastMediaControlIntent.categoryForCast(
                            CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID,
                        ),
                    )
                    .build()
            }
        },
    )
}
