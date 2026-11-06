package com.akshar.data.backup

/** The file is password protected and no password was given yet. */
class BackupPasswordRequiredException : Exception("This backup is password protected.")

/** Turns the text of a backup file, plain or encrypted, into backup JSON for [BackupCodec]. */
object BackupFiles {

    /** Larger than any real ledger; stops a wrong pick (a video, say) from filling memory. */
    const val MAX_BYTES = 64L * 1024 * 1024

    /**
     * @throws BackupPasswordRequiredException if [text] is encrypted and [password] is null.
     * @throws WrongBackupPasswordException if the password does not open it.
     */
    fun openBackup(text: String, password: CharArray?): String {
        if (!BackupCrypto.isEncrypted(text)) return text
        if (password == null) throw BackupPasswordRequiredException()
        return BackupCrypto.decrypt(text, password)
    }
}
