package com.aegypius.muzei.nowplaying.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Every dimension here was measured against the live artwork service, so these are
 * observations rather than invented cases.
 */
class ArtworkSizeTest {

    @Test
    fun `rejects the poor image a near-miss album title returns`() {
        // A misspelled "Appetite for Destruction" answers 200 with this, where the
        // correct title gives 800x800. Without this rule it becomes the wallpaper.
        assertFalse(ArtworkSize.isUsable(width = 160, height = 160))
    }

    @Test
    fun `accepts real album covers`() {
        assertTrue(ArtworkSize.isUsable(width = 800, height = 800))
        assertTrue(ArtworkSize.isUsable(width = 1000, height = 1000))
    }

    @Test
    fun `accepts the wide artist art the fallback publishes`() {
        // The fallback's images are landscape, with short sides well under any
        // sensible minimum. Judging the shorter side, or both, would reject the
        // fallback exactly when it is needed.
        assertTrue(ArtworkSize.isUsable(width = 600, height = 337))
        assertTrue(ArtworkSize.isUsable(width = 600, height = 409))
        assertTrue(ArtworkSize.isUsable(width = 1000, height = 560))
    }

    @Test
    fun `rejects dimensions that could not be read`() {
        // BitmapFactory reports -1 when it cannot decode the bytes at all.
        assertFalse(ArtworkSize.isUsable(width = -1, height = -1))
    }
}
