package com.aegypius.muzei.nowplaying

import android.net.Uri
import com.aegypius.muzei.nowplaying.domain.AlbumKey
import com.google.android.apps.muzei.api.provider.Artwork

/**
 * Builds the Muzei artwork for an album key.
 *
 * One place, because both the listener's publish and the provider's fallback need
 * it and the caption rule below must not drift between them.
 *
 * The caption names the album and its artist, never the track: under an
 * album-level token Muzei has no reason to update it mid-record. See
 * docs/adr/0002-album-level-identity.md.
 */
internal fun artworkFor(key: AlbumKey, artworkUrl: String): Artwork =
    Artwork.Builder()
        .token(key.token)
        .title(key.album ?: key.albumArtist)
        .byline(key.albumArtist)
        .persistentUri(Uri.parse(artworkUrl))
        .build()
