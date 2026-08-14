package com.aegypius.muzei.nowplaying.domain

import kotlin.random.Random

/**
 * Albums shown before anything has played.
 *
 * Published through the ordinary lookup rather than as a bundled image, so the first
 * run exercises the whole path — key, URL, fetch, render — and a broken pipeline is
 * visible immediately instead of looking like "no music detected".
 *
 * The trade-off is that the caption names an album nobody played, and ADR-0002 fixes
 * that caption format, so it cannot be marked as a sample. See
 * docs/adr/0009-sample-album-before-anything-plays.md.
 */
object SampleAlbums {

    val all: List<AlbumKey> = listOf(
        albumKey("Radiohead", "In Rainbows"),
        albumKey("Portishead", "Dummy"),
        albumKey("Massive Attack", "Mezzanine"),
        albumKey("Pink Floyd", "The Dark Side of the Moon"),
        albumKey("Daft Punk", "Discovery"),
    )

    fun random(random: Random = Random.Default): AlbumKey = all.random(random)

    private fun albumKey(albumArtist: String, album: String): AlbumKey = requireNotNull(
        AlbumKey.of(Track(title = null, artist = null, albumArtist = albumArtist, album = album)),
    ) { "sample album $albumArtist / $album must yield a key" }
}
