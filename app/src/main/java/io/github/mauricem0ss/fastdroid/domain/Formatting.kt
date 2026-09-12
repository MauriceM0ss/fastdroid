package io.github.mauricem0ss.fastdroid.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object Formatting {
    /** "16h 05m", or "45m" under an hour. */
    fun duration(millis: Long): String {
        val totalMinutes = (millis.coerceAtLeast(0) / 60_000L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours == 0L) "${minutes}m" else "${hours}h ${"%02d".format(minutes)}m"
    }

    /** "13:42:07" — hours are not wrapped at 24, so a 30h fast reads 30:00:00. */
    fun clock(millis: Long): String {
        val totalSeconds = millis.coerceAtLeast(0) / 1000
        return "%02d:%02d:%02d".format(totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)
    }

    /**
     * "16h", "16h 30m"; plus the "16:8" ratio for whole-hour targets of 12–23h,
     * the range where fasting/eating windows are named that way. "1:23" would just confuse.
     */
    fun target(minutes: Int): String {
        val hours = minutes / 60
        val base = if (minutes % 60 == 0) "${hours}h" else "${hours}h ${minutes % 60}m"
        return if (minutes % 60 == 0 && hours in 12..23) "$base · $hours:${24 - hours}" else base
    }

    /**
     * The cells of a month grid, week rows starting on [firstDayOfWeek], padded
     * with nulls so every row has seven cells.
     */
    fun monthCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
        val first = month.atDay(1)
        val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
        val cells = List<LocalDate?>(leading) { null } + days
        val trailing = (7 - cells.size % 7) % 7
        return cells + List(trailing) { null }
    }
}
