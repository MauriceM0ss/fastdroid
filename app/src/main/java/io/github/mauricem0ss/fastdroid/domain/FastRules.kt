package io.github.mauricem0ss.fastdroid.domain

import io.github.mauricem0ss.fastdroid.data.Fast
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** How a calendar day went, judged by the fasts that ended on it. */
enum class DayStatus { TARGET_REACHED, ENDED_EARLY }

enum class EditError(val message: String) {
    START_IN_FUTURE("Start can't be in the future"),
    END_IN_FUTURE("End can't be in the future"),
    END_BEFORE_START("End must be after start"),
    OVERLAPS("Overlaps another fast"),
    ALREADY_FASTING("A fast is already running"),
}

object FastRules {
    const val DEFAULT_TARGET_MINUTES = 16 * 60
    const val MIN_TARGET_MINUTES = 60
    const val MAX_TARGET_MINUTES = 72 * 60
    const val TARGET_STEP_MINUTES = 30

    private const val MINUTE = 60_000L

    fun clampTarget(minutes: Int): Int = minutes.coerceIn(MIN_TARGET_MINUTES, MAX_TARGET_MINUTES)

    fun targetEndAt(fast: Fast): Long = fast.startedAt + fast.targetMinutes * MINUTE

    fun elapsedMillis(fast: Fast, now: Long): Long =
        ((fast.endedAt ?: now) - fast.startedAt).coerceAtLeast(0)

    fun reachedTarget(fast: Fast, now: Long): Boolean =
        elapsedMillis(fast, now) >= fast.targetMinutes * MINUTE

    /** 0f..1f progress towards the target; capped at 1 once it is reached. */
    fun progress(fast: Fast, now: Long): Float =
        (elapsedMillis(fast, now).toFloat() / (fast.targetMinutes * MINUTE)).coerceIn(0f, 1f)

    /** A fast counts towards the day it ended on; a running fast has no day yet. */
    fun attributedDay(fast: Fast, zone: ZoneId): LocalDate? =
        fast.endedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    /**
     * Status per day. One successful fast makes the day a success even if an
     * earlier attempt that day was cut short.
     */
    fun dayStatuses(fasts: List<Fast>, zone: ZoneId): Map<LocalDate, DayStatus> =
        fasts.mapNotNull { fast -> attributedDay(fast, zone)?.let { it to fast } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, dayFasts) ->
                if (dayFasts.any { reachedTarget(it, it.endedAt!!) }) DayStatus.TARGET_REACHED
                else DayStatus.ENDED_EARLY
            }

    /** Epoch-millis bounds [start, end) of a month in [zone]. */
    fun monthBounds(month: YearMonth, zone: ZoneId): LongRange {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start until end
    }

    /**
     * The checks that need no database. Overlap with other fasts is checked by
     * the repository against the DAO. [endedAt] null means the fast keeps running.
     */
    fun validateTimes(startedAt: Long, endedAt: Long?, now: Long): EditError? = when {
        startedAt > now -> EditError.START_IN_FUTURE
        endedAt != null && endedAt > now -> EditError.END_IN_FUTURE
        endedAt != null && endedAt <= startedAt -> EditError.END_BEFORE_START
        else -> null
    }
}
