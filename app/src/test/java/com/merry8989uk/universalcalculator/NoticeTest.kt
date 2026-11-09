package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ai.NoticeClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The notice board: what the developer can say to every installed app.
 *
 * There is no push service here. A push service means an account, a config file,
 * Play Services and a second thing that can fail silently; for a message read
 * once when the app opens, asking the worker is enough.
 */
class NoticeTest {

    @Test fun aNoticeIsReadOutOfTheWorkersAnswer() {
        val notice = NoticeClient.parse(
            """{"notice":{"id":"7","title":"New engines","body":"Interest now handles TDS.","level":"important","link":"https://example.com","published":"2026-11-09T07:00:00Z"}}""",
        )
        assertEquals("7", notice?.id)
        assertEquals("New engines", notice?.title)
        assertTrue("an important one is not marked important", notice?.isImportant == true)
    }

    @Test fun anEmptyBoardIsNotAnError() {
        assertNull(NoticeClient.parse("""{"notice":null}"""))
        assertNull(NoticeClient.parse("not json at all"))
    }

    @Test fun aNoticeWithNoTitleOrBodyIsIgnored() {
        // half a message is worse than none: it would show an empty card
        assertNull(NoticeClient.parse("""{"notice":{"id":"1","title":"","body":"x"}}"""))
        assertNull(NoticeClient.parse("""{"notice":{"id":"1","title":"x","body":""}}"""))
        assertNull(NoticeClient.parse("""{"notice":{"id":"","title":"x","body":"y"}}"""))
    }

    @Test fun theWorkerKeepsTheBoardBehindAToken() {
        val worker = listOf(
            File("backend/cloudflare/worker.js"),
            File("../backend/cloudflare/worker.js"),
        ).firstOrNull { it.exists() }?.readText() ?: return

        assertTrue("there is no notice route", worker.contains("\"/notice\""))
        // reading is public — the app has no account — but writing is not
        assertTrue("writing the board is not checked", worker.contains("statsAllowed(request, env)"))
        assertTrue("a notice cannot be cleared", worker.contains("notice:current"))
        assertTrue("a notice is not length-checked", worker.contains("slice(0, 1000)"))
    }

    @Test fun theNoticeCanBeSwipedAway() {
        val strip = listOf(
            File("app/src/main/java/com/merry8989uk/universalcalculator/ui/NoticeStrip.kt"),
            File("src/main/java/com/merry8989uk/universalcalculator/ui/NoticeStrip.kt"),
        ).firstOrNull { it.exists() }?.readText() ?: return

        assertTrue("the notice cannot be swiped", strip.contains("detectHorizontalDragGestures"))
        // both ways: there is no list here to imply a direction
        assertTrue("it only swipes one way", strip.contains("abs(offsetX.value)"))
        // let go early and it comes back rather than vanishing
        assertTrue("a short drag loses the message", strip.contains("offsetX.animateTo(0f"))
        // and it must actually report the dismissal, or it reappears next launch
        assertTrue("swiping does not dismiss it", strip.contains("onDismiss()"))
        // a gesture with no visible alternative is one some people never find
        assertTrue("there is no tap alternative", strip.contains(".clickable { onDismiss() }"))
    }

    @Test fun theAppShowsItOnceAndNotOnEveryLaunch() {
        val client = listOf(
            File("app/src/main/java/com/merry8989uk/universalcalculator/ai/NoticeClient.kt"),
            File("src/main/java/com/merry8989uk/universalcalculator/ai/NoticeClient.kt"),
        ).firstOrNull { it.exists() }?.readText() ?: return
        assertTrue("the seen id is not kept", client.contains("fun markSeen("))
        assertTrue("a seen notice would show again", client.contains("fun isNew("))

        val root = listOf(
            File("app/src/main/java/com/merry8989uk/universalcalculator/ui/AppRoot.kt"),
            File("src/main/java/com/merry8989uk/universalcalculator/ui/AppRoot.kt"),
        ).firstOrNull { it.exists() }?.readText() ?: return
        assertTrue("the app never asks for a notice", root.contains("NoticeClient.refresh("))
        assertTrue("the notice is never drawn", root.contains("NoticeStrip("))
        assertTrue("dismissing does not record it", root.contains("NoticeClient.markSeen("))
        // and it must not be fetched on the main thread
        assertTrue("the fetch blocks the screen it opens on",
            root.contains("withContext(kotlinx.coroutines.Dispatchers.IO)"))
    }
}
