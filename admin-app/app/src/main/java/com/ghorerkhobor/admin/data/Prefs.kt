package com.ghorerkhobor.admin.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

object Prefs {

    private const val FILE = "ghorerkhobor_admin_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_ADMIN_KEY = "admin_key"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_PIN_SALT = "pin_salt"
    private const val KEY_THEME_MODE = "theme_mode"

    fun serverUrl(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_SERVER_URL, "").orEmpty()

    fun adminKey(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_ADMIN_KEY, "").orEmpty()

    fun saveServerSettings(context: Context, serverUrl: String, adminKey: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER_URL, serverUrl.trim())
            .putString(KEY_ADMIN_KEY, adminKey.trim())
            .apply()
    }

    fun isPinSet(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_PIN_HASH, null) != null

    fun setPin(context: Context, pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val hash = hashPin(pin, saltHex)
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PIN_SALT, saltHex)
            .putString(KEY_PIN_HASH, hash)
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val saltHex = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return hashPin(pin, saltHex) == storedHash
    }

    private fun hashPin(pin: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltHex.toByteArray(Charsets.UTF_8))
        val hashed = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashed.joinToString("") { "%02x".format(it) }
    }

    fun themeMode(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_THEME_MODE, "system").orEmpty()

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode)
            .apply()
    }
}
