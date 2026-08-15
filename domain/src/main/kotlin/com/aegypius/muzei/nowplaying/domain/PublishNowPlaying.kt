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
    private val gate: PublishGate = PublishGate { true },
) {
    /**
     * @param player which app is playing, so that blocking it later can put back
     * the album this one displaces. Null when that is unknown, which costs only the
     * ability to undo this particular publish.
     */
    suspend fun publish(track: Track, player: Player? = null) {
        // No artist of any kind means there is nothing to look up. Publishing a
        // meaningless request would replace good artwork with a miss.
        val key = AlbumKey.of(track) ?: return

        // Checked here rather than at the callback, so the answer is current: a
        // connection can change between a track starting and its artwork being
        // wanted. Nothing is remembered either, since nothing was shown.
        if (!gate.allowsPublishing()) return
        withContext(dispatcher) {
            publisher.publish(key, ArtworkUrl.of(key))
            // Remembered after publishing, not before: nothing should be restored
            // that was never shown.
            val showing = lastAlbum.load()
            lastAlbum.save(
                showing?.replacedBy(key.token, player) ?: PublishedAlbum(key.token, player),
            )
        }
    }
}
