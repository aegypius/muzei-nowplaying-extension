package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class AlbumKeyTest {

    @Test
    fun `album artist is preferred over the track artist`() {
        val key = AlbumKey.of(
            track(albumArtist = "Various Artists", artist = "Portishead", album = "Now 42"),
        )!!

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

    @Test
    fun `a token round-trips back into the key that produced it`() {
        // Muzei hands back only the token when it reports a failed artwork, so the
        // fallback has to rebuild the key from it. Colons are the token's own
        // delimiter, so a name containing one is the case that matters.
        val original = AlbumKey.of(track(albumArtist = "A:B", album = "C:D"))!!

        assertEquals(original, AlbumKey.fromToken(original.token))
    }

    @Test
    fun `an album-less token round-trips without inventing an album`() {
        val original = AlbumKey.of(track(albumArtist = "Radiohead"))!!

        assertEquals(original, AlbumKey.fromToken(original.token))
    }

    @Test
    fun `an unparseable token yields no key`() {
        assertNull(AlbumKey.fromToken("not a token"))
    }

    @Test
    fun `a token with a negative length is rejected rather than crashing`() {
        // A hand-rolled parser is only safe if it refuses everything it did not
        // write; this input previously reached substring(3, 2) and threw.
        assertNull(AlbumKey.fromToken("-1:1:X"))
    }

    @Test
    fun `a token with trailing junk is rejected`() {
        val valid = AlbumKey.of(track(albumArtist = "Radiohead"))!!.token

        assertNull(AlbumKey.fromToken(valid + "extra"))
    }
}
