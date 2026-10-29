package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ai.Attachments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Asking a question about a photo.
 *
 * A photograph of an exam paper once came back as the marks line printed in its
 * header — "11 x 1 = 11" — because the picture was put through the expression
 * reader, which is told to return only an arithmetic string. These tests pin the
 * two things that had to change: the picture is answered when a question comes
 * with it, and the assistant is told what the attachment actually is.
 */
class PhotoQuestionTest {

    // ---- how an attachment is described to the assistant ----

    @Test fun aPhotoIsIntroducedAsAPhoto() {
        val text = Attachments.contextFor("image", "Option D is right.", "Which option for question 4?")
        assertTrue("the assistant is not told this is a photo: $text", text.contains("photo"))
        assertTrue("the question was dropped", text.contains("Which option for question 4?"))
        assertTrue("the answer from the picture was dropped", text.contains("Option D is right."))
    }

    @Test fun audioIsIntroducedAsAudio() {
        val text = Attachments.contextFor("audio", "two plus two", "what is that")
        assertTrue(text.contains("audio"))
        assertTrue(text.contains("two plus two"))
    }

    @Test fun aTextFileIsJustAppended() {
        val text = Attachments.contextFor("text", "some notes", "summarise this")
        assertTrue(text.contains("some notes"))
        assertTrue(text.contains("summarise this"))
    }

    @Test fun aQuestionWithNoAttachmentIsJustTheQuestion() {
        assertEquals("what is 2+2", Attachments.contextFor("", "", "what is 2+2"))
    }

    @Test fun anEmptyQuestionStillGetsAnInstruction() {
        val text = Attachments.contextFor("image", "a picture of a sum", "")
        assertTrue("nothing tells the model what to do", text.isNotBlank())
        assertTrue(text.contains("a picture of a sum"))
    }

    // ---- what the worker is told to do ----

    private val worker: String? = listOf(
        File("../backend/cloudflare/worker.js"),
        File("backend/cloudflare/worker.js"),
    ).firstOrNull { it.exists() }?.readText()

    @Test fun thereIsAPromptForAnsweringAQuestionAboutAPhoto() {
        val w = worker ?: return
        assertTrue("no answer-image prompt", w.contains("ANSWER_IMAGE_SYSTEM"))
        val prompt = w.substringAfter("const ANSWER_IMAGE_SYSTEM = [").substringBefore("].join")
        assertTrue("it does not mention answering", prompt.contains("Answer the question"))
        assertTrue("it does not handle a question number", prompt.contains("question number"))
        assertTrue("it does not handle multiple choice", prompt.contains("multiple-choice"))
    }

    @Test fun theWorkerHasAnAnswerMode() {
        val w = worker ?: return
        assertTrue("no answer mode", w.contains("mode") && w.contains("answer"))
        assertTrue("the question is not read", w.contains("body?.question"))
    }

    @Test fun theExpressionReaderStillAsksForAnExpression() {
        // the calculator's own photo button is meant to read one sum, and must
        // keep doing exactly that
        val w = worker ?: return
        val prompt = w.substringAfter("const OCR_SYSTEM = [").substringBefore("].join")
        assertTrue(prompt.contains("ONLY a calculator expression"))
    }

    @Test fun theAssistantIsToldToAnswerWhatWasAsked() {
        val w = worker ?: return
        val prompt = w.substringAfter("const SYSTEM = [").substringBefore("].join")
        assertTrue("nothing about answering the actual question", prompt.contains("actually asked"))
        assertTrue("nothing about question numbers", prompt.contains("question number"))
        assertTrue("nothing about choosing an option", prompt.contains("choose between options"))
    }

    @Test fun theWorkerAsksSarvamNotCloudflare() {
        val w = worker ?: return
        assertTrue("no Sarvam chat call", w.contains("sarvamChat"))
        assertTrue("no Sarvam vision call", w.contains("sarvamVision"))
        assertTrue("the old Workers AI binding is still called", !w.contains("env.AI.run"))
        assertTrue("a Cloudflare model is still named", !w.contains("@cf/meta/"))
        assertTrue("the key is not read from a secret", w.contains("SARVAM_API_KEY"))
    }

    @Test fun theVisionModelIsOneThatTakesAnImage() {
        // sarvam-105b is text only; glm5.3-flash is the one that reads pictures
        val w = worker ?: return
        assertTrue(w.contains("glm5.3-flash"))
        assertTrue("the image is not sent as an image",
            w.contains("image_url"))
    }

    @Test fun theKeyIsNeverInTheRepository() {
        // a Sarvam key starts with sk_ and is long; none may be committed
        val root = listOf(File(".."), File(".")).firstOrNull { File(it, "backend").exists() } ?: return
        val offenders = ArrayList<String>()
        root.walkTopDown()
            .onEnter { it.name !in setOf(".git", "build", ".gradle", "node_modules") }
            .filter { it.isFile && it.length() < 2_000_000 }
            .filter { it.extension in setOf("kt", "kts", "js", "json", "toml", "yml", "yaml", "md", "xml", "properties", "txt") }
            .forEach { f ->
                val text = runCatching { f.readText() }.getOrNull() ?: return@forEach
                if (Regex("sk_[A-Za-z0-9_]{20,}").containsMatchIn(text)) {
                    offenders.add(f.relativeTo(root).path)
                }
            }
        assertTrue("a Sarvam key is committed in: $offenders", offenders.isEmpty())
    }
}
