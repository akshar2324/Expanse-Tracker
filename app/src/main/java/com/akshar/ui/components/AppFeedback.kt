package com.akshar.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal val LocalAppFeedback = compositionLocalOf<AppFeedback?> { null }

internal class AppFeedback(private val scope: CoroutineScope) {
    val hostState = SnackbarHostState()
    private var messageJob: Job? = null

    fun show(message: String, duration: SnackbarDuration = SnackbarDuration.Short) {
        messageJob?.cancel()
        messageJob = scope.launch { hostState.showSnackbar(message, duration = duration) }
    }
}
