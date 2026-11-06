package com.akshar.data.backup

import android.util.Log
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Where automatic backups are written. The app uses a Storage Access Framework folder. */
interface BackupDestination {
    /** False when the folder is gone or the app's access to it was removed. */
    fun isAccessible(): Boolean
    fun list(): List<BackupFileEntry>
    /** Writes [content] as a new file and returns the name it was saved under. */
    fun write(name: String, content: ByteArray): String
    fun delete(name: String): Boolean
}

sealed class AutoBackupOutcome {
    data class Success(val fileName: String, val deleted: List<String>, val warning: String? = null) : AutoBackupOutcome()
    /** [retryable] failures (I/O hiccups) are worth another attempt; the rest need the user. */
    data class Failed(val message: String, val retryable: Boolean) : AutoBackupOutcome()
}

/**
 * One automatic backup run: export, optionally encrypt, write, then rotate old copies.
 * Kept free of WorkManager so it can be tested with a fake folder.
 */
object AutoBackupRunner {

    const val MSG_NO_FOLDER = "Choose a folder for automatic backups."
    const val MSG_FOLDER_LOST = "AkSpend can no longer reach the backup folder. Choose the folder again."
    const val MSG_NO_PASSWORD = "The backup password is missing. Set it again or turn off encryption."

    suspend fun run(
        settings: AutoBackupSettings,
        destination: BackupDestination?,
        password: CharArray?,
        now: Long,
        exportJson: suspend () -> String
    ): AutoBackupOutcome {
        if (destination == null || settings.folderUri == null) return AutoBackupOutcome.Failed(MSG_NO_FOLDER, retryable = false)
        if (!safely { destination.isAccessible() }) return AutoBackupOutcome.Failed(MSG_FOLDER_LOST, retryable = false)
        if (settings.encrypt && (password == null || password.isEmpty())) {
            return AutoBackupOutcome.Failed(MSG_NO_PASSWORD, retryable = false)
        }

        val content = try {
            val json = exportJson()
            if (settings.encrypt) BackupCrypto.encrypt(json, password!!) else json
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Automatic backup export failed", e)
            return AutoBackupOutcome.Failed("Could not read your data for the backup.", retryable = true)
        }

        currentCoroutineContext().ensureActive()
        val savedName = try {
            destination.write(AutoBackupRotation.fileName(now), content.toByteArray(Charsets.UTF_8))
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            return AutoBackupOutcome.Failed(MSG_FOLDER_LOST, retryable = false)
        } catch (e: IOException) {
            Log.e(TAG, "Automatic backup write failed", e)
            return AutoBackupOutcome.Failed("Could not write the backup file. Is the storage full?", retryable = true)
        } catch (e: Exception) {
            Log.e(TAG, "Automatic backup write failed", e)
            return AutoBackupOutcome.Failed("Could not write the backup file.", retryable = true)
        }

        // Keep the new, complete backup even after a clock change. Report retention failures
        // separately so the UI can show both the successful backup time and the problem.
        currentCoroutineContext().ensureActive()
        val deleted = mutableListOf<String>()
        var warning: String? = null
        try {
            AutoBackupRotation.filesToDelete(destination.list(), settings.keepCount, savedName)
                .forEach { file ->
                    if (destination.delete(file.name)) deleted += file.name
                    else warning = MSG_ROTATION_FAILED
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not remove old automatic backups", e)
            warning = MSG_ROTATION_FAILED
        }
        return AutoBackupOutcome.Success(savedName, deleted, warning)
    }

    const val MSG_ROTATION_FAILED = "Backup saved, but older backups could not be removed. Check the folder access and retention setting."

    private inline fun safely(block: () -> Boolean): Boolean = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    private const val TAG = "AutoBackup"
}
