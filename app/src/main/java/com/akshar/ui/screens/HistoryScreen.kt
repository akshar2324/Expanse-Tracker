package com.akshar.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.akshar.ui.components.*
import com.akshar.ui.theme.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.akshar.data.activity.ActivityFilter
import com.akshar.data.activity.ActivitySort
import com.akshar.data.activity.ActivityTotals
import com.akshar.data.activity.ActivityType
import com.akshar.data.activity.DatePreset
import com.akshar.data.activity.SavedActivityFilter
import com.akshar.data.model.Transaction
import com.akshar.TransactionRoutes
import com.akshar.ui.viewmodel.ActivityViewModel
import com.akshar.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    activityViewModel: ActivityViewModel = viewModel(factory = ActivityViewModel.Factory),
) {
    val filter by activityViewModel.filter.collectAsStateWithLifecycle()
    val results by activityViewModel.results.collectAsStateWithLifecycle()
    val amountRange by activityViewModel.amountRange.collectAsStateWithLifecycle()
    val hasAnyTransactions by activityViewModel.hasAnyTransactions.collectAsStateWithLifecycle()
    val categories by activityViewModel.categories.collectAsStateWithLifecycle()
    val paymentMethods by activityViewModel.paymentMethods.collectAsStateWithLifecycle()
    val accounts by activityViewModel.accounts.collectAsStateWithLifecycle()
    val savedFilters by activityViewModel.savedFilters.collectAsStateWithLifecycle()

    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var showDateRangePicker by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val confirmDelete = rememberConfirmDeleteState()
    ConfirmDeleteDialog(confirmDelete)

    val transactions = results.transactions
    val totals = results.totals
    val currencySymbol = viewModel.getCurrencySymbol()
    val groupByDay = filter.sort == ActivitySort.NEWEST || filter.sort == ActivitySort.OLDEST
    val groups = remember(transactions, groupByDay) {
        if (groupByDay) transactions.groupBy { dayKey(it.date) }.toList()
        else listOf("" to transactions)
    }
    val activeSaved = savedFilters.firstOrNull { it.filter == filter }
    val finance = AkSpendTheme.finance

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("activity-list"),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            bottom = LocalFloatingBarInset.current + Spacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(key = "header") {
            val activeCount = filter.activeCount
            ScreenHeader(
                title = "Activity",
                subtitle = "Every expense, income and transfer",
                insetHorizontally = false,
                actions = {
                    GlassIconButton(
                        icon = if (showFilters) Icons.Rounded.FilterListOff else Icons.Rounded.Tune,
                        contentDescription = buildString {
                            append(if (showFilters) "Hide filters" else "Show filters")
                            if (activeCount > 0) append(", $activeCount active")
                        },
                        onClick = { showFilters = !showFilters },
                        tint = if (activeCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }

        item(key = "search") {
            OutlinedTextField(
                value = filter.query,
                onValueChange = activityViewModel::setQuery,
                placeholder = { Text("Search description, category or payment") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(onClick = { activityViewModel.setQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
                shape = PillShape,
                colors = glassTextFieldColors()
            )
        }

        if (savedFilters.isNotEmpty()) {
            item(key = "saved") {
                SavedFiltersRow(
                    saved = savedFilters,
                    active = activeSaved,
                    onApply = { saved ->
                        if (saved == activeSaved) activityViewModel.clearFilters() else activityViewModel.applySaved(saved)
                    },
                    onDelete = { saved ->
                        confirmDelete.request(
                            title = "Delete “${saved.name}”?",
                            message = "The saved filter is removed. Your transactions are not affected."
                        ) { activityViewModel.deleteSaved(saved.name) }
                    }
                )
            }
        }

        item(key = "filters") {
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically(motionSpec()) + fadeIn(motionSpec()),
                exit = shrinkVertically(motionSpec(Durations.quick)) + fadeOut(motionSpec(Durations.quick))
            ) {
                FiltersPanel(
                    filter = filter,
                    amountError = amountRange.error,
                    categories = categories,
                    paymentMethods = paymentMethods,
                    accounts = accounts.map { it.id to it.name },
                    activityViewModel = activityViewModel,
                    onCustomRange = {
                        focusManager.clearFocus()
                        showDateRangePicker = true
                    },
                    onSave = {
                        focusManager.clearFocus()
                        showSaveDialog = true
                    }
                )
            }
        }

        if (hasAnyTransactions == true) {
            item(key = "summary") {
                ResultsSummary(
                    count = totals.count,
                    spent = signedMoney(totals.spent.toDouble(), positive = false, currencySymbol = currencySymbol, decimals = 2),
                    received = signedMoney(totals.received.toDouble(), positive = true, currencySymbol = currencySymbol, decimals = 2),
                    filtering = filter.isFiltering,
                    onClear = activityViewModel::clearFilters
                )
            }
        }

        if (results.loaded && transactions.isEmpty() && hasAnyTransactions != null) {
            item(key = "empty") {
                val nothingLogged = hasAnyTransactions == false
                EmptyState(
                    icon = if (nothingLogged) Icons.Rounded.Inbox else Icons.Rounded.SearchOff,
                    title = if (nothingLogged) "Nothing here yet" else "No matches",
                    message = if (nothingLogged) "Transactions you add will appear here, newest first."
                    else "Nothing matches your search and filters. Clear them to see everything again.",
                    action = {
                        FilledTonalButton(
                            onClick = {
                                if (nothingLogged) navController.navigate(TransactionRoutes.ADD)
                                else activityViewModel.clearFilters()
                            },
                            shape = PillShape,
                            modifier = Modifier.heightIn(min = Spacing.touchTarget)
                        ) {
                            Text(
                                if (nothingLogged) "Add transaction" else "Clear filters",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                )
            }
        } else {
            groups.forEach { (day, dayTransactions) ->
                if (dayTransactions.isEmpty()) return@forEach
                if (day.isNotEmpty()) {
                    item(key = "day-$day") {
                        val dayTotals = remember(dayTransactions) { ActivityTotals.of(dayTransactions) }
                        val net = (dayTotals.received - dayTotals.spent).toDouble()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.sm, start = Spacing.xs, end = Spacing.xs),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f, fill = false).semantics { heading() }
                            )
                            Text(
                                // Same precision as the rows below it, so paise are never hidden.
                                text = netMoney(net, currencySymbol, decimals = 2),
                                style = MaterialTheme.typography.labelLarge.money(),
                                color = if (net >= 0) finance.income else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item(key = "group-$day-${dayTransactions.first().id}") {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = Spacing.xs)
                    ) {
                        dayTransactions.forEachIndexed { index, transaction ->
                            if (index > 0) GlassDivider()
                            TransactionItem(
                                transaction = transaction,
                                currencySymbol = currencySymbol,
                                timePattern = if (groupByDay) "h:mm a" else "d MMM yyyy",
                                onClick = {
                                    if (transaction.transferId != null) {
                                        navController.navigate("addTransfer/${transaction.transferId}")
                                    } else {
                                        navController.navigate(TransactionRoutes.edit(transaction.id))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveFilterDialog(
            existingNames = savedFilters.map { it.name },
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                if (activityViewModel.saveCurrent(name)) showSaveDialog = false
            }
        )
    }

    if (showDateRangePicker) {
        CustomRangeDialog(
            filter = filter,
            onDismiss = { showDateRangePicker = false },
            onConfirm = { start, end ->
                activityViewModel.setCustomRange(start, end)
                showDateRangePicker = false
            }
        )
    }
}

@Composable
private fun FiltersPanel(
    filter: ActivityFilter,
    amountError: String?,
    categories: List<String>,
    paymentMethods: List<String>,
    accounts: List<Pair<Long, String>>,
    activityViewModel: ActivityViewModel,
    onCustomRange: () -> Unit,
    onSave: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(Spacing.lg)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterGroupLabel("Type")
            ChipFlow {
                ActivityType.entries.forEach { type ->
                    GlassFilterChip(
                        label = when (type) {
                            ActivityType.ALL -> "All"
                            ActivityType.EXPENSE -> "Expenses"
                            ActivityType.INCOME -> "Income"
                            ActivityType.TRANSFER -> "Transfers"
                        },
                        selected = filter.type == type,
                        onClick = { activityViewModel.setType(type) }
                    )
                }
            }

            FilterGroupLabel("Date range")
            ChipFlow {
                listOf(DatePreset.ALL_TIME, DatePreset.THIS_MONTH, DatePreset.LAST_MONTH, DatePreset.LAST_30_DAYS, DatePreset.LAST_7_DAYS, DatePreset.THIS_YEAR).forEach { preset ->
                    GlassFilterChip(
                        label = when (preset) {
                            DatePreset.THIS_MONTH -> "This month"
                            DatePreset.LAST_MONTH -> "Last month"
                            DatePreset.LAST_30_DAYS -> "Last 30 days"
                            DatePreset.LAST_7_DAYS -> "Last 7 days"
                            DatePreset.THIS_YEAR -> "This year"
                            else -> "All time"
                        },
                        selected = filter.datePreset == preset,
                        onClick = { activityViewModel.setDatePreset(preset) }
                    )
                }
                GlassFilterChip(
                    label = if (filter.datePreset == DatePreset.CUSTOM) customRangeLabel(filter) else "Custom…",
                    selected = filter.datePreset == DatePreset.CUSTOM,
                    onClick = onCustomRange
                )
            }

            if (categories.isNotEmpty() || filter.categories.isNotEmpty()) {
                FilterGroupLabel(if (filter.categories.isEmpty()) "Categories" else "Categories (${filter.categories.size} selected)")
                ChipFlow {
                    GlassFilterChip(label = "Any", selected = filter.categories.isEmpty(), onClick = activityViewModel::clearCategories)
                    // Keep a category chosen in a saved filter visible even if no entry uses it any more.
                    (categories + filter.categories.filterNot { it in categories }).forEach { category ->
                        GlassFilterChip(
                            label = category,
                            selected = category in filter.categories,
                            onClick = { activityViewModel.toggleCategory(category) }
                        )
                    }
                }
            }

            if (accounts.size > 1 || filter.accountId != null) {
                FilterGroupLabel("Account")
                ChipFlow {
                    GlassFilterChip(label = "All accounts", selected = filter.accountId == null, onClick = { activityViewModel.setAccount(null) })
                    accounts.forEach { (id, name) ->
                        GlassFilterChip(label = name, selected = filter.accountId == id, onClick = { activityViewModel.setAccount(id) })
                    }
                }
            }

            if (paymentMethods.isNotEmpty() || filter.paymentMethod != null) {
                FilterGroupLabel("Payment method")
                ScrollableFilterRow(
                    items = listOf("ALL") + (paymentMethods + listOfNotNull(filter.paymentMethod)).distinct(),
                    selectedItem = filter.paymentMethod ?: "ALL",
                    onSelect = { activityViewModel.setPaymentMethod(it.takeUnless { m -> m == "ALL" }) }
                )
            }

            FilterGroupLabel("Amount")
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AmountField(
                    label = "Min",
                    value = filter.minAmountText,
                    onValueChange = activityViewModel::setMinAmount,
                    isError = amountError != null,
                    modifier = Modifier.weight(1f)
                )
                AmountField(
                    label = "Max",
                    value = filter.maxAmountText,
                    onValueChange = activityViewModel::setMaxAmount,
                    isError = amountError != null,
                    modifier = Modifier.weight(1f)
                )
            }
            if (amountError != null) {
                Text(
                    text = "$amountError. The amount filter is off until this is fixed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            FilterGroupLabel("Sort by")
            ChipFlow {
                ActivitySort.entries.forEach { sort ->
                    GlassFilterChip(
                        label = when (sort) {
                            ActivitySort.NEWEST -> "Newest"
                            ActivitySort.OLDEST -> "Oldest"
                            ActivitySort.HIGHEST -> "Highest amount"
                            ActivitySort.LOWEST -> "Lowest amount"
                        },
                        selected = filter.sort == sort,
                        onClick = { activityViewModel.setSort(sort) }
                    )
                }
            }

            WrapRow(modifier = Modifier.padding(top = Spacing.xs), verticalSpacing = Spacing.sm) {
                FilledTonalButton(
                    onClick = onSave,
                    enabled = filter.isFiltering,
                    shape = PillShape,
                    modifier = Modifier.heightIn(min = Spacing.touchTarget)
                ) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Save filter", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(
                    onClick = activityViewModel::clearFilters,
                    enabled = filter.isFiltering,
                    modifier = Modifier.heightIn(min = Spacing.touchTarget)
                ) {
                    Text("Clear filters", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ResultsSummary(
    count: Int,
    spent: String,
    received: String,
    filtering: Boolean,
    onClear: () -> Unit,
) {
    val entries = "$count ${if (count == 1) "entry" else "entries"}"
    val largeText = LocalDensity.current.fontScale >= 1.3f
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(Spacing.lg)) {
        if (largeText) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    if (filtering) "$entries match" else entries,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (filtering) TextButton(onClick = onClear) { Text("Clear filters") }
                SummaryFigure(label = "Spent", value = spent, color = AkSpendTheme.finance.expense)
                SummaryFigure(label = "Received", value = received, color = AkSpendTheme.finance.income)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (filtering) "$entries match" else entries,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }
                )
                if (filtering) {
                    TextButton(onClick = onClear, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
                        Text("Clear filters", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                SummaryFigure(label = "Spent", value = spent, color = AkSpendTheme.finance.expense, modifier = Modifier.weight(1f))
                SummaryFigure(label = "Received", value = received, color = AkSpendTheme.finance.income, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryFigure(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.money(), color = color)
    }
}

@Composable
private fun SavedFiltersRow(
    saved: List<SavedActivityFilter>,
    active: SavedActivityFilter?,
    onApply: (SavedActivityFilter) -> Unit,
    onDelete: (SavedActivityFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FilterGroupLabel("Saved filters")
        Row(verticalAlignment = Alignment.CenterVertically) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(saved, key = { it.name }) { item ->
                    GlassFilterChip(label = item.name, selected = item == active, onClick = { onApply(item) })
                }
            }
            if (active != null) {
                IconButton(onClick = { onDelete(active) }, modifier = Modifier.size(Spacing.touchTarget)) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete saved filter ${active.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveFilterDialog(
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    val trimmed = name.trim()
    val replaces = existingNames.any { it.equals(trimmed, ignoreCase = true) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        GlassCard(
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = Spacing.gutter).heightIn(max = 560.dp),
            fill = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text("Save filter", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(SavedActivityFilter.MAX_NAME_LENGTH) },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = glassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    if (replaces) "Replaces the saved filter with this name"
                    else "${name.length}/${SavedActivityFilter.MAX_NAME_LENGTH}",
                    style = MaterialTheme.typography.bodySmall,
                )
                WrapRow {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onSave(trimmed) }, enabled = trimmed.isNotEmpty()) {
                        Text(if (replaces) "Replace" else "Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(
    filter: ActivityFilter,
    onDismiss: () -> Unit,
    onConfirm: (startDay: Int, endDay: Int) -> Unit,
) {
    // The picker works in UTC midnights; saved days are calendar days, so convert through UTC.
    val utc = remember { TimeZone.getTimeZone("UTC") }
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = filter.customStartDay?.let { ActivityFilter.dayStart(it, utc) },
        initialSelectedEndDateMillis = filter.customEndDay?.let { ActivityFilter.dayStart(it, utc) },
    )
    val start = state.selectedStartDateMillis
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = start != null,
                onClick = {
                    if (start != null) {
                        val end = state.selectedEndDateMillis ?: start
                        onConfirm(ActivityFilter.dayKey(start, utc), ActivityFilter.dayKey(end, utc))
                    }
                }
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DateRangePicker(
            state = state,
            title = { Text("Custom date range", modifier = Modifier.padding(start = Spacing.xxl, top = Spacing.lg)) },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun customRangeLabel(filter: ActivityFilter): String {
    val start = filter.customStartDay ?: return "Custom…"
    val end = filter.customEndDay ?: start
    val format = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val first = format.format(Date(ActivityFilter.dayStart(start)))
    return if (start == end) first else "$first – ${format.format(Date(ActivityFilter.dayStart(end)))}"
}

@Composable
private fun AmountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        colors = glassTextFieldColors(),
        modifier = modifier
    )
}

/** Chips that wrap onto new lines instead of scrolling, so large fonts never hide options. */
@Composable
private fun ChipFlow(content: @Composable () -> Unit) = WrapRow(content = content)

@Composable
private fun GlassFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = PillShape,
        modifier = Modifier.heightIn(min = Spacing.touchTarget),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = AkSpendTheme.glass.fill,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

private fun dayKey(timestamp: Long): String {
    val today = Calendar.getInstance()
    val day = Calendar.getInstance().apply { timeInMillis = timestamp }
    val sameYear = today.get(Calendar.YEAR) == day.get(Calendar.YEAR)
    val dayDiff = today.get(Calendar.DAY_OF_YEAR) - day.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> "Today"
        sameYear && dayDiff == 1 -> "Yesterday"
        sameYear -> SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
private fun FilterGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.xs)
    )
}

// Horizontal row of selectable capsules; each one is a full 48dp touch target.
@Composable
fun ScrollableFilterRow(
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit,
    labelFor: (String) -> String = { if (it == "ALL") "All payments" else it }
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(items) { item ->
            FilterChip(
                selected = item == selectedItem,
                onClick = { onSelect(item) },
                label = { Text(labelFor(item), style = MaterialTheme.typography.labelLarge) },
                shape = PillShape,
                modifier = Modifier.heightIn(min = Spacing.touchTarget),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = AkSpendTheme.glass.fill,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// Helpers
fun List<Transaction>.sortedByByAmountDescending() = sortedByDescending { it.amount }

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    currencySymbol: String = "₹",
    // Activity groups rows by day, so it shows the time; lists without day headers show the date.
    timePattern: String = "h:mm a",
) {
    val formatter = remember(timePattern) { SimpleDateFormat(timePattern, Locale.getDefault()) }
    val isExpense = transaction.type == "EXPENSE"
    val isTransfer = transaction.transferId != null
    val finance = AkSpendTheme.finance

    GlassListRow(
        title = transaction.description.ifBlank { transaction.category },
        subtitle = listOfNotNull(
            if (isTransfer) "Transfer" else transaction.category,
            transaction.paymentMethod.takeIf { it.isNotBlank() },
            formatter.format(Date(transaction.date))
        ).joinToString(" · "),
        leading = {
            IconBadge(
                icon = if (isTransfer) Icons.Rounded.SwapHoriz else categoryIcon(transaction.category),
                tint = if (isTransfer) finance.transfer else categoryTint(transaction.category),
                size = 44.dp
            )
        },
        trailing = {
            Text(
                text = "${if (isExpense) "-" else "+"}$currencySymbol${String.format("%,.2f", transaction.amount)}",
                style = MaterialTheme.typography.titleSmall.money(),
                color = when {
                    isTransfer -> MaterialTheme.colorScheme.onSurfaceVariant
                    isExpense -> finance.expense
                    else -> finance.income
                }
            )
        },
        onClick = onClick
    )
}
