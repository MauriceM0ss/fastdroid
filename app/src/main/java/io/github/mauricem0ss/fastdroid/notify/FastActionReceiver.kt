package io.github.mauricem0ss.fastdroid.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.mauricem0ss.fastdroid.container
import kotlinx.coroutines.launch

/** Notification buttons, notification dismissal, the target alarm, and boot. */
class FastActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = context.container
        val repository = container.repository
        val pending = goAsync()
        container.appScope.launch {
            try {
                when (intent.action) {
                    ACTION_END -> repository.endFast()?.let { container.notifier.showEnded(it) }
                    ACTION_RESUME -> {
                        container.notifier.cancelEnded()
                        repository.resumeFast(intent.getLongExtra(EXTRA_FAST_ID, 0))
                    }
                    ACTION_DISMISSED -> repository.onActiveNotificationDismissed()
                    ACTION_TARGET_REACHED -> repository.onTargetReached()
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED -> repository.syncSystemState()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_END = "io.github.mauricem0ss.fastdroid.END"
        const val ACTION_RESUME = "io.github.mauricem0ss.fastdroid.RESUME"
        const val ACTION_DISMISSED = "io.github.mauricem0ss.fastdroid.DISMISSED"
        const val ACTION_TARGET_REACHED = "io.github.mauricem0ss.fastdroid.TARGET_REACHED"
        const val EXTRA_FAST_ID = "fast_id"

        const val REQUEST_END = 1
        const val REQUEST_RESUME = 2
        const val REQUEST_DISMISSED = 3
        const val REQUEST_TARGET = 4
    }
}
