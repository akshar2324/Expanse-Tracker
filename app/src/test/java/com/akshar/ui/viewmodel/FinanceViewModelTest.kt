package com.akshar.ui.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.ExpenseTrackerApp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = ExpenseTrackerApp::class)
class FinanceViewModelTest {

    private lateinit var viewModel: FinanceViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = FinanceViewModel(app)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun restoreDataFromJson_invalidJson_returnsFalseAndSetsErrorStatus() = runTest {
        // Arrange
        val invalidJson = "this is not valid json"

        // Act
        val result = viewModel.restoreDataFromJson(invalidJson)

        // Assert
        assertFalse(result)
        val status = viewModel.backupStatus.first()
        assertEquals("Failed to restore backup: invalid JSON format.", status)
    }
}
