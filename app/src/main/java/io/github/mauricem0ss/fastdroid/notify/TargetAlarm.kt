package io.github.mauricem0ss.fastdroid.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.mauricem0ss.fastdroid.data.Fast
import io.github.mauricem0ss.fastdroid.domain.FastRules

/** One alarm at the running fast's target time. Nothing ticks in the background. */
class TargetAlarm(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(fast: Fast) {
        val at = FastRules.targetEndAt(fast)
        if (at <= System.currentTimeMillis()) {
            cancel()
            return
        }
        val pending = pendingIntent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun cancel() = alarmManager.cancel(pendingIntent())

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        FastActionReceiver.REQUEST_TARGET,
        Intent(context, FastActionReceiver::class.java).setAction(FastActionReceiver.ACTION_TARGET_REACHED),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
