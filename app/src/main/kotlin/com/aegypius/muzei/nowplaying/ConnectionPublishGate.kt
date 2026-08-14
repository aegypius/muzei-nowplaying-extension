package com.aegypius.muzei.nowplaying

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.preference.PreferenceManager
import com.aegypius.muzei.nowplaying.domain.PublishGate
import com.aegypius.muzei.nowplaying.domain.PublishPolicy

/**
 * Answers whether publishing is allowed on the connection in use right now.
 *
 * NET_CAPABILITY_NOT_METERED rather than asking whether the radio is wifi: the
 * user's concern is data cost, not the medium, and a metered hotspot looks exactly
 * like wifi to the older API the predecessor used.
 */
class ConnectionPublishGate(private val context: Context) : PublishGate {

    override fun allowsPublishing(): Boolean {
        // Short-circuited deliberately: with the setting off there is no reason to
        // ask the system anything, and the question is the only part that needs a
        // permission.
        if (!unmeteredOnly()) return true

        return PublishPolicy.allows(
            unmeteredOnly = true,
            connectionIsUnmetered = connectionIsUnmetered(),
        )
    }

    private fun unmeteredOnly(): Boolean = PreferenceManager
        .getDefaultSharedPreferences(context)
        .getBoolean(KEY_UNMETERED_ONLY, false)

    /** Null when the state cannot be read; PublishPolicy decides what that means. */
    private fun connectionIsUnmetered(): Boolean? {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity
            ?.getNetworkCapabilities(connectivity.activeNetwork)
            ?: return null
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private companion object {
        /** Must match the key in res/xml/preferences.xml. */
        const val KEY_UNMETERED_ONLY = "unmetered_only"
    }
}
