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
        assertTrue("it does not mention answering the named question",
            prompt.contains("answer THAT one only"))
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

    @Test fun noRemovedCloudflareNameIsStillReferredTo() {
        // deleting a constant and leaving a use of it compiles fine and only
        // fails at runtime, which is exactly how the photo path broke once
        val w = worker ?: return
        for (gone in listOf("VISION_ALLOWED", "VISION_MODELS", "FALLBACK_MODELS", "WHISPER_MODELS", "imageShapes")) {
            assertTrue("$gone was removed but is still used", !w.contains(gone))
        }
    }

    @Test fun withNoQuestionTypedTheDefaultAsksForTheQuestionsToBeSolved() {
        // a picture of a question paper holds the questions; describing it is
        // not an answer, and neither is reading the marks line in its header
        val text = Attachments.contextFor("image", "a page of questions", "")
        assertTrue("the default does not ask for a solution: $text", text.contains("solve", ignoreCase = true))
        assertTrue("the default does not mention the questions", text.contains("question", ignoreCase = true))
    }

    @Test fun thePromptSolvesWhatIsPrintedOnThePage() {
        val w = worker ?: return
        val prompt = w.substringAfter("const ANSWER_IMAGE_SYSTEM = [").substringBefore("].join")
        assertTrue("it does not say the question is in the picture",
            prompt.contains("THE QUESTION IS USUALLY PRINTED IN THE PICTURE"))
        assertTrue("it does not say to start at question 1", prompt.contains("starting at number 1"))
        assertTrue("it does not ignore the marks header", prompt.contains("Full Marks"))
        assertTrue("it does not say what to do when it runs out of room",
            prompt.contains("which numbers are left"))
    }

    @Test fun aPageOfAnswersHasRoomToFinish() {
        val w = worker ?: return
        // six worked answers were cut off at 900 tokens
        val block = w.substringAfter("ANSWER_IMAGE_SYSTEM, question, image,")
        assertTrue("the answer budget is still too small: $block", !block.startsWith(" 900"))
    }

    @Test fun theAssistantKeepsTheConversation() {
        // a follow-up that starts from nothing is a search box, not a chat
        val w = worker ?: return
        assertTrue("history is not accepted", w.contains("body?.history"))
        assertTrue("history is not passed to the model", w.contains("...earlier"))
    }

    @Test fun aHardQuestionGoesToTheThinkingModel() {
        val w = worker ?: return
        assertTrue("no routing", w.contains("function pickModel"))
        assertTrue("no thinking model", w.contains("deepseekv4.1-flash"))
        val fn = w.substringAfter("function pickModel").substringBefore("\n}")
        assertTrue("numbers do not route to thinking", fn.contains("[0-9]"))
        assertTrue("Indian scripts are not routed to sarvam", fn.contains("u0900"))
    }

    @Test fun theAnswerComesBeforeTheReasoning() {
        val w = worker ?: return
        assertTrue("no answer-first format", w.contains("const ANSWER_FORMAT"))
        val f = w.substringAfter("const ANSWER_FORMAT = [").substringBefore("].join")
        assertTrue("it does not ask for the answers first", f.contains("Start with a line: ANSWERS"))
        assertTrue("it does not ask for explanations after", f.contains("EXPLANATIONS"))
        assertTrue("it does not ask why the wrong options are wrong",
            f.contains("why each of the other options is wrong"))
    }

    @Test fun everySubjectIsAnsweredNotJustMaths() {
        val w = worker ?: return
        val sys = w.substringAfter("const SYSTEM = [").substringBefore("].join")
        assertTrue("science is not mentioned", sys.contains("chemistry"))
        assertTrue("commerce is not mentioned", sys.contains("commerce"))
        assertTrue("arts is not mentioned", sys.contains("history"))
        assertTrue("it does not refuse non-maths", sys.contains("Never refuse"))
    }

    @Test fun theAssistantCanSearchTheWeb() {
        val w = worker ?: return
        assertTrue("no search", w.contains("async function webSearch"))
        assertTrue("no search tool", w.contains("name: \"web_search\""))
        assertTrue("no search loop", w.contains("async function sarvamChatWithTools"))
        assertTrue("no fallback when the search engine refuses", w.contains("en.wikipedia.org"))
        assertTrue("the last round must answer rather than search again", w.contains("const last = round === ROUNDS - 1"))
    }

    @Test fun theModelIsToldTodaysDate() {
        // it answered as if it were months earlier until this was added
        val w = worker ?: return
        assertTrue("no dated system prompt", w.contains("function systemWithDate"))
        assertTrue("no date in the prompt", w.contains("Today is "))
        assertTrue("search results are not trusted over memory",
            w.contains("Believe the search results over what you remember"))
    }

    @Test fun aPdfIsReadPastItsFirstFewPages() {
        val file = listOf(
            File("../app/src/main/java/com/merry8989uk/universalcalculator/ai/Attachments.kt"),
            File("app/src/main/java/com/merry8989uk/universalcalculator/ai/Attachments.kt"),
        ).firstOrNull { it.exists() } ?: return
        val text = file.readText()
        val pages = Regex("MAX_PDF_PAGES = (\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull()
        assertTrue("only $pages pages are read", pages != null && pages >= 20)
        assertTrue("the reader does not say how much it read", text.contains("The rest was not read"))
    }

    @Test fun theSearchHasARealEngineAndTwoFallbacks() {
        val w = worker ?: return
        assertTrue("Tavily is not used", w.contains("api.tavily.com"))
        assertTrue("the search key is not read from a secret", w.contains("TAVILY_API_KEY"))
        assertTrue("no DuckDuckGo fallback", w.contains("html.duckduckgo.com"))
        assertTrue("no Wikipedia fallback", w.contains("en.wikipedia.org"))
        // Tavily must be tried before the keyless engines, not after
        val tavilyAt = w.indexOf("api.tavily.com")
        val ddgAt = w.indexOf("html.duckduckgo.com")
        assertTrue("Tavily is tried after DuckDuckGo", tavilyAt in 0 until ddgAt)
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
