package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PublishArtistFallbackTest {

    private val key = AlbumKey.of(track(albumArtist = "Radiohead", album = "In Rainbows"))!!

    @Test
    fun `retries without the album and remembers what it published`() {
        val publisher = RecordingPublisher()
        val store = InMemoryLastAlbum(key.token)

        PublishArtistFallback(store, publisher).after(failedUrl = ArtworkUrl.of(key), key = key)

        val (published, url) = publisher.published.single()
        assertEquals(key.withoutAlbum(), published)
        assertEquals(ArtworkUrl.of(key.withoutAlbum()), url)
        // Otherwise every cold start restores the album key that just missed, and
        // misses again.
        assertEquals(key.withoutAlbum().token, store.saved)
    }

    @Test
    fun `the retry keeps the player and the album it displaced`() {
        val publisher = RecordingPublisher()
        val spotify = Player("com.spotify.music")
        val displaced = AlbumKey.of(track(albumArtist = "Amenra", album = "Mass VI"))!!
        val store = InMemoryLastAlbum()
        store.save(PublishedAlbum(key.token, spotify, displaced = displaced.token))

        PublishArtistFallback(store, publisher).after(failedUrl = ArtworkUrl.of(key), key = key)

        // A repair of the publish that just missed, not a new album arriving:
        // blocking the player should still put back what it originally took.
        assertEquals(spotify, store.savedAlbum?.player)
        assertEquals(displaced.token, store.savedAlbum?.displaced)
    }

    @Test
    fun `publishes nothing when there is nothing left to try`() {
        val publisher = RecordingPublisher()
        val artistOnly = ArtworkUrl.of(key.withoutAlbum())

        PublishArtistFallback(InMemoryLastAlbum(key.token), publisher)
            .after(failedUrl = artistOnly, key = key)

        assertTrue(publisher.published.isEmpty())
    }
}
