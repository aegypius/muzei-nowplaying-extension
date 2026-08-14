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
    fun `publishes nothing when there is nothing left to try`() {
        val publisher = RecordingPublisher()
        val artistOnly = ArtworkUrl.of(key.withoutAlbum())

        PublishArtistFallback(InMemoryLastAlbum(key.token), publisher)
            .after(failedUrl = artistOnly, key = key)

        assertTrue(publisher.published.isEmpty())
    }
}
