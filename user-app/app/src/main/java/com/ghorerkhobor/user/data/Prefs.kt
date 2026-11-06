package com.ghorerkhobor.user.data

import android.content.Context
import java.util.UUID

object Prefs {

    private const val FILE = "ghorerkhobor_prefs"
    private const val KEY_STOPPED = "emergency_stopped"
    private const val KEY_CRASH_COUNT = "crash_count"
    private const val KEY_CRASH_WINDOW_START = "crash_window_start"
    private const val CRASH_WINDOW_MS = 60000L
    private const val CRASH_LIMIT = 4

    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_PERSON = "person_name"
    private const val KEY_ROLE = "role"
    private const val KEY_REGISTERED = "registered"
    private const val KEY_LAST_SYNC_AT = "last_sync_at"
    private const val KEY_LAST_SYNC_RESULT = "last_sync_result"

    fun isStopped(context: Context): Boolean {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_STOPPED, false)
    }

    fun setStopped(context: Context, stopped: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_STOPPED, stopped)
            .apply()
    }

    fun registerCrashAndCheckShouldStop(context: Context): Boolean {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val windowStart = prefs.getLong(KEY_CRASH_WINDOW_START, 0L)
        var count = prefs.getInt(KEY_CRASH_COUNT, 0)
        if (now - windowStart > CRASH_WINDOW_MS) {
            count = 0
        }
        count += 1
        val editor = prefs.edit().putInt(KEY_CRASH_COUNT, count)
        if (count == 1) {
            editor.putLong(KEY_CRASH_WINDOW_START, now)
        }
        editor.apply()
        if (count >= CRASH_LIMIT) {
            setStopped(context, true)
            return true
        }
        return false
    }

    fun deviceId(context: Context): String {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    fun serverUrl(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_SERVER_URL, "").orEmpty()

    fun apiKey(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_API_KEY, "").orEmpty()

    fun personName(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_PERSON, "").orEmpty()

    fun role(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_ROLE, "").orEmpty()

    fun isRegistered(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_REGISTERED, false)

    fun saveSetup(context: Context, serverUrl: String, apiKey: String, person: String, role: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER_URL, serverUrl.trim())
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_PERSON, person.trim())
            .putString(KEY_ROLE, role.trim())
            .apply()
    }

    fun setRegistered(context: Context, registered: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REGISTERED, registered)
            .apply()
    }

    fun setLastSync(context: Context, atMillis: Long, result: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SYNC_AT, atMillis)
            .putString(KEY_LAST_SYNC_RESULT, result)
            .apply()
    }

    fun lastSyncAt(context: Context): Long =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getLong(KEY_LAST_SYNC_AT, 0L)

    fun lastSyncResult(context: Context): String =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_LAST_SYNC_RESULT, "").orEmpty()
}
