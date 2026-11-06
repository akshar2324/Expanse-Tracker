package com.akshar.insights

import android.content.Context
import androidx.core.content.edit
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

/** Device-only storage of normalised dismissed merchant keys, never amounts or dates. */
class SubscriptionDismissals(context: Context) {
    private val appContext = context.applicationContext
    private val prefs by lazy { appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    /** Observe storage itself so multiple screen ViewModels and restored sessions stay in sync. */
    val keys: Flow<Set<String>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY || key == null) trySend(read())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    private fun read(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty().toSet()

    /** Call on an IO dispatcher; apply persists asynchronously and updates observers immediately. */
    fun dismiss(key: String) {
        if (key.isBlank()) return
        synchronized(LOCK) {
            prefs.edit { putStringSet(KEY, read() + key) }
        }
    }

    private companion object {
        val LOCK = Any()
        const val PREFS = "insight_prefs"
        const val KEY = "dismissed_subscriptions"
    }
}
