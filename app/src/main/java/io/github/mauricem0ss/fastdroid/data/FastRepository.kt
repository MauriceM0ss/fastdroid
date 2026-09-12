package io.github.mauricem0ss.fastdroid.data

import io.github.mauricem0ss.fastdroid.domain.EditError
import io.github.mauricem0ss.fastdroid.domain.FastRules
import io.github.mauricem0ss.fastdroid.notify.FastNotifier
import io.github.mauricem0ss.fastdroid.notify.TargetAlarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Every change to a fast goes through here, so the database, the ongoing
 * notification and the target alarm can never disagree. Callers are the UI,
 * the notification actions and the boot receiver.
 */
class FastRepository(
    private val dao: FastDao,
    private val settings: SettingsStore,
    private val notifier: FastNotifier,
    private val alarm: TargetAlarm,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    val activeFast: Flow<Fast?> = dao.observeActive()
    val lastCompleted: Flow<Fast?> = dao.observeLastCompleted()

    fun endedBetween(from: Long, to: Long): Flow<List<Fast>> = dao.observeEndedBetween(from, to)

    suspend fun startFast(startedAt: Long = clock()): EditError? = mutex.withLock {
        if (dao.getActive() != null) return EditError.ALREADY_FASTING
        FastRules.validateTimes(startedAt, null, clock())?.let { return it }
        if (dao.findOverlapping(startedAt, Long.MAX_VALUE, excludeId = 0).isNotEmpty()) return EditError.OVERLAPS
        val id = dao.insert(Fast(startedAt = startedAt, endedAt = null, targetMinutes = settings.targetMinutes.value))
        publish(dao.get(id))
        null
    }

    /** Ends the running fast and returns it, so the caller can offer an undo. */
    suspend fun endFast(endedAt: Long = clock()): Fast? = mutex.withLock {
        val active = dao.getActive() ?: return null
        val ended = active.copy(endedAt = endedAt.coerceAtLeast(active.startedAt + 1))
        dao.update(ended)
        publish(null)
        ended
    }

    /** Undo of [endFast]: reopens a fast, unless another one has started since. */
    suspend fun resumeFast(id: Long): EditError? = mutex.withLock {
        if (dao.getActive() != null) return EditError.ALREADY_FASTING
        val fast = dao.get(id) ?: return null
        if (dao.findOverlapping(fast.startedAt, Long.MAX_VALUE, excludeId = id).isNotEmpty()) return EditError.OVERLAPS
        val resumed = fast.copy(endedAt = null)
        dao.update(resumed)
        publish(resumed)
        null
    }

    /** Saves an edited fast, or inserts it when [Fast.id] is 0 (a fast added after the fact). */
    suspend fun saveFast(fast: Fast): EditError? = mutex.withLock {
        FastRules.validateTimes(fast.startedAt, fast.endedAt, clock())?.let { return it }
        if (fast.endedAt == null) {
            val active = dao.getActive()
            if (active != null && active.id != fast.id) return EditError.ALREADY_FASTING
        }
        val overlapping = dao.findOverlapping(fast.startedAt, fast.endedAt ?: Long.MAX_VALUE, excludeId = fast.id)
        if (overlapping.isNotEmpty()) return EditError.OVERLAPS
        val normalized = fast.copy(targetMinutes = FastRules.clampTarget(fast.targetMinutes))
        if (normalized.id == 0L) dao.insert(normalized) else dao.update(normalized)
        publish(dao.getActive())
        null
    }

    suspend fun deleteFast(fast: Fast) = mutex.withLock {
        dao.delete(fast)
        publish(dao.getActive())
    }

    /** Re-derives the notification and alarm from the database, e.g. after a reboot. */
    suspend fun syncSystemState() = mutex.withLock { publish(dao.getActive()) }

    /** The alarm fired: alert once, and flip the ongoing notification to its "reached" wording. */
    suspend fun onTargetReached() = mutex.withLock {
        val active = dao.getActive() ?: return
        if (!FastRules.reachedTarget(active, clock())) {
            // Fired early (the target was edited later) — just re-arm.
            publish(active)
            return
        }
        notifier.showTargetReached(active)
        notifier.showActive(active)
    }

    suspend fun onActiveNotificationDismissed() = mutex.withLock {
        dao.getActive()?.let { notifier.showActive(it) }
    }

    private fun publish(active: Fast?) {
        if (active == null) {
            notifier.cancelActive()
            alarm.cancel()
        } else {
            notifier.showActive(active)
            alarm.schedule(active)
        }
    }
}
