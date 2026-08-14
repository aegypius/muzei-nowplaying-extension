package com.aegypius.muzei.nowplaying

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import com.aegypius.muzei.nowplaying.domain.PublishNowPlaying
import com.aegypius.muzei.nowplaying.domain.SessionMetadata
import com.aegypius.muzei.nowplaying.domain.WinningSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Watches every active media session and publishes what is playing.
 *
 * This is a notification listener because that is the only way to reach media
 * sessions: MediaSessionManager.getActiveSessions requires notification-listener
 * access, and Android grants it only through system settings.
 *
 * The arbitration rule and the metadata reading both live in :domain, where they
 * are tested. This class is the Android boundary: it manages callbacks and
 * lifecycles and nothing else.
 */
class NowPlayingListenerService : NotificationListenerService() {

    // Keyed by the session's own token, not by its package: one app can own two
    // sessions at once, and collapsing them would leave the one that actually
    // plays unobserved.
    private val winningSession = WinningSession<MediaSession.Token>()
    private val registered =
        mutableMapOf<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()
    private var observingSessions = false

    // Which sessions were last seen playing, so that a repeated PLAYING report is
    // told apart from entering PLAYING. PlaybackState is re-emitted for position
    // and buffering updates, and treating those as "started playing" would let any
    // still-playing session steal the wallpaper back on every tick.
    private val playing = mutableSetOf<MediaSession.Token>()

    private lateinit var sessionManager: MediaSessionManager
    private lateinit var scope: CoroutineScope
    private lateinit var publishNowPlaying: PublishNowPlaying

    private val activeSessionsChanged =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            syncControllers(controllers.orEmpty())
        }

    override fun onCreate() {
        super.onCreate()
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        // Callbacks arrive on the main thread and setArtwork does database I/O, so
        // the publish itself moves to the IO dispatcher.
        // limitedParallelism(1) rather than plain IO: setArtwork replaces the whole
        // table, so two publishes racing on different threads could leave the
        // stale album as the winner.
        publishNowPlaying = PublishNowPlaying(
            MuzeiArtworkPublisher(this),
            SharedPreferencesLastAlbum(this),
            Dispatchers.IO.limitedParallelism(1),
            ConnectionPublishGate(this),
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Access has just been granted, so sessions are reachable for the first
        // time. Registering in onCreate alone would miss everything already playing.
        addActiveSessionsListener()
        syncControllers(activeControllers())
    }

    override fun onListenerDisconnected() {
        removeActiveSessionsListener()
        unregisterAll()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        removeActiveSessionsListener()
        unregisterAll()
        scope.cancel()
        super.onDestroy()
    }

    private fun component() = ComponentName(this, NowPlayingListenerService::class.java)

    private fun addActiveSessionsListener() {
        // onListenerConnected can fire more than once per process; adding twice
        // would deliver every change twice and leak the extra registration.
        if (observingSessions) return
        try {
            sessionManager.addOnActiveSessionsChangedListener(activeSessionsChanged, component())
            observingSessions = true
        } catch (e: SecurityException) {
            // Notification-listener access has not been granted, or was revoked.
            Log.i(TAG, "cannot observe media sessions: ${e.message}")
        }
    }

    private fun removeActiveSessionsListener() {
        if (!observingSessions) return
        sessionManager.removeOnActiveSessionsChangedListener(activeSessionsChanged)
        observingSessions = false
    }

    private fun activeControllers(): List<MediaController> = try {
        sessionManager.getActiveSessions(component())
    } catch (e: SecurityException) {
        Log.i(TAG, "cannot read active sessions: ${e.message}")
        emptyList()
    }

    /**
     * Every active session is watched, not only the first: which one matters is
     * decided by what starts playing, and that cannot be known in advance.
     */
    private fun syncControllers(controllers: List<MediaController>) {
        val current = controllers.associateBy { it.sessionToken }

        (registered.keys - current.keys).forEach { unregister(it) }

        current.forEach { (session, controller) ->
            if (registered.containsKey(session)) return@forEach
            val callback = SessionCallback(session, controller)
            registered[session] = controller to callback
            controller.registerCallback(callback)
        }

        seedOwnership(controllers)
    }

    /**
     * Sessions already playing when watching starts have no state change left to
     * report, so ownership has to be seeded.
     *
     * getActiveSessions is ordered by relevance, most relevant first, so the first
     * playing session wins. Replaying every playing session's state instead would
     * hand the wallpaper to the least relevant one.
     */
    private fun seedOwnership(controllers: List<MediaController>) {
        val alreadyPlaying = controllers.filter {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        // Recorded so their next position tick is not mistaken for a fresh start.
        alreadyPlaying.forEach { playing += it.sessionToken }

        val mostRelevant = alreadyPlaying.firstOrNull() ?: return
        winningSession.startedPlaying(mostRelevant.sessionToken)
        publish(mostRelevant.metadata)
    }

    private fun unregister(session: MediaSession.Token) {
        registered.remove(session)?.let { (controller, callback) ->
            controller.unregisterCallback(callback)
        }
        playing -= session
        winningSession.destroyed(session)
    }

    private fun unregisterAll() = registered.keys.toList().forEach { unregister(it) }

    private fun publish(metadata: MediaMetadata?) {
        val track = SessionMetadata.readTrack { key -> metadata?.getString(key) }
        scope.launch { publishNowPlaying.publish(track) }
    }

    private inner class SessionCallback(
        private val session: MediaSession.Token,
        private val controller: MediaController,
    ) : MediaController.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            val isPlaying = state?.state == PlaybackState.STATE_PLAYING
            val wasPlaying = session in playing
            if (isPlaying) playing += session else playing -= session

            // Only the transition into playing takes ownership. Without this a
            // still-playing session would republish on every position update and
            // steal the wallpaper back from the real winner.
            if (!isPlaying || wasPlaying) return

            winningSession.startedPlaying(session)
            publish(controller.metadata)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            // Pausing does not surrender ownership: the winner is whoever started
            // playing most recently. See docs/adr/0003-sticky-when-idle.md.
            if (!winningSession.owns(session)) return
            publish(metadata)
        }

        override fun onSessionDestroyed() {
            unregister(session)
        }
    }

    private companion object {
        const val TAG = "NowPlayingListener"
    }
}
