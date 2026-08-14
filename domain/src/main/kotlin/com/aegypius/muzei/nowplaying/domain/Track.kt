package com.aegypius.muzei.nowplaying.domain

/**
 * One song's metadata as a media session reports it — the raw input, before it is
 * reduced to an album key. Every field is optional because sessions routinely
 * omit any of them.
 *
 * The title is carried for completeness and is deliberately unused: under an
 * album-level token there is nothing for it to change. See
 * docs/adr/0002-album-level-identity.md.
 */
data class Track(
    val title: String?,
    val artist: String?,
    val albumArtist: String?,
    val album: String?,
)
