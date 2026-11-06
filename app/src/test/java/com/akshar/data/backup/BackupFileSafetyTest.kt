package com.akshar.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.akshar.data.db.AppDatabase
import com.akshar.data.db.LegacyDatabaseFixtures
import com.akshar.data.db.MIGRATION_4_5
import com.akshar.data.db.MIGRATION_5_6
import com.akshar.ui.viewmodel.DatabaseFileValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupFileSafetyTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun exportAuthorityMatchesInstalledApplication() {
        val file = File(context.cacheDir, "akspend_ledger_backup.db").apply { writeText("test") }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            assertEquals("${context.packageName}.fileprovider", uri.authority)
        } finally { file.delete() }
    }

    @Test
    fun unrelatedSqliteWithSupportedVersionIsRejected() {
        val file = File(context.cacheDir, "unrelated.db")
        try {
            SQLiteDatabase.openOrCreateDatabase(file, null).use {
                it.execSQL("CREATE TABLE notes (body TEXT)")
                it.version = 6
            }
            assertNotNull(DatabaseFileValidator.problemWith(file, 6, context))
        } finally { SQLiteDatabase.deleteDatabase(file) }
    }

    @Test
    fun realVersionsFourFiveAndSixAreValidatedAndMigrateWithoutLosingMoney() = runBlocking {
        for (version in 4..6) {
            val file = File(context.cacheDir, "legacy-$version.db")
            try {
                LegacyDatabaseFixtures.create(file, version)
                assertNull("version $version", DatabaseFileValidator.problemWith(file, 6, context))
                // Validation must not migrate or otherwise modify the selected backup.
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use {
                    assertEquals(version, it.version)
                }
                val db = Room.databaseBuilder(context, AppDatabase::class.java, file.absolutePath)
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6).build()
                try {
                    val tx = db.financeDao().getTransactionsList().single()
                    assertEquals(42L, tx.id)
                    assertEquals(123456.78, tx.amount, 0.0)
                    assertEquals(1L, tx.accountId)
                    assertNull(tx.transferId)
                    if (version < 6) assertEquals(1L, db.financeDao().getAccountsList().single().id)
                } finally { db.close() }
            } finally { SQLiteDatabase.deleteDatabase(file) }
        }
    }
}
