package com.akshar.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.akshar.ExpenseTrackerApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Runs one automatic backup in the background. Everything stays on the device. */
class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runLock.withLock { runBackup() }
    }

    private suspend fun runBackup(): Result {
        val store = AutoBackupStore(applicationContext)
        val settings = store.read()
        val manual = inputData.getBoolean(KEY_MANUAL, false)
        if (!settings.enabled && !manual) return Result.success()

        val destination = settings.folderUri?.let { SafBackupDestination(applicationContext, Uri.parse(it)) }
        val password = if (settings.encrypt) store.loadPassword() else null
        val repository = (applicationContext as ExpenseTrackerApp).repository
        val now = System.currentTimeMillis()
        val outcome = try {
            AutoBackupRunner.run(settings, destination, password, now) {
                BackupCodec.encode(repository.exportSnapshot())
            }
        } finally {
            password?.fill('\u0000')
        }

        return when (outcome) {
            is AutoBackupOutcome.Success -> {
                store.recordSuccess(now, outcome.fileName)
                outcome.warning?.let { store.recordError(now, it) }
                Result.success()
            }
            is AutoBackupOutcome.Failed -> {
                store.recordError(now, outcome.message)
                if (outcome.retryable && runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
        }
    }

    companion object {
        const val KEY_MANUAL = "manual"
        private const val MAX_RETRIES = 2
        private val runLock = Mutex()
    }
}

/** Schedules automatic backups with WorkManager, which works fully offline. */
object AutoBackupScheduler {

    const val PERIODIC_WORK = "auto_backup_periodic"
    const val NOW_WORK = "auto_backup_now"

    /** Starts, updates or cancels the periodic backup to match [settings]. */
    fun sync(context: Context, settings: AutoBackupSettings) {
        val workManager = WorkManager.getInstance(context)
        if (!settings.enabled || settings.folderUri == null) {
            workManager.cancelUniqueWork(PERIODIC_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(settings.frequency.days, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /**
     * A backup the user asked for. It is not held back by the battery constraint, and it
     * replaces an earlier manual run that is still waiting to retry, so a tap always runs now.
     * Runs can't overlap: the worker holds a lock while writing.
     */
    fun backUpNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(NOW_WORK, ExistingWorkPolicy.REPLACE, manualRequest())
    }

    private fun manualRequest() = OneTimeWorkRequestBuilder<AutoBackupWorker>()
        .setInputData(workDataOf(AutoBackupWorker.KEY_MANUAL to true))
        .build()
}

/** A Storage Access Framework folder the user picked and granted persistent access to. */
class SafBackupDestination(context: Context, private val treeUri: Uri) : BackupDestination {

    private val appContext = context.applicationContext

    private fun folder(): DocumentFile? = DocumentFile.fromTreeUri(appContext, treeUri)

    override fun isAccessible(): Boolean {
        val granted = appContext.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isWritePermission
        }
        if (!granted) return false
        val dir = folder() ?: return false
        return dir.exists() && dir.isDirectory && dir.canWrite()
    }

    override fun list(): List<BackupFileEntry> =
        folder()?.listFiles().orEmpty()
            .filter { it.isFile }
            .mapNotNull { file -> file.name?.let { BackupFileEntry(it, file.lastModified()) } }

    override fun write(name: String, content: ByteArray): String {
        val dir = folder() ?: throw IOException("Backup folder unavailable")
        // SAF providers often rename duplicates as "name (1).json", which is outside our
        // rotation namespace. Choose our own suffix and never overwrite an existing backup.
        var candidate = name
        var suffix = 1
        while (dir.findFile(candidate) != null) {
            candidate = name.removeSuffix(".json") + "-${suffix++}.json"
        }
        val file = dir.createFile(MIME_JSON, candidate) ?: throw IOException("Could not create $name")
        try {
            val stream = appContext.contentResolver.openOutputStream(file.uri, "w")
                ?: throw IOException("Could not open $name")
            stream.use { it.write(content) }
        } catch (e: Exception) {
            runCatching { file.delete() }
            throw e
        }
        return file.name ?: name
    }

    override fun delete(name: String): Boolean = folder()?.findFile(name)?.delete() == true

    companion object {
        const val MIME_JSON = "application/json"

        /** Read and write access the app keeps across restarts. */
        const val PERSISTED_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
