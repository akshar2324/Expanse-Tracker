package com.akshar.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Asks before deleting. Deletes in AkSpend are permanent (no trash, no undo), so a single
 * mis-tap on a bin icon must never remove a budget, goal, debt or transaction.
 *
 * ```
 * val confirmDelete = rememberConfirmDeleteState()
 * ConfirmDeleteDialog(confirmDelete)
 * IconButton(onClick = { confirmDelete.request("Delete Food budget?") { viewModel.delete(id) } })
 * ```
 */
@Stable
class ConfirmDeleteState {
    internal class Request(val title: String, val message: String, val onConfirm: () -> Unit)

    internal var pending by mutableStateOf<Request?>(null)
        private set

    fun request(title: String, message: String = "This can't be undone.", onConfirm: () -> Unit) {
        pending = Request(title, message, onConfirm)
    }

    internal fun dismiss() {
        pending = null
    }
}

@Composable
fun rememberConfirmDeleteState(): ConfirmDeleteState = remember { ConfirmDeleteState() }

@Composable
fun ConfirmDeleteDialog(state: ConfirmDeleteState) {
    val request = state.pending ?: return
    AlertDialog(
        onDismissRequest = state::dismiss,
        title = { Text(request.title) },
        text = { Text(request.message) },
        confirmButton = {
            TextButton(
                onClick = {
                    request.onConfirm()
                    state.dismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = state::dismiss) { Text("Cancel") }
        }
    )
}
