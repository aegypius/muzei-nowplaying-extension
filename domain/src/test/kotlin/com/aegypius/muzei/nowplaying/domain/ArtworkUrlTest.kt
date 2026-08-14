package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ArtworkUrlTest {

    @Test
    fun `looks artwork up by album artist and album`() {
        val key = AlbumKey.of(track(albumArtist = "Radiohead", album = "In Rainbows"))!!

        // Verified against the live service: this exact URL answers 200 image/webp.
        assertEquals(
            "https://artwork.shuttlemusicplayer.app/api/v1/artwork" +
                "?artist=Radiohead&album=In%20Rainbows",
            ArtworkUrl.of(key),
        )
    }

    @Test
    fun `omits the album entirely when none is tagged, which yields artist art`() {
        val key = AlbumKey.of(track(albumArtist = "Radiohead"))!!

        // Measured: the endpoint answers artist art for an album-less request, and
        // rejects an empty album parameter with 400. So it must be absent, not blank.
        assertEquals(
            "https://artwork.shuttlemusicplayer.app/api/v1/artwork?artist=Radiohead",
            ArtworkUrl.of(key),
        )
    }

    @Test
    fun `percent-encodes characters that would otherwise change the query`() {
        val key = AlbumKey.of(track(albumArtist = "Simon & Garfunkel", album = "Bookends?"))!!

        assertEquals(
            "https://artwork.shuttlemusicplayer.app/api/v1/artwork" +
                "?artist=Simon%20%26%20Garfunkel&album=Bookends%3F",
            ArtworkUrl.of(key),
        )
    }
}
