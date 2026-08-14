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

    // Chosen by the person whose wallpaper this is. The only technical requirement
    // is that the artwork service actually has them: each was verified to return an
    // album cover for the exact URL this app builds, apostrophe encoding included.
    //
    // Note that a 200 does not by itself mean a good cover. A misspelled album title
    // was measured returning a 5 kB image where the correct one returns 67 kB, and
    // nothing in the app can tell those apart, since only a non-2xx triggers the
    // artist fallback. So spellings here are canonical, not casual.
    val all: List<AlbumKey> = listOf(
        albumKey("Guns N' Roses", "Appetite for Destruction"),
        albumKey("Carpenter Brut", "Trilogy"),
        albumKey("Amenra", "Mass VI"),
        albumKey("Massive Attack", "Mezzanine"),
        albumKey("Pink Floyd", "The Dark Side of the Moon"),
    )

    fun random(random: Random = Random.Default): AlbumKey = all.random(random)

    private fun albumKey(albumArtist: String, album: String): AlbumKey = requireNotNull(
        AlbumKey.of(Track(title = null, artist = null, albumArtist = albumArtist, album = album)),
    ) { "sample album $albumArtist / $album must yield a key" }
}
