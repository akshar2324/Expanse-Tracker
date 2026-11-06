package com.akshar.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * The app lock's passcode, attempt limit and on/off state, kept in the `vault_settings` prefs.
 *
 * - There is no default passcode. The lock is only active once the user has set their own
 *   passcode, so a fresh install is never "protected" by a code anyone could guess.
 * - The passcode is stored as a salted PBKDF2 hash, never as text. A plain-text code saved by an
 *   older version is converted on first read and the text copy is deleted.
 * - After [FREE_ATTEMPTS] wrong tries, each further wrong try locks the keypad for a growing time,
 *   up to [MAX_LOCKOUT_MILLIS]. The count survives restarts, so closing the app doesn't reset it.
 *
 * [now] is the wall clock in milliseconds. A lockout that ends more than [MAX_LOCKOUT_MILLIS] in
 * the future, which only happens if the clock was moved back, is capped so it can't lock forever.
 */
class AppLock(private val prefs: SharedPreferences, private val now: () -> Long = System::currentTimeMillis) {

    sealed interface Attempt {
        data object Unlocked : Attempt
        /** Wrong passcode. [triesBeforeWait] is 0 when the next wrong try starts a wait. */
        data class Wrong(val triesBeforeWait: Int) : Attempt
        /** Too many wrong tries: the keypad is locked until [untilMillis]. */
        data class Waiting(val untilMillis: Long) : Attempt
    }

    init {
        migrateLegacyPin()
    }

    /** True when the user has set a passcode. */
    val hasPin: Boolean get() = prefs.getString(KEY_HASH, null) != null

    /** True when opening the app should ask for the passcode. */
    val isActive: Boolean get() = hasPin && prefs.getBoolean(KEY_ENABLED, true)

    val biometricsEnabled: Boolean get() = isActive && prefs.getBoolean(KEY_BIOMETRICS, true)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun setBiometrics(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRICS, enabled).apply()
    }

    /** Saves a new passcode and turns the lock on. Throws for anything but [PIN_LENGTH] digits. */
    fun setPin(pin: String) {
        require(isValidPin(pin)) { "passcode must be $PIN_LENGTH digits" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, encode(salt))
            .putString(KEY_HASH, encode(hash(pin, salt)))
            .putBoolean(KEY_ENABLED, true)
            .putInt(KEY_FAILED, 0)
            .remove(KEY_WAIT_UNTIL)
            .remove(KEY_LEGACY_PIN)
            .commit()
    }

    /** When the keypad is locked, the time it opens again; otherwise null. */
    fun waitingUntil(): Long? {
        val until = prefs.getLong(KEY_WAIT_UNTIL, 0L)
        val current = now()
        if (until <= current) return null
        return minOf(until, current + MAX_LOCKOUT_MILLIS)
    }

    fun tryUnlock(pin: String): Attempt {
        waitingUntil()?.let { return Attempt.Waiting(it) }
        if (!hasPin) return Attempt.Unlocked
        val salt = decode(prefs.getString(KEY_SALT, null))
        val stored = decode(prefs.getString(KEY_HASH, null))
        if (salt != null && stored != null && isValidPin(pin) && MessageDigest.isEqual(stored, hash(pin, salt))) {
            prefs.edit().putInt(KEY_FAILED, 0).remove(KEY_WAIT_UNTIL).apply()
            return Attempt.Unlocked
        }
        val failed = prefs.getInt(KEY_FAILED, 0) + 1
        val wait = lockoutFor(failed)
        prefs.edit().putInt(KEY_FAILED, failed).apply {
            if (wait > 0) putLong(KEY_WAIT_UNTIL, now() + wait)
        }.apply()
        return if (wait > 0) Attempt.Waiting(now() + wait) else Attempt.Wrong(FREE_ATTEMPTS - failed)
    }

    private fun migrateLegacyPin() {
        val legacy = prefs.getString(KEY_LEGACY_PIN, null) ?: return
        if (!hasPin && isValidPin(legacy)) {
            val enabled = prefs.getBoolean(KEY_ENABLED, true)
            setPin(legacy)
            setEnabled(enabled)
        } else {
            prefs.edit().remove(KEY_LEGACY_PIN).apply()
        }
    }

    companion object {
        const val PREFS = "vault_settings"
        const val KEY_ENABLED = "vault_enabled"
        const val KEY_BIOMETRICS = "vault_biometrics"
        const val KEY_HASH = "vault_pin_hash"
        private const val KEY_SALT = "vault_pin_salt"
        private const val KEY_FAILED = "vault_failed_attempts"
        private const val KEY_WAIT_UNTIL = "vault_wait_until"
        private const val KEY_LEGACY_PIN = "vault_pin"

        const val PIN_LENGTH = 4
        const val FREE_ATTEMPTS = 5
        const val MAX_LOCKOUT_MILLIS = 15 * 60_000L

        /** How long the app may stay in the background before it asks for the passcode again. */
        const val RELOCK_AFTER_MILLIS = 30_000L

        // SHA-1 is the PBKDF2 variant available on every supported Android version (minSdk 24).
        private const val ALGORITHM = "PBKDF2WithHmacSHA1"
        private const val ITERATIONS = 40_000

        fun from(context: Context) = AppLock(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

        fun isValidPin(pin: String) = pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

        /** No wait for the first [FREE_ATTEMPTS] wrong tries, then 30 s, doubling, up to 15 min. */
        fun lockoutFor(failedAttempts: Int): Long {
            if (failedAttempts < FREE_ATTEMPTS) return 0L
            val doublings = (failedAttempts - FREE_ATTEMPTS).coerceAtMost(5)
            return minOf(30_000L shl doublings, MAX_LOCKOUT_MILLIS)
        }

        private fun hash(pin: String, salt: ByteArray): ByteArray {
            val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
            return try {
                SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        }

        private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

        private fun decode(text: String?): ByteArray? =
            text?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
    }
}
