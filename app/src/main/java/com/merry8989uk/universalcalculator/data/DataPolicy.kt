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

    /**
     * True when recorded diagnostics may be sent off the device.
     *
     * A third switch, because keeping a log and sending it are different
     * promises. The master switch still wins, and the log has to be on for there
     * to be anything worth sending.
     */
    fun mayUpload(
        savingEnabled: Boolean,
        diagnosticsEnabled: Boolean,
        shareEnabled: Boolean,
    ): Boolean = savingEnabled && diagnosticsEnabled && shareEnabled

    /**
     * True when a picture, a sketch or an attachment may leave the device for
     * the assistant.
     *
     * [mayUpload] is a different question, and answering it either way says
     * nothing about this one: it is about the diagnostics log going to the
     * developer's warehouse, which a user may refuse while still happily
     * photographing a page of homework. A photo, a sketch or a picked file is
     * the user's own work rather than a log about the app, so it travels only
     * while the master switch is on — "keep nothing" is the one setting that
     * also means "send nothing of mine off this device".
     *
     * It is named for what it decides rather than folded into [mayStore], so the
     * screens that hold a picture have something to ask that reads as the
     * question they are actually asking.
     */
    fun maySendContent(savingEnabled: Boolean): Boolean = savingEnabled

    /** True when the diagnostics log may be written to. */
    fun mayLog(savingEnabled: Boolean, diagnosticsEnabled: Boolean): Boolean =
        savingEnabled && diagnosticsEnabled
}
