package com.example.tranki

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.material.snackbar.Snackbar

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val googleTestPreference: Preference? = findPreference("test_google_api")
            googleTestPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                testGoogleCloudAPI()
                true
            }

            val ankiTestPreference: Preference? = findPreference("test_anki_api")
            ankiTestPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                testAnkiAPI()
                true
            }

            val savePreference: Preference? = findPreference("save_key")
            savePreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                saveSettings()
                true
            }
        }

        private fun testGoogleCloudAPI() {
            val credsPreference: EditTextPreference? = findPreference("google_api_json");
            val credsValue = credsPreference?.text!!

            val result = Translate.test(requireContext(), credsValue)
            if (result.first) {
                Toast.makeText(
                    requireContext(),
                    "Google Cloud Translation API Authentication successful!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Snackbar.make(
                    requireView(),
                    "Google Cloud Translation API Authentication failed. Message: ${result.second}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        private fun testAnkiAPI() {
            val urlPreference: EditTextPreference? = findPreference("anki_connect_url")
            val url = urlPreference?.text!!

            val keyEnabledPreference: SwitchPreference? = findPreference("anki_key_enabled")
            val keyEnabled = keyEnabledPreference?.isEnabled!!

            val keyPreference: EditTextPreference? = findPreference("anki_api_key")
            val key = keyPreference?.text!!

            val keyNulled = if (keyEnabled) key else null

            val result = Anki.test(url, keyNulled)
            if (result.first) {
                Toast.makeText(
                    requireContext(),
                    "AnkiConnect API Authentication successful!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Snackbar.make(
                    requireView(),
                    "AnkiConnect Connection failed. Message: ${result.second}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        private fun saveSettings() {
            val credsPreference: EditTextPreference? = findPreference("google_api_json");
            val creds = credsPreference?.text!!

            val urlPreference: EditTextPreference? = findPreference("anki_connect_url")
            val url = urlPreference?.text!!

            val keyEnabledPreference: SwitchPreference? = findPreference("anki_key_enabled")
            val keyEnabled = keyEnabledPreference?.isEnabled!!

            val keyPreference: EditTextPreference? = findPreference("anki_api_key")
            val key = keyPreference?.text!!

            val prefs = Preferences.Preferences(creds, url, keyEnabled, key)
            try {
                prefs.save(requireActivity())
            } catch (e: Exception) {
                val message = "Error saving preferences: " +
                        (e.message ?: "Unknown Error")
                Log.e("AnkiConnect", message, e)
                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            Toast.makeText(
                requireContext(),
                "Successful save!",
                Toast.LENGTH_SHORT
            ).show()
        }

    }
}