package com.aegypius.muzei.nowplaying.domain

/**
 * Remembers the album last published, so it can be restored when Muzei asks for
 * artwork in a process where nothing has played yet.
 *
 * This is the only thing connecting the two components: the listener writes what is
 * playing, and the provider reads it back — possibly after a restart, in a process
 * where the listener never ran.
 *
 * A PublishedAlbum rather than a bare token, because blocking a player has to know
 * which player put the current album up and what that album displaced. The token
 * inside it stays opaque to the storage side: AlbumKey owns both writing and
 * parsing it.
 */
interface LastAlbum {
    fun save(album: PublishedAlbum)
    fun load(): PublishedAlbum?
}
