package com.aegypius.muzei.nowplaying

import android.media.MediaMetadata
import com.aegypius.muzei.nowplaying.domain.SessionMetadata
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * :domain cannot see Android, so it holds the metadata key strings as literals.
 * This ties them to the platform's own constants, which is the only thing that can
 * actually fail if one is wrong — the :domain test asserts against the same
 * literals and so cannot.
 *
 * Runs without Robolectric because a `static final String` is inlined at compile
 * time rather than read from the android.jar stub at runtime.
 */
class SessionMetadataKeysTest {

    @Test
    fun `the keys match the constants the platform publishes metadata under`() {
        assertEquals(MediaMetadata.METADATA_KEY_TITLE, SessionMetadata.KEY_TITLE)
        assertEquals(MediaMetadata.METADATA_KEY_ARTIST, SessionMetadata.KEY_ARTIST)
        assertEquals(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, SessionMetadata.KEY_ALBUM_ARTIST)
        assertEquals(MediaMetadata.METADATA_KEY_ALBUM, SessionMetadata.KEY_ALBUM)
    }
}
