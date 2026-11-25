package com.akshar.alerts

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex

private val Context.budgetAlertData by preferencesDataStore("budget_alerts")

/** DataStore keeps settings and delivery claims durable, with all disk work off the main thread. */
class BudgetAlertStore internal constructor(private val data: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.budgetAlertData)
    val enabled = data.data.map { it[ENABLED] ?: false }
    suspend fun setEnabled(value: Boolean) { data.edit { it[ENABLED] = value } }
    suspend fun sent(): Set<String> = data.data.first()[SENT].orEmpty()

    /** Claim before posting: a process death may lose an alert, but cannot send one twice. */
    suspend fun claim(key: String): Boolean {
        var claimed = false
        data.edit { prefs ->
            val sent = prefs[SENT].orEmpty()
            if (prefs[ENABLED] == true && key !in sent) {
                prefs[SENT] = sent + key
                claimed = true
            }
        }
        return claimed
    }

    companion object {
        private val ENABLED = booleanPreferencesKey("enabled")
        private val SENT = stringSetPreferencesKey("sent")
        // Serializes workers and disabling, so no alert can post after switching off finishes.
        val runLock = Mutex()
    }
}
