package com.akshar.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Redraws the budget widget when transactions, budgets, the app lock or the currency change. It listens to Room's
 * invalidation tracker, so nothing is queried unless a widget is actually on the home screen.
 * The widget's own `updatePeriodMillis` covers day and month roll-over.
 */
object BudgetWidgetUpdater {

    private const val DEBOUNCE_MS = 1_500L

    // SharedPreferences holds listeners weakly, so keep a strong reference for the process.
    private var settingsListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    @OptIn(FlowPreview::class)
    fun start(context: Context, database: RoomDatabase, scope: CoroutineScope) {
        val appContext = context.applicationContext
        val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        database.invalidationTracker.addObserver(object : InvalidationTracker.Observer(arrayOf("transactions", "budgets")) {
            override fun onInvalidated(tables: Set<String>) {
                changes.tryEmit(Unit)
            }
        })
        // Turning the app lock on or off, or changing the currency, changes what the widget shows.
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == BudgetWidgetModel.KEY_LOCK_ENABLED || key == com.akshar.security.AppLock.KEY_HASH || key == com.akshar.utils.CurrencyUtils.KEY_COUNTRY) changes.tryEmit(Unit)
        }
        settingsListener = listener
        appContext.getSharedPreferences(BudgetWidgetModel.LOCK_PREFS, Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(listener)
        appContext.getSharedPreferences(com.akshar.utils.CurrencyUtils.PREFS, Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(listener)
        scope.launch {
            changes.debounce(DEBOUNCE_MS).collect {
                try {
                    if (GlanceAppWidgetManager(appContext).getGlanceIds(BudgetWidget::class.java).isNotEmpty()) {
                        BudgetWidget().updateAll(appContext)
                    }
                } catch (e: Exception) {
                    Log.w("BudgetWidget", "Widget refresh failed", e)
                }
            }
        }
    }
}
