package com.merry8989uk.universalcalculator.sync

import android.content.Context

/**
 * Remembers the sync code and when the last upload happened.
 *
 * The code is the only key to the user's data, so it is kept on the device and
 * never sent anywhere. Losing it means the stored blob can never be read again,
 * which the UI says plainly.
 */
object SyncStore {

    private const val PREFS = "sync"
    private const val KEY_CODE = "code"
    private const val KEY_LAST = "last_upload"
    private const val KEY_SERVER_TIME = "server_time"
    private const val KEY_VERSION = "version"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun code(context: Context): String = prefs(context).getString(KEY_CODE, "").orEmpty()

    fun hasCode(context: Context): Boolean = code(context).length >= MIN_CODE_LENGTH

    fun setCode(context: Context, code: String) {
        prefs(context).edit().putString(KEY_CODE, code).apply()
    }

    fun lastUpload(context: Context): Long = prefs(context).getLong(KEY_LAST, 0L)

    fun noteUpload(context: Context, serverTime: Long) {
        prefs(context).edit()
            .putLong(KEY_LAST, System.currentTimeMillis())
            .putLong(KEY_SERVER_TIME, serverTime)
            .apply()
    }

    /**
     * The version of the stored backup this device last saw.
     *
     * A write has to name the version it is replacing, so a phone that has been
     * offline cannot quietly overwrite a newer backup made somewhere else.
     */
    fun version(context: Context): Int = prefs(context).getInt(KEY_VERSION, 0)

    fun noteVersion(context: Context, version: Int) {
        prefs(context).edit().putInt(KEY_VERSION, version).apply()
    }

    fun forget(context: Context) {
        prefs(context).edit().clear().apply()
    }

    /** Short codes are too easy to guess, so this is enforced in the UI too. */
    const val MIN_CODE_LENGTH = 12
}
