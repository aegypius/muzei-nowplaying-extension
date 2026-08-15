package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ownership when some sessions may not have it at all.
 *
 * The predicate is over sessions rather than players because that is what this
 * class identifies. The caller knows which player a session belongs to; here a
 * session is simply eligible or not.
 */
class WinningSessionEligibilityTest {

    private val blockedSession = "youtube-session"

    private fun winningSession() = WinningSession<String> { it != blockedSession }

    @Test
    fun `an ineligible session never owns the wallpaper`() {
        val winning = winningSession()

        winning.startedPlaying(blockedSession)

        assertFalse(winning.owns(blockedSession))
    }

    @Test
    fun `an ineligible session leaves the current winner in place`() {
        val winning = winningSession()
        winning.startedPlaying("spotify-session")

        // You start a video while music is paused. The wallpaper must not change,
        // and the music session must keep ownership so its next track still counts.
        winning.startedPlaying(blockedSession)

        assertTrue(winning.owns("spotify-session"))
    }

    @Test
    fun `a session that becomes ineligible while winning stops owning at once`() {
        // You block the app whose video is on the wallpaper. It is still playing and
        // still the winner, and nothing will start playing to displace it — so
        // ownership has to end the moment it is blocked, or its next chapter
        // change publishes the wrong cover all over again.
        var blocked = false
        val winning = WinningSession<String> { it != "youtube-session" || !blocked }
        winning.startedPlaying("youtube-session")

        blocked = true

        assertFalse(winning.owns("youtube-session"))
    }

    @Test
    fun `destroying an ineligible session leaves the winner alone`() {
        val winning = winningSession()
        winning.startedPlaying("spotify-session")
        winning.startedPlaying(blockedSession)

        winning.destroyed(blockedSession)

        assertTrue(winning.owns("spotify-session"))
    }
}
