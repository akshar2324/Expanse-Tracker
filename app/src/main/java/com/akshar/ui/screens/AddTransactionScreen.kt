package com.akshar.ui.screens

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.akshar.ui.theme.*
import com.akshar.ui.components.*
import com.akshar.ui.theme.AkSpendTheme
import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshar.data.model.Transaction
import com.akshar.ui.viewmodel.FinanceViewModel
import com.akshar.utils.CategoryMemory
import com.akshar.utils.DuplicateCandidate
import com.akshar.utils.DuplicateMatch
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    transactionId: Long? = null, // non-null opens the screen in edit mode
    prefill: com.akshar.sms.SmsDraft? = null, // a transaction read from a bank message, to review
    onSaved: () -> Unit = onNavigateBack
) {
    val context = LocalContext.current
    // Large text: options wrap at their own width, so short names share a line and long ones get room.
    val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f
    // A phone on its side: a pinned title and Save button would leave almost no room for the form.
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val compactHeight = configuration.screenHeightDp < 480 && configuration.screenWidthDp > configuration.screenHeightDp
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    // --- State Holding (saveable so input survives rotation and process death) ---
    var isExpense by rememberSaveable { mutableStateOf(true) }
    var amountStr by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var dateSelected by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var paymentMethod by rememberSaveable { mutableStateOf("Cash") }
    var prefilledFromTransaction by rememberSaveable { mutableStateOf(false) }
    // Once the user taps a category themselves, learned suggestions stop changing it.
    var categoryTouched by rememberSaveable { mutableStateOf(false) }

    val expenseCategories = remember {
        listOf(
            "Food", "Grocery", "Transportation", "Fuel", "Shopping",
            "Entertainment", "Bills", "EMI", "Rent", "Health",
            "Education", "Travel", "Gifts", "Subscription", "Other"
        )
    }

    val incomeCategories = remember {
        listOf(
            "Salary", "Freelance", "Business", "Bonus", "Interest",
            "Investment", "Gift", "Refund", "Other"
        )
    }

    val currentCategories = if (isExpense) expenseCategories else incomeCategories
    var categorySelected by rememberSaveable { mutableStateOf(currentCategories.first()) }

    // Sync category reset if type swaps (keeps a valid or originally loaded category)
    LaunchedEffect(isExpense) {
        // While an edited transaction is (re)loading, keep the restored category as is.
        if (transactionId != null && editingTransaction == null) return@LaunchedEffect
        val keepLoaded = editingTransaction?.let { it.category == categorySelected && (it.type == "EXPENSE") == isExpense } == true
        if (categorySelected !in currentCategories && !keepLoaded) {
            categorySelected = currentCategories.first()
        }
    }

    // Edit mode: load the transaction once and prefill the form
    LaunchedEffect(transactionId) {
        if (transactionId == null) return@LaunchedEffect
        val tx = viewModel.getTransactionById(transactionId)
        if (tx == null) {
            onNavigateBack()
            return@LaunchedEffect
        }
        editingTransaction = tx
        // After a configuration change keep what the user has typed; only prefill once.
        if (prefilledFromTransaction) return@LaunchedEffect
        prefilledFromTransaction = true
        isExpense = tx.type == "EXPENSE"
        amountStr = BigDecimal(tx.amount.toString()).stripTrailingZeros().toPlainString()
        description = tx.description
        dateSelected = tx.date
        categorySelected = tx.category
        paymentMethod = tx.paymentMethod
    }

    // Bank message import: fill the form once; after a configuration change keep the user's edits.
    LaunchedEffect(prefill) {
        if (prefill == null || prefilledFromTransaction) return@LaunchedEffect
        prefilledFromTransaction = true
        isExpense = prefill.isExpense
        amountStr = prefill.amount
        description = prefill.description
        dateSelected = prefill.dateMillis
        categorySelected = prefill.category
        prefill.paymentMethod?.let { paymentMethod = it }
    }

    // Learned merchant -> category mappings, used to preselect the category on new entries.
    var memory by remember { mutableStateOf(CategoryMemory.EMPTY) }
    var categoryFromMemory by remember { mutableStateOf(false) }
    LaunchedEffect(transactionId) {
        if (transactionId == null) memory = runCatching { viewModel.categoryMemory() }.getOrDefault(CategoryMemory.EMPTY)
    }
    LaunchedEffect(description, isExpense, memory) {
        if (transactionId != null || categoryTouched) return@LaunchedEffect
        // A bank message draft already chose its category (learned first); keep it until the text changes.
        if (prefill != null && description == prefill.description) return@LaunchedEffect
        val learned = memory.lookup(description, isExpense)?.takeIf { it in currentCategories }
        if (learned != null) categorySelected = learned
        categoryFromMemory = learned != null
    }

    val paymentMethods by viewModel.activePaymentMethods.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.getCurrencySymbol()

    LaunchedEffect(paymentMethods) {
        if (!paymentMethods.contains(paymentMethod)) {
            paymentMethod = paymentMethods.firstOrNull() ?: "Cash"
        }
    }

    // Date formatting helper
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Error states
    var amountError by remember { mutableStateOf<String?>(null) }

    val accent = if (isExpense) AkSpendTheme.finance.expense else AkSpendTheme.finance.income
    val onAccent = MaterialTheme.colorScheme.surface
    // Flat styles (Atelier) keep filled controls in ink/champagne like every other button and
    // leave red/green to the amount itself, so the form stays calm.
    val flatStyle = AkSpendTheme.style.isFlat
    val fillAccent = if (flatStyle) MaterialTheme.colorScheme.primary else accent
    val onFillAccent = if (flatStyle) MaterialTheme.colorScheme.onPrimary else onAccent
    val glass = AkSpendTheme.glass

    val amountFocus = remember { FocusRequester() }
    // New entries start with the keyboard on the amount: it's the one field every entry needs.
    // Only once: after rotation or a text size change the keyboard stays the way the user left it.
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (autoFocused) return@LaunchedEffect
        autoFocused = true
        if (transactionId == null && prefill == null) runCatching { amountFocus.requestFocus() }
    }

    val scope = rememberCoroutineScope()
    val haptics = rememberAppHaptics()
    var checkingDuplicate by remember { mutableStateOf(false) }
    var duplicate by remember { mutableStateOf<DuplicateMatch?>(null) }
    var commitAfterCheck by remember { mutableStateOf(false) }

    fun commit(amt: Double) {
        viewModel.addTransaction(
            amount = amt,
            type = if (isExpense) "EXPENSE" else "INCOME",
            category = categorySelected,
            description = description,
            date = dateSelected,
            paymentMethod = paymentMethod,
            existing = editingTransaction,
            referenceKey = if (editingTransaction == null) prefill?.referenceKey else null
        )
        haptics.confirm()
        onSaved()
    }

    fun parsedAmount(): Double? = com.akshar.utils.MoneyUtils.parseAmount(amountStr)?.takeIf { it > 0 }

    fun save() {
        val amt = parsedAmount()
        if (amt == null) {
            amountError = "Please enter a valid amount greater than zero."
            haptics.reject()
            return
        }
        // Only imported entries are checked; hand-typed ones never nag.
        if (prefill == null || editingTransaction != null) {
            commit(amt)
            return
        }
        if (checkingDuplicate) return
        checkingDuplicate = true
        scope.launch {
            val candidate = DuplicateCandidate(
                amount = com.akshar.utils.MoneyUtils.roundMoney(amt),
                type = if (isExpense) "EXPENSE" else "INCOME",
                date = dateSelected,
                referenceKey = prefill.referenceKey
            )
            val match = runCatching { viewModel.findLikelyDuplicate(candidate) }.getOrNull()
            // Only state is written here: this may resume off the main thread, and saving navigates.
            if (match == null) commitAfterCheck = true else duplicate = match
            checkingDuplicate = false
        }
    }

    LaunchedEffect(commitAfterCheck) {
        if (!commitAfterCheck) return@LaunchedEffect
        commitAfterCheck = false
        parsedAmount()?.let { commit(it) }
    }

    duplicate?.let { match ->
        val existing = match.existing
        AlertDialog(
            onDismissRequest = { duplicate = null },
            title = { Text(if (match.sameReference) "Already saved" else "Possible duplicate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        if (match.sameReference) {
                            "A transaction with this bank reference number is already in your history."
                        } else {
                            "You already have ${if (existing.type == "EXPENSE") "an expense" else "an income"} " +
                                "of this amount on the same day."
                        }
                    )
                    GlassCard(contentPadding = PaddingValues(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            existing.description.ifBlank { existing.category },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "${viewModel.formatCurrencyValue(existing.amount)} · ${dateFormatter.format(Date(existing.date))} · ${existing.category}",
                            style = MaterialTheme.typography.bodySmall.money(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Save this one too only if it's a separate payment.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    duplicate = null
                    parsedAmount()?.let { commit(it) }
                }) { Text("Save anyway") }
            },
            dismissButton = {
                TextButton(onClick = { duplicate = null }) { Text("Cancel") }
            }
        )
    }

    val header: @Composable () -> Unit = {
        ScreenHeader(
            title = if (transactionId != null) "Edit transaction" else "Add transaction",
            subtitle = when {
                prefill == null -> null
                prefill.confident -> "From a bank message. Check it, then save."
                else -> "From a bank message. This one is a best guess, so check each field."
            },
            onBack = onNavigateBack
        )
    }

    @Composable
    fun OptionChip(label: String, selected: Boolean, fill: Color, textColor: Color, modifier: Modifier, onSelect: () -> Unit) {
        Box(
            modifier = modifier
                .heightIn(min = Spacing.touchTarget)
                .glassPanel(PillShape, glass, fill)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = if (largeText) Spacing.lg else 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                maxLines = if (largeText) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    /** Three equal columns normally; with large text, chips sized to their label that wrap. */
    @Composable
    fun <T> OptionGrid(options: List<T>, chip: @Composable (T, Modifier) -> Unit) {
        if (largeText) {
            WrapRow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                options.forEach { chip(it, Modifier.widthIn(min = 96.dp)) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(3).forEach { rowList ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowList.forEach { chip(it, Modifier.weight(1f)) }
                        // Fill structural blank spaces to preserve even alignment
                        repeat(3 - rowList.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }

    // --- Save button: pinned under the form so it's always reachable, even with the keyboard up.
    // On a short landscape screen it scrolls with the form instead (see compactHeight).
    @Composable
    fun SaveArea() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter, vertical = Spacing.md)
        ) {
            // The amount field may be scrolled off screen, so repeat its error beside the button.
            if (amountError != null) {
                Text(
                    text = amountError!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.sm)
                )
            }
            Button(
                onClick = { save() },
                enabled = !checkingDuplicate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = fillAccent,
                    contentColor = onFillAccent
                )
            ) {
                Text(
                    text = if (isExpense) "Save expense" else "Save income",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Edit mode: deleting lives here, behind a confirmation, instead of on every list row.
            val existing = editingTransaction
            if (existing != null) {
                var confirmDelete by remember { mutableStateOf(false) }
                Spacer(modifier = Modifier.height(Spacing.sm))
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.touchTarget),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete transaction", style = MaterialTheme.typography.labelLarge)
                }
                if (confirmDelete) {
                    AlertDialog(
                        onDismissRequest = { confirmDelete = false },
                        title = { Text("Delete this transaction?") },
                        text = { Text("It will be removed from your history and totals. This can't be undone.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    confirmDelete = false
                                    haptics.heavy()
                                    viewModel.deleteTransaction(existing)
                                    onNavigateBack()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("Delete") }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        if (!compactHeight) header()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (compactHeight) header()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.gutter)
                    .padding(bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {

                // --- Type Slider (Income vs Expense Toggle) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .glassPanel(PillShape, glass)
                        .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(PillShape)
                            .background(if (isExpense) fillAccent else Color.Transparent)
                            .selectable(selected = isExpense, role = Role.Tab) { isExpense = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isExpense) onFillAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(PillShape)
                            .background(if (!isExpense) fillAccent else Color.Transparent)
                            .selectable(selected = !isExpense, role = Role.Tab) { isExpense = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (!isExpense) onFillAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // --- Amount Field (Numeric with Validation) ---
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        amountError = null
                    },
                    label = { Text("Amount") },
                    placeholder = { Text("0", style = MaterialTheme.typography.displaySmall.money()) },
                    leadingIcon = {
                        Text(
                            text = currencySymbol,
                            style = MaterialTheme.typography.headlineMedium.money(),
                            color = accent,
                            modifier = Modifier.padding(start = Spacing.md)
                        )
                    },
                    isError = amountError != null,
                    supportingText = {
                        if (amountError != null) {
                            Text(text = amountError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Tip: you can type sums like 120+45")
                        }
                    },
                    // The phone pad has digits, "." and "+" (for sums like 120+45) and no word suggestions.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.displaySmall.money().copy(color = accent),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(amountFocus)
                )

                // --- Main Category Selector ---
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            
                OptionGrid(currentCategories) { category, chipModifier ->
                    val selected = category == categorySelected
                    OptionChip(
                        label = category,
                        selected = selected,
                        fill = if (selected) fillAccent else glass.fill,
                        textColor = if (selected) onFillAccent else MaterialTheme.colorScheme.onSurface,
                        modifier = chipModifier
                    ) {
                        categorySelected = category
                        categoryTouched = true
                        categoryFromMemory = false
                    }
                }

                // --- Description / Note Field ---
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. Lunch, salary, bills, fuel...") },
                    supportingText = if (categoryFromMemory) {
                        { Text("Category set from what you picked for this before.") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // --- Date Picker Dialog Access button ---
                GlassCard(
                    contentPadding = PaddingValues(0.dp),
                    onClickLabel = "Change date",
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = dateSelected }
                        DatePickerDialog(
                            context,
                            { _: DatePicker, y: Int, m: Int, d: Int ->
                                val selectedCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                dateSelected = selectedCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Date",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormatter.format(Date(dateSelected)),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = AkSpendTheme.emphasis
                                    )
                                )
                            }
                        }
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // --- Payment Method Selector (Horizontal List) ---
                Text(
                    text = "Paid with",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            
                OptionGrid(paymentMethods) { method, chipModifier ->
                    val isSelected = method == paymentMethod
                    OptionChip(
                        label = method,
                        selected = isSelected,
                        fill = if (isSelected) MaterialTheme.colorScheme.primary else glass.fill,
                        textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = chipModifier
                    ) { paymentMethod = method }
                }
            }
            if (compactHeight) SaveArea()
        }
        if (!compactHeight) SaveArea()
    }
}

