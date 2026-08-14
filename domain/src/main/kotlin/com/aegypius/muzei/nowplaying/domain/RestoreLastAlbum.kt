package com.aegypius.muzei.nowplaying.domain

/**
 * Answers Muzei's request for artwork when nothing is playing in this process.
 *
 * Not a suspend function: Muzei calls the provider on a binder thread, already off
 * the main thread, so there is no dispatcher to hop to.
 */
class RestoreLastAlbum(
    private val lastAlbum: LastAlbum,
    private val publisher: ArtworkPublisher,
    private val sample: () -> AlbumKey = { SampleAlbums.random() },
) {
    fun publish() {
        // A remembered album that no longer parses is treated as no album at all:
        // better a sample than nothing, and the token format could have changed
        // since it was written.
        val key = lastAlbum.load()?.let(AlbumKey::fromToken) ?: sample()

        publisher.publish(key, ArtworkUrl.of(key))

        // Remembered, including a sample. Muzei calls this whenever it wants more
        // artwork, not only the first time, so an unremembered sample would be
        // reshuffled on every call and turn the wallpaper into a slideshow.
        lastAlbum.save(key.token)
    }
}
