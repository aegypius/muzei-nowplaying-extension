package com.aegypius.muzei.nowplaying.domain

/** Records what was published, so a test can assert on it. */
internal class RecordingPublisher : ArtworkPublisher {
    val published = mutableListOf<Pair<AlbumKey, String>>()

    override fun publish(key: AlbumKey, artworkUrl: String) {
        published += key to artworkUrl
    }
}

/** An in-memory stand-in for the persisted last album. */
internal class InMemoryLastAlbum(private var token: String? = null) : LastAlbum {
    val saved: String? get() = token

    override fun save(token: String) {
        this.token = token
    }

    override fun load(): String? = token
}
