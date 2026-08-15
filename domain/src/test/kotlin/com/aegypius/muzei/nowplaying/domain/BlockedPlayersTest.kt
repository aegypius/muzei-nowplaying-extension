package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockedPlayersTest {

    @Test
    fun `every player is allowed when nothing is blocked`() {
        val blocked = BlockedPlayers()

        assertTrue(blocked.allows(Player("com.google.android.youtube")))
    }

    @Test
    fun `a blocked player is refused`() {
        val blocked = BlockedPlayers(setOf(Player("com.google.android.youtube")))

        assertFalse(blocked.allows(Player("com.google.android.youtube")))
    }

    @Test
    fun `blocking one player leaves the others alone`() {
        val blocked = BlockedPlayers(setOf(Player("com.google.android.youtube")))

        assertTrue(blocked.allows(Player("com.spotify.music")))
    }

    @Test
    fun `an unknown player is allowed`() {
        // Artwork restored after a restart has no player: it was published by this
        // app, not by anything playing. Refusing it would leave the wallpaper blank.
        val blocked = BlockedPlayers(setOf(Player("com.google.android.youtube")))

        assertTrue(blocked.allows(null))
    }
}
