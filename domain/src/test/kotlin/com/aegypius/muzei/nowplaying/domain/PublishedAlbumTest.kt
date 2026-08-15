package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PublishedAlbumTest {

    private val spotify = Player("com.spotify.music")
    private val youtube = Player("com.google.android.youtube")

    private val massVI = AlbumKey.of(track(albumArtist = "Amenra", album = "Mass VI"))!!.token
    private val homonym = AlbumKey.of(track(artist = "Amenra"))!!.token

    @Test
    fun `publishing an album records what it displaced`() {
        val showing = PublishedAlbum(massVI, spotify)

        val next = showing.replacedBy(homonym, youtube)

        assertEquals(homonym, next.token)
        assertEquals(youtube, next.player)
        assertEquals(massVI, next.displaced)
    }

    @Test
    fun `republishing the same album displaces nothing`() {
        // Every track of a record publishes the same album key. Treating that as a
        // change would make the album displace itself, and blocking would then
        // restore what is already on screen.
        val showing = PublishedAlbum(massVI, spotify, displaced = homonym)

        val next = showing.replacedBy(massVI, spotify)

        assertEquals(homonym, next.displaced)
    }

    @Test
    fun `blocking the player that put the album up restores what it displaced`() {
        val showing = PublishedAlbum(homonym, youtube, displaced = massVI)

        val restored = showing.afterBlocking(youtube)

        assertEquals(massVI, restored?.token)
    }

    @Test
    fun `the restored album has no player and displaces nothing`() {
        // It was put back by this app rather than by anything playing, so blocking
        // a second player must not claim to restore it a second time.
        val showing = PublishedAlbum(homonym, youtube, displaced = massVI)

        val restored = showing.afterBlocking(youtube)

        assertNull(restored?.player)
        assertNull(restored?.displaced)
    }

    @Test
    fun `blocking a player that did not put the album up changes nothing`() {
        val showing = PublishedAlbum(massVI, spotify, displaced = homonym)

        assertNull(showing.afterBlocking(youtube))
    }

    @Test
    fun `blocking restores nothing when the album displaced nothing`() {
        val showing = PublishedAlbum(homonym, youtube, displaced = null)

        assertNull(showing.afterBlocking(youtube))
    }
}
