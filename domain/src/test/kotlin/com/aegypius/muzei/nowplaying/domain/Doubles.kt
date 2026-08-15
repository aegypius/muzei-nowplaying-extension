package com.aegypius.muzei.nowplaying.domain

/** Records what was published, so a test can assert on it. */
internal class RecordingPublisher : ArtworkPublisher {
    val published = mutableListOf<Pair<AlbumKey, String>>()

    override fun publish(key: AlbumKey, artworkUrl: String) {
        published += key to artworkUrl
    }
}

/**
 * An in-memory stand-in for the persisted last album.
 *
 * Takes a bare token, since most tests care only about which album was remembered;
 * those that also care about the player or what it displaced use [savedAlbum].
 */
internal class InMemoryLastAlbum(token: String? = null) : LastAlbum {
    private var album: PublishedAlbum? = token?.let { PublishedAlbum(it) }

    val saved: String? get() = album?.token
    val savedAlbum: PublishedAlbum? get() = album

    override fun save(album: PublishedAlbum) {
        this.album = album
    }

    override fun load(): PublishedAlbum? = album
}
