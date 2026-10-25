package com.merry8989uk.universalcalculator.data

import android.content.Context
import com.merry8989uk.universalcalculator.calc.NamedMatrix
import com.merry8989uk.universalcalculator.calc.NamedMatrixText
import com.merry8989uk.universalcalculator.calc.defaultNamedMatrices
import java.io.File

/**
 * The named matrices, kept on the device.
 *
 * They live in a file rather than in the settings, because a matrix is content
 * the user made, not a preference — and it should survive a settings reset.
 */
object MatrixStore {

    private const val FILE = "named_matrices.txt"

    private fun file(context: Context) = File(context.filesDir, FILE)

    fun load(context: Context): List<NamedMatrix> {
        val f = file(context)
        if (!f.exists()) return defaultNamedMatrices()
        val read = runCatching { NamedMatrixText.parse(f.readText()) }.getOrDefault(emptyList())
        return read.ifEmpty { defaultNamedMatrices() }
    }

    fun save(context: Context, matrices: List<NamedMatrix>) {
        runCatching { file(context).writeText(NamedMatrixText.serialize(matrices)) }
    }
}
