package com.aegypius.muzei.nowplaying.domain

/**
 * Tracks which media session is allowed to change the wallpaper.
 *
 * Several sessions are alive at once — a paused player, a podcast, a video — and
 * exactly one of them wins: the one that most recently started playing.
 *
 * Sessions are identified by an opaque handle, so this module never sees a
 * MediaController. The caller must use something that identifies a *session*: an
 * app's package name is not enough, because one app can own two at once.
 */
class WinningSession<SESSION> {

    private var winner: SESSION? = null

    fun startedPlaying(session: SESSION) {
        winner = session
    }

    /**
     * The winner went away. Ownership is not inherited: nothing owns the wallpaper
     * until some session starts playing again, so the last artwork stays put.
     */
    fun destroyed(session: SESSION) {
        if (winner == session) winner = null
    }

    fun owns(session: SESSION): Boolean = winner == session
}
