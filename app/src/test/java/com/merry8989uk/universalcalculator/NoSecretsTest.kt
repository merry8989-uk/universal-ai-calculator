package com.merry8989uk.universalcalculator

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Nothing in the source should look like a key, a token or a password.
 *
 * CI runs a proper secret scanner over the whole history, but that finds a leak
 * after it has been pushed. This runs on every build and looks at what is about
 * to be committed, so the leak never leaves the machine.
 *
 * The patterns are assembled from pieces rather than written out in full: a test
 * that contains a realistic-looking token is itself flagged by the scanners that
 * read this file.
 */
class NoSecretsTest {

    private val root: File? = listOf(File("."), File(".."))
        .firstOrNull { File(it, "app/src/main").exists() }

    /** The shapes that are almost always a real credential. */
    private val patterns: List<Pair<String, Regex>> = listOf(
        // Cloudflare API token
        "Cloudflare API token" to Regex("cfat" + "[A-Za-z0-9_-]{20,}"),
        // Google API key
        "Google API key" to Regex("AIza" + "[A-Za-z0-9_-]{30,}"),
        // OpenAI-style secret
        "OpenAI secret" to Regex("sk" + "-[A-Za-z0-9]{30,}"),
        // a GitHub personal access token
        "GitHub token" to Regex("gh" + "p_[A-Za-z0-9]{30,}"),
        // a long literal assigned to something named like a secret
        "hardcoded credential" to Regex(
            "(?i)(api[_-]?key|secret|password|passwd|token)\\s*=\\s*\"" + "[A-Za-z0-9/+_-]{24,}\"",
        ),
    )

    /** Files that are source rather than build output or binary. */
    private fun sources(): List<File> {
        val base = root ?: return emptyList()
        return base.walkTopDown()
            .onEnter { it.name !in setOf(".git", "build", ".gradle", "node_modules", "output") }
            .filter { it.isFile }
            .filter { it.extension in setOf("kt", "kts", "js", "ts", "json", "yml", "yaml", "xml", "md", "py", "properties") }
            .toList()
    }

    @Test fun theSourceHasNoCredentialsInIt() {
        val files = sources()
        if (files.isEmpty()) return
        val found = ArrayList<String>()
        for (file in files) {
            val text = runCatching { file.readText() }.getOrNull() ?: continue
            for ((name, pattern) in patterns) {
                val hit = pattern.find(text) ?: continue
                // the test's own patterns are not a leak
                if (file.name == "NoSecretsTest.kt") continue
                found.add("${file.path}: looks like a $name")
            }
        }
        assertTrue("these files appear to contain a credential:\n" + found.joinToString("\n"), found.isEmpty())
    }

    @Test fun theKeyStoreIsNotInTheSourceTree() {
        val files = sources()
        if (files.isEmpty()) return
        val base = root ?: return
        val keyStores = base.walkTopDown()
            .onEnter { it.name != ".git" }
            .filter { it.isFile && (it.extension == "p12" || it.extension == "jks" || it.extension == "keystore") }
            .map { it.path }
            .toList()
        assertTrue(
            "a signing key is in the tree; it belongs in a GitHub secret, not the repo: $keyStores",
            keyStores.isEmpty(),
        )
    }

    @Test fun thePropertiesFileHasNoSigningPassword() {
        val base = root ?: return
        val props = listOf(
            File(base, "gradle.properties"),
            File(base, "local.properties"),
        ).filter { it.exists() }
        for (file in props) {
            val text = file.readText()
            val suspicious = Regex("(?i)(storePassword|keyPassword|signingPassword)\\s*=\\s*[^\\s]{6,}")
                .containsMatchIn(text)
            assertTrue("${file.name} appears to hold a signing password", !suspicious)
        }
    }
}
