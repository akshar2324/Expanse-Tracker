package com.akshar.sms.inbox

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.akshar.sms.SmsDraft
import kotlinx.coroutines.flow.Flow

/**
 * A payment picked up automatically from a notification, waiting for the user to review it.
 * Only the parsed fields are kept; the notification text itself is never stored. [dedupeKey]
 * is a one-way hash so the same notification, reposted or updated, is only queued once.
 */
@Entity(tableName = "pending_imports", indices = [Index(value = ["dedupeKey"], unique = true)])
data class PendingImport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Plain decimal string with two places, always greater than zero. */
    val amount: String,
    val isExpense: Boolean,
    val description: String,
    val dateMillis: Long,
    val category: String,
    val paymentMethod: String?,
    val confident: Boolean,
    val accountTail: String?,
    /** Friendly name of the app the notification came from, e.g. "Messages". */
    val source: String,
    val receivedAt: Long,
    val dedupeKey: String,
) {
    fun toDraft() = SmsDraft(
        amount = amount,
        isExpense = isExpense,
        description = description,
        dateMillis = dateMillis,
        category = category,
        paymentMethod = paymentMethod,
        confident = confident,
        accountTail = accountTail,
    )
}

@Dao
interface PendingImportDao {
    @Query("SELECT * FROM pending_imports ORDER BY receivedAt DESC, id DESC")
    fun observeAll(): Flow<List<PendingImport>>

    @Query("SELECT COUNT(*) FROM pending_imports")
    fun observeCount(): Flow<Int>

    /** Returns -1 when an entry with the same dedupe key is already queued. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: PendingImport): Long

    @Query("DELETE FROM pending_imports WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_imports")
    suspend fun clear()

    @Query("DELETE FROM pending_imports WHERE receivedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

/**
 * Kept apart from the main finance database: these rows are short-lived drafts, they are not
 * part of backups, and the main schema doesn't need a migration for them.
 */
@Database(entities = [PendingImport::class], version = 1, exportSchema = false)
abstract class ImportInboxDatabase : RoomDatabase() {
    abstract fun pendingImportDao(): PendingImportDao

    companion object {
        @Volatile private var INSTANCE: ImportInboxDatabase? = null

        fun getInstance(context: Context): ImportInboxDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, ImportInboxDatabase::class.java, "import_inbox_db")
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }

        fun closeAndResetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
