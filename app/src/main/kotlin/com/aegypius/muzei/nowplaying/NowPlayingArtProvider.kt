package com.aegypius.muzei.nowplaying

import com.aegypius.muzei.nowplaying.domain.AlbumKey
import com.aegypius.muzei.nowplaying.domain.PublishArtistFallback
import com.aegypius.muzei.nowplaying.domain.RestoreLastAlbum
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider

/**
 * Exposes the album currently playing to Muzei.
 *
 * Thin glue on purpose: the album key, the lookup URL and the fallback rule live
 * in :domain, where they are tested. Publishing is driven by
 * NowPlayingListenerService, not from here.
 */
class NowPlayingArtProvider : MuzeiArtProvider() {

    /**
     * Muzei is asking for artwork, which happens when the provider is first selected
     * and after a restart, before anything has played in this process.
     *
     * The album last published is restored if there is one. If nothing has ever
     * played, a sample album is published through the ordinary lookup so the
     * wallpaper is never blank with no explanation.
     */
    override fun onLoadRequested(initial: Boolean) {
        val context = context ?: return
        RestoreLastAlbum(
            lastAlbum = SharedPreferencesLastAlbum(context),
            publisher = { key, artworkUrl -> setArtwork(artworkFor(key, artworkUrl)) },
        ).publish()
    }

    /**
     * Muzei could not load the artwork.
     *
     * The inherited default deletes it, which for a single-artwork provider leaves
     * the wallpaper with nothing at all — the API's own documentation warns about
     * exactly that. What to try next, and when to stop, is ArtistFallback's
     * decision.
     */
    override fun onInvalidArtwork(artwork: Artwork) {
        val context = context ?: return
        val failedUrl = artwork.persistentUri?.toString() ?: return
        val key = artwork.token?.let(AlbumKey::fromToken) ?: return

        // Deliberately never calls super, which would delete the artwork and leave
        // the wallpaper with nothing: a stale cover beats an empty provider. See
        // docs/adr/0003-sticky-when-idle.md.
        PublishArtistFallback(
            lastAlbum = SharedPreferencesLastAlbum(context),
            publisher = { fallbackKey, artworkUrl ->
                setArtwork(artworkFor(fallbackKey, artworkUrl))
            },
        ).after(failedUrl = failedUrl, key = key)
    }

    private companion object {
        const val TAG = "NowPlayingArtProvider"
    }
}
