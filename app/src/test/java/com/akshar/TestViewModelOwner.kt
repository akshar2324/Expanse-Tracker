package com.akshar

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.akshar.ui.viewmodel.FinanceViewModel

/**
 * Owns the ViewModels a test creates, so `@After` can stop their database flows before it closes
 * the database. Without this a flow still sharing on a background thread queries the closed
 * connection pool and fails whichever test happens to be running.
 */
class TestViewModelOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    private var created = 0

    fun finance(app: Application): FinanceViewModel =
        // A fresh factory: getInstance() caches the first test's Application for the whole JVM.
        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(app))
            .get("finance-${created++}", FinanceViewModel::class.java)

    fun clear() = viewModelStore.clear()
}
