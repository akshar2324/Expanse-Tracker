package com.akshar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.akshar.ExpenseTrackerApp
import com.akshar.data.model.Transaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Pending undo offers survive rotation and are presented in deletion order. */
class ActivityActionsViewModel(
    private val delete: suspend (Long) -> List<Transaction>,
    private val restore: suspend (List<Transaction>) -> Unit,
) : ViewModel() {
    data class Notice(val id: Long, val message: String, val rows: List<Transaction> = emptyList())
    private val mutex = Mutex()
    private var nextId = 0L
    private val _notices = MutableStateFlow<List<Notice>>(emptyList())
    val notices = _notices.asStateFlow()

    fun delete(id: Long) {
        viewModelScope.launch {
            mutex.withLock {
                try {
                    val rows = delete.invoke(id)
                    if (rows.isNotEmpty()) enqueue("Transaction deleted", rows)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    enqueue("Couldn't delete transaction")
                }
            }
        }
    }

    fun resolve(id: Long, undo: Boolean) {
        viewModelScope.launch {
            mutex.withLock {
                val notice = _notices.value.firstOrNull { it.id == id } ?: return@withLock
                try {
                    if (undo && notice.rows.isNotEmpty()) restore(notice.rows)
                    _notices.value = _notices.value.filterNot { it.id == id }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _notices.value = _notices.value.filterNot { it.id == id }
                    enqueue("Couldn't restore transaction. Try Undo again.", notice.rows)
                }
            }
        }
    }

    private fun enqueue(message: String, rows: List<Transaction> = emptyList()) {
        _notices.value += Notice(++nextId, message, rows)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseTrackerApp
                ActivityActionsViewModel(app.repository::deleteActivityTransaction, app.repository::restoreActivityTransactions)
            }
        }
    }
}
