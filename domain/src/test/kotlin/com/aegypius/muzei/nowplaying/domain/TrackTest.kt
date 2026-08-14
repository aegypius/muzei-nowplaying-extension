package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrackTest {

    @Test
    fun `a session reporting no artist at all yields no album key`() {
        val track = Track(title = "Unknown", artist = null, albumArtist = null, album = null)

        assertNull(AlbumKey.of(track))
    }

    @Test
    fun `blank fields count as absent`() {
        // Measured: the service answers 400 for an empty album parameter, and a
        // blank artist would look up nothing at all. Sessions do report "".
        val key = AlbumKey.of(track(albumArtist = "  ", artist = "Portishead", album = ""))

        assertEquals("Portishead", key?.albumArtist)
        assertNull(key?.album)
    }
}
