package io.github.mauricem0ss.fastdroid.ui.common

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mauricem0ss.fastdroid.data.Fast
import io.github.mauricem0ss.fastdroid.domain.FastRules
import io.github.mauricem0ss.fastdroid.ui.theme.LocalPalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Uppercase section label, the CSS `--accent2` passive-label style. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalPalette.current.accent2,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(p.radius)
    Column(
        modifier
            .fillMaxWidth()
            .background(p.surface, shape)
            .border(1.dp, p.border, shape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        content = content,
    )
}

/** A label/value line inside a [Panel]; tappable when [onClick] is set. */
@Composable
fun InfoRow(label: String, value: String, onClick: (() -> Unit)? = null, valueColor: Color? = null) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = p.muted)
        Text(
            if (onClick != null) "$value  ✎" else value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor ?: if (onClick != null) p.accentHi else p.text,
        )
    }
}

@Composable
fun StatusDot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(color, CircleShape))
}

/** Colour for a completed fast: success if it reached its target, star/amber if it ended early. */
@Composable
fun fastColor(fast: Fast): Color {
    val p = LocalPalette.current
    return if (FastRules.reachedTarget(fast, fast.endedAt ?: System.currentTimeMillis())) p.success else p.star
}

/** Time formatting that follows the phone's 12/24-hour setting. */
class TimeFormats(is24Hour: Boolean) {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val clock = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a")
    private val weekday = DateTimeFormatter.ofPattern("EEE")
    private val date = DateTimeFormatter.ofPattern("EEE d MMM")

    fun time(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zone).format(clock)

    /** "20:04" today, "Fri 20:04" otherwise. */
    fun dayTime(millis: Long): String {
        val dt = Instant.ofEpochMilli(millis).atZone(zone)
        return if (dt.toLocalDate() == LocalDate.now(zone)) dt.format(clock)
        else dt.format(weekday) + " " + dt.format(clock)
    }

    fun date(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zone).format(date)
}

@Composable
fun rememberTimeFormats(): TimeFormats {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    return remember(is24) { TimeFormats(is24) }
}
