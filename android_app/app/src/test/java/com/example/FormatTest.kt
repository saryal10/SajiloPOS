package com.example

import com.example.ui.util.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Formatting is user-facing money, so it gets tested like business logic:
 * Nepali digit grouping, machine-parseable values for text fields, and the
 * rolling windows the analytics queries are built on.
 */
class FormatTest {

    @Test
    fun `money uses nepalese digit grouping`() {
        assertEquals("रू 1,25,000", Format.money(125000.0, 0))
        assertEquals("रू 12,500", Format.money(12500.0, 0))
        assertEquals("रू 950", Format.money(950.0, 0))
    }

    @Test
    fun `money respects decimals and custom symbol`() {
        assertEquals("रू 1,250.50", Format.money(1250.5, 2))
        assertEquals("Rs 99.99", Format.money(99.99, 2, symbol = "Rs"))
        assertEquals("रू 1,250.567", Format.money(1250.567, 3))
    }

    @Test
    fun `money rounds to the nearest rupee by default`() {
        assertEquals("रू 125", Format.money(124.6, 0))
        assertEquals("रू 125", Format.money(125.4, 0))
    }

    @Test
    fun `number formats by decimal count`() {
        assertEquals("1,000", Format.number(1000.0, 0))
        assertEquals("1,000.25", Format.number(1000.25, 2))
        assertEquals("1,000.250", Format.number(1000.25, 3))
    }

    @Test
    fun `plain numbers stay machine parseable`() {
        // Regression guard: cash tender fields are parsed with toDoubleOrNull().
        listOf(0.0, 99.99, 1250.5, 125000.0, 1234567.89).forEach { value ->
            val formatted = Format.plain(value, 2)
            assertTrue("plain($value) = $formatted must parse", formatted.toDoubleOrNull() != null)
        }
    }

    @Test
    fun `compact shortens large amounts for chart labels`() {
        assertEquals("950", Format.compact(950.0))
        assertEquals("12.5k", Format.compact(12500.0))
        assertEquals("1.3M", Format.compact(1_250_000.0))
        assertEquals("-12.5k", Format.compact(-12500.0))
    }

    @Test
    fun `percent renders with the percent sign`() {
        assertEquals("13%", Format.percent(13.0))
        assertEquals("12.5%", Format.percent(12.5, 1))
        assertEquals("0%", Format.percent(0.0))
    }

    @Test
    fun `startOfToday strips the time component`() {
        val midday = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.MARCH)
            set(Calendar.DAY_OF_MONTH, 14)
            set(Calendar.HOUR_OF_DAY, 13)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 30)
            set(Calendar.MILLISECOND, 500)
        }.timeInMillis

        val start = Format.startOfToday(midday)
        val check = Calendar.getInstance().apply { timeInMillis = start }

        assertEquals(0, check.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, check.get(Calendar.MINUTE))
        assertEquals(0, check.get(Calendar.SECOND))
        assertEquals(0, check.get(Calendar.MILLISECOND))
        assertEquals(14, check.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `today window is a single day and seven day window spans six days back`() {
        val today = Format.startOfToday()

        assertEquals(today, Format.startOfWindow(1))
        assertEquals(today - 6 * Format.DAY_MS, Format.startOfWindow(7))
        assertEquals(today - 29 * Format.DAY_MS, Format.startOfWindow(30))
    }

    @Test
    fun `window start never runs past today`() {
        val today = Format.startOfToday()
        val windows = listOf(1, 7, 30).map { Format.startOfWindow(it) }

        assertTrue(windows.all { it <= today })
        assertEquals(today, windows.max())
    }

    @Test
    fun `relative time reads naturally`() {
        val now = 1_700_000_000_000L

        assertEquals("Just now", Format.relative(now, now))
        assertEquals("5m ago", Format.relative(now - 5 * 60_000L, now))
        assertEquals("3h ago", Format.relative(now - 3 * 3_600_000L, now))
        assertEquals("Yesterday", Format.relative(now - 26 * 3_600_000L, now))
        assertEquals("4d ago", Format.relative(now - 4 * Format.DAY_MS, now))
        // Beyond a week it falls back to an absolute date.
        assertNotNull(Format.relative(now - 30 * Format.DAY_MS, now))
    }

    @Test
    fun `absolute dates include the year and a time`() {
        val timestamp = 1_700_000_000_000L // 2023-11-14 22:13 UTC

        assertTrue("dateTime should carry the year", Format.dateTime(timestamp).contains("2023"))
        assertTrue("timeOnly should carry a clock time", Format.timeOnly(timestamp).matches(Regex(""".*\d{1,2}:\d{2}.*""")))
        assertTrue(Format.dayMonth(timestamp).isNotBlank())
    }
}