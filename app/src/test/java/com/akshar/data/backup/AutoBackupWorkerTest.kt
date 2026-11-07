package com.akshar.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AutoBackupWorkerTest {
    private lateinit var context: Context
    private lateinit var store: AutoBackupStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(AutoBackupStore.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        store = AutoBackupStore(context)
    }

    @After
    fun tearDown() = unmockkAll()

    private fun worker(manual: Boolean = false, attempt: Int = 0): AutoBackupWorker {
        val params = mockk<WorkerParameters>(relaxed = true)
        every { params.inputData } returns Data.Builder().putBoolean(AutoBackupWorker.KEY_MANUAL, manual).build()
        every { params.runAttemptCount } returns attempt
        return AutoBackupWorker(context, params)
    }

    @Test
    fun `opt out skips scheduled work without an error`() = runTest {
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertNull(store.read().lastError)
    }

    @Test
    fun `manual work reports a missing folder even while scheduling is off`() = runTest {
        assertEquals(ListenableWorker.Result.failure(), worker(manual = true).doWork())
        assertEquals(AutoBackupRunner.MSG_NO_FOLDER, store.read().lastError)
        assertEquals(0L, store.read().lastSuccessAt)
    }

    @Test
    fun `revoked folder permission records an actionable permanent failure`() = runTest {
        store.setEnabled(true)
        store.setFolder("content://com.android.externalstorage.documents/tree/primary%3ABackups")
        assertEquals(ListenableWorker.Result.failure(), worker().doWork())
        assertEquals(AutoBackupRunner.MSG_FOLDER_LOST, store.read().lastError)
    }
    @Test
    fun `transient failure retries only up to the limit and records its message`() = runTest {
        store.setEnabled(true)
        mockkObject(AutoBackupRunner)
        coEvery { AutoBackupRunner.run(any(), any(), any(), any(), any()) } returns
            AutoBackupOutcome.Failed("Storage unavailable", retryable = true)
        assertEquals(ListenableWorker.Result.retry(), worker(attempt = 0).doWork())
        assertEquals(ListenableWorker.Result.failure(), worker(attempt = 2).doWork())
        assertEquals("Storage unavailable", store.read().lastError)
    }

    @Test
    fun `successful backup records its time and clears the previous error`() = runTest {
        store.setEnabled(true)
        store.recordError(1, "Earlier error")
        mockkObject(AutoBackupRunner)
        coEvery { AutoBackupRunner.run(any(), any(), any(), any(), any()) } returns
            AutoBackupOutcome.Success("akspend-auto-test.json", emptyList())
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertTrue(store.read().lastSuccessAt > 0)
        assertEquals("akspend-auto-test.json", store.read().lastFileName)
        assertNull(store.read().lastError)
    }

    @Test
    fun `retention warning is retained alongside the successful backup time`() = runTest {
        store.setEnabled(true)
        mockkObject(AutoBackupRunner)
        coEvery { AutoBackupRunner.run(any(), any(), any(), any(), any()) } returns
            AutoBackupOutcome.Success("akspend-auto-test.json", emptyList(), AutoBackupRunner.MSG_ROTATION_FAILED)
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertTrue(store.read().lastSuccessAt > 0)
        assertEquals(AutoBackupRunner.MSG_ROTATION_FAILED, store.read().lastError)
    }

}
