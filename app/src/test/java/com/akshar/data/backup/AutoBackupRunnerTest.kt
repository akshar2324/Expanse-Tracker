package com.akshar.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AutoBackupRunnerTest {

    private class FakeFolder(
        var accessible: Boolean = true,
        val files: MutableMap<String, ByteArray> = linkedMapOf(),
        var failWrite: Exception? = null
    ) : BackupDestination {
        override fun isAccessible() = accessible
        override fun list() = files.keys.map { BackupFileEntry(it, 0L) }
        override fun write(name: String, content: ByteArray): String {
            failWrite?.let { throw it }
            files[name] = content
            return name
        }
        override fun delete(name: String) = files.remove(name) != null
    }

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_780_000_000_000L
    private val settings = AutoBackupSettings(enabled = true, folderUri = "content://tree/backups", keepCount = 2)
    private val json = """{"transactions":[]}"""

    @Test
    fun `writes a plain backup and rotates old ones`() = runTest {
        val folder = FakeFolder()
        repeat(3) { folder.files[AutoBackupRotation.fileName(now - (it + 1) * day)] = ByteArray(1) }
        folder.files["manual.db"] = ByteArray(1)

        val outcome = AutoBackupRunner.run(settings, folder, null, now) { json } as AutoBackupOutcome.Success

        assertEquals(AutoBackupRotation.fileName(now), outcome.fileName)
        assertEquals(json, String(folder.files.getValue(outcome.fileName)))
        assertEquals(2, outcome.deleted.size)
        assertEquals(
            setOf(AutoBackupRotation.fileName(now), AutoBackupRotation.fileName(now - day), "manual.db"),
            folder.files.keys
        )
    }

    @Test
    fun `encrypted backups need the password to open`() = runTest {
        val folder = FakeFolder()
        val outcome = AutoBackupRunner.run(settings.copy(encrypt = true), folder, "pw123456".toCharArray(), now) { json }
            as AutoBackupOutcome.Success
        val written = String(folder.files.getValue(outcome.fileName))
        assertTrue(BackupCrypto.isEncrypted(written))
        assertEquals(json, BackupFiles.openBackup(written, "pw123456".toCharArray()))
    }

    @Test
    fun `revoked folder access is reported and not retried`() = runTest {
        val outcome = AutoBackupRunner.run(settings, FakeFolder(accessible = false), null, now) { json }
        assertEquals(AutoBackupOutcome.Failed(AutoBackupRunner.MSG_FOLDER_LOST, retryable = false), outcome)
    }

    @Test
    fun `security exception while writing counts as lost access`() = runTest {
        val outcome = AutoBackupRunner.run(settings, FakeFolder(failWrite = SecurityException("revoked")), null, now) { json }
        assertEquals(AutoBackupOutcome.Failed(AutoBackupRunner.MSG_FOLDER_LOST, retryable = false), outcome)
    }

    @Test
    fun `io errors are retryable`() = runTest {
        val outcome = AutoBackupRunner.run(settings, FakeFolder(failWrite = IOException("full")), null, now) { json }
        assertTrue((outcome as AutoBackupOutcome.Failed).retryable)
    }

    @Test
    fun `missing folder or password stops before exporting`() = runTest {
        var exported = false
        val noFolder = AutoBackupRunner.run(settings.copy(folderUri = null), null, null, now) { exported = true; json }
        assertEquals(AutoBackupRunner.MSG_NO_FOLDER, (noFolder as AutoBackupOutcome.Failed).message)
        val noPassword = AutoBackupRunner.run(settings.copy(encrypt = true), FakeFolder(), null, now) { exported = true; json }
        assertEquals(AutoBackupRunner.MSG_NO_PASSWORD, (noPassword as AutoBackupOutcome.Failed).message)
        assertFalse(exported)
    }

    @Test
    fun `failed rotation still counts as a successful backup`() = runTest {
        val folder = object : BackupDestination {
            override fun isAccessible() = true
            override fun list(): List<BackupFileEntry> = throw IOException("listing failed")
            override fun write(name: String, content: ByteArray) = name
            override fun delete(name: String) = false
        }
        val outcome = AutoBackupRunner.run(settings, folder, null, now) { json } as AutoBackupOutcome.Success
        assertEquals(AutoBackupRunner.MSG_ROTATION_FAILED, outcome.warning)
    }
    @Test
    fun `failed deletion is visible without losing the successful backup`() = runTest {
        val folder = object : BackupDestination {
            override fun isAccessible() = true
            override fun list() = (0..3).map { BackupFileEntry(AutoBackupRotation.fileName(now - it * day), 0) }
            override fun write(name: String, content: ByteArray) = name
            override fun delete(name: String) = false
        }
        val outcome = AutoBackupRunner.run(settings, folder, null, now) { json } as AutoBackupOutcome.Success
        assertTrue(outcome.deleted.isEmpty())
        assertEquals(AutoBackupRunner.MSG_ROTATION_FAILED, outcome.warning)
    }

    @Test(expected = CancellationException::class)
    fun `cancelling the export propagates without writing`() = runTest {
        AutoBackupRunner.run(settings, FakeFolder(), null, now) { throw CancellationException() }
    }

}
