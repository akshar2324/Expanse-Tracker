package com.akshar.data.backup

import android.util.Base64
import org.json.JSONException
import org.json.JSONObject
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** The password does not open this backup (or the file was changed after it was written). */
class WrongBackupPasswordException : GeneralSecurityException("Wrong password, or the backup file is damaged.")

/**
 * Optional password protection for JSON backups.
 *
 * An encrypted backup is itself a small JSON document, so it still opens as text:
 * ```
 * {"format":"akspend-encrypted-backup","version":1,"kdf":"PBKDF2-HMAC-SHA256",
 *  "iterations":310000,"salt":"…","iv":"…","ciphertext":"…"}
 * ```
 * The key is derived from the password with PBKDF2-HMAC-SHA256 and a random 16-byte salt, and
 * the backup JSON is sealed with AES-256-GCM under a random 12-byte IV. The header fields are
 * bound in as associated data, so changing the iteration count or salt also fails to decrypt.
 *
 * PBKDF2 is computed here on top of `HmacSHA256` rather than through `SecretKeyFactory`, because
 * `PBKDF2WithHmacSHA256` only exists on Android 8.0+ and the app supports 7.0. Every device
 * therefore derives the same key, and a backup made on one phone opens on another.
 */
object BackupCrypto {

    const val FORMAT = "akspend-encrypted-backup"
    private const val VERSION = 1
    private const val KDF = "PBKDF2-HMAC-SHA256"

    /** Stored in each file so the work factor can be raised for future backups. */
    const val DEFAULT_ITERATIONS = 310_000
    /** Bounds for files we read, so a crafted header cannot make decryption hang. */
    private const val MIN_ITERATIONS = 1_000
    private const val MAX_ITERATIONS = 5_000_000

    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BYTES = 32
    private const val TAG_BITS = 128

    private val random = SecureRandom()

    /** True when [text] looks like an encrypted backup rather than a plain one. */
    fun isEncrypted(text: String): Boolean {
        val trimmed = text.trimStart()
        if (!trimmed.startsWith("{")) return false
        return try {
            JSONObject(trimmed).optString("format") == FORMAT
        } catch (e: JSONException) {
            false
        }
    }

    fun encrypt(plainJson: String, password: CharArray, iterations: Int = DEFAULT_ITERATIONS): String {
        require(password.isNotEmpty()) { "Password must not be empty" }
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Unsupported iteration count" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val key = deriveKey(password, salt, iterations)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(associatedData(iterations, salt))
            val sealed = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
            return JSONObject().apply {
                put("format", FORMAT)
                put("version", VERSION)
                put("kdf", KDF)
                put("iterations", iterations)
                put("salt", b64(salt))
                put("iv", b64(iv))
                put("ciphertext", b64(sealed))
            }.toString()
        } finally {
            key.fill(0)
        }
    }

    /**
     * Returns the plain backup JSON.
     * @throws WrongBackupPasswordException if the password is wrong or the file was altered.
     * @throws JSONException if [encrypted] is not an encrypted backup this version understands.
     */
    fun decrypt(encrypted: String, password: CharArray): String {
        val root = JSONObject(encrypted.trim())
        if (root.optString("format") != FORMAT) throw JSONException("Not an encrypted AkSpend backup")
        if (root.optInt("version", -1) != VERSION || root.optString("kdf") != KDF) {
            throw JSONException("This backup was made by a newer version of the app")
        }
        val iterations = root.getInt("iterations")
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) throw JSONException("Unsupported iteration count")
        val salt = unb64(root.getString("salt"))
        val iv = unb64(root.getString("iv"))
        val sealed = unb64(root.getString("ciphertext"))
        if (salt.size != SALT_BYTES || iv.size != IV_BYTES || sealed.size < TAG_BITS / 8) {
            throw JSONException("Encrypted backup header is damaged")
        }
        val key = deriveKey(password, salt, iterations)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(associatedData(iterations, salt))
            return String(cipher.doFinal(sealed), Charsets.UTF_8)
        } catch (e: AEADBadTagException) {
            throw WrongBackupPasswordException()
        } finally {
            key.fill(0)
        }
    }

    /** PBKDF2 (RFC 8018) with HMAC-SHA256, producing a 256-bit key. */
    internal fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int, keyBytes: Int = KEY_BYTES): ByteArray {
        val passwordBytes = Charsets.UTF_8.encode(java.nio.CharBuffer.wrap(password)).let { buffer ->
            ByteArray(buffer.remaining()).also { buffer.get(it) }
        }
        try {
            val mac = Mac.getInstance("HmacSHA256")
            // HMAC keys may not be empty in some providers; the encrypt path already rejects
            // empty passwords, so a single zero byte only matters for decrypt attempts.
            mac.init(SecretKeySpec(if (passwordBytes.isEmpty()) ByteArray(1) else passwordBytes, "HmacSHA256"))
            val hLen = mac.macLength
            val blocks = (keyBytes + hLen - 1) / hLen
            val output = ByteArray(keyBytes)
            val u = ByteArray(hLen)
            val t = ByteArray(hLen)
            for (block in 1..blocks) {
                mac.update(salt)
                mac.update(byteArrayOf((block ushr 24).toByte(), (block ushr 16).toByte(), (block ushr 8).toByte(), block.toByte()))
                mac.doFinal(u, 0)
                u.copyInto(t)
                repeat(iterations - 1) {
                    mac.update(u)
                    mac.doFinal(u, 0)
                    for (i in 0 until hLen) t[i] = (t[i].toInt() xor u[i].toInt()).toByte()
                }
                val offset = (block - 1) * hLen
                t.copyInto(output, offset, 0, minOf(hLen, keyBytes - offset))
            }
            u.fill(0)
            t.fill(0)
            return output
        } finally {
            passwordBytes.fill(0)
        }
    }

    private fun associatedData(iterations: Int, salt: ByteArray): ByteArray =
        "$FORMAT|$VERSION|$KDF|$iterations|${b64(salt)}".toByteArray(Charsets.UTF_8)

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun unb64(text: String): ByteArray = try {
        Base64.decode(text, Base64.NO_WRAP)
    } catch (e: IllegalArgumentException) {
        throw JSONException("Encrypted backup is damaged")
    }
}
