package com.akshar.ui.viewmodel

import com.akshar.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityActionsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val entry = Transaction(41, 12.34, "EXPENSE", "Food", "Lunch", 123L, "Cash", timestamp = 456L)
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun teardown() = Dispatchers.resetMain()

    @Test fun `queued undo offers restore their own exact snapshots once`() = runTest(dispatcher) {
        val restored = mutableListOf<List<Transaction>>()
        val vm = ActivityActionsViewModel({ listOf(entry.copy(id = it)) }, { restored += it })
        vm.delete(41)
        vm.delete(42)
        advanceUntilIdle()
        val offers = vm.notices.value
        assertEquals(listOf(41L, 42L), offers.map { it.rows.single().id })
        vm.resolve(offers[0].id, true)
        vm.resolve(offers[0].id, true)
        vm.resolve(offers[1].id, false)
        advanceUntilIdle()
        assertEquals(listOf(listOf(entry)), restored)
        assertTrue(vm.notices.value.isEmpty())
    }

    @Test fun `duplicate delete does not offer undo and failures are visible`() = runTest(dispatcher) {
        val vm = ActivityActionsViewModel({ if (it == 1L) emptyList() else error("database failure") }, {})
        vm.delete(1)
        vm.delete(2)
        advanceUntilIdle()
        assertEquals("Couldn't delete transaction", vm.notices.value.single().message)
        assertTrue(vm.notices.value.single().rows.isEmpty())
    }

    @Test fun `failed undo keeps the exact snapshot available for retry`() = runTest(dispatcher) {
        var attempts = 0
        val restored = mutableListOf<List<Transaction>>()
        val vm = ActivityActionsViewModel({ listOf(entry) }, {
            if (++attempts == 1) error("database failure")
            restored += it
        })
        vm.delete(41)
        advanceUntilIdle()
        vm.resolve(vm.notices.value.single().id, true)
        advanceUntilIdle()
        val retry = vm.notices.value.single()
        assertEquals(listOf(entry), retry.rows)
        vm.resolve(retry.id, true)
        advanceUntilIdle()
        assertEquals(listOf(listOf(entry)), restored)
        assertTrue(vm.notices.value.isEmpty())
    }
}
