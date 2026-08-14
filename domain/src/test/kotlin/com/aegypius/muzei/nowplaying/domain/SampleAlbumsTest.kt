package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SampleAlbumsTest {

    @Test
    fun `every sample names an album and builds a usable lookup`() {
        assertTrue(SampleAlbums.all.size > 1, "one sample would never vary")

        SampleAlbums.all.forEach { key ->
            assertNotNull(key.album, "sample ${key.albumArtist} has no album")
            assertTrue(
                ArtworkUrl.of(key).startsWith("https://artwork.shuttlemusicplayer.app"),
                "unusable lookup for ${key.token}",
            )
        }
    }
}
