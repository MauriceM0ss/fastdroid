package io.github.mauricem0ss.fastdroid

import android.app.Application
import android.content.Context
import kotlinx.coroutines.launch

class FastDroidApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.createChannels()
        // A force-stop clears alarms and notifications; put them back whenever the process starts.
        container.appScope.launch { container.repository.syncSystemState() }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as FastDroidApp).container
