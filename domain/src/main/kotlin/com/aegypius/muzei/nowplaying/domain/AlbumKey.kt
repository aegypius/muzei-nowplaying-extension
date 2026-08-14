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
     */
    val token: String
        get() = "${albumArtist.length}:$albumArtist:${album?.length ?: -1}:${album ?: ""}"

    /** The same key with the album dropped, which is what the artist fallback asks for. */
    fun withoutAlbum(): AlbumKey = copy(album = null)

    companion object {
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
         * Sessions report blank strings as readily as nulls, and the two mean the
         * same thing here. Treating "" as present would send an empty parameter,
         * which the service answers with 400 rather than with artwork.
         */
        private fun String?.orNullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    }
}
