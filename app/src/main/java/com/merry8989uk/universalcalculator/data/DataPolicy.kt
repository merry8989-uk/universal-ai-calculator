package com.merry8989uk.universalcalculator.data

/**
 * Whether this app may keep anything at all.
 *
 * There are several switches that each cover one thing — the history, the
 * archive, the logs — and asking a user to find and turn off all of them is not
 * an answer. This is the one switch: when it is off, nothing is written
 * anywhere, and the individual switches underneath it do not matter.
 *
 * It is off only because the user said so. The default is on, because a
 * calculator that forgets what you just did is annoying, and the archive is the
 * whole point of the app for someone who wants a record.
 */
object DataPolicy {

    /**
     * True when something may be stored.
     *
     * [savingEnabled] is the user's own switch. When it is false, everything
     * stops — the archive, the history, the diagnostics log and the local
     * counters.
     */
    fun mayStore(savingEnabled: Boolean): Boolean = savingEnabled

    /** True when the home screen may keep its short history. */
    fun mayKeepHistory(savingEnabled: Boolean, historyEnabled: Boolean): Boolean =
        savingEnabled && historyEnabled

    /** True when the permanent archive may be written to. */
    fun mayArchive(savingEnabled: Boolean): Boolean = savingEnabled

    /** True when the diagnostics log may be written to. */
    fun mayLog(savingEnabled: Boolean, diagnosticsEnabled: Boolean): Boolean =
        savingEnabled && diagnosticsEnabled
}
