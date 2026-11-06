package com.akshar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akshar.ExpenseTrackerApp
import com.akshar.insights.MonthEndForecast
import com.akshar.insights.MonthEndForecaster
import com.akshar.insights.SubscriptionCandidate
import com.akshar.insights.SubscriptionDismissals
import com.akshar.insights.SubscriptionFinder
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.util.TimeZone

/**
 * Month-end forecast and subscription suggestions, recomputed off the main thread whenever the
 * ledger, accounts or reminders change. Creating a reminder goes through [FinanceViewModel].
 */
class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ExpenseTrackerApp).repository
    private val dismissals = SubscriptionDismissals(application)

    // Restarts immediately when visible, and updates dates across midnight without a ledger edit.
    private val clock = flow {
        while (currentCoroutineContext().isActive) {
            emit(System.currentTimeMillis())
            delay(60_000)
        }
    }

    val forecast: StateFlow<MonthEndForecast> = combine(
        repository.allAccounts,
        repository.allTransactions,
        repository.allRecurringTransactions,
        clock,
    ) { accounts, transactions, reminders, now ->
        MonthEndForecaster(TimeZone.getDefault()).forecast(now, accounts, transactions, reminders)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthEndForecast.NoData)

    val subscriptions: StateFlow<List<SubscriptionCandidate>> = combine(
        repository.allTransactions,
        repository.allRecurringTransactions,
        dismissals.keys,
        clock,
    ) { transactions, reminders, dismissed, now ->
        SubscriptionFinder(TimeZone.getDefault()).find(now, transactions, reminders, dismissed)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissSubscription(candidate: SubscriptionCandidate) {
        viewModelScope.launch(Dispatchers.IO) { dismissals.dismiss(candidate.key) }
    }
}
