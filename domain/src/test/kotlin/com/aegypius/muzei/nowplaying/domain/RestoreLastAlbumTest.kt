package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RestoreLastAlbumTest {

    @Test
    fun `publishes the album that was playing before the restart`() {
        val remembered = AlbumKey.of(track(albumArtist = "Radiohead", album = "In Rainbows"))!!
        val publisher = RecordingPublisher()

        RestoreLastAlbum(InMemoryLastAlbum(remembered.token), publisher).publish()

        assertEquals(remembered, publisher.published.single().first)
    }

    @Test
    fun `publishes a sample album when nothing has ever played`() {
        val sample = AlbumKey.of(track(albumArtist = "Portishead", album = "Dummy"))!!
        val publisher = RecordingPublisher()

        // Deliberately goes through the real lookup rather than a bundled image, so
        // a broken pipeline shows up on first run instead of looking like "no music
        // detected".
        RestoreLastAlbum(InMemoryLastAlbum(), publisher, sample = { sample }).publish()

        val (key, url) = publisher.published.single()
        assertEquals(sample, key)
        assertEquals(ArtworkUrl.of(sample), url)
    }

    @Test
    fun `a remembered album that no longer parses falls back to a sample`() {
        val sample = AlbumKey.of(track(albumArtist = "Portishead", album = "Dummy"))!!
        val publisher = RecordingPublisher()

        // Written by an older token format, or corrupted. Better a sample than a
        // wallpaper that stays empty.
        RestoreLastAlbum(InMemoryLastAlbum("not a token"), publisher, sample = { sample })
            .publish()

        assertEquals(sample, publisher.published.single().first)
    }

    @Test
    fun `remembers the sample it published, so it does not reshuffle`() {
        val sample = AlbumKey.of(track(albumArtist = "Portishead", album = "Dummy"))!!
        val store = InMemoryLastAlbum()

        // onLoadRequested fires whenever Muzei wants more artwork, not only the first
        // time. Without remembering the sample, every call would pick a different
        // album and the wallpaper would become a random slideshow -- which ADR-0003
        // exists to prevent.
        RestoreLastAlbum(store, RecordingPublisher(), sample = { sample }).publish()

        assertEquals(sample.token, store.saved)
    }
}
