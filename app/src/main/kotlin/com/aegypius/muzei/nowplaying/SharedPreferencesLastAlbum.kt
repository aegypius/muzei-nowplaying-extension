package com.aegypius.muzei.nowplaying

import android.content.Context
import com.aegypius.muzei.nowplaying.domain.LastAlbum

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
     */
    override fun save(token: String) {
        preferences.edit().putString(KEY_TOKEN, token).commit()
    }

    override fun load(): String? = preferences.getString(KEY_TOKEN, null)

    private companion object {
        const val PREFERENCES_NAME = "now_playing"
        const val KEY_TOKEN = "last_album_token"
    }
}
