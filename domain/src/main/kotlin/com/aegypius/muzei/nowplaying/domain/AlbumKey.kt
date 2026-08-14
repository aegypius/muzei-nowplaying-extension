package com.aegypius.muzei.nowplaying.domain

/**
 * The identity of the artwork on screen: album artist plus album.
 *
 * Two tracks sharing an album key are the same Now Playing and produce no
 * wallpaper change. See docs/adr/0002-album-level-identity.md.
 */
data class AlbumKey internal constructor(
    val albumArtist: String,
    val album: String?,
) {
    /**
     * The value written into Muzei's artwork token, which it dedupes on.
     *
     * Length-prefixed rather than joined with a separator: any separator can
     * legitimately occur inside an artist or album name, and two different keys
     * sharing a token would make Muzei skip the second as already published.
     *
     * This value is also what gets persisted as the last album, so changing the
     * format invalidates what was already stored. The cost is one blank wallpaper
     * until something plays, because an unparseable value is treated as absent.
     */
    val token: String
        get() = "${albumArtist.length}:$albumArtist:${album?.length ?: ABSENT_ALBUM}:${album ?: ""}"

    /** The same key with the album dropped, which is what the artist fallback asks for. */
    fun withoutAlbum(): AlbumKey = copy(album = null)

    companion object {

        /** Written as the album length when there is no album. */
        private const val ABSENT_ALBUM = -1

        /**
         * Reduces a track to the identity of its artwork, or refuses.
         *
         * An artist alone is enough: with no album the lookup asks for artist art,
         * which the service answers. With no artist of any kind there is nothing
         * to look up, so this returns null rather than publishing a meaningless
         * request.
         */
        fun of(track: Track): AlbumKey? {
            // Album artist rather than artist, so a compilation stays one album.
            val albumArtist = track.albumArtist.orNullIfBlank()
                ?: track.artist.orNullIfBlank()
                ?: return null
            return AlbumKey(albumArtist = albumArtist, album = track.album.orNullIfBlank())
        }

        /**
         * Rebuilds a key from its token.
         *
         * Muzei hands back only the token when it reports a failed artwork, so the
         * artist fallback needs this to know which album failed. Returns null for
         * anything this class did not write.
         */
        fun fromToken(token: String): AlbumKey? {
            val artistLengthEnd = token.indexOf(':')
            if (artistLengthEnd <= 0) return null
            val artistLength = token.substring(0, artistLengthEnd).toIntOrNull() ?: return null
            if (artistLength < 0) return null

            val artistStart = artistLengthEnd + 1
            val artistEnd = artistStart + artistLength
            if (artistEnd >= token.length || token[artistEnd] != ':') return null

            val albumLengthEnd = token.indexOf(':', artistEnd + 1)
            if (albumLengthEnd < 0) return null
            val albumLength = token
                .substring(artistEnd + 1, albumLengthEnd)
                .toIntOrNull()
                ?: return null

            val albumStart = albumLengthEnd + 1
            val albumArtist = token.substring(artistStart, artistEnd)
            return when {
                // -1 is how an absent album is written; nothing may follow it.
                albumLength == ABSENT_ALBUM ->
                    if (albumStart == token.length) AlbumKey(albumArtist, null) else null
                albumLength >= 0 ->
                    if (albumStart + albumLength == token.length) {
                        AlbumKey(albumArtist, token.substring(albumStart))
                    } else {
                        null
                    }
                else -> null
            }
        }

        /**
         * Sessions report blank strings as readily as nulls, and the two mean the
         * same thing here. Treating "" as present would send an empty parameter,
         * which the service answers with 400 rather than with artwork.
         */
        private fun String?.orNullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    }
}
