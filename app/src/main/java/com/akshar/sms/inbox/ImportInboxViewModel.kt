package com.akshar.sms.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ImportInboxViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ImportInboxDatabase.getInstance(application).pendingImportDao()

    val pending: StateFlow<List<PendingImport>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun dismiss(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }

    fun dismissAll() {
        viewModelScope.launch { dao.clear() }
    }
}
