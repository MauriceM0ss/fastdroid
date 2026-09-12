package io.github.mauricem0ss.fastdroid.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One fast. [endedAt] is null while it is running — at most one row may be in
 * that state. [targetMinutes] is snapshotted when the fast starts, so changing
 * the default target later never rewrites history.
 */
@Entity(tableName = "fasts", indices = [Index("startedAt"), Index("endedAt")])
data class Fast(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long?,
    val targetMinutes: Int,
) {
    val isActive: Boolean get() = endedAt == null
}
