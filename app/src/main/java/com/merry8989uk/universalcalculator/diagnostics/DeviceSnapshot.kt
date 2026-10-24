package com.merry8989uk.universalcalculator.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * What the device looked like when something went wrong.
 *
 * A stack trace says what broke; this says where. A crash that only happens on a
 * phone with 2 GB of RAM, or in a locale where the decimal separator is a comma,
 * looks completely different once that is written next to it.
 *
 * Nothing here identifies the person using the phone: no accounts, no phone
 * number, no advertising id, no location.
 */
object DeviceSnapshot {

    fun lines(context: Context): List<String> = buildList {
        val runtime = Runtime.getRuntime()
        val totalRam = runtime.maxMemory()
        val freeRam = runtime.freeMemory()
        add("app      : ${com.merry8989uk.universalcalculator.BuildConfig.VERSION_NAME}")

        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memory = manager?.let {
            ActivityManager.MemoryInfo().also { info -> it.getMemoryInfo(info) }
        }
        add("device   : ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
        add("android  : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        add("abi      : ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        add("cores    : ${runtime.availableProcessors()}")
        add(
            "heap     : ${mb(freeRam)} free of ${mb(totalRam)}" +
                (memory?.let { "  |  device RAM ${mb(it.totalMem)}, ${mb(it.availMem)} free" } ?: ""),
        )
        memory?.let {
            add("low mem  : ${it.lowMemory}  (threshold ${mb(it.threshold)})")
        }
        add("storage  : ${mb(freeStorageBytes())} free")
        add("screen   : ${screenDescription(context)}")
        add("locale   : ${Locale.getDefault()}  |  tz ${TimeZone.getDefault().id}")
        add("uptime   : ${uptime(context)}")
        add("density  : ${context.resources.displayMetrics.density}")
    }

    /** One block, ready to paste into a report. */
    fun block(context: Context): String = lines(context).joinToString("\n")

    private fun mb(bytes: Long): String = "${bytes / (1024 * 1024)} MB"

    private fun freeStorageBytes(): Long = try {
        val stat = StatFs(Environment.getDataDirectory().path)
        stat.availableBlocksLong * stat.blockSizeLong
    } catch (e: Exception) {
        0L
    }

    private fun screenDescription(context: Context): String {
        val metrics = context.resources.displayMetrics
        val orientation = when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "landscape"
            Configuration.ORIENTATION_PORTRAIT -> "portrait"
            else -> "square"
        }
        return "${metrics.widthPixels}x${metrics.heightPixels}dp @${metrics.density}x, $orientation"
    }

    private fun uptime(context: Context): String {
        val millis = android.os.SystemClock.elapsedRealtime()
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return "${hours}h ${minutes}m since boot"
    }
}
