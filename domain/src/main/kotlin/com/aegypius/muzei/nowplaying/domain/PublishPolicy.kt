package com.aegypius.muzei.nowplaying.domain

/** Decides whether the current connection is one the user is willing to publish on. */
object PublishPolicy {

    /**
     * @param connectionIsUnmetered null when the state could not be read at all,
     * which is treated as metered: holding back is the safe answer when the answer
     * is unknown.
     */
    fun allows(unmeteredOnly: Boolean, connectionIsUnmetered: Boolean?): Boolean =
        !unmeteredOnly || connectionIsUnmetered == true
}

/**
 * Whether publishing is permitted right now.
 *
 * Asked at publish time rather than observed, because the answer changes as the
 * connection does and only matters at the moment something would be sent. The gate
 * is applied to live now-playing publishes only: restoring an album already seen is
 * usually served from this app's cache, and refusing it would leave the wallpaper
 * blank with no explanation.
 */
fun interface PublishGate {
    fun allowsPublishing(): Boolean
}
