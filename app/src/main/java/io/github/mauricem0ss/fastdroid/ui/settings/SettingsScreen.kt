package io.github.mauricem0ss.fastdroid.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mauricem0ss.fastdroid.BuildConfig
import io.github.mauricem0ss.fastdroid.data.AppTheme
import io.github.mauricem0ss.fastdroid.ui.MainViewModel
import io.github.mauricem0ss.fastdroid.ui.common.Panel
import io.github.mauricem0ss.fastdroid.ui.common.SectionLabel
import io.github.mauricem0ss.fastdroid.ui.common.TargetPicker
import io.github.mauricem0ss.fastdroid.ui.theme.LocalPalette
import io.github.mauricem0ss.fastdroid.ui.theme.paletteFor

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val target by vm.defaultTarget.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()

    var canNotify by remember { mutableStateOf(vm.canNotify()) }
    var canPromote by remember { mutableStateOf(vm.canPromote()) }
    // Both can be changed in system settings while the app is in the background.
    LifecycleResumeEffect(Unit) {
        val notifyNow = vm.canNotify()
        val promoteNow = vm.canPromote()
        if ((notifyNow && !canNotify) || promoteNow != canPromote) vm.resyncNotifications()
        canNotify = notifyNow
        canPromote = promoteNow
        onPauseOrDispose { }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = vm.canNotify()
        if (granted) vm.resyncNotifications()
        else context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SectionLabel("Default fasting target")
        Panel {
            Spacer(Modifier.height(8.dp))
            TargetPicker(target, vm::setDefaultTarget)
            Text(
                "Used for new fasts. A running fast's target can be changed from the timer.",
                color = p.muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Theme")
        Panel {
            ThemeRow("Dark Terminal", AppTheme.TERMINAL, theme, vm::setTheme)
            ThemeRow("GitHub Dark", AppTheme.GITHUB, theme, vm::setTheme)
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("Notifications")
        Panel {
            Text(
                if (canNotify) "On — a running fast shows its start, target end and elapsed time."
                else "Off — the timer still works, but there's no ongoing notification or target alert.",
                color = if (canNotify) p.text else p.star,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 10.dp),
            )
            if (!canNotify) {
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        }
                    },
                    modifier = Modifier.padding(bottom = 10.dp),
                ) { Text("Allow notifications", color = p.accentHi) }
            }
            if (canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                Text(
                    if (canPromote) "Live Update on — the elapsed time also shows as a chip in the status bar."
                    else "Live Update off — no timer chip in the status bar.",
                    color = if (canPromote) p.muted else p.star,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                if (!canPromote) {
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        },
                        modifier = Modifier.padding(bottom = 10.dp),
                    ) { Text("Turn on Live Update", color = p.accentHi) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionLabel("About")
        Panel {
            Text(
                "FastDroid ${BuildConfig.VERSION_NAME} · everything stays on this device.",
                color = p.muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun ThemeRow(label: String, value: AppTheme, current: AppTheme, onSelect: (AppTheme) -> Unit) {
    val p = LocalPalette.current
    val swatch = paletteFor(value)
    Row(
        Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = value == current,
            onClick = { onSelect(value) },
            colors = RadioButtonDefaults.colors(selectedColor = p.accent, unselectedColor = p.muted),
        )
        Text(label, color = p.text, modifier = Modifier.weight(1f))
        listOf(swatch.bg, swatch.surface2, swatch.accent, swatch.success, swatch.star).forEach { color ->
            Swatch(color, swatch.border)
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun Swatch(color: Color, border: Color) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        Modifier.size(16.dp).background(color, shape).border(1.dp, border, shape)
    )
}
