package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PublishDisplacedAlbumTest {

    private val youtube = Player("com.google.android.youtube")
    private val spotify = Player("com.spotify.music")

    private val massVI = AlbumKey.of(track(albumArtist = "Amenra", album = "Mass VI"))!!
    private val homonym = AlbumKey.of(track(artist = "Amenra"))!!

    @Test
    fun `blocking the player that put the album up publishes what it displaced`() {
        val publisher = RecordingPublisher()
        val lastAlbum = InMemoryLastAlbum()
        lastAlbum.save(PublishedAlbum(homonym.token, youtube, displaced = massVI.token))

        PublishDisplacedAlbum(lastAlbum, publisher).after(youtube)

        val (key, url) = publisher.published.single()
        assertEquals(massVI, key)
        assertEquals(ArtworkUrl.of(massVI), url)
    }

    @Test
    fun `the restored album is remembered, so a restart shows it too`() {
        val publisher = RecordingPublisher()
        val lastAlbum = InMemoryLastAlbum()
        lastAlbum.save(PublishedAlbum(homonym.token, youtube, displaced = massVI.token))

        PublishDisplacedAlbum(lastAlbum, publisher).after(youtube)

        assertEquals(massVI.token, lastAlbum.saved)
    }

    @Test
    fun `blocking a player that did not put the album up publishes nothing`() {
        val publisher = RecordingPublisher()
        val lastAlbum = InMemoryLastAlbum()
        lastAlbum.save(PublishedAlbum(massVI.token, spotify, displaced = homonym.token))

        PublishDisplacedAlbum(lastAlbum, publisher).after(youtube)

        assertTrue(publisher.published.isEmpty())
        assertEquals(massVI.token, lastAlbum.saved)
    }

    @Test
    fun `blocking publishes nothing when nothing has ever been published`() {
        val publisher = RecordingPublisher()

        PublishDisplacedAlbum(InMemoryLastAlbum(), publisher).after(youtube)

        assertTrue(publisher.published.isEmpty())
    }

    @Test
    fun `a displaced album that no longer parses is left alone`() {
        // Written by an older token format. There is nothing to restore, and the
        // wallpaper keeps what it has rather than going blank.
        val publisher = RecordingPublisher()
        val lastAlbum = InMemoryLastAlbum()
        lastAlbum.save(PublishedAlbum(homonym.token, youtube, displaced = "not a token"))

        PublishDisplacedAlbum(lastAlbum, publisher).after(youtube)

        assertTrue(publisher.published.isEmpty())
        assertEquals(homonym.token, lastAlbum.saved)
    }
}
