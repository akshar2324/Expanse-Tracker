package com.akshar.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.ui.viewmodel.DatabaseFileValidator
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BackupCodecTest {

    @Test
    fun `legacy backup without ids keeps transactions linked to accounts in order`() {
        val legacy = """
            {"transactions":[{"amount":5.5,"type":"EXPENSE","category":"Food","description":"Tea","date":1,"paymentMethod":"Cash","accountId":2}],
             "accounts":[{"name":"Cash","type":"Cash","openingBalance":0.0,"isArchived":false},
                         {"name":"Bank","type":"Bank","openingBalance":100.0,"isArchived":false}]}
        """.trimIndent()
        val snapshot = BackupCodec.decode(legacy)
        assertEquals(listOf(1L, 2L), snapshot.accounts.map { it.id })
        assertEquals("Bank", snapshot.accounts.first { it.id == snapshot.transactions.single().accountId }.name)
    }

    @Test
    fun `backup without accounts gets the default account`() {
        val snapshot = BackupCodec.decode("""{"transactions":[]}""")
        assertEquals(1L, snapshot.accounts.single().id)
    }

    @Test(expected = JSONException::class)
    fun `object with no known sections is rejected so it cannot wipe the ledger`() {
        BackupCodec.decode("{}")
    }

    @Test(expected = JSONException::class)
    fun `section of the wrong type is rejected`() {
        BackupCodec.decode("""{"transactions":"oops"}""")
    }

    @Test(expected = JSONException::class)
    fun `row missing a required field is rejected`() {
        BackupCodec.decode("""{"transactions":[{"type":"EXPENSE","category":"Food","date":1}]}""")
    }

    @Test(expected = JSONException::class)
    fun `future JSON version cannot replace the ledger`() {
        BackupCodec.decode("""{"version":999,"transactions":[]}""")
    }

    @Test
    fun `database file validator rejects header-only corrupt files`() {
        val dir = kotlin.io.path.createTempDirectory("akspend-validator").toFile()
        try {
            val text = File(dir, "notes.db").apply { writeText("hello ".repeat(40)) }
            assertNotNull(DatabaseFileValidator.problemWith(text, 6, androidx.test.core.app.ApplicationProvider.getApplicationContext()))

            fun sqliteWithVersion(v: Int) = File(dir, "v$v.db").apply {
                val bytes = ByteArray(4096)
                "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII).copyInto(bytes)
                bytes[63] = v.toByte()
                writeBytes(bytes)
            }
            assertNotNull(DatabaseFileValidator.problemWith(sqliteWithVersion(6), 6, androidx.test.core.app.ApplicationProvider.getApplicationContext()))
            assertNotNull(DatabaseFileValidator.problemWith(sqliteWithVersion(4), 6, androidx.test.core.app.ApplicationProvider.getApplicationContext()))
            assertNotNull(DatabaseFileValidator.problemWith(sqliteWithVersion(7), 6, androidx.test.core.app.ApplicationProvider.getApplicationContext()))
            assertNotNull(DatabaseFileValidator.problemWith(sqliteWithVersion(0), 6, androidx.test.core.app.ApplicationProvider.getApplicationContext()))
            assertNotNull(DatabaseFileValidator.problemWith(File(dir, "missing.db"), 6, androidx.test.core.app.ApplicationProvider.getApplicationContext()))
        } finally {
            dir.deleteRecursively()
        }
    }
}
