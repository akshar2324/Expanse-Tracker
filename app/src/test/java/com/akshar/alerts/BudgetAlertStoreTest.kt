@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.akshar.alerts

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BudgetAlertStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun `opt in and claims survive file reopening and concurrent delivery attempts`() = runTest {
        val file = File(temporary.root, "alerts.preferences_pb")
        val diskScope = CoroutineScope(SupervisorJob(backgroundScope.coroutineContext[Job]) + StandardTestDispatcher(testScheduler))
        val data = PreferenceDataStoreFactory.create(scope = diskScope, produceFile = { file })
        val store = BudgetAlertStore(data)
        val november = BudgetAlert("2026-11", "food", 80).key
        val december = BudgetAlert("2026-12", "food", 80).key
        assertFalse(store.enabled.first())
        assertFalse(store.claim(november))
        assertTrue(store.sent().isEmpty())
        store.setEnabled(true)
        val claims = List(10) { async { BudgetAlertStore(data).claim(november) } }.awaitAll()
        assertEquals(1, claims.count { it })
        diskScope.coroutineContext[Job]!!.cancelAndJoin()
        val reopenedData = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        val recreated = BudgetAlertStore(reopenedData)
        assertTrue(recreated.enabled.first())
        assertEquals(setOf(november), recreated.sent())
        assertFalse(recreated.claim(november))
        recreated.setEnabled(false)
        assertFalse(recreated.claim(december))
        recreated.setEnabled(true)
        assertFalse(recreated.claim(november))
        assertTrue(recreated.claim(december))
        assertTrue(recreated.claim(BudgetAlert("2026-12", "food", 100).key))
    }
}
