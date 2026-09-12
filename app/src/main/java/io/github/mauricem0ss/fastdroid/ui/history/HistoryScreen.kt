package io.github.mauricem0ss.fastdroid.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mauricem0ss.fastdroid.data.Fast
import io.github.mauricem0ss.fastdroid.domain.DayStatus
import io.github.mauricem0ss.fastdroid.domain.FastRules
import io.github.mauricem0ss.fastdroid.domain.Formatting
import io.github.mauricem0ss.fastdroid.ui.MainViewModel
import io.github.mauricem0ss.fastdroid.ui.common.FastEditDialog
import io.github.mauricem0ss.fastdroid.ui.common.Panel
import io.github.mauricem0ss.fastdroid.ui.common.SectionLabel
import io.github.mauricem0ss.fastdroid.ui.common.StatusDot
import io.github.mauricem0ss.fastdroid.ui.common.fastColor
import io.github.mauricem0ss.fastdroid.ui.common.rememberTimeFormats
import io.github.mauricem0ss.fastdroid.ui.common.truncateToMinute
import io.github.mauricem0ss.fastdroid.ui.theme.LocalPalette
import io.github.mauricem0ss.fastdroid.ui.theme.MonoDigits
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val p = LocalPalette.current
    val zone = ZoneId.systemDefault()
    val month by vm.month.collectAsStateWithLifecycle()
    val fasts by vm.monthFasts.collectAsStateWithLifecycle()
    val defaultTarget by vm.defaultTarget.collectAsStateWithLifecycle()
    val today = LocalDate.now(zone)

    var selected by remember(month) { mutableStateOf(if (month == YearMonth.from(today)) today else null) }
    var editing by remember { mutableStateOf<Fast?>(null) }

    val statuses = remember(fasts) { FastRules.dayStatuses(fasts, zone) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        MonthHeader(
            month = month,
            canGoForward = month < YearMonth.from(today),
            onPrevious = { vm.showMonth(month.minusMonths(1)) },
            onNext = { vm.showMonth(month.plusMonths(1)) },
        )
        Spacer(Modifier.height(8.dp))
        MonthGrid(month, today, selected, statuses, onSelect = { selected = it })
        Spacer(Modifier.height(12.dp))
        Legend()
        Spacer(Modifier.height(20.dp))

        val day = selected
        if (day == null) {
            Text("Tap a day to see its fasts.", color = p.muted, style = MaterialTheme.typography.bodyMedium)
        } else {
            DayDetail(
                day = day,
                fasts = fasts.filter { FastRules.attributedDay(it, zone) == day },
                onEdit = { editing = it },
                onAdd = { editing = newFastFor(day, defaultTarget, zone) },
            )
        }
    }

    editing?.let { fast ->
        FastEditDialog(
            initial = fast,
            onSave = vm::saveFast,
            onDelete = { vm.deleteFast(it); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun MonthHeader(month: YearMonth, canGoForward: Boolean, onPrevious: () -> Unit, onNext: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onPrevious) { Text("‹", fontSize = 26.sp, color = p.accentHi) }
        Text(
            month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium,
            color = p.text,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onNext, enabled = canGoForward) {
            Text("›", fontSize = 26.sp, color = if (canGoForward) p.accentHi else p.surface2)
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate?,
    statuses: Map<LocalDate, DayStatus>,
    onSelect: (LocalDate) -> Unit,
) {
    val p = LocalPalette.current
    val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val cells = remember(month, firstDay) { Formatting.monthCells(month, firstDay) }

    Row(Modifier.fillMaxWidth()) {
        (0 until 7).forEach { i ->
            Text(
                firstDay.plus(i.toLong()).getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2).uppercase(),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = p.accent2,
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                    if (date != null) {
                        DayCell(
                            date = date,
                            status = statuses[date],
                            isToday = date == today,
                            isSelected = date == selected,
                            isFuture = date.isAfter(today),
                            onClick = { onSelect(date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    status: DayStatus?,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(p.radius)
    Column(
        Modifier
            .fillMaxSize()
            .background(if (isSelected) p.accentDim else Color.Transparent, shape)
            .border(1.dp, if (isToday) p.accent else Color.Transparent, shape)
            .clickable(enabled = !isFuture, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            fontFamily = MonoDigits,
            fontSize = 14.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isFuture) p.muted.copy(alpha = 0.4f) else p.text,
        )
        Spacer(Modifier.height(4.dp))
        StatusDot(
            when (status) {
                DayStatus.TARGET_REACHED -> p.success
                DayStatus.ENDED_EARLY -> p.star
                null -> Color.Transparent
            },
            size = 7.dp,
        )
    }
}

@Composable
private fun Legend() {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(p.success)
        Text("  Target reached", color = p.muted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(18.dp))
        StatusDot(p.star)
        Text("  Ended early", color = p.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DayDetail(day: LocalDate, fasts: List<Fast>, onEdit: (Fast) -> Unit, onAdd: () -> Unit) {
    val p = LocalPalette.current
    val formats = rememberTimeFormats()
    SectionLabel(day.format(DateTimeFormatter.ofPattern("EEEE d MMMM")))
    Panel {
        if (fasts.isEmpty()) {
            Text(
                "No fast ended on this day.",
                color = p.muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
        fasts.forEach { fast ->
            val endedAt = fast.endedAt ?: return@forEach
            Row(
                Modifier.fillMaxWidth().clickable { onEdit(fast) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(fastColor(fast))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${formats.dayTime(fast.startedAt)} → ${formats.time(endedAt)}",
                        color = p.text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Target ${Formatting.target(fast.targetMinutes)}",
                        color = p.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    Formatting.duration(FastRules.elapsedMillis(fast, endedAt)),
                    fontFamily = MonoDigits,
                    color = fastColor(fast),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    TextButton(onClick = onAdd, modifier = Modifier.padding(top = 4.dp)) {
        Text("+ Add a fast for this day", color = p.accentHi)
    }
}

/** A missed fast pre-filled to end at noon on [day] (or now, if that's still ahead) and run the default target. */
private fun newFastFor(day: LocalDate, targetMinutes: Int, zone: ZoneId): Fast {
    val noon = day.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()
    val end = truncateToMinute(minOf(noon, System.currentTimeMillis()))
    return Fast(startedAt = end - targetMinutes * 60_000L, endedAt = end, targetMinutes = targetMinutes)
}
