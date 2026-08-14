package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AlbumKeyTest {

    @Test
    fun `album artist is preferred over the track artist`() {
        val key = AlbumKey.of(track(albumArtist = "Various Artists", artist = "Portishead", album = "Now 42"))!!

        assertEquals("Various Artists", key.albumArtist)
    }

    @Test
    fun `falls back to the track artist when no album artist is tagged`() {
        val key = AlbumKey.of(track(artist = "Portishead", album = "Dummy"))!!

        assertEquals("Portishead", key.albumArtist)
    }

    @Test
    fun `distinct keys never share a token, even when a name contains the separator`() {
        // A naive "artist|album" join maps both of these to "A|B|C", so Muzei would
        // treat two different albums as the same artwork and skip the second.
        val separatorInArtist = AlbumKey.of(track(albumArtist = "A|B", album = "C"))!!
        val separatorInAlbum = AlbumKey.of(track(albumArtist = "A", album = "B|C"))!!

        assertNotEquals(separatorInAlbum.token, separatorInArtist.token)
    }
}
