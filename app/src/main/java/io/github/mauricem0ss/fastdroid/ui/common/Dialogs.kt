@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.mauricem0ss.fastdroid.ui.common

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mauricem0ss.fastdroid.data.Fast
import io.github.mauricem0ss.fastdroid.domain.EditError
import io.github.mauricem0ss.fastdroid.domain.FastRules
import io.github.mauricem0ss.fastdroid.domain.Formatting
import io.github.mauricem0ss.fastdroid.ui.theme.LocalPalette
import io.github.mauricem0ss.fastdroid.ui.theme.MonoDigits
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

private val PRESET_HOURS = listOf(12, 14, 16, 18, 20, 23)

/** Presets for the common schedules plus a ±30 min stepper for anything else. */
@Composable
fun TargetPicker(minutes: Int, onChange: (Int) -> Unit) {
    val p = LocalPalette.current
    Column {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESET_HOURS.forEach { hours ->
                FilterChip(
                    selected = minutes == hours * 60,
                    onClick = { onChange(hours * 60) },
                    label = { Text("$hours:${24 - hours}") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = p.surface2,
                        labelColor = p.text,
                        selectedContainerColor = p.accentDim,
                        selectedLabelColor = p.accentHi,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = minutes == hours * 60,
                        borderColor = p.border,
                        selectedBorderColor = p.accent,
                    ),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                onClick = { onChange(FastRules.clampTarget(minutes - FastRules.TARGET_STEP_MINUTES)) },
                enabled = minutes > FastRules.MIN_TARGET_MINUTES,
            ) { Text("−30m") }
            Text(
                Formatting.target(minutes),
                style = MaterialTheme.typography.titleMedium,
                color = p.text,
            )
            OutlinedButton(
                onClick = { onChange(FastRules.clampTarget(minutes + FastRules.TARGET_STEP_MINUTES)) },
                enabled = minutes < FastRules.MAX_TARGET_MINUTES,
            ) { Text("+30m") }
        }
    }
}

@Composable
fun TargetDialog(title: String, initialMinutes: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(initialMinutes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TargetPicker(minutes) { minutes = it } },
        confirmButton = { TextButton(onClick = { onSave(minutes) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** "Started earlier…": pick when the fast actually began. */
@Composable
fun StartEarlierDialog(onStart: (Long) -> Unit, onDismiss: () -> Unit) {
    var startedAt by rememberSaveable { mutableStateOf(truncateToMinute(System.currentTimeMillis() - 60 * 60_000L)) }
    val error = FastRules.validateTimes(startedAt, null, System.currentTimeMillis())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("When did you start?") },
        text = {
            Column {
                DateTimeField("Started", startedAt) { startedAt = it }
                ErrorText(error)
            }
        },
        confirmButton = { TextButton(onClick = { onStart(startedAt) }, enabled = error == null) { Text("Start") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Edit a fast's times and target, or add a missed one (id 0). [onSave] reports
 * the repository's verdict back so overlap errors show inside the dialog.
 */
@Composable
fun FastEditDialog(
    initial: Fast,
    onSave: (Fast, (EditError?) -> Unit) -> Unit,
    onDelete: ((Fast) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val p = LocalPalette.current
    val isNew = initial.id == 0L
    var startedAt by rememberSaveable { mutableStateOf(initial.startedAt) }
    var running by rememberSaveable { mutableStateOf(initial.endedAt == null) }
    var endedAt by rememberSaveable { mutableStateOf(initial.endedAt ?: truncateToMinute(System.currentTimeMillis())) }
    var target by rememberSaveable { mutableIntStateOf(initial.targetMinutes) }
    var saveError by remember { mutableStateOf<EditError?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val candidate = initial.copy(startedAt = startedAt, endedAt = if (running) null else endedAt, targetMinutes = target)
    val localError = FastRules.validateTimes(candidate.startedAt, candidate.endedAt, System.currentTimeMillis())

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this fast?") },
            text = { Text("It will be removed from your history.") },
            confirmButton = {
                TextButton(onClick = { onDelete(initial) }) { Text("Delete", color = p.danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add fast" else "Edit fast") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DateTimeField("Started", startedAt) { startedAt = it; saveError = null }
                if (initial.endedAt == null && !isNew) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Still running", color = p.text)
                        Switch(
                            checked = running,
                            onCheckedChange = { running = it; saveError = null },
                            colors = SwitchDefaults.colors(checkedTrackColor = p.accent, checkedThumbColor = p.onAccent),
                        )
                    }
                }
                if (!running) {
                    DateTimeField("Ended", endedAt) { endedAt = it; saveError = null }
                }
                Spacer(Modifier.height(12.dp))
                SectionLabel("Target")
                TargetPicker(target) { target = it }
                ErrorText(localError ?: saveError)
                if (onDelete != null && !isNew) {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Delete fast", color = p.danger)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = localError == null,
                onClick = { onSave(candidate) { error -> saveError = error; if (error == null) onDismiss() } },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ErrorText(error: EditError?) {
    if (error != null) {
        Text(
            error.message,
            color = LocalPalette.current.danger,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** A label with separate date and time buttons, each opening its Material picker. */
@Composable
fun DateTimeField(label: String, millis: Long, onChange: (Long) -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()
    val formats = rememberTimeFormats()
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = p.muted, maxLines = 1, modifier = Modifier.width(76.dp))
        TextButton(onClick = { pickDate = true }) { Text(formats.date(millis), color = p.accentHi) }
        TextButton(onClick = { pickTime = true }) {
            Text(formats.time(millis), color = p.accentHi, fontFamily = MonoDigits, fontWeight = FontWeight.Medium)
        }
    }

    if (pickDate) {
        val today = LocalDate.now(zone)
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                // The picker speaks UTC midnight millis regardless of the phone's zone.
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)

                override fun isSelectableYear(year: Int): Boolean = year <= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { utc ->
                        val date = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        onChange(LocalDateTime.of(date, dateTime.toLocalTime()).atZone(zone).toInstant().toEpochMilli())
                    }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
            colors = DatePickerDefaults.colors(containerColor = p.surface),
        ) {
            DatePicker(
                state = state,
                colors = DatePickerDefaults.colors(
                    containerColor = p.surface,
                    selectedDayContainerColor = p.accent,
                    selectedDayContentColor = p.onAccent,
                    todayDateBorderColor = p.accent,
                    todayContentColor = p.accentHi,
                ),
            )
        }
    }

    if (pickTime) {
        val state = rememberTimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = p.surface2,
                        selectorColor = p.accent,
                        clockDialSelectedContentColor = p.onAccent,
                        timeSelectorSelectedContainerColor = p.accentDim,
                        timeSelectorSelectedContentColor = p.accentHi,
                        timeSelectorUnselectedContainerColor = p.surface2,
                        periodSelectorSelectedContainerColor = p.accentDim,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val time = LocalTime.of(state.hour, state.minute)
                    onChange(LocalDateTime.of(dateTime.toLocalDate(), time).atZone(zone).toInstant().toEpochMilli())
                    pickTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Cancel") } },
        )
    }
}

fun truncateToMinute(millis: Long): Long = millis - millis % 60_000L
