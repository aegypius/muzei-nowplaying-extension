package com.aegypius.muzei.nowplaying.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Hands a Now Playing to whatever can show it. Implemented at the Muzei boundary. */
fun interface ArtworkPublisher {
    fun publish(key: AlbumKey, artworkUrl: String)
}

/**
 * Publishes the album a track belongs to, or nothing at all.
 *
 * The dispatcher is injected because publishing does database I/O while the
 * metadata callbacks that trigger it arrive on the main thread.
 */
class PublishNowPlaying(
    private val publisher: ArtworkPublisher,
    private val lastAlbum: LastAlbum,
    private val dispatcher: CoroutineDispatcher,
) {
    suspend fun publish(track: Track) {
        // No artist of any kind means there is nothing to look up. Publishing a
        // meaningless request would replace good artwork with a miss.
        val key = AlbumKey.of(track) ?: return
        withContext(dispatcher) {
            publisher.publish(key, ArtworkUrl.of(key))
            // Remembered after publishing, not before: nothing should be restored
            // that was never shown.
            lastAlbum.save(key.token)
        }
    }
}
