package io.github.mauricem0ss.fastdroid.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against real SQLite on a device, where Room query quirks actually show up. */
@RunWith(AndroidJUnit4::class)
class FastDaoTest {
    private lateinit var db: FastDatabase
    private lateinit var dao: FastDao

    private val hour = 3_600_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, FastDatabase::class.java)
            .build()
        dao = db.fastDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun activeFastIsTheOneWithoutEnd() = runTest {
        dao.insert(Fast(startedAt = 0, endedAt = 16 * hour, targetMinutes = 960))
        assertNull(dao.getActive())
        val id = dao.insert(Fast(startedAt = 20 * hour, endedAt = null, targetMinutes = 960))
        assertEquals(id, dao.getActive()?.id)
        assertEquals(id, dao.observeActive().first()?.id)
    }

    @Test
    fun endedBetweenUsesEndTimeAndHalfOpenRange() = runTest {
        dao.insert(Fast(startedAt = 0, endedAt = 10 * hour, targetMinutes = 960))
        dao.insert(Fast(startedAt = 11 * hour, endedAt = 20 * hour, targetMinutes = 960))
        dao.insert(Fast(startedAt = 21 * hour, endedAt = null, targetMinutes = 960))
        val result = dao.observeEndedBetween(5 * hour, 20 * hour).first()
        assertEquals(listOf(10 * hour), result.map { it.endedAt })
    }

    @Test
    fun overlapTreatsRunningFastAsOpenEnded() = runTest {
        val running = dao.insert(Fast(startedAt = 10 * hour, endedAt = null, targetMinutes = 960))
        assertTrue(dao.findOverlapping(100 * hour, 101 * hour, excludeId = 0).any { it.id == running })
        assertTrue(dao.findOverlapping(0, 10 * hour, excludeId = 0).isEmpty())
        assertTrue(dao.findOverlapping(100 * hour, 101 * hour, excludeId = running).isEmpty())
    }

    @Test
    fun touchingFastsDoNotOverlap() = runTest {
        dao.insert(Fast(startedAt = 0, endedAt = 10 * hour, targetMinutes = 960))
        assertTrue(dao.findOverlapping(10 * hour, 12 * hour, excludeId = 0).isEmpty())
        assertEquals(1, dao.findOverlapping(9 * hour, 12 * hour, excludeId = 0).size)
    }
}
