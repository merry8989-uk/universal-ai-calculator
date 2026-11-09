package com.merry8989uk.universalcalculator.sync

/**
 * What one backup carries, and the words the screen uses to say it.
 *
 * The rule and the sentence live in the same file on purpose. The bug behind
 * this was not a wrong condition but a wrong label: the switch said "history"
 * while the payload always carried the archive as well, and the archive is by
 * far the larger part of what leaves the phone. Turning the switch off was
 * therefore a promise about a small thing that said nothing about the big one.
 *
 * There is now one switch for everything that is the user's own working — the
 * archive of every answer, and the recent history — and [SWITCH_TITLE] and
 * [SWITCH_DETAIL] describe exactly that and no more.
 *
 * Settings and the user's own calculators always travel. They are the shape of
 * the app rather than a record of what the user worked out, they are small, and
 * a restore without them would not be a restore — a phone with the right
 * answers and the wrong theme and no saved calculators would look like a
 * stranger's app. So they are not behind a switch, and the detail line says so
 * rather than leaving the user to wonder.
 *
 * Included is the default, so somebody who never touches the switch gets the
 * same backup as before this change. Turning it off is now the thing it looks
 * like: a backup of the setup only.
 */
object BackupContents {

    /** The switch itself. Named for what it decides, not for one of the parts. */
    const val SWITCH_TITLE = "Include my calculations"

    /** The line under the switch: every part that travels, and every part that does not. */
    const val SWITCH_DETAIL =
        "Your whole archive and the recent history go with the backup. " +
            "Off sends only your settings and your own calculators."

    /** Whether the recent history travels. */
    fun historyIncluded(includeCalculations: Boolean): Boolean = includeCalculations

    /**
     * Whether the archive travels.
     *
     * [archiveKeptOnPhone] is the master saving switch, not this one: a phone
     * that has been told to keep nothing has no archive to send, and an empty
     * one would only replace a good copy on the server with nothing.
     */
    fun archiveIncluded(includeCalculations: Boolean, archiveKeptOnPhone: Boolean): Boolean =
        includeCalculations && archiveKeptOnPhone
}
