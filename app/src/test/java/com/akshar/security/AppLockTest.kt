package com.akshar.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLockTest {
    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences(AppLock.PREFS, Context.MODE_PRIVATE)
    private var clock = 1_000_000L
    private fun lock() = AppLock(prefs) { clock }

    @Before fun clear() { prefs.edit().clear().commit() }

    @Test fun freshInstall_hasNoPasscodeAndNoLock() {
        val lock = lock()
        assertFalse(lock.hasPin)
        assertFalse("no default passcode can open the app", lock.isActive)
        assertFalse(lock.biometricsEnabled)
    }

    @Test fun passcodeIsStoredHashed_andUnlocks() {
        lock().setPin("2468")
        val all = prefs.all.values.joinToString()
        assertFalse("passcode must not be stored as text", all.contains("2468"))
        val lock = lock()
        assertTrue(lock.isActive)
        assertEquals(AppLock.Attempt.Unlocked, lock.tryUnlock("2468"))
        assertEquals(AppLock.Attempt.Wrong(4), lock.tryUnlock("1234"))
    }

    @Test fun legacyTextPasscode_isConvertedAndKeepsWorking() {
        prefs.edit().putString("vault_pin", "1357").putBoolean(AppLock.KEY_ENABLED, false).commit()
        val lock = lock()
        assertNull(prefs.getString("vault_pin", null))
        assertTrue(lock.hasPin)
        assertFalse("keeps the user's on/off choice", lock.isActive)
        assertEquals(AppLock.Attempt.Unlocked, lock.tryUnlock("1357"))
    }

    @Test fun wrongTries_startAGrowingWait_thatSurvivesARestart() {
        lock().setPin("2468")
        var lock = lock()
        repeat(4) { assertTrue(lock.tryUnlock("0000") is AppLock.Attempt.Wrong) }
        assertEquals(AppLock.Attempt.Waiting(clock + 30_000), lock.tryUnlock("0000"))

        lock = lock()
        assertEquals("even the right code waits", AppLock.Attempt.Waiting(clock + 30_000), lock.tryUnlock("2468"))
        clock += 30_000
        assertEquals(AppLock.Attempt.Waiting(clock + 60_000), lock.tryUnlock("0000"))
        clock += 60_000
        assertEquals(AppLock.Attempt.Unlocked, lock.tryUnlock("2468"))
        assertTrue("a success resets the count", lock.tryUnlock("0000") is AppLock.Attempt.Wrong)
    }

    @Test fun lockoutIsCapped_evenIfTheClockMovesBack() {
        assertEquals(0L, AppLock.lockoutFor(4))
        assertEquals(30_000L, AppLock.lockoutFor(5))
        assertEquals(AppLock.MAX_LOCKOUT_MILLIS, AppLock.lockoutFor(50))
        lock().setPin("2468")
        repeat(5) { lock().tryUnlock("0000") }
        clock -= 365L * 24 * 60 * 60 * 1000
        assertEquals(clock + AppLock.MAX_LOCKOUT_MILLIS, lock().waitingUntil())
    }

    @Test fun onlyFourDigitPasscodes() {
        assertFalse(AppLock.isValidPin("123"))
        assertFalse(AppLock.isValidPin("12a4"))
        assertFalse(AppLock.isValidPin("１２３４"))
        assertTrue(AppLock.isValidPin("0000"))
    }
}
