package com.akshar.data.backup

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** A file in the backup folder, as listed by the destination. */
data class BackupFileEntry(val name: String, val lastModified: Long)

/**
 * File naming and rotation for automatic backups.
 *
 * Only files this feature wrote (`akspend-auto-…`) are ever considered for deletion, so manual
 * backups or other files the user keeps in the same folder are never touched.
 */
object AutoBackupRotation {

    const val PREFIX = "akspend-auto-"
    const val MIN_KEEP = 1
    const val MAX_KEEP = 30
    const val DEFAULT_KEEP = 7

    private val NAME = Regex("""^akspend-auto-(\d{8}-\d{6})(?:-\d+)?\.json$""")

    private fun stampFormat() = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /** UTC timestamp in the name keeps names sortable and unique per second. */
    fun fileName(now: Long): String = "$PREFIX${stampFormat().format(Date(now))}.json"

    fun isAutoBackup(name: String): Boolean = NAME.matches(name)

    /** When the file was made: from its name, or its modified time if the name was edited. */
    fun createdAt(entry: BackupFileEntry): Long {
        val stamp = NAME.matchEntire(entry.name)?.groupValues?.get(1) ?: return entry.lastModified
        return try {
            stampFormat().parse(stamp)?.time ?: entry.lastModified
        } catch (e: ParseException) {
            entry.lastModified
        }
    }

    /** Automatic backups to delete so that only the [keep] newest remain. */
    fun filesToDelete(files: List<BackupFileEntry>, keep: Int, savedName: String? = null): List<BackupFileEntry> {
        val keepCount = keep.coerceIn(MIN_KEEP, MAX_KEEP)
        return files
            .filter { isAutoBackup(it.name) }
            .sortedWith(compareByDescending<BackupFileEntry> { it.name == savedName }
                .thenByDescending { createdAt(it) }.thenByDescending { it.name })
            .drop(keepCount)
    }
}
