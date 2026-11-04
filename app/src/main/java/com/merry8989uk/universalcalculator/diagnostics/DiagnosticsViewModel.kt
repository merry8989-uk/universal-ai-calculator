package com.merry8989uk.universalcalculator.diagnostics

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiagnosticsViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app.applicationContext

    var logBytes: Long by mutableStateOf(0L)
        private set

    var message: String? by mutableStateOf(null)
        private set

    fun refresh() {
        logBytes = DiagnosticsLogger.sizeBytes()
    }

    fun clear() {
        DiagnosticsLogger.clear()
        refresh()
        message = "Logs cleared"
    }

    /** Where the last export landed, so it can be named even if sharing fails. */
    var lastExportPath: String? by mutableStateOf(null)
        private set

    var exporting: Boolean by mutableStateOf(false)
        private set

    fun exportAndShare(format: LogExporter.Format, appName: String, version: String) {
        if (exporting) return
        exporting = true
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    LogExporter.export(context, format, appName, version)
                }
                lastExportPath = file.absolutePath
                // share now reports its own failure instead of throwing into the void
                val problem = LogExporter.share(context, file, format.mime)
                message = if (problem == null) {
                    "Exported ${file.name} · ${(file.length() / 1024).coerceAtLeast(1)} KB"
                } else {
                    problem
                }
            } catch (e: Exception) {
                message = "Export failed: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                exporting = false
            }
        }
    }

    fun dismissMessage() {
        message = null
    }
}
