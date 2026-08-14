package com.aegypius.muzei.nowplaying

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button

/**
 * Asks for notification-listener access, which is the only route to media sessions.
 *
 * Muzei launches this before enabling the provider — it is registered as the
 * provider's `setupActivity` meta-data — and acts on the result code, so returning
 * RESULT_CANCELED tells Muzei not to enable a provider that could never publish
 * anything.
 *
 * The permission cannot be requested in-app: Android grants notification-listener
 * access only through system settings, with no runtime dialog. The best available
 * is a deep link straight to this app's own toggle.
 *
 * See docs/adr/0008-setup-screen-uses-platform-apis.md for why this uses platform
 * APIs rather than AppCompat and the support libraries the predecessor used.
 */
class SetupActivity : Activity() {

    /**
     * Both flags are saved: a rotation in Settings, or this process being evicted
     * while Settings is in front, would otherwise lose them and leave the user on
     * a screen that never responds to their answer.
     */
    private var returnedFromSettings = false
    private var promptShown = false

    private var prompt: AlertDialog? = null

    private val listenerComponent: ComponentName
        get() = ComponentName(this, NowPlayingListenerService::class.java)

    private val accessGranted: Boolean
        get() = getSystemService(NotificationManager::class.java)
            .isNotificationListenerAccessGranted(listenerComponent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let {
            returnedFromSettings = it.getBoolean(STATE_RETURNED, false)
            promptShown = it.getBoolean(STATE_PROMPTED, false)
        }

        if (accessGranted) {
            finishWithResult()
            return
        }

        setContentView(R.layout.activity_setup)
        findViewById<Button>(R.id.grant_access).setOnClickListener { openSettings() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_RETURNED, returnedFromSettings)
        outState.putBoolean(STATE_PROMPTED, promptShown)
    }

    override fun onResume() {
        super.onResume()

        if (accessGranted) {
            finishWithResult()
            return
        }

        // Asked once, and only once: coming back without access is worth one prompt,
        // and repeating it is nagging. The prompt's own retry button is the user
        // choosing to try again, which is different.
        if (returnedFromSettings && !promptShown) {
            returnedFromSettings = false
            promptShown = true
            showPrompt(R.string.setup_not_granted_title, R.string.setup_not_granted_message)
        }
    }

    override fun onDestroy() {
        // Held and dismissed explicitly: a dialog left showing when the activity goes
        // away leaks its window.
        prompt?.dismiss()
        prompt = null
        super.onDestroy()
    }

    /**
     * Deep-links to this app's own entry rather than the full list of every app with
     * a notification listener. The generic list is the fallback, because the detail
     * screen is absent on some ROMs — Muzei's own code guards every comparable call
     * the same way.
     */
    private fun openSettings() {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                listenerComponent.flattenToString(),
            )
        val list = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

        for (intent in listOf(detail, list)) {
            try {
                startActivity(intent)
                // Only once the launch succeeded, so a failure does not leave this
                // armed and prompt about a screen that never opened.
                returnedFromSettings = true
                return
            } catch (e: ActivityNotFoundException) {
                Log.i(TAG, "no activity for ${intent.action}: ${e.message}")
            } catch (e: SecurityException) {
                Log.i(TAG, "not allowed to open ${intent.action}: ${e.message}")
            }
        }

        // Neither screen exists. Nothing this app can do but say so.
        promptShown = true
        showPrompt(R.string.setup_no_settings_title, R.string.setup_no_settings_message)
    }

    private fun showPrompt(titleRes: Int, messageRes: Int) {
        prompt?.dismiss()
        prompt = AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setMessage(messageRes)
            .setPositiveButton(R.string.setup_open_settings) { _, _ -> openSettings() }
            .setNegativeButton(R.string.setup_close) { _, _ -> finishWithResult() }
            .setOnDismissListener { prompt = null }
            .show()
    }

    /**
     * Muzei reads this: RESULT_OK enables the provider, RESULT_CANCELED leaves it
     * alone. Re-checked rather than assumed, so the result always matches reality
     * even if access changed while this screen was open.
     */
    private fun finishWithResult() {
        setResult(if (accessGranted) RESULT_OK else RESULT_CANCELED)
        finish()
    }

    private companion object {
        const val TAG = "NowPlayingSetup"
        const val STATE_RETURNED = "returnedFromSettings"
        const val STATE_PROMPTED = "promptShown"
    }
}
