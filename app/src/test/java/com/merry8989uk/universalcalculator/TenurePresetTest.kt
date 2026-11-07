package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Growth
import com.merry8989uk.universalcalculator.calc.TimeUnit
import com.merry8989uk.universalcalculator.ui.TENURE_PRESETS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one-tap tenures.
 *
 * Each carries its own unit. A chip reading "6 months" while the box beside it
 * says years would set six years, which is not what anybody read on the chip —
 * so the number and the unit travel together, and this is the check that they
 * still do.
 */
class TenurePresetTest {

    private fun yearsOf(label: String): Double {
        val preset = TENURE_PRESETS.first { it.label == label }
        return Growth.years(preset.value.toDouble(), preset.unit)
    }

    @Test fun theFourTheDesignAskedForAreThere() {
        assertEquals(
            listOf("52 weeks", "6 months", "1 year", "5 years"),
            TENURE_PRESETS.map { it.label },
        )
    }

    @Test fun eachPresetCarriesItsOwnUnit() {
        for (preset in TENURE_PRESETS) {
            assertTrue("no unit on ${preset.label}", preset.unit == TimeUnit.WEEK ||
                preset.unit == TimeUnit.MONTH || preset.unit == TimeUnit.YEAR)
        }
        // and the weeks chip is weeks, not years
        assertEquals(TimeUnit.WEEK, TENURE_PRESETS.first { it.label == "52 weeks" }.unit)
        assertEquals(TimeUnit.MONTH, TENURE_PRESETS.first { it.label == "6 months" }.unit)
    }

    @Test fun everyPresetMeansWhatItsLabelSays() {
        // this is the whole point: 52 weeks and 1 year are the same tenure, and
        // a chip that said weeks while setting years would break that
        assertEquals(1.0, yearsOf("52 weeks"), 1e-9)
        assertEquals(1.0, yearsOf("1 year"), 1e-9)
        assertEquals(0.5, yearsOf("6 months"), 1e-9)
        assertEquals(5.0, yearsOf("5 years"), 1e-9)
    }

    @Test fun noPresetIsRepeated() {
        assertEquals(TENURE_PRESETS.size, TENURE_PRESETS.map { it.label }.toSet().size)
    }
}
