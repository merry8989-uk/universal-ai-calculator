package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.AttachStage
import com.merry8989uk.universalcalculator.ui.ReadTarget
import com.merry8989uk.universalcalculator.ui.SketchStage
import com.merry8989uk.universalcalculator.ui.SketchState
import com.merry8989uk.universalcalculator.ui.attachAfterPick
import com.merry8989uk.universalcalculator.ui.attachAfterRead
import com.merry8989uk.universalcalculator.ui.attachCanSend
import com.merry8989uk.universalcalculator.ui.kind
import com.merry8989uk.universalcalculator.ui.readTarget
import com.merry8989uk.universalcalculator.ui.sketchAfterRead
import com.merry8989uk.universalcalculator.ui.sketchCleared
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A picture the user picked has to be seen before it is sent, and a read has to
 * be seen before it is used.
 *
 * Both screens went from "the user picked or drew something" straight to "the
 * model has it", so the photo was on the wire before it was on screen, and the
 * text that came back was posted as a question on the user's behalf — which is
 * why a user who wanted to correct a misread expression had to retype it. The
 * stages below are the fix said as a decision: what the screen should be showing
 * after a pick and after a read, and what happens to the drawing when a read
 * fails.
 *
 * These are the parts that need no Android and no network: the transitions
 * between stages. What the screens draw for each stage is checked by reading the
 * screens themselves at the end.
 */
class AttachFlowTest {

    // ---- the picture, before it is sent ----

    @Test fun pickingAPictureHoldsItForALookAndDoesNotSendIt() {
        val after = attachAfterPick("image", "photo")
        assertEquals(
            "a picked picture did not end up held for the user to see",
            AttachStage.Previewing("image", "photo"),
            after,
        )
        assertFalse(
            "picking a photo was treated as agreeing to send it",
            after is AttachStage.Sending,
        )
        assertFalse("a pick produced text nobody asked for yet", after is AttachStage.Showing)
    }

    @Test fun aHeldPictureIsTheOnlyThingSendWillTake() {
        assertTrue(attachCanSend(AttachStage.Previewing("image", "photo")))
        assertFalse("nothing picked, yet there is something to send", attachCanSend(AttachStage.Idle))
        assertFalse(
            "text already read was offered as something to send again",
            attachCanSend(AttachStage.Showing("image", "photo", "2+2")),
        )
        assertFalse(
            "a request already in flight could be sent a second time",
            attachCanSend(AttachStage.Sending("image", "photo")),
        )
    }

    // ---- what a read did to the screen ----

    @Test fun aReadThatCameBackWithTextIsShownAndNotSent() {
        val after = attachAfterRead(
            picked = AttachStage.Sending("image", "photo"),
            text = "x^2 - 4 = 0",
            error = null,
        )
        assertEquals(
            "the text the model read was not put on screen for the user",
            AttachStage.Showing("image", "photo", "x^2 - 4 = 0"),
            after,
        )
    }

    @Test fun aFailedReadKeepsThePictureSoItCanBeTriedAgain() {
        val after = attachAfterRead(
            picked = AttachStage.Sending("image", "photo"),
            text = "",
            error = "Could not read maths in that photo.",
        )
        assertTrue("a failed read was not a failure", after is AttachStage.Failed)
        val failed = after as AttachStage.Failed
        assertTrue("the failure dropped the picture it was reading", failed.keepsPicture)
        assertTrue("there was nothing left to try again", attachCanSend(failed))
        assertEquals(
            "the failure did not say what went wrong",
            "Could not read maths in that photo.",
            failed.message,
        )
        assertEquals("the failure forgot what it was holding", "photo", failed.label)
    }

    @Test fun aReadThatCameBackEmptyIsAFailureAndNotAnEmptyAnswer() {
        val after = attachAfterRead(
            picked = AttachStage.Sending("image", "photo"),
            text = "   ",
            error = null,
        )
        assertTrue("a blank read was shown as if it were text", after is AttachStage.Failed)
        val failed = after as AttachStage.Failed
        assertTrue("a blank read left no message at all", failed.message.isNotBlank())
        assertTrue("a blank read lost the picture", failed.keepsPicture)
    }

    @Test fun aRefusedUploadHoldsNothingToTryAgain() {
        // A refusal at the moment of picking never read the file, so there is no
        // picture in hand: offering Try again would only refuse again.
        val refused = AttachStage.Failed(
            kind = "image",
            label = "photo",
            message = "Not sent: data saving is off.",
            keepsPicture = false,
        )
        assertFalse("a refused upload offered a retry that cannot work", attachCanSend(refused))
    }

