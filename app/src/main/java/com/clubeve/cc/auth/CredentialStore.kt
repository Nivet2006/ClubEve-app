package com.clubeve.cc.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores USN + password encrypted on-device using AES-256 via EncryptedSharedPreferences.
 * Only written when the user explicitly checks "Remember Me".
 */
object CredentialStore {

    private const val FILE_NAME = "pr_secure_prefs"
    private const val KEY_USN = "saved_usn"
    private const val KEY_PASSWORD = "saved_password"

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(context: Context, usn: String, password: String) {
        prefs(context).edit()
            .putString(KEY_USN, usn)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun load(context: Context): Pair<String, String>? {
        val p = prefs(context)
        val usn = p.getString(KEY_USN, null) ?: return null
        val password = p.getString(KEY_PASSWORD, null) ?: return null
        return usn to password
    }

    fun hasSaved(context: Context): Boolean = load(context) != null

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
