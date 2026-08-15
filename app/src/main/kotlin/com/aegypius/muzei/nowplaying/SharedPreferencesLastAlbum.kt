package com.aegypius.muzei.nowplaying

import android.content.Context
import com.aegypius.muzei.nowplaying.domain.LastAlbum
import com.aegypius.muzei.nowplaying.domain.Player
import com.aegypius.muzei.nowplaying.domain.PublishedAlbum

/**
 * Persists the last published album so the provider can restore it after a restart.
 *
 * The provider and the listener service run in the same process, so a plain
 * SharedPreferences is enough; nothing here needs cross-process visibility.
 */
class SharedPreferencesLastAlbum(context: Context) : LastAlbum {

    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * commit() rather than apply(): surviving process death is the entire point of
     * writing this down, and apply() can lose the value if the process dies before
     * its background write lands. The caller is already on an IO dispatcher.
     *
     * The three values are written together, since a record naming a player that
     * does not match its token would restore the wrong album on the next block.
     */
    override fun save(album: PublishedAlbum) {
        preferences.edit()
            .putString(KEY_TOKEN, album.token)
            .putString(KEY_PLAYER, album.player?.packageName)
            .putString(KEY_DISPLACED, album.displaced)
            .commit()
    }

    /**
     * A record written before this app knew about players has a token and nothing
     * else, which reads back as an album whose player is unknown — exactly what it
     * is. Nothing needs migrating.
     */
    override fun load(): PublishedAlbum? {
        val token = preferences.getString(KEY_TOKEN, null) ?: return null
        return PublishedAlbum(
            token = token,
            player = preferences.getString(KEY_PLAYER, null)?.let(::Player),
            displaced = preferences.getString(KEY_DISPLACED, null),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "now_playing"
        const val KEY_TOKEN = "last_album_token"
        const val KEY_PLAYER = "last_album_player"
        const val KEY_DISPLACED = "last_album_displaced"
    }
}
