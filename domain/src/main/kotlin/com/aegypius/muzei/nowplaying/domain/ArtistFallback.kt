package com.aegypius.muzei.nowplaying.domain

/**
 * The retry after a miss: ask for the album artist alone, which the service
 * answers with artist art rather than a cover.
 *
 * See docs/adr/0001-remote-only-artwork.md.
 */
object ArtistFallback {

    fun after(failedUrl: String, key: AlbumKey): String? {
        val artistOnly = ArtworkUrl.of(key.withoutAlbum())
        return artistOnly.takeIf { it != failedUrl }
    }
}
