package com.akshar.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.data.model.Budget
import com.akshar.data.model.Transaction
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BackupCryptoTest {

    // Real files use DEFAULT_ITERATIONS; tests use the minimum to stay fast.
    private val iterations = 1_000

    private val plain = BackupCodec.encode(
        BackupSnapshot(
            transactions = listOf(
                Transaction(id = 7, amount = 12.34, type = "EXPENSE", category = "Food", description = "Chai ☕", date = 1L, paymentMethod = "UPI")
            ),
            budgets = listOf(Budget(id = 1, category = "Food", limitAmount = 500.0, month = "2026-11"))
        )
    )

    @Test
    fun `encrypt then decrypt returns the original backup`() {
        val sealed = BackupCrypto.encrypt(plain, "correct horse".toCharArray(), iterations)
        assertTrue(BackupCrypto.isEncrypted(sealed))
        assertFalse(sealed.contains("Chai"))
        val opened = BackupCrypto.decrypt(sealed, "correct horse".toCharArray())
        assertEquals(plain, opened)
        assertEquals(12.34, BackupCodec.decode(opened).transactions.single().amount, 0.0)
    }

    @Test(expected = WrongBackupPasswordException::class)
    fun `wrong password fails`() {
        val sealed = BackupCrypto.encrypt(plain, "correct horse".toCharArray(), iterations)
        BackupCrypto.decrypt(sealed, "battery staple".toCharArray())
    }

    @Test(expected = WrongBackupPasswordException::class)
    fun `tampered ciphertext fails`() {
        val sealed = JSONObject(BackupCrypto.encrypt(plain, "pw123456".toCharArray(), iterations))
        val bytes = android.util.Base64.decode(sealed.getString("ciphertext"), android.util.Base64.NO_WRAP)
        bytes[0] = (bytes[0].toInt() xor 1).toByte()
        sealed.put("ciphertext", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
        BackupCrypto.decrypt(sealed.toString(), "pw123456".toCharArray())
    }

    @Test(expected = WrongBackupPasswordException::class)
    fun `changing the iteration count in the header fails`() {
        val sealed = JSONObject(BackupCrypto.encrypt(plain, "pw123456".toCharArray(), iterations))
        sealed.put("iterations", iterations + 1)
        BackupCrypto.decrypt(sealed.toString(), "pw123456".toCharArray())
    }

    @Test(expected = JSONException::class)
    fun `absurd iteration count is rejected before deriving`() {
        val sealed = JSONObject(BackupCrypto.encrypt(plain, "pw123456".toCharArray(), iterations))
        sealed.put("iterations", Int.MAX_VALUE)
        BackupCrypto.decrypt(sealed.toString(), "pw123456".toCharArray())
    }

    @Test
    fun `every encryption uses a fresh salt and iv`() {
        val a = JSONObject(BackupCrypto.encrypt(plain, "pw123456".toCharArray(), iterations))
        val b = JSONObject(BackupCrypto.encrypt(plain, "pw123456".toCharArray(), iterations))
        assertNotEquals(a.getString("salt"), b.getString("salt"))
        assertNotEquals(a.getString("iv"), b.getString("iv"))
        assertNotEquals(a.getString("ciphertext"), b.getString("ciphertext"))
    }

    @Test
    fun `key derivation matches the JDK PBKDF2 implementation`() {
        val salt = "NaCl-salt-16byte".toByteArray()
        val password = "pässwörd".toCharArray()
        val expected = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password, salt, 2_000, 256)).encoded
        assertArrayEquals(expected, BackupCrypto.deriveKey(password, salt, 2_000))
        // Longer than one HMAC block, to cover the block counter.
        val expectedLong = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password, salt, 3, 512 + 128)).encoded
        assertArrayEquals(expectedLong, BackupCrypto.deriveKey(password, salt, 3, keyBytes = 80))
    }

    @Test
    fun `plain backups pass through and encrypted ones need a password`() {
        assertFalse(BackupCrypto.isEncrypted(plain))
        assertEquals(plain, BackupFiles.openBackup(plain, null))
        val sealed = BackupCrypto.encrypt(plain, "pw123456".toCharArray(), iterations)
        try {
            BackupFiles.openBackup(sealed, null)
            throw AssertionError("expected a password prompt")
        } catch (expected: BackupPasswordRequiredException) {
        }
        assertEquals(plain, BackupFiles.openBackup(sealed, "pw123456".toCharArray()))
    }

    @Test
    fun `non json text is not treated as encrypted`() {
        assertFalse(BackupCrypto.isEncrypted("SQLite format 3"))
        assertFalse(BackupCrypto.isEncrypted("{broken"))
    }
}
