package com.akshar.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBackupRotationTest {

    private val day = 24L * 60 * 60 * 1000
    private val start = 1_780_000_000_000L

    private fun auto(dayIndex: Int, modified: Long = 0L) =
        BackupFileEntry(AutoBackupRotation.fileName(start + dayIndex * day), modified)

    @Test
    fun `keeps the newest N and deletes the rest`() {
        val files = (0 until 10).map { auto(it) }.shuffled()
        val deleted = AutoBackupRotation.filesToDelete(files, keep = 3)
        assertEquals((0 until 7).map { auto(it).name }.toSet(), deleted.map { it.name }.toSet())
    }

    @Test
    fun `nothing is deleted while under the limit`() {
        assertTrue(AutoBackupRotation.filesToDelete(listOf(auto(0), auto(1)), keep = 7).isEmpty())
    }

    @Test
    fun `files the feature did not write are never deleted`() {
        val files = listOf(
            BackupFileEntry("akspend_ledger_backup.db", 1),
            BackupFileEntry("my notes.json", 2),
            BackupFileEntry("akspend-auto-old.json", 3),
            auto(0), auto(1), auto(2)
        )
        val deleted = AutoBackupRotation.filesToDelete(files, keep = 1)
        assertEquals(listOf(auto(1).name, auto(0).name), deleted.map { it.name })
    }

    @Test
    fun `order comes from the name not the modified time`() {
        // A copy tool can reset modified times; the stamp in the name still wins.
        val files = listOf(auto(0, modified = 999_999_999_999_999), auto(1, modified = 1))
        assertEquals(listOf(auto(0).name), AutoBackupRotation.filesToDelete(files, keep = 1).map { it.name })
    }

    @Test
    fun `duplicate names from the document provider still rotate`() {
        val dup = BackupFileEntry(auto(5).name.removeSuffix(".json") + "-1.json", 0)
        assertTrue(AutoBackupRotation.isAutoBackup(dup.name))
        val deleted = AutoBackupRotation.filesToDelete(listOf(auto(0), dup, auto(5)), keep = 2)
        assertEquals(listOf(auto(0).name), deleted.map { it.name })
    }

    @Test
    fun `keep count is clamped to at least one`() {
        val deleted = AutoBackupRotation.filesToDelete(listOf(auto(0), auto(1)), keep = 0)
        assertEquals(listOf(auto(0).name), deleted.map { it.name })
    }

    @Test
    fun `file names are stamped in UTC and round trip`() {
        val name = AutoBackupRotation.fileName(0L)
        assertEquals("akspend-auto-19700101-000000.json", name)
        assertEquals(0L, AutoBackupRotation.createdAt(BackupFileEntry(name, 42)))
        assertFalse(AutoBackupRotation.isAutoBackup("akspend-auto-19700101.json"))
    }
    @Test
    fun `new backup survives a backwards clock change while respecting the count`() {
        val files = (0..4).map { auto(it) }
        val deleted = AutoBackupRotation.filesToDelete(files, keep = 2, savedName = auto(0).name)
        assertEquals(setOf(auto(1).name, auto(2).name, auto(3).name), deleted.map { it.name }.toSet())
    }

}
