package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ai.CONTENT_REFUSED
import com.merry8989uk.universalcalculator.ai.UploadDecision
import com.merry8989uk.universalcalculator.ai.uploadDecision
import com.merry8989uk.universalcalculator.data.DataPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A picture the user made does not leave the phone behind their back.
 *
 * The master switch was read in one place — the queue that sends the diagnostics
 * log — and nowhere else. Two screens posted pictures anyway: the handwriting pad
 * turned the strokes into a JPEG and sent it, and the assistant's camera button
 * sent the photo it had just taken. A user who had turned data saving off, and
 * who had never touched the diagnostics switch, was still uploading photographs
 * of their own work.
 *
 * The fix is one named rule ([DataPolicy.maySendContent]) and one decision built
 * on it ([uploadDecision]) that turns a refusal into words instead of a request.
 * These tests hold both: the rule itself, and the fact that a refusal is said
 * plainly rather than dressed up as a network failure with a Retry button under
 * it. Everything below runs on a plain JVM, with no emulator, because "may this
 * leave the device" is a property of a function and not of a phone.
 */
class ContentUploadTest {

    private fun source(path: String): String? = listOf(File(path), File("../$path"), File("../../$path"))
        .firstOrNull { it.exists() }?.readText()

    private val gate: String?
        get() = source("app/src/main/java/com/merry8989uk/universalcalculator/ai/UploadGate.kt")

    private val pad: String?
        get() = source("app/src/main/java/com/merry8989uk/universalcalculator/ui/HandwritingScreen.kt")

    private val assistant: String?
        get() = source("app/src/main/java/com/merry8989uk/universalcalculator/ui/AskAiScreen.kt")

    private val calculator: String?
        get() = source("app/src/main/java/com/merry8989uk/universalcalculator/ui/CalculatorScreen.kt")

    private val attachments: String?
        get() = source("app/src/main/java/com/merry8989uk/universalcalculator/ai/Attachments.kt")

    private val strings: String?
        get() = source("app/src/main/res/values/strings.xml")

    // ---- the rule ----

    @Test fun withDataSavingOffNoPictureMayLeaveTheDevice() {
        assertFalse("a picture may leave with data saving off", DataPolicy.maySendContent(false))
        assertTrue(
            "the pad would still post its sketch with data saving off",
            uploadDecision(false) is UploadDecision.Refused,
        )
    }

    @Test fun withDataSavingOnThePictureMayBeSent() {
        assertTrue(DataPolicy.maySendContent(true))
        assertEquals(UploadDecision.Allowed, uploadDecision(true))
    }

    @Test fun refusingTheDiagnosticsWarehouseIsNotRefusingAPicture() {
        // Saying no to the log going to the warehouse says nothing about a
        // photograph, and saying yes to it does not let one out on its own.
        assertFalse(DataPolicy.mayUpload(true, diagnosticsEnabled = true, shareEnabled = false))
        assertTrue(DataPolicy.maySendContent(true))
        assertTrue(DataPolicy.mayUpload(true, diagnosticsEnabled = true, shareEnabled = true))
        assertFalse(DataPolicy.maySendContent(false))
    }

    // ---- what a refusal is, and what it is not ----

    @Test fun aRefusalSaysWhichSettingStoppedItRatherThanBlamingTheConnection() {
        val message = CONTENT_REFUSED.lowercase()
        assertTrue("the refusal does not name the setting that stopped it", message.contains("data saving"))
        assertTrue("the refusal does not say the picture stayed on the device", message.contains("device"))
        for (wrong in listOf("connection", "reach", "internet", "try again", "offline")) {
            assertFalse(
                "a refusal reads like a network failure: it says \"$wrong\"",
                message.contains(wrong),
            )
        }
    }

