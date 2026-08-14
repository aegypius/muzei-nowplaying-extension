package com.aegypius.muzei.nowplaying.domain

/**
 * Reads a Track out of a media session's metadata.
 *
 * Takes a lookup function rather than an Android MediaMetadata, so which key
 * feeds which field is decided and tested here instead of at the Android
 * boundary. The caller passes `metadata::getString`.
 *
 * The key strings are Android's own public constants, read from android.jar
 * rather than transcribed: `javap -constants android.media.MediaMetadata`.
 */
object SessionMetadata {

    const val KEY_TITLE = "android.media.metadata.TITLE"
    const val KEY_ARTIST = "android.media.metadata.ARTIST"
    const val KEY_ALBUM_ARTIST = "android.media.metadata.ALBUM_ARTIST"
    const val KEY_ALBUM = "android.media.metadata.ALBUM"

    fun readTrack(value: (String) -> String?): Track = Track(
        title = value(KEY_TITLE),
        artist = value(KEY_ARTIST),
        albumArtist = value(KEY_ALBUM_ARTIST),
        album = value(KEY_ALBUM),
    )
}
