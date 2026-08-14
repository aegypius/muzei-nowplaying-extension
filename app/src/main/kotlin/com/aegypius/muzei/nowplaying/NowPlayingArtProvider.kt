package com.aegypius.muzei.nowplaying

import android.net.Uri
import android.util.Log
import com.google.android.apps.muzei.api.provider.Artwork
import com.google.android.apps.muzei.api.provider.MuzeiArtProvider

/**
 * Publishes the album currently playing to Muzei.
 *
 * At present it publishes one fixed album, so that provider discovery, the
 * manifest wiring and the delivery chain can be verified on a real device before
 * any now-playing detection exists (ticket a4e011). The placeholder points at the
 * production artwork endpoint rather than a bundled image, so the request shape is
 * exercised too.
 */
class NowPlayingArtProvider : MuzeiArtProvider() {

    override fun onLoadRequested(initial: Boolean) {
        publish(artworkUri(ALBUM_ARTIST, ALBUM))
    }

    /**
     * Muzei could not load the artwork.
     *
     * The inherited default deletes it, which for a single-artwork provider leaves
     * the wallpaper with nothing at all — the API's own documentation warns about
     * exactly that. Instead, retry with the album omitted, which the endpoint
     * answers with artist art. See docs/adr/0001-remote-only-artwork.md.
     */
    override fun onInvalidArtwork(artwork: Artwork) {
        val fallback = artworkUri(ALBUM_ARTIST, album = null)
        if (artwork.persistentUri == fallback) {
            // The fallback failed too. Deliberately not calling super: keeping a
            // stale cover beats emptying the provider. See ADR-0003.
            Log.i(TAG, "artist fallback also failed for token ${artwork.token}")
            return
        }
        publish(fallback)
    }

    /**
     * setArtwork replaces the whole table: one artwork, always the current album.
     * MuzeiArtProvider is itself a ProviderClient, so this needs no authority and
     * no context — which is why neither is duplicated from the manifest.
     */
    private fun publish(artworkUri: Uri) {
        setArtwork(
            Artwork.Builder()
                .token(albumKey(ALBUM_ARTIST, ALBUM))
                // The caption names the album and its artist, never the track:
                // under an album-level token Muzei has no reason to update it
                // mid-record. See docs/adr/0002-album-level-identity.md.
                .title(ALBUM)
                .byline(ALBUM_ARTIST)
                .persistentUri(artworkUri)
                .build(),
        )
    }

    private companion object {
        const val TAG = "NowPlayingArtProvider"

        const val ARTWORK_ENDPOINT = "https://artwork.shuttlemusicplayer.app/api/v1/artwork"

        // Fixed until the notification listener supplies real metadata (a4e011).
        const val ALBUM_ARTIST = "Radiohead"
        const val ALBUM = "In Rainbows"

        /**
         * Album artist plus album, per ADR-0002. Ticket 37a746 owns the real
         * type and its serialisation; this is the placeholder's stand-in.
         */
        fun albumKey(albumArtist: String, album: String) = "$albumArtist|$album"

        /** Omitting the album yields artist art rather than a cover. */
        fun artworkUri(albumArtist: String, album: String?): Uri =
            Uri.parse(ARTWORK_ENDPOINT).buildUpon()
                .appendQueryParameter("artist", albumArtist)
                .apply { if (album != null) appendQueryParameter("album", album) }
                .build()
    }
}
