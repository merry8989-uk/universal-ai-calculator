package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.formula.RawFormula
import org.json.JSONObject

/**
 * Reads the bundled formula file.
 *
 * `org.json` is a stub on the Android runtime, but the real implementation is already on the
 * test classpath (`testImplementation("org.json:json:20240303")` in app/build.gradle.kts) and
 * this file only ever runs in tests — so the platform stub is not a reason to hand-write a
 * parser here.
 *
 * The file is an asset this repo ships, so a malformed one is a build problem rather than a
 * runtime condition: unparseable text still yields an empty list, which the tests treat as
 * "nothing to check" and fail on separately via their own size assertions.
 */
object MiniJson {

    fun readFormulas(text: String): List<RawFormula> = runCatching {
        val array = JSONObject(text).getJSONArray("formulas")
        val out = ArrayList<RawFormula>(array.length())
        for (i in 0 until array.length()) {
            val entry = array.getJSONObject(i)
            if (!entry.has("c") || !entry.has("t") || !entry.has("f")) continue
            out.add(RawFormula(entry.getString("c"), entry.getString("t"), entry.getString("f")))
        }
        out
    }.getOrDefault(emptyList())
}
