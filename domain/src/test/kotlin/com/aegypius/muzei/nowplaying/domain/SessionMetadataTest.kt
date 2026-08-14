package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionMetadataTest {

    @Test
    fun `reads each field from the key the platform publishes it under`() {
        // Key strings verified against android.jar: javap -constants
        // android.media.MediaMetadata.
        val metadata = mapOf(
            "android.media.metadata.TITLE" to "Weird Fishes",
            "android.media.metadata.ARTIST" to "Radiohead",
            "android.media.metadata.ALBUM_ARTIST" to "Radiohead",
            "android.media.metadata.ALBUM" to "In Rainbows",
        )

        val track = SessionMetadata.readTrack(metadata::get)

        assertEquals(
            Track(
                title = "Weird Fishes",
                artist = "Radiohead",
                albumArtist = "Radiohead",
                album = "In Rainbows",
            ),
            track,
        )
    }

    @Test
    fun `a session publishing nothing yields an empty track, which has no album key`() {
        val track = SessionMetadata.readTrack { null }

        assertEquals(Track(title = null, artist = null, albumArtist = null, album = null), track)
        assertNull(AlbumKey.of(track))
    }

    @Test
    fun `a radio stream with only an artist still yields a usable key`() {
        val metadata = mapOf("android.media.metadata.ARTIST" to "Radiohead")

        val key = AlbumKey.of(SessionMetadata.readTrack(metadata::get))

        assertEquals("Radiohead", key?.albumArtist)
        assertNull(key?.album)
    }
}
