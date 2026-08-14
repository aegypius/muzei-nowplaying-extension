package com.aegypius.muzei.nowplaying.domain

/** Builds a Track for tests, so each one names only the fields it cares about. */
internal fun track(
    title: String? = null,
    artist: String? = null,
    albumArtist: String? = null,
    album: String? = null,
) = Track(title = title, artist = artist, albumArtist = albumArtist, album = album)
