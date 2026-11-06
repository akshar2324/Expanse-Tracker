package com.akshar.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.akshar.data.activity.ActivityFilter
import com.akshar.data.activity.ActivityFilterCodec
import com.akshar.data.activity.ActivitySort
import com.akshar.data.activity.ActivityType
import com.akshar.data.activity.DatePreset
import com.akshar.data.activity.SavedActivityFilter
import com.akshar.data.activity.SavedFilterStore
import com.akshar.data.activity.TransactionQuery
import com.akshar.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

/**
 * ViewModel behaviour with a fake search on virtual time. The SQL itself is covered by
 * ActivitySearchQueryTest; here the fake records which queries reach the database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActivityViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val queries = mutableListOf<TransactionQuery>()
    private val store = FakeStore()

    private val all = listOf(
        tx(1, "EXPENSE", 120.0, "Food", "Morning coffee"),
        tx(2, "INCOME", 5000.0, "Salary", "Pay"),
        tx(3, "EXPENSE", 40.25, "Food", "Snacks"),
    )

    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun tx(id: Long, type: String, amount: Double, category: String, description: String) = Transaction(
        id = id, amount = amount, type = type, category = category, description = description, date = id, paymentMethod = "Cash"
    )

    /** Mimics the DAO for the fields these tests use. */
    private fun fakeSearch(query: TransactionQuery): Flow<List<Transaction>> {
        queries += query
        val needle = query.searchPattern?.trim('%')
        return flowOf(all.filter { tx ->
            (needle == null || tx.description.contains(needle, true) || tx.category.contains(needle, true)) &&
                (query.type == ActivityType.ALL || tx.type == query.type.name) &&
                (query.minAmount == null || tx.amount >= query.minAmount)
        })
    }

    private fun TestScope.viewModel(state: SavedStateHandle = SavedStateHandle()): ActivityViewModel {
        val vm = ActivityViewModel(
            savedState = state,
            search = ::fakeSearch,
            hasAnyTransactions = flowOf(true),
            usedCategories = flowOf(listOf("Food", "Salary")),
            usedPaymentMethods = flowOf(listOf("Cash")),
            accounts = flowOf(emptyList()),
            savedFilterStore = store,
            clock = { 1_000L },
            computeDispatcher = dispatcher,
        )
        backgroundScope.launch { vm.results.collect {} }
        backgroundScope.launch { vm.savedFilters.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun `initial results cover everything with totals`() = runTest(dispatcher) {
        val vm = viewModel()
        val results = vm.results.value
        assertTrue(results.loaded)
        assertEquals(3, results.totals.count)
        assertEquals(BigDecimal("160.25"), results.totals.spent)
        assertEquals(BigDecimal("5000.00"), results.totals.received)
    }

    @Test
    fun `typing is debounced into a single query`() = runTest(dispatcher) {
        val vm = viewModel()
        queries.clear()
        vm.setQuery("c"); advanceTimeBy(100)
        vm.setQuery("co"); advanceTimeBy(100)
        vm.setQuery("coffee"); runCurrent()
        assertEquals("the field updates at once", "coffee", vm.filter.value.query)
        assertTrue("no query before the debounce elapses", queries.isEmpty())

        advanceTimeBy(ActivityViewModel.SEARCH_DEBOUNCE_MILLIS + 1)
        assertEquals(listOf("%coffee%"), queries.map { it.searchPattern })
        assertEquals(listOf(1L), vm.results.value.transactions.map { it.id })
    }

    @Test
    fun `clearing the search applies without waiting`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.setQuery("coffee"); advanceUntilIdle()
        vm.setQuery(""); runCurrent()
        assertEquals(3, vm.results.value.transactions.size)
    }

    @Test
    fun `non-text filters apply immediately`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.setType(ActivityType.INCOME); runCurrent()
        assertEquals(listOf(2L), vm.results.value.transactions.map { it.id })
        assertEquals(BigDecimal("0.00"), vm.results.value.totals.spent)
    }

    @Test
    fun `invalid amount range reports an error and does not filter`() = runTest(dispatcher) {
        val vm = viewModel()
        backgroundScope.launch { vm.amountRange.collect {} }
        vm.setMinAmount("500"); vm.setMaxAmount("100"); runCurrent()
        assertEquals("Minimum is more than maximum", vm.amountRange.value.error)
        assertNull(queries.last().minAmount)
        assertEquals(3, vm.results.value.transactions.size)

        vm.setMaxAmount(""); runCurrent()
        assertNull(vm.amountRange.value.error)
        assertEquals(listOf(2L), vm.results.value.transactions.map { it.id })
    }

    @Test
    fun `categories toggle on and off`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.toggleCategory("Food"); vm.toggleCategory("Salary")
        assertEquals(setOf("Food", "Salary"), vm.filter.value.categories)
        vm.toggleCategory("Food")
        assertEquals(setOf("Salary"), vm.filter.value.categories)
        vm.clearCategories()
        assertTrue(vm.filter.value.categories.isEmpty())
    }

    @Test
    fun `presets clear a custom range and custom ranges are ordered`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.setCustomRange(20261031, 20261001)
        assertEquals(DatePreset.CUSTOM, vm.filter.value.datePreset)
        assertEquals(20261001, vm.filter.value.customStartDay)
        vm.setDatePreset(DatePreset.LAST_MONTH)
        assertEquals(DatePreset.LAST_MONTH, vm.filter.value.datePreset)
        assertNull(vm.filter.value.customStartDay)
    }

    @Test
    fun `clear filters resets everything but the sort order`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.setSort(ActivitySort.HIGHEST)
        vm.setQuery("x"); vm.setType(ActivityType.EXPENSE); vm.setAccount(2); vm.setMinAmount("5")
        vm.clearFilters()
        assertEquals(ActivityFilter(sort = ActivitySort.HIGHEST), vm.filter.value)
        assertFalse(vm.filter.value.isFiltering)
    }

    @Test
    fun `filter is restored from saved state`() = runTest(dispatcher) {
        val state = SavedStateHandle()
        viewModel(state).apply { setType(ActivityType.EXPENSE); setMinAmount("100") }
        val restored = viewModel(state)
        assertEquals(ActivityType.EXPENSE, restored.filter.value.type)
        assertEquals("100", restored.filter.value.minAmountText)
        assertEquals(listOf(1L), restored.results.value.transactions.map { it.id })
    }

    @Test
    fun `save and apply a named filter`() = runTest(dispatcher) {
        val vm = viewModel()
        assertFalse(vm.saveCurrent("   "))
        vm.setType(ActivityType.EXPENSE); vm.toggleCategory("Food")
        assertTrue(vm.saveCurrent("Food spend"))
        runCurrent()
        assertEquals(listOf("Food spend"), vm.savedFilters.value.map { it.name })

        vm.clearFilters()
        vm.applySaved(vm.savedFilters.value.single())
        assertEquals(ActivityType.EXPENSE, vm.filter.value.type)
        assertEquals(setOf("Food"), vm.filter.value.categories)

        vm.deleteSaved("Food spend"); runCurrent()
        assertTrue(vm.savedFilters.value.isEmpty())
    }

    @Test
    fun `codec round trips every field and tolerates bad input`() {
        val filter = ActivityFilter(
            query = "rent \"quoted\"", type = ActivityType.TRANSFER, categories = setOf("Food", "Rent"),
            accountId = 3, paymentMethod = "UPI", datePreset = DatePreset.CUSTOM, customStartDay = 20260101,
            customEndDay = 20260131, minAmountText = "1,000", maxAmountText = "2000.5", sort = ActivitySort.LOWEST
        )
        assertEquals(filter, ActivityFilterCodec.decodeFilter(ActivityFilterCodec.encodeFilter(filter)))
        assertEquals(ActivityFilter(), ActivityFilterCodec.decodeFilter("""{"type":"NOPE","sort":"???"}"""))
        assertNull(ActivityFilterCodec.decodeFilter("not json"))

        val saved = listOf(SavedActivityFilter("A", filter), SavedActivityFilter("B", ActivityFilter()))
        assertEquals(saved, ActivityFilterCodec.decodeSaved(ActivityFilterCodec.encodeSaved(saved)))
        assertEquals(emptyList<SavedActivityFilter>(), ActivityFilterCodec.decodeSaved("[oops"))
        assertEquals(1, ActivityFilterCodec.decodeSaved("""[{"name":"","filter":{}},{"name":"ok","filter":{}},5]""").size)
    }

    private class FakeStore : SavedFilterStore {
        private val state = MutableStateFlow<List<SavedActivityFilter>>(emptyList())
        override val savedFilters: Flow<List<SavedActivityFilter>> = state
        override suspend fun save(name: String, filter: ActivityFilter) = state.update { list ->
            list.filterNot { it.name.equals(name.trim(), true) } + SavedActivityFilter(name.trim(), filter)
        }
        override suspend fun delete(name: String) = state.update { list -> list.filterNot { it.name.equals(name, true) } }
    }
}
