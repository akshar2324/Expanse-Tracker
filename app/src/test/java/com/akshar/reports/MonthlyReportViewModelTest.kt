@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.akshar.reports

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.ui.viewmodel.MonthlyReportViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [32], application = Application::class)
class MonthlyReportViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `build preserves selected month and creates no PDF before sharing`() = runTest(dispatcher) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val directory = File(application.cacheDir, "monthly_reports")
        val before = directory.listFiles()?.toList().orEmpty()
        val saved = SavedStateHandle(mapOf("reportMonth" to "2026-11"))
        val gate = CompletableDeferred<Unit>()
        var loads = 0
        val vm = MonthlyReportViewModel(application, saved, dispatcher) { month ->
            loads++
            gate.await()
            MonthlyReports.build(month, emptyList(), emptyList())
        }
        var shared = false
        vm.share { shared = true }
        vm.build()
        runCurrent()
        assertTrue(vm.state.value.busy)
        vm.build()
        vm.selectMonth("2026-10")
        assertEquals("2026-11", vm.month.value)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, loads)
        assertFalse(shared)
        assertEquals("2026-11", vm.state.value.report!!.month)
        assertEquals(before, directory.listFiles()?.toList().orEmpty())
        vm.selectMonth("2026-10")
        assertNull(vm.state.value.report)
        assertEquals("2026-10", saved.get<String>("reportMonth"))
    }

    @Test fun `build failure shows recoverable message and permits retry`() = runTest(dispatcher) {
        var fail = true
        val vm = MonthlyReportViewModel(ApplicationProvider.getApplicationContext(), SavedStateHandle(mapOf("reportMonth" to "2026-11")), dispatcher) { month ->
            if (fail) error("private database detail")
            MonthlyReports.build(month, emptyList(), emptyList())
        }
        vm.build()
        advanceUntilIdle()
        assertFalse(vm.state.value.busy)
        assertNull(vm.state.value.report)
        assertFalse(vm.state.value.error!!.contains("private database detail"))
        fail = false
        vm.build()
        advanceUntilIdle()
        assertNotNull(vm.state.value.report)
        assertNull(vm.state.value.error)
    }
}
