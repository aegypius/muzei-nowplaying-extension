package com.aegypius.muzei.nowplaying

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * The one thread everything that writes to Muzei or to this app's own state runs on.
 *
 * setArtwork replaces the whole table, so two publishes racing on different threads
 * can leave the stale album as the winner. That applies across components, not only
 * within one: blocking a player from settings publishes the album it displaced while
 * the listener may still be publishing for that same player.
 *
 * The seen-players list is serialised here too, because it is read-modify-written
 * and successive session changes would otherwise lose an entry.
 */
object Publishing {
    val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
}
