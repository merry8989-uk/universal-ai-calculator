package com.merry8989uk.universalcalculator.ui

/**
 * Features that are built but switched off.
 *
 * A flag rather than deleted code: the work is done and tested, and turning it
 * back on is one line. Deleting it would mean writing it again.
 */
internal object Features {

    /**
     * The classic scientific pad.
     *
     * Built, wired and tested, but switched off at the user's request while it
     * is reworked. While this is false:
     *
     *  - the Scientific tab always shows the modern pad
     *  - there is no Sci Classic tab, even in minimal mode
     *  - Settings offers no "Classic Scientific" layout
     *  - a device that had it chosen falls back to the modern pad
     *
     * Nothing is removed, so setting this to true brings all of it back.
     */
    const val SCI_CLASSIC: Boolean = false
}
