package com.aegypius.muzei.nowplaying

import android.util.Log
import com.aegypius.muzei.nowplaying.domain.AlbumKey
import com.aegypius.muzei.nowplaying.domain.ArtistFallback
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
     * Muzei is asking for more artwork, and there is nothing to give it.
     *
     * The wallpaper changes when the music changes, not when Muzei asks. Until
     * something plays, the previous artwork stays, which is what ADR-0003 wants.
     * Republishing the last album across a restart is ticket 504243.
     */
    override fun onLoadRequested(initial: Boolean) = Unit

    /**
     * Muzei could not load the artwork.
     *
     * The inherited default deletes it, which for a single-artwork provider leaves
     * the wallpaper with nothing at all — the API's own documentation warns about
     * exactly that. What to try next, and when to stop, is ArtistFallback's
     * decision.
     */
    override fun onInvalidArtwork(artwork: Artwork) {
        val failedUrl = artwork.persistentUri?.toString() ?: return
        val key = artwork.token?.let(AlbumKey::fromToken) ?: return

        val next = ArtistFallback.after(failedUrl = failedUrl, key = key)
        if (next == null) {
            // Deliberately not calling super: keeping a stale cover beats emptying
            // the provider. See docs/adr/0003-sticky-when-idle.md.
            Log.i(TAG, "nothing left to try for ${artwork.token}; keeping current artwork")
            return
        }

        // Published under the album-less key, which is what the retry actually
        // looks up. Reusing the failed artwork's token would make Muzei skip it as
        // already published, so the fallback would never reach the wallpaper.
        setArtwork(artworkFor(key.withoutAlbum(), next))
    }

    private companion object {
        const val TAG = "NowPlayingArtProvider"
    }
}
