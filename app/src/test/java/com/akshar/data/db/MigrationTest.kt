package com.akshar.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.data.model.MerchantCategory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test.db"

    @After
    fun cleanup() {
        context.deleteDatabase(name)
    }

    private fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(*ALL_MIGRATIONS)
        .allowMainThreadQueries()
        .build()

    /**
     * Builds a version 6 database (today's schema without the tables version 7 added), then lets
     * Room upgrade it. Room checks every table against the entities when it opens, so a migration
     * that creates the wrong columns, types or keys fails here.
     */
    @Test
    fun migrate6To7_addsMerchantCategoriesAndKeepsTransactions() = runBlocking {
        open().apply { openHelper.writableDatabase; close() }
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("DROP TABLE merchant_categories")
            db.execSQL(
                "INSERT INTO transactions (amount, type, category, description, date, paymentMethod, timestamp, accountId) " +
                    "VALUES (250.5, 'EXPENSE', 'Food', 'Swiggy', 1700000000000, 'UPI', 1700000000000, 1)"
            )
            db.version = 6
            db.rawQuery("SELECT name FROM sqlite_master WHERE name = 'merchant_categories'", null).use { assertFalse(it.moveToFirst()) }
        }

        val migrated = open()
        try {
            val dao = migrated.financeDao()
            val saved = dao.getTransactionsList().single()
            assertEquals(250.5, saved.amount, 0.0)
            assertEquals("Swiggy", saved.description)

            dao.upsertMerchantCategory(MerchantCategory("swiggy", "EXPENSE", "Swiggy", "Food", 1L))
            assertEquals("Food", dao.getMerchantCategoriesList().single().category)
            assertEquals(DATABASE_VERSION, migrated.openHelper.readableDatabase.version)
        } finally {
            migrated.close()
        }
    }
}
