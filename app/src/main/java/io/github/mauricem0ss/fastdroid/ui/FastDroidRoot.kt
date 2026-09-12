package io.github.mauricem0ss.fastdroid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mauricem0ss.fastdroid.ui.history.HistoryScreen
import io.github.mauricem0ss.fastdroid.ui.settings.SettingsScreen
import io.github.mauricem0ss.fastdroid.ui.theme.LocalPalette
import io.github.mauricem0ss.fastdroid.ui.timer.TimerScreen

enum class Tab(val label: String) { TIMER("Timer"), HISTORY("History"), SETTINGS("Settings") }

@Composable
fun FastDroidRoot(vm: MainViewModel) {
    val p = LocalPalette.current
    var tab by rememberSaveable { mutableStateOf(Tab.TIMER) }
    val snackbar = remember { SnackbarHostState() }

    BackHandler(enabled = tab != Tab.TIMER) { tab = Tab.TIMER }

    LaunchedEffect(vm) {
        vm.messages.collect { message ->
            val result = snackbar.showSnackbar(
                message = message.text,
                actionLabel = if (message.undoFastId != null) "Undo" else null,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed && message.undoFastId != null) {
                vm.resumeFast(message.undoFastId)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Column(Modifier.fillMaxSize()) {
            Header()
            TabStrip(tab, onSelect = { tab = it })
            Box(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()) {
                when (tab) {
                    Tab.TIMER -> TimerScreen(vm)
                    Tab.HISTORY -> HistoryScreen(vm)
                    Tab.SETTINGS -> SettingsScreen(vm)
                }
            }
        }
        SnackbarHost(
            snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = p.surface2,
                contentColor = p.text,
                actionColor = p.accent,
            )
        }
    }
}

/** The family's near-black top bar with the `>_` prompt wordmark. */
@Composable
private fun Header() {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().background(p.headerBg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(">_", color = p.headerLogo, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text("fastdroid", color = p.headerText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }
    }
}

@Composable
private fun TabStrip(selected: Tab, onSelect: (Tab) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().height(44.dp).background(p.surface)) {
        Tab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                Modifier.weight(1f).fillMaxHeight().clickable { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label.uppercase(),
                    color = if (active) p.accent else p.muted,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                )
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp)
                        .background(if (active) p.accent else p.surface)
                )
            }
        }
    }
}
