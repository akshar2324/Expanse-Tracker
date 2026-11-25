package com.akshar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akshar.alerts.BudgetAlertStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

class BudgetAlertsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = BudgetAlertStore(application)
    val enabled = store.enabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setEnabled(value: Boolean) {
        viewModelScope.launch {
            BudgetAlertStore.runLock.withLock { store.setEnabled(value) }
        }
    }
}
