package com.akshar.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.data.backup.AutoBackupStore
import com.akshar.data.backup.BackupCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AutoBackupViewModelTest {
    private lateinit var owner: ViewModelStore
    private lateinit var subject: AutoBackupViewModel
    private lateinit var file: File

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences(AutoBackupStore.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        owner = ViewModelStore()
        subject = ViewModelProvider(owner, ViewModelProvider.AndroidViewModelFactory(app))[AutoBackupViewModel::class.java]
        file = File.createTempFile("restore-test-", ".json", app.cacheDir)
    }

    @After
    fun tearDown() {
        owner.clear()
        file.delete()
        Dispatchers.resetMain()
    }

    private fun open(text: String) {
        file.writeText(text)
        subject.openRestoreFile(Uri.fromFile(file))
    }

    @Test
    fun `plain backup is validated before offering replacement`() = runTest {
        open("""{"transactions":[]}""")
        val ready = subject.restoreState.first { it is JsonRestoreState.Ready } as JsonRestoreState.Ready
        assertEquals(0, ready.transactionCount)
        assertFalse(subject.settings.value.enabled)
    }

    @Test
    fun `encrypted backup prompts and allows retry after a wrong password`() = runTest {
        open(BackupCrypto.encrypt("""{"transactions":[]}""", "correct password".toCharArray(), 1_000))
        subject.restoreState.first { it is JsonRestoreState.NeedsPassword }
        val wrong = "wrong password".toCharArray()
        subject.submitRestorePassword(wrong)
        val retry = subject.restoreState.first { it is JsonRestoreState.NeedsPassword } as JsonRestoreState.NeedsPassword
        assertTrue(retry.wrongPassword)
        assertTrue(wrong.all { it == '\u0000' })
        subject.submitRestorePassword("correct password".toCharArray())
        subject.restoreState.first { it is JsonRestoreState.Ready }
    }

    @Test
    fun `malformed rows are rejected before offering replacement`() = runTest {
        open("""{"transactions":[{"type":"EXPENSE"}]}""")
        subject.restoreState.first { it is JsonRestoreState.Error }
    }

    @Test
    fun `dismissing restore cancels pending file processing`() = runTest {
        open("""{"transactions":[]}""")
        subject.clearRestore()
        assertEquals(JsonRestoreState.Idle, subject.restoreState.value)
    }
}
