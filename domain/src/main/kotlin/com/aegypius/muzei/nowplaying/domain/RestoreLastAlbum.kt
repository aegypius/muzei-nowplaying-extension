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
        val showing = lastAlbum.load()
        val remembered = showing?.token?.let(AlbumKey::fromToken)
        val key = remembered ?: sample()

        publisher.publish(key, ArtworkUrl.of(key))

        // Remembered, including a sample. Muzei calls this whenever it wants more
        // artwork, not only the first time, so an unremembered sample would be
        // reshuffled on every call and turn the wallpaper into a slideshow.
        //
        // Restoring is not a new album arriving: what is put back keeps the player
        // and the displaced album it already had, and a sample published in place of
        // an unreadable record starts a fresh history rather than claiming to
        // displace something nobody can read.
        lastAlbum.save(
            showing?.takeIf { remembered != null } ?: PublishedAlbum(key.token),
        )
    }
}
