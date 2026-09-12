package io.github.mauricem0ss.fastdroid.domain

import io.github.mauricem0ss.fastdroid.data.Fast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class FastRulesTest {
    private val zone = ZoneId.of("Europe/Amsterdam")

    private fun at(text: String): Long =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun fast(start: String, end: String?, targetHours: Int = 16) =
        Fast(startedAt = at(start), endedAt = end?.let(::at), targetMinutes = targetHours * 60)

    @Test
    fun `fast across midnight belongs to the day it ended`() {
        val f = fast("2026-09-10T20:00", "2026-09-11T12:00")
        assertEquals(LocalDate.of(2026, 9, 11), FastRules.attributedDay(f, zone))
    }

    @Test
    fun `running fast has no day`() {
        assertNull(FastRules.attributedDay(fast("2026-09-10T20:00", null), zone))
    }

    @Test
    fun `exactly reaching the target counts`() {
        val f = fast("2026-09-10T20:00", "2026-09-11T12:00")
        assertTrue(FastRules.reachedTarget(f, f.endedAt!!))
    }

    @Test
    fun `one minute short does not count`() {
        val f = fast("2026-09-10T20:00", "2026-09-11T11:59")
        assertFalse(FastRules.reachedTarget(f, f.endedAt!!))
    }

    @Test
    fun `target is judged against the fast's own snapshot, not the current default`() {
        val f = fast("2026-09-10T20:00", "2026-09-11T10:00", targetHours = 14)
        assertTrue(FastRules.reachedTarget(f, f.endedAt!!))
    }

    @Test
    fun `a successful fast wins over an early one on the same day`() {
        val early = fast("2026-09-10T20:00", "2026-09-11T02:00")
        val full = fast("2026-09-11T03:00", "2026-09-11T20:00")
        val statuses = FastRules.dayStatuses(listOf(early, full), zone)
        assertEquals(DayStatus.TARGET_REACHED, statuses[LocalDate.of(2026, 9, 11)])
    }

    @Test
    fun `day with only an early end is marked ended early`() {
        val statuses = FastRules.dayStatuses(listOf(fast("2026-09-10T20:00", "2026-09-11T09:00")), zone)
        assertEquals(DayStatus.ENDED_EARLY, statuses[LocalDate.of(2026, 9, 11)])
        assertEquals(1, statuses.size)
    }

    @Test
    fun `progress is capped at one`() {
        val f = fast("2026-09-10T20:00", "2026-09-11T20:00")
        assertEquals(1f, FastRules.progress(f, f.endedAt!!))
    }

    @Test
    fun `month bounds survive the DST change`() {
        val bounds = FastRules.monthBounds(YearMonth.of(2026, 10), zone)
        assertEquals(at("2026-10-01T00:00"), bounds.first)
        assertEquals(at("2026-11-01T00:00"), bounds.last + 1)
        // October 2026 has 31 days plus the extra hour when clocks go back.
        assertEquals((31L * 24 + 1) * 3_600_000L, bounds.last + 1 - bounds.first)
    }

    @Test
    fun `validation catches impossible times`() {
        val now = at("2026-09-11T12:00")
        assertEquals(EditError.START_IN_FUTURE, FastRules.validateTimes(at("2026-09-11T13:00"), null, now))
        assertEquals(EditError.END_IN_FUTURE, FastRules.validateTimes(at("2026-09-11T08:00"), at("2026-09-11T12:01"), now))
        assertEquals(EditError.END_BEFORE_START, FastRules.validateTimes(at("2026-09-11T08:00"), at("2026-09-11T08:00"), now))
        assertNull(FastRules.validateTimes(at("2026-09-10T20:00"), at("2026-09-11T12:00"), now))
        assertNull(FastRules.validateTimes(at("2026-09-10T20:00"), null, now))
    }

    @Test
    fun `target is clamped to the allowed range`() {
        assertEquals(60, FastRules.clampTarget(0))
        assertEquals(72 * 60, FastRules.clampTarget(100 * 60))
    }
}
