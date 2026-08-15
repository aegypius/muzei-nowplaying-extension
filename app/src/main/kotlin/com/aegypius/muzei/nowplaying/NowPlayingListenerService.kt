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
import com.aegypius.muzei.nowplaying.domain.Player
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
    private val winningSession = WinningSession<MediaSession.Token> { session ->
        // Asked at the moment a session starts playing rather than remembered, so
        // blocking a player takes effect on the very next thing it plays.
        players.blocked().allows(playerOf(session))
    }
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
    private lateinit var players: PlayerDirectory

    private val activeSessionsChanged =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            syncControllers(controllers.orEmpty())
        }

    override fun onCreate() {
        super.onCreate()
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        players = PlayerDirectory(this)
        // Callbacks arrive on the main thread and setArtwork does database I/O, so
        // the publish itself moves off it. The dispatcher is shared with the settings
        // screen rather than created here: see Publishing.
        publishNowPlaying = PublishNowPlaying(
            MuzeiArtworkPublisher(this),
            SharedPreferencesLastAlbum(this),
            Publishing.dispatcher,
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

        // Every player that owns a session is offered in settings, including one
        // already blocked: the list is how a player is found in the first place, and
        // a blocked player that vanished from it could never be unblocked.
        remember(controllers.map { Player(it.packageName) })

        seedOwnership(controllers)
    }

    /**
     * Records the players seen, off the main thread: it reads the package manager
     * and writes a cached icon to disk, while session callbacks arrive on the main
     * thread. Serialised, because the list is read-modify-written and two overlapping
     * session changes would otherwise lose an entry.
     */
    private fun remember(seen: List<Player>) {
        scope.launch(Publishing.dispatcher) {
            seen.forEach { players.remember(it) }
        }
    }

    /**
     * Which player a session belongs to, or null once it has been unregistered.
     *
     * Read from the registered controller rather than stored beside the token, so
     * there is one place where a session's player is known.
     */
    private fun playerOf(session: MediaSession.Token): Player? =
        registered[session]?.first?.packageName?.let(::Player)

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

        // The most relevant session may belong to a blocked player, in which case
        // ownership is refused and nothing is published. Falling through to the next
        // one would be a different rule than the one that applies while running.
        val mostRelevant = alreadyPlaying.firstOrNull() ?: return
        winningSession.startedPlaying(mostRelevant.sessionToken)
        if (!winningSession.owns(mostRelevant.sessionToken)) return

        publish(mostRelevant.metadata, playerOf(mostRelevant.sessionToken))
    }

    private fun unregister(session: MediaSession.Token) {
        registered.remove(session)?.let { (controller, callback) ->
            controller.unregisterCallback(callback)
        }
        playing -= session
        winningSession.destroyed(session)
    }

    private fun unregisterAll() = registered.keys.toList().forEach { unregister(it) }

    private fun publish(metadata: MediaMetadata?, player: Player?) {
        val track = SessionMetadata.readTrack { key -> metadata?.getString(key) }
        scope.launch { publishNowPlaying.publish(track, player) }
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
            // A blocked player is refused ownership, so this is also what stops it
            // publishing. One check, in one place, rather than a second gate here.
            if (!winningSession.owns(session)) return

            publish(controller.metadata, playerOf(session))
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            // Pausing does not surrender ownership: the winner is whoever started
            // playing most recently. See docs/adr/0003-sticky-when-idle.md.
            if (!winningSession.owns(session)) return
            publish(metadata, playerOf(session))
        }

        override fun onSessionDestroyed() {
            unregister(session)
        }
    }

    private companion object {
        const val TAG = "NowPlayingListener"
    }
}
