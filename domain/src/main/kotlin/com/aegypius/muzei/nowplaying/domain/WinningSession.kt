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
 *
 * @param eligible whether a session may own the wallpaper at all. Asked at the
 * moment a session starts playing rather than stored, so that blocking a player
 * takes effect on the very next thing it plays. The predicate is over sessions,
 * not players, because that is what this class identifies; the caller knows which
 * player a session belongs to.
 */
class WinningSession<SESSION>(private val eligible: (SESSION) -> Boolean = { true }) {

    private var winner: SESSION? = null

    /**
     * An ineligible session does not merely fail to win: it leaves the current
     * winner untouched. Clearing ownership instead would let a blocked player
     * silence the app that is actually playing music.
     */
    fun startedPlaying(session: SESSION) {
        if (!eligible(session)) return
        winner = session
    }

    /**
     * The winner went away. Ownership is not inherited: nothing owns the wallpaper
     * until some session starts playing again, so the last artwork stays put.
     */
    fun destroyed(session: SESSION) {
        if (winner == session) winner = null
    }

    /**
     * Eligibility is asked here too, not only when a session starts playing. A
     * player can be blocked while its session is already the winner and still
     * playing, and nothing would come along to displace it: ownership has to end
     * with the block, or the next thing that session reports goes to the wallpaper.
     */
    fun owns(session: SESSION): Boolean = winner == session && eligible(session)
}