    @Test fun aRetryThatFailsAgainStillHoldsThePicture() {
        val once = attachAfterRead(AttachStage.Sending("image", "photo"), "", "No connection.")
        val twice = attachAfterRead(once, "", "No connection.")
        assertTrue("the picture was lost on the second failure", attachCanSend(twice))
        assertTrue("the retry lost what it was holding", twice.kind() == "image")
    }

    // ---- where an accepted reading goes ----

    @Test fun aReadPictureGoesIntoTheInputTheUserWouldHaveTypedIn() {
        assertEquals(ReadTarget.INPUT, readTarget("image"))
        assertEquals(
            "a file's text was put in the question box instead of kept as context",
            ReadTarget.ATTACHMENT,
            readTarget("file"),
        )
    }

    // ---- the sketch pad ----

    @Test fun aSketchReadThatFoundAnExpressionIsHeldForTheUser() {
        val drawn = SketchState(stage = SketchStage.Previewing, hasDrawing = true)
        val after = sketchAfterRead(drawn, "3/4 + 1/2", null)
        assertEquals(SketchStage.Read("3/4 + 1/2"), after.stage)
        assertTrue("accepting a reading threw the drawing away", after.hasDrawing)
    }

    @Test fun aFailedSketchReadKeepsTheDrawingAndSaysWhy() {
        val drawn = SketchState(stage = SketchStage.Previewing, hasDrawing = true)
        val after = sketchAfterRead(drawn, null, "Could not reach the reader. Check your connection.")
        assertTrue("a failed read was not a failure", after.stage is SketchStage.Failed)
        val failedStage = after.stage as SketchStage.Failed
        assertEquals(
            "the failure did not say what went wrong",
            "Could not reach the reader. Check your connection.",
            failedStage.message,
        )
        assertTrue("the drawing was dropped by a failed read", after.hasDrawing)
        assertTrue("the pad offered no way to try the same drawing again", after.canRetry)
    }

    @Test fun onlyClearingThePadDropsTheDrawing() {
        val drawn = SketchState(stage = SketchStage.Previewing, hasDrawing = true)
        val failed = sketchAfterRead(drawn, "", null)
        assertTrue("a failed read cleared the drawing", failed.hasDrawing)
        assertFalse("a blank pad still had a drawing", sketchCleared().hasDrawing)
        assertEquals(SketchStage.Drawing, sketchCleared().stage)
    }

    @Test fun anEmptySketchReadFailsWithAReasonRatherThanSilence() {
        val after = sketchAfterRead(SketchState(hasDrawing = true), null, null)
        val failed = after.stage as SketchStage.Failed
        assertTrue("an empty read said nothing at all", failed.message.isNotBlank())
    }

    // ---- the screens themselves ----

    private fun source(path: String): String? =
        listOf(File("app/src/main/java/com/merry8989uk/universalcalculator/$path"),
               File("src/main/java/com/merry8989uk/universalcalculator/$path"))
            .firstOrNull { it.exists() }?.readText()

    @Test fun bothScreensAskTheDataSwitchBeforeTheyReadOrSend() {
        val ask = source("ui/AskAiScreen.kt") ?: return
        val pad = source("ui/HandwritingScreen.kt") ?: return
        // The gate has to be in front of the pick and in front of the send: the
        // switch can be turned off while a preview is on screen.
        assertTrue(
            "the attach row no longer asks the data switch",
            ask.contains("uploadDecision(savingEnabled)"),
        )
        assertTrue(
            "the attach row asks the switch only once, so the send is ungated",
            ask.split("uploadDecision(savingEnabled)").size - 1 >= 2,
        )
        assertTrue(
            "the pad no longer asks the data switch",
            pad.contains("uploadDecision(savingEnabled)"),
        )
        assertTrue(
            "the pad asks the switch only once, so the send is ungated",
            pad.split("uploadDecision(savingEnabled)").size - 1 >= 2,
        )
    }

    @Test fun neitherScreenAsksTheAssistantFromThePickerAnyMore() {
        val ask = source("ui/AskAiScreen.kt") ?: return
        val pad = source("ui/HandwritingScreen.kt") ?: return
        // The bug was a launcher that ended in send(...). The launchers now end
        // in a held picture; the ask is a separate tap on the card.
        assertTrue(
            "the picker still ends in an ask, so the picture is sent unseen",
            ask.contains("attachAfterPick("),
        )
        assertFalse(
            "the auto-send comment survived the fix",
            ask.contains("give the solution straight away"),
        )
        assertTrue(
            "the pad no longer shows the page before sending it",
            pad.contains("SketchStage.Previewing"),
        )
    }
}
