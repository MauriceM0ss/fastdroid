package io.github.mauricem0ss.fastdroid.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FastDao {
    @Query("SELECT * FROM fasts WHERE endedAt IS NULL LIMIT 1")
    fun observeActive(): Flow<Fast?>

    @Query("SELECT * FROM fasts WHERE endedAt IS NULL LIMIT 1")
    suspend fun getActive(): Fast?

    @Query("SELECT * FROM fasts WHERE endedAt IS NOT NULL ORDER BY endedAt DESC LIMIT 1")
    fun observeLastCompleted(): Flow<Fast?>

    /** Completed fasts whose end falls in [from, to) — a fast belongs to the day it ended. */
    @Query("SELECT * FROM fasts WHERE endedAt >= :from AND endedAt < :to ORDER BY endedAt")
    fun observeEndedBetween(from: Long, to: Long): Flow<List<Fast>>

    /**
     * Fasts other than [excludeId] that intersect [from, to). A running fast
     * extends indefinitely, so callers pass Long.MAX_VALUE as [to] for one.
     */
    @Query(
        "SELECT * FROM fasts WHERE id != :excludeId AND startedAt < :to " +
            "AND (endedAt IS NULL OR endedAt > :from)"
    )
    suspend fun findOverlapping(from: Long, to: Long, excludeId: Long): List<Fast>

    @Query("SELECT * FROM fasts WHERE id = :id")
    suspend fun get(id: Long): Fast?

    @Insert
    suspend fun insert(fast: Fast): Long

    @Update
    suspend fun update(fast: Fast)

    @Delete
    suspend fun delete(fast: Fast)
}
