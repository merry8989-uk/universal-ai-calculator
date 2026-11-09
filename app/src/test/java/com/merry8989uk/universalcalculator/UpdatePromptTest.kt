package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.updater.updatePromptVisible
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The update prompt, once "Later" is something the app remembers.
 *
 * It was reported as coming back on every single launch: the dismissal lived in
 * a field on the view model, so the process dying threw it away and the dialog
 * was waiting again the next time the app opened. The answer is a stored
 * version, not a stored flag — the release the user put aside has to stay
 * quiet, and the one after it still has to be allowed to ask — and the prompt
 * itself belongs in Settings, where the user goes to look for it, rather than
 * in the way of somebody opening a calculator.
 *
 * The rule below is a plain function for exactly this reason: whether a modal
 * may appear is a decision about two strings and a flag, and it can be read and
 * tested without Android, a view model or a network.
 */
class UpdatePromptTest {

    // ---- the rule ----

    @Test fun aVersionTheUserPutAsideDoesNotComeBack() {
        assertFalse(
            "the version the user said later to was raised again",
            updatePromptVisible(dismissedVersion = "1.4.2", latestVersion = "1.4.2", checking = false),
        )
    }

    @Test fun aNewerVersionMayAskOnce() {
        assertTrue(
            "a genuinely new release was silenced along with the old one",
            updatePromptVisible(dismissedVersion = "1.4.2", latestVersion = "1.5.0", checking = false),
        )
    }

    @Test fun aVersionNeverPutAsideMayAsk() {
        assertTrue(
            "nothing has been put aside, so there is nothing to keep quiet",
            updatePromptVisible(dismissedVersion = "", latestVersion = "1.5.0", checking = false),
        )
    }

    @Test fun aCheckThatIsStillRunningRaisesNothing() {
        assertFalse(
            "half an answer was shown while the check was still running",
            updatePromptVisible(dismissedVersion = "", latestVersion = "1.5.0", checking = true),
        )
    }

    @Test fun nothingToOfferRaisesNothing() {
        assertFalse(updatePromptVisible(dismissedVersion = "", latestVersion = null, checking = false))
        assertFalse(updatePromptVisible(dismissedVersion = "", latestVersion = "", checking = false))
    }

    // ---- the wiring, checked in the source ----

    private fun source(path: String): String? =
        listOf(File("app/src/main/java/com/merry8989uk/universalcalculator/$path"),
               File("src/main/java/com/merry8989uk/universalcalculator/$path"))
            .firstOrNull { it.exists() }?.readText()

    @Test fun theAppOpeningAsksNothingAndShowsNothing() {
        val root = source("ui/AppRoot.kt") ?: return
        assertFalse("the app still asks the release page on the way in", root.contains("checkQuietly"))
        assertFalse("a modal is still drawn over the calculator at launch", root.contains("UpdatePrompt("))
    }

    @Test fun theDismissalOutlivesTheProcess() {
        val vm = source("updater/UpdateViewModel.kt") ?: return
        assertFalse(
            "the dismissal is held in a field again, which is what lost it",
            vm.contains("var dismissedVersion"),
        )
        assertTrue("Later never reaches the settings store", vm.contains("setDismissedUpdateVersion("))

        val repo = source("settings/SettingsRepository.kt") ?: return
        assertTrue(
            "the dismissed version has no key in the settings store",
            repo.contains("stringPreferencesKey(\"update_dismissed_version\")"),
        )
        assertTrue("nothing writes the dismissed version", repo.contains("fun setDismissedUpdateVersion("))
    }

    @Test fun thePromptIsReachableOnlyFromSettings() {
        val screen = source("settings/SettingsScreen.kt") ?: return
        assertTrue("Settings does not consult the rule", screen.contains("updatePromptVisible("))
        assertTrue("Later is not passed through to the view model", screen.contains("updater::later"))
        assertTrue(
            "the deliberate check is gone from Settings",
            screen.contains("updater.check(BuildConfig.VERSION_NAME)"),
        )
    }
}
