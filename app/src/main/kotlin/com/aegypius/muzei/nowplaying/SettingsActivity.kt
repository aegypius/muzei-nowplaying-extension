package com.aegypius.muzei.nowplaying

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

/**
 * The provider's settings, reachable from Muzei's provider list.
 *
 * Registered as the provider's `settingsActivity` meta-data. Unlike the setup
 * screen, Muzei does not read a result code here.
 */
class SettingsActivity :
    AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.settings, SettingsFragment())
            }
        }
    }

    /**
     * Opens a preference's `android:fragment`, which the preference library does not
     * do by itself: it reports the click here and the host decides how to navigate.
     *
     * Added to the back stack, so the system back gesture returns to the settings
     * list rather than leaving the screen entirely.
     */
    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference,
    ): Boolean {
        val fragmentName = pref.fragment ?: return false
        val fragment = supportFragmentManager.fragmentFactory
            .instantiate(classLoader, fragmentName)

        supportFragmentManager.commit {
            replace(R.id.settings, fragment)
            addToBackStack(null)
        }
        // The activity title is deliberately left alone. Setting it here would need
        // restoring on every back-stack pop, and the screen it names is already the
        // one thing on display.
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }
}
