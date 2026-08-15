package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
    fun `restoring keeps the player and the album it displaced`() {
        val youtube = Player("com.google.android.youtube")
        val displaced = AlbumKey.of(track(albumArtist = "Amenra", album = "Mass VI"))!!
        val remembered = AlbumKey.of(track(artist = "Amenra"))!!
        val store = InMemoryLastAlbum()
        store.save(PublishedAlbum(remembered.token, youtube, displaced = displaced.token))

        // Putting back what was already on screen is not a new album arriving, so
        // blocking after a restart must still know what to undo.
        RestoreLastAlbum(store, RecordingPublisher()).publish()

        assertEquals(youtube, store.savedAlbum?.player)
        assertEquals(displaced.token, store.savedAlbum?.displaced)
    }

    @Test
    fun `a sample published over an unreadable record starts a fresh history`() {
        val sample = AlbumKey.of(track(albumArtist = "Portishead", album = "Dummy"))!!
        val store = InMemoryLastAlbum()
        store.save(
            PublishedAlbum("not a token", Player("com.spotify.music"), displaced = "also not"),
        )

        RestoreLastAlbum(store, RecordingPublisher(), sample = { sample }).publish()

        // Claiming the sample displaced something nobody can parse would leave a
        // block restoring an album that cannot be looked up.
        assertEquals(sample.token, store.saved)
        assertNull(store.savedAlbum?.player)
        assertNull(store.savedAlbum?.displaced)
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
