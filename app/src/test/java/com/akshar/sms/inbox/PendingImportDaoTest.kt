package com.akshar.sms.inbox

import androidx.room.Room
import android.content.Context
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

    @Test
    fun upgradePreservesExistingDraftsAndNewReferenceSurvivesReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "inbox-migration-test"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { legacy ->
            legacy.execSQL("""CREATE TABLE pending_imports (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, amount TEXT NOT NULL,
                isExpense INTEGER NOT NULL, description TEXT NOT NULL, dateMillis INTEGER NOT NULL,
                category TEXT NOT NULL, paymentMethod TEXT, confident INTEGER NOT NULL,
                accountTail TEXT, source TEXT NOT NULL, receivedAt INTEGER NOT NULL, dedupeKey TEXT NOT NULL)""")
            legacy.execSQL("CREATE UNIQUE INDEX index_pending_imports_dedupeKey ON pending_imports(dedupeKey)")
            legacy.execSQL("""INSERT INTO pending_imports VALUES
                (1, '10.00', 1, 'Shop', 1000, 'Other', NULL, 1, NULL, 'Messages', 1000, 'old')""")
            legacy.version = 1
        }
        fun open() = Room.databaseBuilder(context, ImportInboxDatabase::class.java, name)
            .addMigrations(INBOX_MIGRATION_1_2).allowMainThreadQueries().build()
        try {
            val migrated = open()
            try {
                val old = migrated.pendingImportDao().observeAll().first().single()
                assertEquals("old", old.dedupeKey)
                assertEquals(null, old.referenceKey)
                migrated.pendingImportDao().insert(item("new").copy(referenceKey = "ref:synthetic"))
            } finally { migrated.close() }
            val reopened = open()
            try {
                val rows = reopened.pendingImportDao().observeAll().first()
                assertEquals(2, rows.size)
                assertEquals("ref:synthetic", rows.first { it.dedupeKey == "new" }.toDraft().referenceKey)
            } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
        Unit
    }
}
