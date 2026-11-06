package com.akshar.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.akshar.ExpenseTrackerApp
import com.akshar.data.activity.ActivityFilter
import com.akshar.data.activity.ActivityFilterCodec
import com.akshar.data.activity.ActivitySort
import com.akshar.data.activity.ActivityTotals
import com.akshar.data.activity.ActivityType
import com.akshar.data.activity.AmountRange
import com.akshar.data.activity.DataStoreSavedFilterStore
import com.akshar.data.activity.DatePreset
import com.akshar.data.activity.SavedActivityFilter
import com.akshar.data.activity.SavedFilterStore
import com.akshar.data.activity.TransactionQuery
import com.akshar.data.model.Account
import com.akshar.data.model.Transaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Results for the current filter. [loaded] is false until the first query returns. */
data class ActivityResults(
    val transactions: List<Transaction> = emptyList(),
    val totals: ActivityTotals = ActivityTotals.EMPTY,
    val loaded: Boolean = false,
)

/**
 * State for the Activity tab: search, filters, saved filters and the filtered list with totals.
 * The filter lives here and in [SavedStateHandle], so it survives rotation and process death.
 * Typing is debounced before it reaches Room; clearing the search applies at once.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ActivityViewModel(
    private val savedState: SavedStateHandle,
    private val search: (TransactionQuery) -> Flow<List<Transaction>>,
    hasAnyTransactions: Flow<Boolean>,
    usedCategories: Flow<List<String>>,
    usedPaymentMethods: Flow<List<String>>,
    accounts: Flow<List<Account>>,
    private val savedFilterStore: SavedFilterStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val searchDebounceMillis: Long = SEARCH_DEBOUNCE_MILLIS,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _filter = MutableStateFlow(
        ActivityFilterCodec.decodeFilter(savedState.get<String>(KEY_FILTER)) ?: ActivityFilter()
    )
    val filter: StateFlow<ActivityFilter> = _filter.asStateFlow()

    val amountRange: StateFlow<AmountRange> = _filter
        .map { AmountRange.parse(it.minAmountText, it.maxAmountText) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AmountRange.parse(_filter.value.minAmountText, _filter.value.maxAmountText))

    private val debouncedQuery: Flow<String> = _filter
        .map { it.query }
        .distinctUntilChanged()
        .debounce { if (it.isBlank()) 0L else searchDebounceMillis }

    /** The query that actually runs: current filter with the debounced search text. */
    private val criteria: Flow<TransactionQuery> = combine(_filter, debouncedQuery) { filter, query ->
        filter.copy(query = query).resolve(clock())
    }.distinctUntilChanged()

    val results: StateFlow<ActivityResults> = criteria
        .flatMapLatest { search(it) }
        .map { list -> ActivityResults(list, ActivityTotals.of(list), loaded = true) }
        .flowOn(computeDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityResults())

    val hasAnyTransactions: StateFlow<Boolean?> = hasAnyTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val categories: StateFlow<List<String>> = usedCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val paymentMethods: StateFlow<List<String>> = usedPaymentMethods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accounts: StateFlow<List<Account>> = accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedFilters: StateFlow<List<SavedActivityFilter>> = savedFilterStore.savedFilters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun edit(transform: (ActivityFilter) -> ActivityFilter) {
        _filter.update(transform)
        savedState[KEY_FILTER] = ActivityFilterCodec.encodeFilter(_filter.value)
    }

    fun setQuery(query: String) = edit { it.copy(query = query) }
    fun setType(type: ActivityType) = edit { it.copy(type = type) }
    fun toggleCategory(category: String) = edit {
        it.copy(categories = if (category in it.categories) it.categories - category else it.categories + category)
    }
    fun clearCategories() = edit { it.copy(categories = emptySet()) }
    fun setAccount(accountId: Long?) = edit { it.copy(accountId = accountId) }
    fun setPaymentMethod(method: String?) = edit { it.copy(paymentMethod = method) }
    fun setSort(sort: ActivitySort) = edit { it.copy(sort = sort) }
    fun setMinAmount(text: String) = edit { it.copy(minAmountText = text) }
    fun setMaxAmount(text: String) = edit { it.copy(maxAmountText = text) }

    /** Presets only; a custom range goes through [setCustomRange]. */
    fun setDatePreset(preset: DatePreset) = edit {
        if (preset == DatePreset.CUSTOM) it else it.copy(datePreset = preset, customStartDay = null, customEndDay = null)
    }

    /** Days are yyyyMMdd, inclusive; reversed input is swapped. */
    fun setCustomRange(startDay: Int, endDay: Int) = edit { it.withCustomRange(startDay, endDay) }

    /** Clears every filter and the search but keeps the chosen sort order. */
    fun clearFilters() = edit { ActivityFilter(sort = it.sort) }

    fun applySaved(saved: SavedActivityFilter) = edit { saved.filter }

    /** Saves the current filter. Returns false (and saves nothing) when [name] is blank. */
    fun saveCurrent(name: String): Boolean {
        if (name.isBlank()) return false
        val snapshot = _filter.value
        viewModelScope.launch { savedFilterStore.save(name, snapshot) }
        return true
    }

    fun deleteSaved(name: String) {
        viewModelScope.launch { savedFilterStore.delete(name) }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 300L
        private const val KEY_FILTER = "activity_filter"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseTrackerApp
                val repository = app.repository
                ActivityViewModel(
                    savedState = createSavedStateHandle(),
                    search = repository::searchTransactions,
                    hasAnyTransactions = repository.hasAnyTransactions,
                    usedCategories = repository.usedCategories,
                    usedPaymentMethods = repository.usedPaymentMethods,
                    accounts = repository.allAccounts,
                    savedFilterStore = DataStoreSavedFilterStore(app),
                )
            }
        }
    }
}
