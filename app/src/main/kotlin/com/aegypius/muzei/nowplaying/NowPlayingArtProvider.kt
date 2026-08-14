package com.aegypius.muzei.nowplaying

import android.net.Uri
import android.util.Log
import com.aegypius.muzei.nowplaying.domain.AlbumKey
import com.aegypius.muzei.nowplaying.domain.ArtistFallback
import com.aegypius.muzei.nowplaying.domain.ArtworkUrl
import com.aegypius.muzei.nowplaying.domain.Track
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider

/**
 * Publishes the album currently playing to Muzei.
 *
 * Thin glue on purpose: the album key, the lookup URL and the fallback rule all
 * live in :domain, where they are unit tested. This class only translates between
 * that and Muzei's API.
 *
 * It still publishes one fixed album, so the delivery chain can be verified on a
 * device before now-playing detection exists (ticket a4e011).
 */
class NowPlayingArtProvider : MuzeiArtProvider() {

    override fun onLoadRequested(initial: Boolean) {
        publish(PLACEHOLDER_KEY, ArtworkUrl.of(PLACEHOLDER_KEY))
    }

    /**
     * Muzei could not load the artwork.
     *
     * The inherited default deletes it, which for a single-artwork provider leaves
     * the wallpaper with nothing at all — the API's own documentation warns about
     * exactly that. The decision of what to try next, including when to stop, is
     * ArtistFallback's.
     */
    override fun onInvalidArtwork(artwork: Artwork) {
        val failedUrl = artwork.persistentUri?.toString() ?: return
        val next = ArtistFallback.after(failedUrl = failedUrl, key = PLACEHOLDER_KEY)
        if (next == null) {
            // Deliberately not calling super: keeping a stale cover beats emptying
            // the provider. See docs/adr/0003-sticky-when-idle.md.
            Log.i(TAG, "nothing left to try for token ${artwork.token}; keeping current artwork")
            return
        }
        publish(PLACEHOLDER_KEY, next)
    }

    /**
     * setArtwork replaces the whole table: one artwork, always the current album.
     * MuzeiArtProvider is itself a ProviderClient, so this needs no authority and
     * no context.
     */
    private fun publish(key: AlbumKey, artworkUrl: String) {
        setArtwork(
            Artwork.Builder()
                .token(key.token)
                // The caption names the album and its artist, never the track:
                // under an album-level token Muzei has no reason to update it
                // mid-record. See docs/adr/0002-album-level-identity.md.
                .title(key.album ?: key.albumArtist)
                .byline(key.albumArtist)
                .persistentUri(Uri.parse(artworkUrl))
                .build(),
        )
    }

    private companion object {
        const val TAG = "NowPlayingArtProvider"

        /** Fixed until the notification listener supplies real metadata (a4e011). */
        val PLACEHOLDER_KEY: AlbumKey = requireNotNull(
            AlbumKey.of(
                Track(
                    title = null,
                    artist = null,
                    albumArtist = "Radiohead",
                    album = "In Rainbows",
                ),
            ),
        ) { "the placeholder track must always yield an album key" }
    }
}
