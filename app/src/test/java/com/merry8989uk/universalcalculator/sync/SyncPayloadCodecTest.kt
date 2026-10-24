package com.merry8989uk.universalcalculator.sync

import com.merry8989uk.universalcalculator.calc.CustomCalculator
import com.merry8989uk.universalcalculator.calc.CustomInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The payload that travels, before encryption. */
class SyncPayloadCodecTest {

    private val calculator = CustomCalculator(
        id = "c1",
        name = "Loan EMI",
        description = "Monthly payment",
        inputs = listOf(CustomInput("p", "Principal", 100000.0), CustomInput("r", "Rate", 0.01)),
        expression = "p*r",
        unit = "\u20B9",
    )

    @Test fun everythingSurvivesARoundTrip() {
        val payload = SyncPayload(
            createdAt = 1_700_000_000_000L,
            settings = mapOf("themeName" to "Midnight Gold", "decimals" to "4"),
            calculators = listOf(calculator),
            history = "1+1 = 2\n2+2 = 4",
        )
        val back = SyncPayloadCodec.decode(SyncPayloadCodec.encode(payload))!!
        assertEquals(payload.createdAt, back.createdAt)
        assertEquals(payload.settings, back.settings)
        assertEquals(payload.calculators, back.calculators)
        assertEquals(payload.history, back.history)
    }

    @Test fun anEmptyPayloadRoundTrips() {
        val payload = SyncPayload(createdAt = 42L)
        val back = SyncPayloadCodec.decode(SyncPayloadCodec.encode(payload))!!
        assertEquals(42L, back.createdAt)
        assertTrue(back.settings.isEmpty())
        assertTrue(back.calculators.isEmpty())
        assertEquals("", back.history)
    }

    @Test fun settingsWithAwkwardCharactersSurvive() {
        val payload = SyncPayload(
            createdAt = 1L,
            settings = mapOf("a=b" to "line\nbreak", "slash" to "back\\slash", "pipe" to "x|y"),
        )
        val back = SyncPayloadCodec.decode(SyncPayloadCodec.encode(payload))!!
        assertEquals("line\nbreak", back.settings["a=b"])
        assertEquals("back\\slash", back.settings["slash"])
        assertEquals("x|y", back.settings["pipe"])
    }

    @Test fun historyWithManyLinesSurvives() {
        val history = (1..40).joinToString("\n") { "$it+$it = ${it * 2}" }
        val payload = SyncPayload(createdAt = 1L, history = history)
        assertEquals(history, SyncPayloadCodec.decode(SyncPayloadCodec.encode(payload))!!.history)
    }

    @Test fun severalCalculatorsSurvive() {
        val two = calculator.copy(id = "c2", name = "Other", inputs = emptyList(), expression = "1+1")
        val payload = SyncPayload(createdAt = 1L, calculators = listOf(calculator, two))
        val back = SyncPayloadCodec.decode(SyncPayloadCodec.encode(payload))!!
        assertEquals(listOf("c1", "c2"), back.calculators.map { it.id })
    }

    @Test fun aWrongHeaderIsRejected() {
        assertNull(SyncPayloadCodec.decode("SOMETHING ELSE\n1\n0\n\n"))
        assertNull(SyncPayloadCodec.decode(""))
    }

    @Test fun aPayloadPromisingMoreThanItCarriesIsRejected() {
        // says five settings, supplies one: the reader must refuse rather than guess
        val broken = "NEXUSSYNC1\n1\n5\na\nb\n"
        assertNull(SyncPayloadCodec.decode(broken))
    }

    @Test fun aPayloadWithAnEmptyCalculatorBlockParses() {
        // an empty block is a valid block: it just means no custom calculators
        val payload = SyncPayloadCodec.decode("NEXUSSYNC1\n1\n0\n\n")
        assertTrue(payload != null)
        assertTrue(payload!!.calculators.isEmpty())
        assertTrue(payload.settings.isEmpty())
    }

    @Test fun theSummaryReadsWell() {
        val one = SyncPayload(createdAt = 1L, calculators = listOf(calculator), settings = mapOf("a" to "b"))
        assertEquals("1 calculator, 1 settings", one.summary())
        val none = SyncPayload(createdAt = 1L)
        assertEquals("0 calculators, 0 settings", none.summary())
        val withHistory = SyncPayload(createdAt = 1L, history = "x = 1")
        assertTrue(withHistory.summary().contains("history included"))
    }
}
