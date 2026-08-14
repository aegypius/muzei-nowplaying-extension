package com.aegypius.muzei.nowplaying

import android.content.Context
import com.aegypius.muzei.nowplaying.domain.AlbumKey
import com.aegypius.muzei.nowplaying.domain.ArtworkPublisher
import com.google.android.apps.muzei.api.provider.ProviderContract

/**
 * Publishes to Muzei from outside the provider.
 *
 * The listener service cannot call the provider instance directly, so it goes
 * through a provider client. The client is obtained from the provider class, which
 * reads the authority out of the manifest, so the authority is not duplicated in
 * code.
 */
class MuzeiArtworkPublisher(private val context: Context) : ArtworkPublisher {

    override fun publish(key: AlbumKey, artworkUrl: String) {
        ProviderContract.getProviderClient(context, NowPlayingArtProvider::class.java)
            .setArtwork(artworkFor(key, artworkUrl))
    }
}
