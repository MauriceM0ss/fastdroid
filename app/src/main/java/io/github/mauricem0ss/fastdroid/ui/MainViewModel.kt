package io.github.mauricem0ss.fastdroid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mauricem0ss.fastdroid.AppContainer
import io.github.mauricem0ss.fastdroid.data.AppTheme
import io.github.mauricem0ss.fastdroid.data.Fast
import io.github.mauricem0ss.fastdroid.domain.EditError
import io.github.mauricem0ss.fastdroid.domain.FastRules
import io.github.mauricem0ss.fastdroid.domain.Formatting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

/** Null until the database has answered, so the timer doesn't flash "not fasting" on launch. */
data class TimerState(val active: Fast?, val lastCompleted: Fast?)

data class UiMessage(val text: String, val undoFastId: Long? = null)

class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val repository = container.repository
    private val settings = container.settings

    val defaultTarget: StateFlow<Int> = settings.targetMinutes
    val theme: StateFlow<AppTheme> = settings.theme

    val timer: StateFlow<TimerState?> =
        combine(repository.activeFast, repository.lastCompleted, ::TimerState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthFasts: StateFlow<List<Fast>> = _month
        .flatMapLatest { month ->
            val bounds = FastRules.monthBounds(month, ZoneId.systemDefault())
            repository.endedBetween(bounds.first, bounds.last + 1)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    fun showMonth(month: YearMonth) {
        _month.value = month
    }

    fun startFast(startedAt: Long? = null, onResult: (EditError?) -> Unit = {}) {
        viewModelScope.launch {
            val error = if (startedAt == null) repository.startFast() else repository.startFast(startedAt)
            error?.let { _messages.emit(UiMessage(it.message)) }
            onResult(error)
        }
    }

    fun endFast() {
        viewModelScope.launch {
            val ended = repository.endFast() ?: return@launch
            val duration = Formatting.duration(FastRules.elapsedMillis(ended, ended.endedAt!!))
            _messages.emit(UiMessage("Fast ended · $duration", undoFastId = ended.id))
        }
    }

    fun resumeFast(id: Long) {
        viewModelScope.launch {
            repository.resumeFast(id)?.let { _messages.emit(UiMessage(it.message)) }
        }
    }

    fun saveFast(fast: Fast, onResult: (EditError?) -> Unit) {
        viewModelScope.launch { onResult(repository.saveFast(fast)) }
    }

    fun deleteFast(fast: Fast) {
        viewModelScope.launch { repository.deleteFast(fast) }
    }

    fun setDefaultTarget(minutes: Int) = settings.setTargetMinutes(minutes)

    fun setTheme(theme: AppTheme) {
        settings.setTheme(theme)
        // The notification's accent colour follows the theme.
        viewModelScope.launch { repository.syncSystemState() }
    }

    /** After notification permission is granted, post the notification a running fast is missing. */
    fun resyncNotifications() {
        viewModelScope.launch { repository.syncSystemState() }
    }

    fun canNotify(): Boolean = container.notifier.canNotify()

    fun canPromote(): Boolean = container.notifier.canPromote()
}
