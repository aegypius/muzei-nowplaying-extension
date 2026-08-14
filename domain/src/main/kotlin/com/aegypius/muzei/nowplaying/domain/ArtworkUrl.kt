package com.aegypius.muzei.nowplaying.domain

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Builds the artwork lookup URL handed to Muzei as an artwork's persistent URI.
 *
 * A String rather than a Uri: this module has no Android on its compile
 * classpath, deliberately. The caller wraps it.
 */
object ArtworkUrl {

    private const val ENDPOINT = "https://artwork.shuttlemusicplayer.app/api/v1/artwork"

    fun of(key: AlbumKey): String =
        buildString {
            append(ENDPOINT)
            append("?artist=").append(encode(key.albumArtist))
            key.album?.let { append("&album=").append(encode(it)) }
        }

    /**
     * URLEncoder is form encoding, which spells a space `+`. That is wrong in a
     * query value here, so it is corrected to the percent form the service was
     * measured to accept.
     */
    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}
