package com.example.tranki

import android.app.Activity
import android.content.Context

object Preferences {
    private const val name = "com.example.tranki"

    private const val urlKey = "anki.url"
    private const val keyEnabledKey = "anki.key.enabled"
    private const val keyKey = "anki.key.value"

    private const val credsKey = "google.cloud.translate.creds"

    class Preferences(
        val creds: String?,
        val ankiUrl: String?,
        val ankiKeyEnabled: Boolean?,
        val ankiKey: String?
    ) {
        fun save(activity: Activity) {
            val prefs = activity.getSharedPreferences(name, Context.MODE_PRIVATE)
            val prefsEdit = prefs.edit()
            try {
                prefsEdit.putString(credsKey, creds!!)
                prefsEdit.putString(urlKey, ankiUrl!!)
                prefsEdit.putBoolean(keyEnabledKey, ankiKeyEnabled!!)
                prefsEdit.putString(keyKey, ankiKey!!)
            } catch (e: NullPointerException) {
                throw e
            }
            prefsEdit.apply()
        }
    }

    fun get(activity: Activity): Preferences {
        val prefs = activity.getSharedPreferences(name, Context.MODE_PRIVATE)
        val creds = prefs.getString(credsKey, null)
        val url = prefs.getString(urlKey, null)
        val keyEnabled = if (prefs.contains(keyEnabledKey)) {
            prefs.getBoolean(keyEnabledKey, false)
        } else {
            null
        }
        val key = prefs.getString(keyKey, null)

        return Preferences(creds, url, keyEnabled, key)
    }
}