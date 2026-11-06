package com.akshar.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class AutoBackupFrequency(val days: Long, val label: String) {
    DAILY(1, "Daily"),
    WEEKLY(7, "Weekly")
}

/** What the user chose for scheduled backups, plus the outcome of the last run. */
data class AutoBackupSettings(
    val enabled: Boolean = false,
    val frequency: AutoBackupFrequency = AutoBackupFrequency.DAILY,
    val folderUri: String? = null,
    val keepCount: Int = AutoBackupRotation.DEFAULT_KEEP,
    val encrypt: Boolean = false,
    val hasPassword: Boolean = false,
    val lastSuccessAt: Long = 0L,
    val lastFileName: String? = null,
    val lastError: String? = null,
    val lastErrorAt: Long = 0L
)

/** Settings and run status for automatic backups, kept in private app preferences. */
class AutoBackupStore(private val prefs: SharedPreferences, private val secrets: BackupPasswordStore) {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
        KeystoreBackupPasswordStore(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
    )

    fun read(): AutoBackupSettings = AutoBackupSettings(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        frequency = prefs.getString(KEY_FREQUENCY, null)
            ?.let { name -> AutoBackupFrequency.entries.firstOrNull { it.name == name } }
            ?: AutoBackupFrequency.DAILY,
        folderUri = prefs.getString(KEY_FOLDER, null),
        keepCount = prefs.getInt(KEY_KEEP, AutoBackupRotation.DEFAULT_KEEP)
            .coerceIn(AutoBackupRotation.MIN_KEEP, AutoBackupRotation.MAX_KEEP),
        encrypt = prefs.getBoolean(KEY_ENCRYPT, false),
        hasPassword = secrets.hasPassword(),
        lastSuccessAt = prefs.getLong(KEY_LAST_SUCCESS, 0L),
        lastFileName = prefs.getString(KEY_LAST_FILE, null),
        lastError = prefs.getString(KEY_LAST_ERROR, null),
        lastErrorAt = prefs.getLong(KEY_LAST_ERROR_AT, 0L)
    )

    fun setEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    fun setFrequency(frequency: AutoBackupFrequency) = prefs.edit().putString(KEY_FREQUENCY, frequency.name).apply()
    fun setKeepCount(count: Int) = prefs.edit()
        .putInt(KEY_KEEP, count.coerceIn(AutoBackupRotation.MIN_KEEP, AutoBackupRotation.MAX_KEEP)).apply()

    /** A new folder clears an old "folder access was removed" error. */
    fun setFolder(uri: String?) = prefs.edit().putString(KEY_FOLDER, uri).remove(KEY_LAST_ERROR).apply()

    /** Turns encryption on with [password], or off (and forgets the password) when null. */
    fun setEncryption(password: CharArray?) {
        if (password == null) {
            secrets.clear()
            prefs.edit().putBoolean(KEY_ENCRYPT, false).apply()
        } else {
            secrets.save(password)
            prefs.edit().putBoolean(KEY_ENCRYPT, true).apply()
        }
    }

    fun loadPassword(): CharArray? = secrets.load()

    fun recordSuccess(at: Long, fileName: String) = prefs.edit()
        .putLong(KEY_LAST_SUCCESS, at)
        .putString(KEY_LAST_FILE, fileName)
        .remove(KEY_LAST_ERROR)
        .remove(KEY_LAST_ERROR_AT)
        .apply()

    fun recordError(at: Long, message: String) = prefs.edit()
        .putString(KEY_LAST_ERROR, message)
        .putLong(KEY_LAST_ERROR_AT, at)
        .apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

    companion object {
        const val PREFS = "auto_backup"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_FREQUENCY = "frequency"
        private const val KEY_FOLDER = "folder_uri"
        private const val KEY_KEEP = "keep_count"
        private const val KEY_ENCRYPT = "encrypt"
        private const val KEY_LAST_SUCCESS = "last_success_at"
        private const val KEY_LAST_FILE = "last_file_name"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_LAST_ERROR_AT = "last_error_at"
    }
}

/** Holds the backup password so scheduled runs can encrypt without asking. */
interface BackupPasswordStore {
    fun save(password: CharArray)
    fun load(): CharArray?
    fun hasPassword(): Boolean
    fun clear()
}

/**
 * Stores the backup password sealed with a non-exportable AES key in the Android Keystore, so
 * the preferences file never holds it in plain text. If the Keystore key is lost (for example
 * after a device restore), [load] returns null and the next run reports that the password must
 * be set again.
 */
class KeystoreBackupPasswordStore(private val prefs: SharedPreferences) : BackupPasswordStore {

    override fun save(password: CharArray) {
        val bytes = Charsets.UTF_8.encode(java.nio.CharBuffer.wrap(password)).let { buffer ->
            ByteArray(buffer.remaining()).also { buffer.get(it) }
        }
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val sealed = cipher.doFinal(bytes)
            prefs.edit()
                .putString(KEY_SEALED, Base64.encodeToString(sealed, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .apply()
        } finally {
            bytes.fill(0)
        }
    }

    override fun load(): CharArray? {
        val sealed = prefs.getString(KEY_SEALED, null) ?: return null
        val iv = prefs.getString(KEY_IV, null) ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, existingKey() ?: return null, GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            val bytes = cipher.doFinal(Base64.decode(sealed, Base64.NO_WRAP))
            val chars = Charsets.UTF_8.decode(java.nio.ByteBuffer.wrap(bytes))
            bytes.fill(0)
            CharArray(chars.remaining()).also { chars.get(it) }
        } catch (e: Exception) {
            null
        }
    }

    override fun hasPassword(): Boolean = prefs.contains(KEY_SEALED)

    override fun clear() {
        prefs.edit().remove(KEY_SEALED).remove(KEY_IV).apply()
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(ALIAS)
        } catch (e: Exception) {
            // Nothing to delete.
        }
    }

    private fun existingKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    private fun key(): SecretKey = existingKey() ?: KeyGenerator
        .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        .apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }
        .generateKey()

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "akspend_auto_backup_password"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SEALED = "password_sealed"
        const val KEY_IV = "password_iv"
    }
}
