package com.merry8989uk.universalcalculator.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.Format

/**
 * The home-screen standard calculator.
 *
 * Every key sends a broadcast back here, the expression is kept in preferences
 * per widget, and the display is refreshed with RemoteViews.
 */
class CalculatorWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_CLICK = "com.merry8989uk.universalcalculator.WIDGET_CLICK"
        const val EXTRA_KEY = "key"
        private const val PREFS = "widget_calc"

        const val STYLE_SOLID = 0
        const val STYLE_DIM = 1
        const val STYLE_CLEAR = 2

        /** key -> view id, in a fixed order so the request codes stay stable. */
        private val KEYS = listOf(
            "C" to R.id.w_clear, "del" to R.id.w_del, "%" to R.id.w_pct, "/" to R.id.w_div,
            "7" to R.id.w7, "8" to R.id.w8, "9" to R.id.w9, "*" to R.id.w_mul,
            "4" to R.id.w4, "5" to R.id.w5, "6" to R.id.w6, "-" to R.id.w_sub,
            "1" to R.id.w1, "2" to R.id.w2, "3" to R.id.w3, "+" to R.id.w_add,
            "0" to R.id.w0, "00" to R.id.w00, "." to R.id.w_dot, "=" to R.id.w_eq,
        )

        private fun prefs(context: Context) =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun styleOf(context: Context, id: Int): Int =
            prefs(context).getInt("style_$id", STYLE_SOLID)

        fun setStyle(context: Context, id: Int, style: Int) {
            prefs(context).edit().putInt("style_$id", style).apply()
        }

        /** Rebuilds the widget from the stored expression and style. */
        fun render(context: Context, manager: AppWidgetManager, id: Int) {
            val expression = prefs(context).getString("expr_$id", "").orEmpty()
            val views = RemoteViews(context.packageName, R.layout.widget_calculator)

            views.setTextViewText(
                R.id.widget_display,
                if (expression.isEmpty()) "0" else expression,
            )
            views.setInt(
                R.id.widget_root,
                "setBackgroundResource",
                when (styleOf(context, id)) {
                    STYLE_CLEAR -> R.drawable.widget_bg_clear
                    STYLE_DIM -> R.drawable.widget_bg_dim
                    else -> R.drawable.widget_bg_solid
                },
            )
            KEYS.forEachIndexed { index, (key, viewId) ->
                views.setOnClickPendingIntent(viewId, clickIntent(context, id, key, index))
            }
            manager.updateAppWidget(id, views)
        }

        private fun clickIntent(context: Context, id: Int, key: String, index: Int): PendingIntent {
            val intent = Intent(context, CalculatorWidgetProvider::class.java).apply {
                action = ACTION_CLICK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(EXTRA_KEY, key)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getBroadcast(context, id * 100 + index, intent, flags)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val editor = prefs(context).edit()
        appWidgetIds.forEach {
            editor.remove("expr_$it")
            editor.remove("style_$it")
        }
        editor.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_CLICK) return

        val id = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val key = intent.getStringExtra(EXTRA_KEY) ?: return

        val store = prefs(context)
        val current = store.getString("expr_$id", "").orEmpty()
        val next = when (key) {
            "C" -> ""
            "del" -> current.dropLast(1)
            "=" -> try {
                Format.number(Evaluator.evaluate(current.ifBlank { "0" }))
            } catch (e: Exception) {
                "error"
            }
            else -> current + key
        }
        store.edit().putString("expr_$id", next).apply()
        render(context, AppWidgetManager.getInstance(context), id)
    }
}
