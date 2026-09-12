package io.github.mauricem0ss.fastdroid.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class FormattingTest {
    @Test
    fun durations() {
        assertEquals("45m", Formatting.duration(45 * 60_000L))
        assertEquals("16h 05m", Formatting.duration((16 * 60 + 5) * 60_000L))
        assertEquals("0m", Formatting.duration(-5))
    }

    @Test
    fun `clock does not wrap past a day`() {
        assertEquals("30:00:00", Formatting.clock(30 * 3_600_000L))
        assertEquals("13:42:07", Formatting.clock((13 * 3600 + 42 * 60 + 7) * 1000L))
    }

    @Test
    fun targets() {
        assertEquals("16h · 16:8", Formatting.target(16 * 60))
        assertEquals("16h 30m", Formatting.target(16 * 60 + 30))
        assertEquals("36h", Formatting.target(36 * 60))
        assertEquals("1h", Formatting.target(60))
        assertEquals("12h · 12:12", Formatting.target(12 * 60))
    }

    @Test
    fun `month grid starting monday`() {
        // 1 September 2026 is a Tuesday.
        val cells = Formatting.monthCells(YearMonth.of(2026, 9), DayOfWeek.MONDAY)
        assertNull(cells[0])
        assertEquals(LocalDate.of(2026, 9, 1), cells[1])
        assertEquals(0, cells.size % 7)
        assertEquals(30, cells.count { it != null })
    }

    @Test
    fun `month grid starting sunday`() {
        val cells = Formatting.monthCells(YearMonth.of(2026, 9), DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 9, 1), cells[2])
        assertEquals(35, cells.size)
    }

    @Test
    fun `month that starts on the first weekday has no padding`() {
        // 1 June 2026 is a Monday.
        val cells = Formatting.monthCells(YearMonth.of(2026, 6), DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 6, 1), cells[0])
    }
}
