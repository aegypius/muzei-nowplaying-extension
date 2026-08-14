package com.aegypius.muzei.nowplaying.domain

/**
 * Remembers the album last published, so it can be restored when Muzei asks for
 * artwork in a process where nothing has played yet.
 *
 * This is the only thing connecting the two components: the listener writes what is
 * playing, and the provider reads it back — possibly after a restart, in a process
 * where the listener never ran.
 *
 * A token rather than an album key, so the storage side needs no knowledge of the
 * format. AlbumKey owns both writing and parsing it.
 */
interface LastAlbum {
    fun save(token: String)
    fun load(): String?
}
