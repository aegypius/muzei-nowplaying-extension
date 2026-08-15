package com.aegypius.muzei.nowplaying.domain

/**
 * The app a media session belongs to, identified by its package name.
 *
 * A type rather than a bare string because it travels through storage, settings
 * and arbitration, and at each of those a package name is easy to confuse with an
 * album token or a label. See CONTEXT.md.
 */
@JvmInline
value class Player(val packageName: String)

/**
 * The players the user has excluded. Every player is allowed until blocked, so an
 * empty set allows everything.
 *
 * A blocked player is refused ownership rather than refused publishing: gating only
 * the publish would let it become the winning session, after which the app actually
 * playing music is ignored for as long as the blocked one lives.
 */
@JvmInline
value class BlockedPlayers(private val blocked: Set<Player> = emptySet()) {

    fun allows(player: Player?): Boolean = player == null || player !in blocked
}
