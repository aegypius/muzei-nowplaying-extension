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

    @Test
    fun `remembers which player published it, and what that displaced`() = runTest {
        val publisher = RecordingPublisher()
        val lastAlbum = InMemoryLastAlbum()
        val dispatcher = CountingDispatcher(StandardTestDispatcher(testScheduler))
        val publish = PublishNowPlaying(publisher, lastAlbum, dispatcher)
        val spotify = Player("com.spotify.music")
        val youtube = Player("com.google.android.youtube")

        publish.publish(track(albumArtist = "Amenra", album = "Mass VI"), spotify)
        val massVI = lastAlbum.saved
        publish.publish(track(artist = "Amenra"), youtube)

        // Blocking YouTube has to know both that YouTube put this up and what it
        // replaced, or there is nothing to put back.
        assertEquals(youtube, lastAlbum.savedAlbum?.player)
        assertEquals(massVI, lastAlbum.savedAlbum?.displaced)
    }

    @Test
    fun `a closed gate publishes nothing and remembers nothing`() = runTest {
        val publisher = RecordingPublisher()
        val lastAlbum = InMemoryLastAlbum()
        val dispatcher = CountingDispatcher(StandardTestDispatcher(testScheduler))

        PublishNowPlaying(publisher, lastAlbum, dispatcher, gate = { false })
            .publish(track(albumArtist = "Radiohead", album = "In Rainbows"))

        assertTrue(publisher.published.isEmpty())
        // Nothing was shown, so there is nothing for a restart to restore.
        assertEquals(null, lastAlbum.saved)
    }
}
