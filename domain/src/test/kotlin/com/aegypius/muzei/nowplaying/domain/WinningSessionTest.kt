package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WinningSessionTest {

    @Test
    fun `the first session to start playing owns the wallpaper`() {
        val winning = WinningSession<String>()

        winning.startedPlaying("com.spotify.music")

        assertTrue(winning.owns("com.spotify.music"))
    }

    @Test
    fun `the session that starts playing most recently takes over`() {
        val winning = WinningSession<String>()

        winning.startedPlaying("com.spotify.music")
        winning.startedPlaying("com.podcast.app")

        assertFalse(winning.owns("com.spotify.music"))
        assertTrue(winning.owns("com.podcast.app"))
    }

    @Test
    fun `a session that has never played owns nothing`() {
        val winning = WinningSession<String>()

        winning.startedPlaying("com.spotify.music")

        assertFalse(winning.owns("com.browser.tab"))
    }

    @Test
    fun `destroying the winner leaves nothing owning the wallpaper`() {
        val winning = WinningSession<String>()
        winning.startedPlaying("com.spotify.music")
        winning.startedPlaying("com.podcast.app")

        // You close the podcast app. The paused Spotify session is still alive, but
        // it does not inherit the wallpaper by being the last one standing: the
        // last artwork simply stays until something plays again. See ADR-0003.
        winning.destroyed("com.podcast.app")

        assertFalse(winning.owns("com.podcast.app"))
        assertFalse(winning.owns("com.spotify.music"))
    }

    @Test
    fun `destroying a losing session leaves the winner alone`() {
        val winning = WinningSession<String>()
        winning.startedPlaying("com.spotify.music")

        winning.destroyed("com.browser.tab")

        assertTrue(winning.owns("com.spotify.music"))
    }
}