    @Test fun aRefusalIsNeverDressedUpAsAFailedRequest() {
        val text = gate ?: return
        // A refusal is the app obeying a setting. If it went through AiResult or
        // AskFailure it would arrive as the assistant's failure, with a request id
        // to look up and a Retry that would refuse again.
        assertFalse("the refusal is built out of an AiResult", text.contains("AiResult"))
        assertFalse("the refusal is built out of an AskFailure", text.contains("AskFailure"))
        assertFalse("the refusal offers a Retry", text.contains("Retry"))
        assertTrue("the decision does not consult the rule", text.contains("maySendContent("))
    }

    // ---- every path that posts a picture asks first ----

    @Test fun theSketchPadAsksBeforeItPostsTheDrawing() {
        val text = pad ?: return
        val asked = text.indexOf("uploadDecision(")
        val posted = text.indexOf("AiClient.solveImage(")
        assertTrue("the pad never asks the rule", asked >= 0)
        assertTrue("the pad posts the sketch", posted >= 0)
        assertTrue("the pad posts the sketch before asking", asked < posted)
        assertTrue("the pad has no switch to ask", text.contains("savingEnabled: Boolean"))
    }

    @Test fun theCameraInTheAssistantAsksBeforeItPostsThePhoto() {
        val text = assistant ?: return
        val camera = text.substring(
            text.indexOf("ActivityResultContracts.TakePicturePreview()"),
            text.indexOf("AiClient.answerImage("),
        )
        assertTrue("the camera posts the photo before asking", camera.contains("uploadDecision("))
    }

    @Test fun theFilePickerAsksBeforeItOpensTheFile() {
        val text = assistant ?: return
        val picker = text.substring(
            text.indexOf("ActivityResultContracts.OpenDocument()"),
            text.indexOf("Attachments.read("),
        )
        assertTrue("the picker reads the file before asking", picker.contains("uploadDecision("))
        assertTrue(
            "the picker does not hand the switch to the reader",
            text.contains("savingEnabled = savingEnabled"),
        )
    }

    @Test fun thePhotoKeyOnTheCalculatorAsksToo() {
        val text = calculator ?: return
        val camera = text.substring(
            text.indexOf("ActivityResultContracts.TakePicturePreview()"),
            text.indexOf("AiClient.solveImage("),
        )
        assertTrue("the calculator's photo key posts before asking", camera.contains("uploadDecision("))
    }

    @Test fun theReaderOfPickedFilesAsksEvenWhenItsCallerForgot() {
        val text = attachments ?: return
        val asked = text.indexOf("uploadDecision(")
        assertTrue("the reader never asks the rule", asked >= 0)
        for (call in listOf("AiClient.answerImage(", "AiClient.solveImage(", "AiClient.transcribe(")) {
            val posted = text.indexOf(call)
            assertTrue("$call is not in the reader", posted >= 0)
            assertTrue("the reader reaches $call before asking", asked < posted)
        }
        assertTrue("the reader has no switch to ask", text.contains("savingEnabled: Boolean"))
    }

    // ---- and the user is told, where they attach it ----

    @Test fun theNoticeUnderTheCameraSaysThePictureGoesToTheAssistant() {
        val words = strings ?: return
        assertTrue(
            "there is no line under the camera saying the picture is sent",
            words.contains("name=\"attach_sent_notice\""),
        )
        assertTrue(
            "the line under the camera does not say it is sent to the assistant",
            words.contains("attach_sent_notice\">A photo or a file you attach is sent to the assistant"),
        )
        val screen = assistant ?: return
        assertTrue("the assistant screen does not draw that line", screen.contains("R.string.attach_sent_notice"))
    }

    @Test fun theNoticeUnderThePadSaysTheDrawingGoesToTheAssistant() {
        val words = strings ?: return
        assertTrue(
            "there is no line under the pad saying the drawing is sent",
            words.contains("name=\"handwriting_sent_notice\""),
        )
        assertTrue(
            "the line under the pad does not say it is sent to the assistant",
            words.contains("handwriting_sent_notice\">What you write here is sent to the assistant"),
        )
        val screen = pad ?: return
        assertTrue("the pad does not draw that line", screen.contains("R.string.handwriting_sent_notice"))
    }
}
