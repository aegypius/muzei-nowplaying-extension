package com.aegypius.muzei.nowplaying

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aegypius.muzei.nowplaying.domain.Player
import com.aegypius.muzei.nowplaying.domain.PublishDisplacedAlbum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Which players may change the wallpaper.
 *
 * Built in code rather than from XML, because the entries are the players this app
 * has actually seen playing: there is nothing to declare until something has
 * played. The preferences store nothing themselves — PlayerDirectory owns the
 * blocklist, so this screen and the listener read the same one.
 */
class BlockedPlayersFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        showPlayers()
    }

    private fun showPlayers() {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)
        val players = PlayerDirectory(context)
        val known = players.known()

        if (known.isEmpty()) {
            screen.addPreference(
                Preference(context).apply {
                    title = getString(R.string.settings_players_empty_title)
                    summary = getString(R.string.settings_players_empty_summary)
                    isSelectable = false
                    isIconSpaceReserved = false
                },
            )
        }

        known.forEach { entry ->
            screen.addPreference(
                CheckBoxPreference(context).apply {
                    title = entry.label
                    // The package name is shown only when it is not already the
                    // title, which is the case when the app's name could not be read.
                    summary = entry.player.packageName.takeIf { it != entry.label }
                    isPersistent = false
                    isChecked = players.isBlocked(entry.player)
                    entry.icon?.let { file ->
                        icon = BitmapDrawable(resources, BitmapFactory.decodeFile(file.path))
                    }
                    setOnPreferenceChangeListener { _, blocked ->
                        block(entry.player, blocked as Boolean)
                        true
                    }
                },
            )
        }

        screen.addPreference(
            Preference(context).apply {
                title = getString(R.string.settings_players_clear_title)
                summary = getString(R.string.settings_players_clear_summary)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    clear()
                    true
                }
            },
        )

        preferenceScreen = screen
    }

    /**
     * Blocking puts back what that player displaced, so the wallpaper being looked
     * at changes rather than only what happens next.
     *
     * On the shared publish thread, not merely off the main one: the listener may
     * still be publishing for the player being blocked, and setArtwork replaces the
     * whole table, so the two must not race. Unblocking publishes nothing — there is
     * nothing to undo, and whatever plays next comes through on its own.
     */
    private fun block(player: Player, blocked: Boolean) {
        val context = requireContext().applicationContext
        lifecycleScope.launch(Publishing.dispatcher) {
            PlayerDirectory(context).setBlocked(player, blocked)
            if (blocked) {
                PublishDisplacedAlbum(
                    lastAlbum = SharedPreferencesLastAlbum(context),
                    publisher = MuzeiArtworkPublisher(context),
                ).after(player)
            }
        }
    }

    /**
     * Forgets the players seen, then rebuilds the screen so the list matches what
     * was just done. The blocklist itself is kept; see PlayerDirectory.
     */
    private fun clear() {
        val context = requireContext().applicationContext
        lifecycleScope.launch(Publishing.dispatcher) {
            PlayerDirectory(context).forgetAll()
            withContext(Dispatchers.Main) { showPlayers() }
        }
    }
}
