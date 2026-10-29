package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.ChatFormat
import com.merry8989uk.universalcalculator.calc.ChatTurn
import com.merry8989uk.universalcalculator.calc.SavedChat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

/**
 * Saved conversations.
 *
 * The point of these is the separation: a chat is kept in its own archive, and
 * never appears among the calculations. The rest is about a plain-text format
 * that has to survive a question containing a pipe, a backslash or a newline —
 * which a maths question often does.
 */
class ChatArchiveTest {

    private fun chat(vararg turns: Pair<String, String>) = SavedChat(
        title = ChatFormat.titleOf(turns.map { ChatTurn(it.first, it.second) }),
        started = "29 October 2026, 14:32",
        model = "deepseekv4.1-flash",
        turns = turns.map { ChatTurn(it.first, it.second) },
    )

    @Test fun aConversationSurvivesARoundTrip() {
        val original = chat(
            "u" to "What is 15 percent of 240?",
            "a" to "ANSWERS\nQ1: 36\n\nEXPLANATIONS\n15/100 x 240 = 36.",
            "u" to "and of 500?",
            "a" to "75",
        )
        val read = ChatFormat.parse(ChatFormat.write(original))
        assertEquals(original.turns, read?.turns)
        assertEquals(original.title, read?.title)
        assertEquals(original.started, read?.started)
    }

    @Test fun aPipeInTheQuestionDoesNotBreakTheLine() {
        val original = chat("u" to "is 3 | 4 the same as 3 divides 4?", "a" to "no")
        val read = ChatFormat.parse(ChatFormat.write(original))
        assertEquals("is 3 | 4 the same as 3 divides 4?", read?.turns?.first()?.text)
    }

    @Test fun aBackslashDoesNotEatTheNextCharacter() {
        // a Windows path in a question used to be the classic way to lose text
        val original = chat("u" to "C:\\Users\\amit", "a" to "a path")
        val read = ChatFormat.parse(ChatFormat.write(original))
        assertEquals("C:\\Users\\amit", read?.turns?.first()?.text)
    }

    @Test fun aLiteralBackslashNStaysLiteral() {
        // "\n" typed by the user must not come back as a line break
        val original = chat("u" to "type \\n here", "a" to "ok")
        val read = ChatFormat.parse(ChatFormat.write(original))
        assertEquals("type \\n here", read?.turns?.first()?.text)
    }

    @Test fun realNewlinesSurvive() {
        val original = chat("u" to "line one\nline two\nline three", "a" to "ok")
        val read = ChatFormat.parse(ChatFormat.write(original))
        assertEquals("line one\nline two\nline three", read?.turns?.first()?.text)
    }

    @Test fun anEmptyFileIsNotAChat() {
        assertNull(ChatFormat.parse(""))
        assertNull(ChatFormat.parse("something else entirely"))
    }

    @Test fun aHalfWrittenFileIsNotAChat() {
        // a crash mid-write leaves a short file; it must not appear as an empty chat
        assertNull(ChatFormat.parse(ChatFormat.HEADER + "\n"))
        assertNull(ChatFormat.parse(ChatFormat.HEADER + "\nonly a title|no date\n"))
    }

    @Test fun aFileWithNoTurnsIsNotAChat() {
        assertNull(ChatFormat.parse(ChatFormat.HEADER + "\ntitle|29 October 2026, 14:32|model\n"))
    }

    @Test fun theTitleIsTheFirstThingAsked() {
        val t = ChatFormat.titleOf(listOf(ChatTurn("a", "hello"), ChatTurn("u", "What is 2+2?")))
        assertEquals("What is 2+2?", t)
    }

    @Test fun aLongQuestionIsShortenedWithAnEllipsis() {
        val long = "a".repeat(200)
        val t = ChatFormat.titleOf(listOf(ChatTurn("u", long)))
        assertTrue("not shortened: ${t.length}", t.length <= 60)
        assertTrue(t.endsWith("..."))
    }

    @Test fun aTitleHasNoCharactersAFileNameCannotHold() {
        val slug = ChatFormat.slug("What is 2+2? (explain!) / please")
        assertTrue("a slash survived: $slug", !slug.contains("/"))
        assertTrue("a question mark survived: $slug", !slug.contains("?"))
        assertTrue("a bracket survived: $slug", !slug.contains("("))
        assertTrue(slug.isNotBlank())
    }

    @Test fun theFileNameSortsByDate() {
        val a = ChatFormat.fileName(LocalDateTime.of(2026, 10, 29, 9, 5), "morning")
        val b = ChatFormat.fileName(LocalDateTime.of(2026, 10, 29, 14, 32), "afternoon")
        assertTrue("the names do not sort by time: $a / $b", a < b)
        assertTrue(a.endsWith(".txt"))
    }

    // ---- the separation that was asked for ----

    @Test fun chatsLiveInTheirOwnFolderNotTheArchive() {
        val store = File("../app/src/main/java/com/merry8989uk/universalcalculator/data/ChatStore.kt")
            .takeIf { it.exists() }
            ?: File("app/src/main/java/com/merry8989uk/universalcalculator/data/ChatStore.kt")
        val text = store.readText()
        assertTrue("the chats folder is not inside the archive folder", text.contains("File(ArchiveStore.root(context), FOLDER)"))
        assertTrue("the folder is not called Chats", text.contains("const val FOLDER = \"Chats\""))
        // and it must never write a calculation into the archive
        assertTrue("the chat store writes archive entries", !text.contains("ArchiveStore.append"))
        assertTrue("the chat store mentions ArchiveEntry", !text.contains("ArchiveEntry"))
    }

    @Test fun theScreenSavesOnlyWhenStoringIsAllowed() {
        val screen = File("app/src/main/java/com/merry8989uk/universalcalculator/ui/AskAiScreen.kt")
            .takeIf { it.exists() }
            ?: File("../app/src/main/java/com/merry8989uk/universalcalculator/ui/AskAiScreen.kt")
        val text = screen.readText()
        assertTrue("the save is not behind the one switch", text.contains("if (savingEnabled)"))
        assertTrue("nothing is saved", text.contains("ChatStore.save"))
    }
}
