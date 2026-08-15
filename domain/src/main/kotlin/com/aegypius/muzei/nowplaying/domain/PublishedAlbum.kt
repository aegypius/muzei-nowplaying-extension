package com.aegypius.muzei.nowplaying.domain

/**
 * The album on the wallpaper: what it is, which player put it there, and the album
 * it displaced.
 *
 * Exactly one step of history, not a list. It exists so that blocking a player can
 * put back what that player took, at the moment the user reaches for the setting,
 * rather than leaving the wrong wallpaper up until something else plays.
 *
 * @param player null when nothing playing put it there: a restored album or a
 * sample is published by this app itself, and no player can be blocked for it.
 */
data class PublishedAlbum(
    val token: String,
    val player: Player? = null,
    val displaced: String? = null,
) {

    /**
     * The record that replaces this one.
     *
     * Republishing the same album displaces nothing: every track of a record
     * publishes the same album key, and treating that as a change would make an
     * album displace itself, so blocking would restore what is already on screen.
     */
    fun replacedBy(token: String, player: Player?): PublishedAlbum = when (token) {
        this.token -> copy(player = player)
        else -> PublishedAlbum(token, player, displaced = this.token)
    }

    /**
     * What to publish instead when [player] is blocked, or null when there is
     * nothing to put back — either the wallpaper did not come from that player, or
     * it displaced nothing.
     *
     * The result carries no player and no history of its own: this app is putting
     * it back, so there is nothing further to undo.
     */
    fun afterBlocking(player: Player): PublishedAlbum? = displaced
        ?.takeIf { this.player == player }
        ?.let { PublishedAlbum(it) }
}
