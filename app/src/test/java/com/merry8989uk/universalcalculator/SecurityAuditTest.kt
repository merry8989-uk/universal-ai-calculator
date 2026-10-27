package com.merry8989uk.universalcalculator

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The security and privacy findings from the outside audit, pinned as tests.
 *
 * These read the source and the configuration rather than running anything,
 * because the things being protected — a public endpoint's limits, a backup
 * setting, what goes into a log — are all decided at build time.
 */
class SecurityAuditTest {

    private fun file(path: String): String? =
        listOf(File(path), File("../$path")).firstOrNull { it.exists() }?.readText()

    private val worker: String by lazy { file("backend/cloudflare/worker.js").orEmpty() }
    private val manifest: String by lazy { file("app/src/main/AndroidManifest.xml").orEmpty() }
    private val aiClient: String by lazy {
        file("app/src/main/java/com/merry8989uk/universalcalculator/ai/AiClient.kt").orEmpty()
    }
    private val syncClient: String by lazy {
        file("app/src/main/java/com/merry8989uk/universalcalculator/sync/SyncClient.kt").orEmpty()
    }
    private val syncService: String by lazy {
        file("app/src/main/java/com/merry8989uk/universalcalculator/sync/SyncService.kt").orEmpty()
    }

    // ---- 1. the public endpoint has limits ----

    @Test fun theWorkerHasABodyCeiling() {
        if (worker.isBlank()) return
        assertTrue("no body ceiling", "MAX_BODY_BYTES" in worker)
        assertTrue(
            "the ceiling must be checked before the JSON is parsed",
            worker.indexOf("MAX_BODY_BYTES") < worker.indexOf("JSON.parse(raw"),
        )
    }

    @Test fun theWorkerRateLimitsEachCaller() {
        if (worker.isBlank()) return
        assertTrue("no rate limiting", "underRateLimit" in worker)
        assertTrue("no per-caller key", "cf-connecting-ip" in worker)
        assertTrue("the refusal is not a 429", "429" in worker)
    }

    @Test fun theAttachedContextHasItsOwnCeiling() {
        if (worker.isBlank()) return
        assertTrue("no context ceiling", "MAX_CONTEXT_CHARS" in worker)
    }

    // ---- 2. a slot id alone cannot destroy a backup ----

    @Test fun aWriteHasToNameTheVersionItReplaces() {
        if (worker.isBlank()) return
        assertTrue("the worker does not check a version", "currentVersion" in worker)
        assertTrue("a stale write is not refused", "409" in worker)
        if (syncClient.isNotBlank()) {
            assertTrue(
                "the app does not send the version with an upload",
                "put(\"version\", version)" in syncClient,
            )
        }
        if (syncService.isNotBlank()) {
            assertTrue(
                "the app does not remember the version it saw",
                "noteVersion" in syncService,
            )
        }
    }

    @Test fun deletingHasToBeConfirmed() {
        if (worker.isBlank()) return
        assertTrue("delete is not confirmed on the server", "needsConfirm" in worker)
        if (syncClient.isNotBlank()) {
            assertTrue("the app does not confirm a delete", "put(\"confirm\", true)" in syncClient)
        }
    }

    // ---- 3. the backup setting matches the privacy claim ----

    @Test fun androidBackupIsOff() {
        if (manifest.isBlank()) return
        assertTrue(
            "allowBackup is still true, which contradicts the privacy policy",
            "android:allowBackup=\"false\"" in manifest,
        )
        assertTrue(
            "there are no extraction rules saying what may leave the device",
            "dataExtractionRules" in manifest,
        )
    }

    @Test fun theExtractionRulesExcludeEverything() {
        val rules = file("app/src/main/res/xml/data_extraction_rules.xml") ?: return
        assertTrue("cloud backup is not excluded", "<cloud-backup>" in rules)
        assertTrue("device transfer is not excluded", "<device-transfer>" in rules)
        for (domain in listOf("file", "sharedpref", "database", "external")) {
            assertTrue("the $domain domain is not excluded", "domain=\"$domain\"" in rules)
        }
    }

    // ---- 4. the logs carry no user content ----

    @Test fun theAiQuestionIsNotLogged() {
        if (aiClient.isBlank()) return
        // the old line was: DiagnosticsLogger.log("AI", "Ask: ${question.take(140)}")
        assertTrue(
            "the question is still written into the log",
            "question.take(140)" !in aiClient,
        )
        assertTrue(
            "the log should carry the size, not the words",
            "question.length" in aiClient,
        )
    }

    @Test fun thePhotoExpressionIsNotLogged() {
        if (aiClient.isBlank()) return
        assertTrue(
            "what the camera read is still written into the log",
            "Photo read as: \$expr" !in aiClient,
        )
    }

    // ---- the documentation matches the backend ----

    @Test fun theCommentsDescribeTheBackendThatExists() {
        if (aiClient.isBlank()) return
        assertTrue("the comments still name a provider the backend does not use", "Gemini" !in aiClient)
        assertTrue("the comments do not name the real backend", "Workers AI" in aiClient)
    }
}
