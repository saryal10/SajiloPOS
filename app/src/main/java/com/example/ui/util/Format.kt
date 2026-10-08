package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Formatting helpers shared by screens. Money uses thin, readable groupings and a
 * dedicated rupee glyph so every amount looks identical across the app.
 */
object Format {

    /** Indian/Nepali digit grouping: 1,25,000 instead of 125,000. */
    private val groupingLocale: Locale = Locale.forLanguageTag("en-IN")

    fun money(value: Double, decimals: Int = 0, symbol: String = "रू"): String =
        symbol + " " + number(value, decimals)

    fun number(value: Double, decimals: Int = 0): String = when (decimals) {
        0 -> String.format(groupingLocale, "%,d", Math.round(value))
        2 -> String.format(groupingLocale, "%,.2f", value)
        else -> String.format(groupingLocale, "%,.${decimals}f", value)
    }

    /** "12.4k" style compact numbers for tight chart labels. */
    fun compact(value: Double): String {
        val abs = kotlin.math.abs(value)
        return when {
            abs >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000)
            abs >= 1_000 -> String.format(Locale.US, "%.1fk", value / 1_000)
            else -> String.format(Locale.US, "%.0f", value)
        }
    }

    fun percent(value: Double, decimals: Int = 0): String =
        String.format(Locale.US, "%.${decimals}f%%", value)

    /** Machine-parseable number (no thousand separators) for text fields. */
    fun plain(value: Double, decimals: Int = 2): String =
        String.format(Locale.US, "%.${decimals}f", value)

    fun startOfToday(now: Long = System.currentTimeMillis()): Long = Calendar.getInstance()
        .apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        .timeInMillis

    fun startOfWindow(days: Int, now: Long = System.currentTimeMillis()): Long =
        startOfToday(now) - (days.toLong() - 1L) * DAY_MS

    fun dateTime(timestamp: Long): String =
        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US).format(Date(timestamp))

    fun timeOnly(timestamp: Long): String =
        SimpleDateFormat("h:mm a", Locale.US).format(Date(timestamp))

    fun dayMonth(timestamp: Long): String =
        SimpleDateFormat("d MMM", Locale.US).format(Date(timestamp))

    fun relative(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diff = now - timestamp
        val minutes = diff / 60_000L
        val hours = diff / 3_600_000L
        val days = diff / 86_400_000L
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> dayMonth(timestamp)
        }
    }

    const val DAY_MS = 86_400_000L
}