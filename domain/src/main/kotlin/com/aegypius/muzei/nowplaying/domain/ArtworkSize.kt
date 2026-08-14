package com.aegypius.muzei.nowplaying.domain

/**
 * Whether an image is large enough to be a wallpaper.
 *
 * The artwork service answers a near-miss album title with 200 and a poor image
 * rather than 404, so nothing else can tell that case apart: only a non-2xx triggers
 * the artist fallback. Measured, a misspelled album title returns 160x160 where the
 * correct one returns 800x800.
 *
 * Judged on the LONGEST side, not the shortest and not both. The artist art the
 * fallback publishes is landscape — 600x337, 600x409, 600x450 were all measured — so
 * a rule on the shorter side would reject the fallback exactly when it is needed.
 *
 * Bytes are not the signal either: a correct cover was measured at 17 kB and
 * 1000x1000, against 5 kB at 160x160 for a bad one.
 */
object ArtworkSize {

    /**
     * Every legitimate image measured had a longest side of 600 or more, and the one
     * bad image had 160, so this sits with margin on both sides. Muzei blurs and dims
     * a wallpaper, which makes anything smaller look poor even when it is the right
     * cover.
     */
    const val MINIMUM_LONGEST_SIDE = 500

    fun isUsable(width: Int, height: Int): Boolean =
        maxOf(width, height) >= MINIMUM_LONGEST_SIDE
}
