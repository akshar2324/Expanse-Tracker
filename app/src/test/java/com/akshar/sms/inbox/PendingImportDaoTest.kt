package com.akshar.sms.inbox

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PendingImportDaoTest {

    private lateinit var db: ImportInboxDatabase
    private lateinit var dao: PendingImportDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ImportInboxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.pendingImportDao()
    }

    @After
    fun tearDown() = db.close()

    private fun item(key: String, receivedAt: Long = 1_000L) = PendingImport(
        amount = "10.00", isExpense = true, description = "Shop", dateMillis = receivedAt, category = "Other",
        paymentMethod = null, confident = true, accountTail = null, source = "Messages",
        receivedAt = receivedAt, dedupeKey = key,
    )

    @Test
    fun sameNotificationTwice_isQueuedOnce() = runBlocking {
        assertTrue(dao.insert(item("a")) > 0)
        assertEquals(-1L, dao.insert(item("a")))
        dao.insert(item("b"))
        assertEquals(2, dao.observeCount().first())
    }

    @Test
    fun oldItemsArePruned_andNewestComesFirst() = runBlocking {
        dao.insert(item("old", receivedAt = 100L))
        dao.insert(item("new", receivedAt = 5_000L))
        assertEquals(listOf("new", "old"), dao.observeAll().first().map { it.dedupeKey })
        dao.deleteOlderThan(1_000L)
        assertEquals(listOf("new"), dao.observeAll().first().map { it.dedupeKey })
    }
}
