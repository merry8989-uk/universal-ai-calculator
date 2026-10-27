package com.merry8989uk.universalcalculator.sync

import android.content.Context

/**
 * What this device remembers about the signed-in account.
 *
 * The password is never written down. What is kept is the id and the verifier
 * the server already holds — enough to prove the account on the next request,
 * and useless for opening the data, which needs a key derived from the password
 * itself.
 */
object AccountStore {

    private const val PREFS = "account"
    private const val KEY_ID = "id"
    private const val KEY_VERIFIER = "verifier"
    private const val KEY_LAST_BACKUP = "last_backup"
    private const val KEY_LAST_RESTORE = "last_restore"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun id(context: Context): String = prefs(context).getString(KEY_ID, "").orEmpty()

    fun verifier(context: Context): String = prefs(context).getString(KEY_VERIFIER, "").orEmpty()

    fun signedIn(context: Context): Boolean =
        id(context).isNotBlank() && verifier(context).isNotBlank()

    fun signIn(context: Context, id: String, verifier: String) {
        prefs(context).edit()
            .putString(KEY_ID, id)
            .putString(KEY_VERIFIER, verifier)
            .apply()
    }

    fun lastBackup(context: Context): Long = prefs(context).getLong(KEY_LAST_BACKUP, 0L)

    fun lastRestore(context: Context): Long = prefs(context).getLong(KEY_LAST_RESTORE, 0L)

    fun noteBackup(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply()
    }

    fun noteRestore(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_RESTORE, System.currentTimeMillis()).apply()
    }

    /** Forgets the account on this device. Nothing stored on the server is touched. */
    fun signOut(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
