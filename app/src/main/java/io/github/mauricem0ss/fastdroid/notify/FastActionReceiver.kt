package io.github.mauricem0ss.fastdroid.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.mauricem0ss.fastdroid.container
import kotlinx.coroutines.launch

/** Notification dismissal, the target alarm, and boot. */
class FastActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = context.container
        val repository = container.repository
        val pending = goAsync()
        container.appScope.launch {
            try {
                when (intent.action) {
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
        const val ACTION_DISMISSED = "io.github.mauricem0ss.fastdroid.DISMISSED"
        const val ACTION_TARGET_REACHED = "io.github.mauricem0ss.fastdroid.TARGET_REACHED"

        const val REQUEST_DISMISSED = 3
        const val REQUEST_TARGET = 4
    }
}
