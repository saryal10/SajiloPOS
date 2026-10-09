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

    fun money(value: Double, decimals: Int = 0, symbol: String = "रू"): String =
        symbol + " " + number(value, decimals)

    /**
     * Indian/Nepali digit grouping: 1,25,000 and 12,50,000 rather than 125,000.
     * Implemented by hand because java.text with Locale "en-IN" still applies
     * Western grouping on several JDK/Android versions.
     */
    fun number(value: Double, decimals: Int = 0): String {
        val negative = value < 0
        val absolute = if (negative) -value else value
        val text = String.format(Locale.US, "%.${decimals}f", absolute)
        val whole = text.substringBefore('.')
        val fraction = text.substringAfter('.', "")
        val grouped = groupIndian(whole) + if (fraction.isEmpty()) "" else ".$fraction"
        return if (negative) "-$grouped" else grouped
    }

    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val groups = ArrayList<String>()
        var index = digits.length
        groups += digits.substring(maxOf(0, index - 3), index)
        index -= 3
        while (index > 0) {
            val take = minOf(2, index)
            groups += digits.substring(index - take, index)
            index -= take
        }
        return groups.reversed().joinToString(",")
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