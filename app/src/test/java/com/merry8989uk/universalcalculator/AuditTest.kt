package com.merry8989uk.universalcalculator

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The audit, kept as a test so it does not have to be done by hand again.
 *
 * These checks look at the source as text. They are blunt, but the things they
 * catch — a string nobody uses, an object nothing reaches, a setting with no
 * control — are exactly the things that quietly pile up.
 */
class AuditTest {

    private val root: File? = listOf(File("app/src/main"), File("src/main"))
        .firstOrNull { it.exists() }

    private fun allKotlin(): List<File> {
        val base = root ?: return emptyList()
        return base.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun kotlinText(): String =
        allKotlin().joinToString("\n") { it.readText() }

    private fun stringsFile(): String? {
        val base = root ?: return null
        val f = File(base, "res/values/strings.xml")
        return if (f.exists()) f.readText() else null
    }

    // ---- no string is left behind ----

    @Test fun everyStringIsUsedSomewhere() {
        val xml = stringsFile() ?: return
        val code = kotlinText()
        val layouts = (root ?: return).walkTopDown()
            .filter { it.isFile && it.extension == "xml" && it.parentFile?.name?.startsWith("values") != true }
            .joinToString("\n") { it.readText() }

        val unused = Regex("<string name=\"([^\"]+)\"").findAll(xml)
            .map { it.groupValues[1] }
            .filter { name ->
                !Regex("R\\.string\\.${Regex.escape(name)}\\b").containsMatchIn(code) &&
                    !Regex("@string/${Regex.escape(name)}\\b").containsMatchIn(layouts)
            }
            .toList()

        assertTrue("these strings are defined but never used: $unused", unused.isEmpty())
    }

    // ---- no object is unreachable ----

    @Test fun everyObjectIsReachable() {
        val files = allKotlin()
        if (files.isEmpty()) return
        val texts = files.associateWith { it.readText() }

        // entry points are named by the manifest, not by code
        val namedByTheSystem = setOf("MainActivity", "UniversalCalculatorApp", "WidgetConfigActivity")

        val unreachable = ArrayList<String>()
        for ((file, text) in texts) {
            for (m in Regex("^(?:internal |private |public )?object (\\w+)", RegexOption.MULTILINE).findAll(text)) {
                val name = m.groupValues[1]
                if (name in namedByTheSystem) continue
                val elsewhere = texts.filterKeys { it != file }.values.joinToString("\n")
                if (!Regex("\\b${Regex.escape(name)}\\b").containsMatchIn(elsewhere)) unreachable.add(name)
            }
        }
        assertTrue("these objects are never reached from anywhere: $unreachable", unreachable.isEmpty())
    }

    // ---- every setting has a control ----

    @Test fun everySettingHasAControlInSettings() {
        val base = root ?: return
        val state = File(base, "java/com/merry8989uk/universalcalculator/settings/SettingsState.kt")
        val screen = File(base, "java/com/merry8989uk/universalcalculator/settings/SettingsScreen.kt")
        if (!state.exists() || !screen.exists()) return

        val fields = Regex("^\\s*val (\\w+)\\s*:", RegexOption.MULTILINE)
            .findAll(state.readText())
            .map { it.groupValues[1] }
            .toList()
        val shown = screen.readText()

        // a few are read-only projections of other settings, not settings of their own
        val notSettings = setOf("savedExpression")
        val withoutControl = fields
            .filter { it !in notSettings }
            .filter { !Regex("state\\.${Regex.escape(it)}\\b").containsMatchIn(shown) }

        assertTrue(
            "these settings have no control on the Settings screen, so they can never be changed: " +
                "$withoutControl",
            withoutControl.isEmpty(),
        )
    }
}
