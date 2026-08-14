package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class PublishNowPlayingTest {

    @Test
    fun `publishes the album key and its lookup url`() = runTest {
        val publisher = RecordingPublisher()
        val dispatcher = CountingDispatcher(StandardTestDispatcher(testScheduler))

        PublishNowPlaying(publisher, InMemoryLastAlbum(), dispatcher).publish(
            track(albumArtist = "Radiohead", album = "In Rainbows"),
        )

        val (key, url) = publisher.published.single()
        assertEquals("In Rainbows", key.album)
        assertEquals(
            "https://artwork.shuttlemusicplayer.app/api/v1/artwork" +
                "?artist=Radiohead&album=In%20Rainbows",
            url,
        )
        // setArtwork does database I/O and callbacks arrive on the main thread, so
        // the work must go through the injected dispatcher rather than run inline.
        assertTrue(dispatcher.dispatched > 0)
    }

    @Test
    fun `publishes nothing when the track names no artist at all`() = runTest {
        val publisher = RecordingPublisher()
        val dispatcher = CountingDispatcher(StandardTestDispatcher(testScheduler))

        PublishNowPlaying(publisher, InMemoryLastAlbum(), dispatcher)
            .publish(track(title = "Unknown"))

        // Replacing good artwork with a lookup that cannot succeed is worse than
        // leaving the wallpaper alone.
        assertTrue(publisher.published.isEmpty())
    }

    @Test
    fun `publishes the artist-only lookup when no album is tagged`() = runTest {
        val publisher = RecordingPublisher()
        val dispatcher = CountingDispatcher(StandardTestDispatcher(testScheduler))

        PublishNowPlaying(publisher, InMemoryLastAlbum(), dispatcher)
            .publish(track(artist = "Radiohead"))

        val (_, url) = publisher.published.single()
        assertEquals(
            "https://artwork.shuttlemusicplayer.app/api/v1/artwork?artist=Radiohead",
            url,
        )
    }

    @Test
    fun `remembers what it published, so a restart can restore it`() = runTest {
        val publisher = RecordingPublisher()
        val lastAlbum = InMemoryLastAlbum()
        val dispatcher = CountingDispatcher(StandardTestDispatcher(testScheduler))

        PublishNowPlaying(publisher, lastAlbum, dispatcher).publish(
            track(albumArtist = "Radiohead", album = "In Rainbows"),
        )

        assertEquals(publisher.published.single().first.token, lastAlbum.saved)
    }
}
