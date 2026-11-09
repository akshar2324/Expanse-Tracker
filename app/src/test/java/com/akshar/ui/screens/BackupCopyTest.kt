package com.akshar.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupCopyTest {
    private val now = 1_800_000_000_000L

    @Test fun underAMinuteIsJustNow() {
        assertEquals("Just now", relativeBackupTime(now, now))
        assertEquals("Just now", relativeBackupTime(now - 59_000, now))
    }

    @Test fun olderBackupsUseMinutes() {
        assertEquals("10 minutes ago", relativeBackupTime(now - 10 * 60_000, now))
    }

    @Test fun restoreConfirmationUsesCorrectPlurals() {
        val one = restoreConfirmation(transactionCount = 1, accountCount = 3)
        assertTrue(one, one.startsWith("This backup has 1 transaction and 3 accounts."))
        val none = restoreConfirmation(transactionCount = 0, accountCount = 0)
        assertTrue(none, none.startsWith("This backup has 0 transactions."))
        assertTrue(one.contains("can't be undone"))
    }
}
