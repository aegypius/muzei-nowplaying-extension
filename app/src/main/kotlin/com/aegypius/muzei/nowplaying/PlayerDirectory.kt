package com.aegypius.muzei.nowplaying

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.preference.PreferenceManager
import com.aegypius.muzei.nowplaying.domain.BlockedPlayers
import com.aegypius.muzei.nowplaying.domain.Player
import java.io.File

/** A player as the settings screen shows it: what to call it, and what to draw. */
data class KnownPlayer(
    val player: Player,
    val label: String,
    val icon: File?,
)

/**
 * The players seen playing, and which of them are blocked.
 *
 * Two stores, because the two things differ in kind. Blocking is a user setting and
 * lives with the others in the default preferences; the list of players seen is
 * state this app accumulates, and lives with the last album.
 *
 * Names and icons are cached the first time a player is seen, because they may be
 * unreadable later: package visibility filtering hides most apps from most apps
 * (see the queries element in the manifest), and an uninstalled app is gone
 * entirely. A player whose name was never readable is shown as its package name,
 * which is still unambiguous and still blockable.
 */
class PlayerDirectory(private val context: Context) {

    private val state =
        context.getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE)

    private val settings = PreferenceManager.getDefaultSharedPreferences(context)

    private val icons = File(context.filesDir, ICON_DIRECTORY)

    /**
     * Records that a player owned a session, caching its name and icon.
     *
     * Does file and package-manager work, so callers keep it off the main thread.
     * Re-resolving is attempted for a player whose name is not cached yet: it may
     * have been installed since, or been invisible on the first attempt.
     */
    fun remember(player: Player) {
        val packageName = player.packageName
        val seen = seen()
        if (packageName in seen && state.contains(labelKey(packageName))) return

        val label = resolveLabel(packageName)
        if (label != null) cacheIcon(packageName)

        val editor = state.edit().putStringSet(KEY_SEEN, seen + packageName)
        if (label != null) editor.putString(labelKey(packageName), label)
        editor.apply()
    }

    /**
     * Every player seen, plus every player blocked, ordered by the name the user
     * reads.
     *
     * Blocked players are included even when they are not in the seen list, which
     * happens after the list is cleared or when a blocked app is uninstalled.
     * Listing only what was seen would leave them blocked with no way to undo it.
     */
    fun known(): List<KnownPlayer> = (seen() + blockedPackages())
        .map { packageName ->
            KnownPlayer(
                player = Player(packageName),
                label = state.getString(labelKey(packageName), null) ?: packageName,
                icon = File(icons, "$packageName.png").takeIf { it.exists() },
            )
        }
        .sortedBy { it.label.lowercase() }

    fun blocked(): BlockedPlayers =
        BlockedPlayers(blockedPackages().map(::Player).toSet())

    fun isBlocked(player: Player): Boolean = player.packageName in blockedPackages()

    fun setBlocked(player: Player, blocked: Boolean) {
        val packages = blockedPackages()
        val updated =
            if (blocked) packages + player.packageName else packages - player.packageName

        // commit() rather than apply(), for the reason SharedPreferencesLastAlbum
        // gives: the choice has to survive the process dying immediately afterwards,
        // and a blocklist that forgot an entry would silently let an app through.
        // The caller is already off the main thread.
        settings.edit().putStringSet(KEY_BLOCKED, updated).commit()
    }

    /**
     * Forgets every player seen, along with the cached names and icons.
     *
     * Blocked players are deliberately kept: a blocklist that emptied itself when
     * the list was tidied would silently start letting a blocked app through. They
     * stay listed too, by [known], so blocking never becomes one-way.
     */
    fun forgetAll() {
        // Read before the edit, since the label keys to remove are derived from it.
        val seen = seen()
        val editor = state.edit().remove(KEY_SEEN)
        seen.forEach { editor.remove(labelKey(it)) }
        editor.apply()
        icons.listFiles()?.forEach { it.delete() }
    }

    private fun seen(): Set<String> = state.getStringSet(KEY_SEEN, null).orEmpty()

    private fun blockedPackages(): Set<String> =
        settings.getStringSet(KEY_BLOCKED, null).orEmpty()

    private fun resolveLabel(packageName: String): String? = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        // Filtered by package visibility, or not installed. The package name is
        // shown instead; there is nothing to recover.
        Log.i(TAG, "no name for $packageName: ${e.message}")
        null
    }

    private fun cacheIcon(packageName: String) {
        val drawable = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.i(TAG, "no icon for $packageName: ${e.message}")
            return
        }

        icons.mkdirs()
        File(icons, "$packageName.png").outputStream().use { out ->
            drawable.toBitmap(iconSizePx()).compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun iconSizePx(): Int =
        (ICON_SIZE_DP * context.resources.displayMetrics.density).toInt()

    /**
     * An application icon is usually an AdaptiveIconDrawable with no intrinsic
     * bounds worth trusting, so it is drawn at the size wanted rather than measured.
     */
    private fun Drawable.toBitmap(size: Int): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            setBounds(0, 0, size, size)
            draw(Canvas(bitmap))
        }

    private fun labelKey(packageName: String) = "player_label_$packageName"

    private companion object {
        const val TAG = "PlayerDirectory"

        /** Shared with SharedPreferencesLastAlbum: both are state, not settings. */
        const val STATE_PREFERENCES = "now_playing"
        const val KEY_SEEN = "seen_players"
        const val ICON_DIRECTORY = "players"
        const val ICON_SIZE_DP = 48

        /** Changing this key resets the user's blocklist to empty, silently. */
        const val KEY_BLOCKED = "blocked_players"
    }
}
