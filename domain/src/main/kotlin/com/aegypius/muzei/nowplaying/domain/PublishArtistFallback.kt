package com.aegypius.muzei.nowplaying.domain

/**
 * Recovers from a miss by asking for artist art instead of a cover.
 *
 * Published under the album-less key, which is what the retry actually looks up:
 * reusing the failed artwork's token would make Muzei skip it as already published.
 * The remembered album is updated to match, so a restart restores what is on screen
 * rather than the album key that just failed.
 *
 * See docs/adr/0001-remote-only-artwork.md.
 */
class PublishArtistFallback(
    private val lastAlbum: LastAlbum,
    private val publisher: ArtworkPublisher,
) {
    fun after(failedUrl: String, key: AlbumKey) {
        val next = ArtistFallback.after(failedUrl = failedUrl, key = key) ?: return
        val artistOnly = key.withoutAlbum()

        publisher.publish(artistOnly, next)
        lastAlbum.save(artistOnly.token)
    }
}
