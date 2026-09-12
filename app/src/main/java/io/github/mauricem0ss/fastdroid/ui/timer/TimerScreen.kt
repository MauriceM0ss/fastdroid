package io.github.mauricem0ss.fastdroid.ui.timer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mauricem0ss.fastdroid.data.Fast
import io.github.mauricem0ss.fastdroid.domain.FastRules
import io.github.mauricem0ss.fastdroid.domain.Formatting
import io.github.mauricem0ss.fastdroid.ui.MainViewModel
import io.github.mauricem0ss.fastdroid.ui.common.FastEditDialog
import io.github.mauricem0ss.fastdroid.ui.common.InfoRow
import io.github.mauricem0ss.fastdroid.ui.common.Panel
import io.github.mauricem0ss.fastdroid.ui.common.StartEarlierDialog
import io.github.mauricem0ss.fastdroid.ui.common.TargetDialog
import io.github.mauricem0ss.fastdroid.ui.common.fastColor
import io.github.mauricem0ss.fastdroid.ui.common.rememberTimeFormats
import io.github.mauricem0ss.fastdroid.ui.theme.LocalPalette
import io.github.mauricem0ss.fastdroid.ui.theme.MonoDigits
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

@Composable
fun TimerScreen(vm: MainViewModel) {
    val state by vm.timer.collectAsStateWithLifecycle()
    val defaultTarget by vm.defaultTarget.collectAsStateWithLifecycle()
    val timer = state ?: return

    // Ticks once a second, aligned to the wall clock, and only while the app is visible.
    val ticker = remember {
        flow {
            while (true) {
                val now = System.currentTimeMillis()
                emit(now)
                delay(1_000 - now % 1_000)
            }
        }
    }
    val now by ticker.collectAsStateWithLifecycle(System.currentTimeMillis())

    var pendingStart by remember { mutableStateOf<Long?>(null) }
    var showStartEarlier by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Fast?>(null) }
    var editingTarget by rememberSaveable { mutableStateOf(false) }

    // Ask for notification permission on the first start; the fast starts either way.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.startFast(pendingStart?.takeIf { it > 0 })
        pendingStart = null
    }
    val start: (Long?) -> Unit = { at ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !vm.canNotify()) {
            pendingStart = at ?: 0
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.startFast(at)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TimerRing(active = timer.active, lastCompleted = timer.lastCompleted, now = now)
        Spacer(Modifier.height(28.dp))

        val formats = rememberTimeFormats()
        val active = timer.active
        if (active != null) {
            Panel {
                InfoRow("Started", formats.dayTime(active.startedAt), onClick = { editing = active })
                InfoRow("Target", Formatting.target(active.targetMinutes), onClick = { editingTarget = true })
                InfoRow("Target end", formats.dayTime(FastRules.targetEndAt(active)))
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = vm::endFast,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalPalette.current.accent,
                    contentColor = LocalPalette.current.onAccent,
                ),
            ) { Text("End fast", fontWeight = FontWeight.Bold) }
        } else {
            Panel {
                InfoRow("Target", Formatting.target(defaultTarget), onClick = { editingTarget = true })
                timer.lastCompleted?.let { last ->
                    InfoRow(
                        "Last fast",
                        Formatting.duration(FastRules.elapsedMillis(last, last.endedAt!!)) +
                            " · ended " + formats.dayTime(last.endedAt),
                        valueColor = fastColor(last),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { start(null) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalPalette.current.accent,
                    contentColor = LocalPalette.current.onAccent,
                ),
            ) { Text("Start fast", fontWeight = FontWeight.Bold) }
            TextButton(onClick = { showStartEarlier = true }, modifier = Modifier.padding(top = 4.dp)) {
                Text("I started earlier…", color = LocalPalette.current.muted)
            }
        }
    }

    if (showStartEarlier) {
        StartEarlierDialog(
            onStart = { at -> showStartEarlier = false; start(at) },
            onDismiss = { showStartEarlier = false },
        )
    }
    editing?.let { fast ->
        FastEditDialog(
            initial = fast,
            onSave = vm::saveFast,
            onDelete = { vm.deleteFast(it); editing = null },
            onDismiss = { editing = null },
        )
    }
    if (editingTarget) {
        val active = timer.active
        TargetDialog(
            title = if (active != null) "Target for this fast" else "Default target",
            initialMinutes = active?.targetMinutes ?: defaultTarget,
            onSave = { minutes ->
                if (active != null) vm.saveFast(active.copy(targetMinutes = minutes)) {}
                else vm.setDefaultTarget(minutes)
                editingTarget = false
            },
            onDismiss = { editingTarget = false },
        )
    }
}

@Composable
private fun TimerRing(active: Fast?, lastCompleted: Fast?, now: Long) {
    val p = LocalPalette.current
    val reached = active != null && FastRules.reachedTarget(active, now)
    val progress = active?.let { FastRules.progress(it, now) } ?: 0f
    val ringColor = if (reached) p.success else p.accent

    Box(Modifier.size(272.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(p.surface2, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (progress > 0f) {
                drawArc(
                    ringColor, -90f, 360f * progress, false, Offset(inset, inset), arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                when {
                    active == null -> "NOT FASTING"
                    reached -> "TARGET REACHED"
                    else -> "FASTING"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (reached) p.success else p.accent2,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                Formatting.clock(active?.let { FastRules.elapsedMillis(it, now) } ?: 0),
                fontFamily = MonoDigits,
                fontSize = 40.sp,
                fontWeight = FontWeight.Medium,
                color = if (active == null) p.muted else p.text,
            )
            Spacer(Modifier.height(6.dp))
            Text(subline(active, lastCompleted, now), style = MaterialTheme.typography.bodyMedium, color = p.muted)
        }
    }
}

private fun subline(active: Fast?, lastCompleted: Fast?, now: Long): String {
    if (active == null) {
        val ended = lastCompleted?.endedAt ?: return "No fasts yet"
        return "Last fast ended ${Formatting.duration(now - ended)} ago"
    }
    val remaining = FastRules.targetEndAt(active) - now
    return if (remaining > 0) "${Formatting.duration(remaining + 59_999)} to go"
    else "+${Formatting.duration(-remaining)} past target"
}
