package com.merry8989uk.universalcalculator.data

/**
 * What happened when a line was offered to the archive.
 *
 * A calculator that refuses to calculate because its diary is full is a worse
 * calculator, so a failed write never stops the sum. What it must not do is
 * pretend: the whole reason the archive exists is that a line written down is
 * still there tomorrow, and a line that was never written while the screen said
 * nothing is a record the user believes in and does not have.
 *
 * Kept as a plain type with one plain function so the decision can be tested
 * without a device or a file system.
 */
sealed interface ArchiveWrite {

    /** The line is in the archive. */
    data object Written : ArchiveWrite

    /**
     * The line is not in the archive.
     *
     * [reason] is for the diagnostics log and for a developer reading a report;
     * the user gets a sentence that says what it means for them.
     */
    data class Failed(val reason: String) : ArchiveWrite
}

/**
 * Runs one write and says plainly whether it happened.
 *
 * Both ways of failing come out the same: the store returns false when the
 * folder could not be written, and throws when the disk is full or the path has
 * gone. A user cannot act on the difference, so neither can this.
 */
fun attemptArchiveWrite(write: () -> Boolean): ArchiveWrite = try {
    if (write()) {
        ArchiveWrite.Written
    } else {
        ArchiveWrite.Failed("the archive folder could not be written")
    }
} catch (e: Exception) {
    ArchiveWrite.Failed(e.message ?: e.javaClass.simpleName)
}
