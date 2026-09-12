package io.github.mauricem0ss.fastdroid.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.mauricem0ss.fastdroid.MainActivity
import io.github.mauricem0ss.fastdroid.R
import io.github.mauricem0ss.fastdroid.data.Fast
import io.github.mauricem0ss.fastdroid.data.SettingsStore
import io.github.mauricem0ss.fastdroid.domain.FastRules
import io.github.mauricem0ss.fastdroid.domain.Formatting
import io.github.mauricem0ss.fastdroid.ui.theme.paletteFor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FastNotifier(private val context: Context, private val settings: SettingsStore) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannel(
            // DEFAULT, not LOW: Pixel hides the status-bar icon of "silent" (LOW) notifications.
            // Sound and vibration are switched off on the channel instead.
            NotificationChannel(CHANNEL_ACTIVE, "Running fast", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "The ongoing timer while you fast"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(true)
            }
        )
        // Importance can't change after creation, so the old LOW channel was replaced rather than edited.
        system.deleteNotificationChannel("fast_active")
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_TARGET, "Target reached", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "One alert when a fast reaches its target"
            }
        )
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_ENDED, "Fast ended", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Confirmation with an undo after ending a fast from a notification"
                setShowBadge(false)
            }
        )
    }

    fun canNotify(): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) && manager.areNotificationsEnabled()

    /** Whether the user allows Live Updates for this app (Android 16+; always false before). */
    fun canPromote(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && manager.canPostPromotedNotifications()

    /**
     * The ongoing notification. The system renders the live elapsed time itself
     * via the chronometer, so nothing in the app has to run to keep it current.
     */
    fun showActive(fast: Fast) {
        if (!canNotify()) return
        val now = System.currentTimeMillis()
        val reached = FastRules.reachedTarget(fast, now)
        val targetEnd = FastRules.targetEndAt(fast)
        val title = if (reached) "Target reached · ${Formatting.target(fast.targetMinutes)}"
        else "Fasting · ${Formatting.target(fast.targetMinutes)}"
        val text = "Started ${time(fast.startedAt)} · ${if (reached) "Reached" else "Ends"} ${time(targetEnd)}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVE)
            .setSmallIcon(R.drawable.ic_stat_fast)
            .setColor(accent())
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(if (reached) "Past target" else null)
            .setUsesChronometer(true)
            .setWhen(fast.startedAt)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            // Android 16+ promotes this to a Live Update: a status-bar chip showing the
            // chronometer, also on the lock screen. Requires ongoing, a title, no custom
            // view and no setColorized(); ignored on older versions.
            .setRequestPromotedOngoing(true)
            .setContentIntent(openApp())
            // Android 14+ lets ongoing notifications be swiped away; this puts it straight back.
            .setDeleteIntent(broadcast(FastActionReceiver.ACTION_DISMISSED, FastActionReceiver.REQUEST_DISMISSED))
            .addAction(0, "End fast", broadcast(FastActionReceiver.ACTION_END, FastActionReceiver.REQUEST_END))
            .build()
        notify(ID_ACTIVE, notification)
    }

    fun cancelActive() {
        manager.cancel(ID_ACTIVE)
        manager.cancel(ID_TARGET)
    }

    fun showTargetReached(fast: Fast) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_TARGET)
            .setSmallIcon(R.drawable.ic_stat_fast)
            .setColor(accent())
            .setContentTitle("${Formatting.target(fast.targetMinutes)} reached")
            .setContentText("Started ${time(fast.startedAt)}. End the fast whenever you're ready.")
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .build()
        notify(ID_TARGET, notification)
    }

    /** Shown after "End fast" on the notification, where a mis-tap is easy and there's no app UI to undo from. */
    fun showEnded(fast: Fast) {
        if (!canNotify()) return
        val resume = PendingIntent.getBroadcast(
            context,
            FastActionReceiver.REQUEST_RESUME,
            Intent(context, FastActionReceiver::class.java)
                .setAction(FastActionReceiver.ACTION_RESUME)
                .putExtra(FastActionReceiver.EXTRA_FAST_ID, fast.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ENDED)
            .setSmallIcon(R.drawable.ic_stat_fast)
            .setColor(accent())
            .setContentTitle("Fast ended · ${Formatting.duration(FastRules.elapsedMillis(fast, fast.endedAt!!))}")
            .setContentText("${time(fast.startedAt)} → ${time(fast.endedAt)}")
            .setAutoCancel(true)
            .setTimeoutAfter(ENDED_TIMEOUT_MS)
            .setContentIntent(openApp())
            .addAction(0, "Undo", resume)
            .build()
        notify(ID_ENDED, notification)
    }

    fun cancelEnded() = manager.cancel(ID_ENDED)

    private fun notify(id: Int, notification: android.app.Notification) {
        if (canNotify()) {
            @Suppress("MissingPermission") // checked in canNotify()
            manager.notify(id, notification)
        }
    }

    /** "20:04", or "Thu 20:04" when it isn't today. */
    private fun time(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val dateTime = Instant.ofEpochMilli(millis).atZone(zone)
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        val clock = dateTime.format(DateTimeFormatter.ofPattern(pattern))
        return if (dateTime.toLocalDate() == LocalDate.now(zone)) clock
        else dateTime.format(DateTimeFormatter.ofPattern("EEE")) + " " + clock
    }

    private fun accent(): Int = paletteFor(settings.theme.value).accent.toArgb()

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun broadcast(action: String, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, FastActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val CHANNEL_ACTIVE = "fast_running"
        const val CHANNEL_TARGET = "fast_target"
        const val CHANNEL_ENDED = "fast_ended"
        const val ID_ACTIVE = 1
        const val ID_TARGET = 2
        const val ID_ENDED = 3
        const val ENDED_TIMEOUT_MS = 10 * 60_000L
    }
}
