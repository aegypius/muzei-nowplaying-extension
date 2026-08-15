package com.aegypius.muzei.nowplaying.domain

/**
 * Puts back what a newly blocked player displaced.
 *
 * Blocking is usually done while looking at the wallpaper the blocked player put
 * there, so the setting has to change what is on screen rather than only what
 * happens next. When there is nothing to put back — the wallpaper came from
 * somewhere else, or it displaced nothing — the wallpaper is left alone, which is
 * still better than replacing it with something arbitrary.
 */
class PublishDisplacedAlbum(
    private val lastAlbum: LastAlbum,
    private val publisher: ArtworkPublisher,
) {
    fun after(blocked: Player) {
        val restored = lastAlbum.load()?.afterBlocking(blocked) ?: return
        val key = AlbumKey.fromToken(restored.token) ?: return

        publisher.publish(key, ArtworkUrl.of(key))
        lastAlbum.save(restored)
    }
}
