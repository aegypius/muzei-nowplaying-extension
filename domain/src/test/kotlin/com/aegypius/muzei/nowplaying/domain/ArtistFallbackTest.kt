package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArtistFallbackTest {

    @Test
    fun `a failed album lookup retries without the album`() {
        val key = AlbumKey.of(track(albumArtist = "Radiohead", album = "In Rainbows"))!!

        assertEquals(
            "https://artwork.shuttlemusicplayer.app/api/v1/artwork?artist=Radiohead",
            ArtistFallback.after(failedUrl = ArtworkUrl.of(key), key = key),
        )
    }

    @Test
    fun `a failed artist lookup gives up rather than retrying itself`() {
        val key = AlbumKey.of(track(albumArtist = "Radiohead", album = "In Rainbows"))!!
        val artistOnly = ArtworkUrl.of(key.withoutAlbum())

        // Without this the provider would republish the same failing URL forever.
        assertNull(ArtistFallback.after(failedUrl = artistOnly, key = key))
    }

    @Test
    fun `an album-less key has nothing left to fall back to`() {
        val key = AlbumKey.of(track(albumArtist = "Radiohead"))!!

        assertNull(ArtistFallback.after(failedUrl = ArtworkUrl.of(key), key = key))
    }
}
