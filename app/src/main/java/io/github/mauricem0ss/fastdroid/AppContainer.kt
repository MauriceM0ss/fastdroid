package io.github.mauricem0ss.fastdroid

import android.content.Context
import io.github.mauricem0ss.fastdroid.data.FastDatabase
import io.github.mauricem0ss.fastdroid.data.FastRepository
import io.github.mauricem0ss.fastdroid.data.SettingsStore
import io.github.mauricem0ss.fastdroid.notify.FastNotifier
import io.github.mauricem0ss.fastdroid.notify.TargetAlarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hand-rolled dependency graph; small enough that a DI framework would be the bigger part. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings = SettingsStore(context)
    val notifier = FastNotifier(context, settings)
    private val database = FastDatabase.create(context)
    val repository = FastRepository(
        dao = database.fastDao(),
        settings = settings,
        notifier = notifier,
        alarm = TargetAlarm(context),
    )
}
