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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshar.data.model.Transaction
import com.akshar.ui.viewmodel.FinanceViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    transactionId: Long? = null // non-null opens the screen in edit mode
) {
    val context = LocalContext.current
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    // --- State Holding ---
    var isExpense by remember { mutableStateOf(true) }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateSelected by remember { mutableStateOf(System.currentTimeMillis()) }
    var paymentMethod by remember { mutableStateOf("Cash") }

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
    var categorySelected by remember { mutableStateOf(currentCategories.first()) }

    // Sync category reset if type swaps (keeps a valid or originally loaded category)
    LaunchedEffect(isExpense) {
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
        isExpense = tx.type == "EXPENSE"
        amountStr = BigDecimal(tx.amount.toString()).stripTrailingZeros().toPlainString()
        description = tx.description
        dateSelected = tx.date
        categorySelected = tx.category
        paymentMethod = tx.paymentMethod
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
    val glass = AkSpendTheme.glass

    val amountFocus = remember { FocusRequester() }
    // New entries start with the keyboard on the amount: it's the one field every entry needs.
    LaunchedEffect(Unit) {
        if (transactionId == null) runCatching { amountFocus.requestFocus() }
    }

    fun save() {
        val amt = com.akshar.utils.MathUtils.evaluateMathExpression(amountStr)
        if (amt == null || amt <= 0) {
            amountError = "Please enter a valid amount greater than zero."
        } else {
            viewModel.addTransaction(
                amount = amt,
                type = if (isExpense) "EXPENSE" else "INCOME",
                category = categorySelected,
                description = description,
                date = dateSelected,
                paymentMethod = paymentMethod,
                existing = editingTransaction
            )
            onNavigateBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader(title = if (transactionId != null) "Edit transaction" else "Add transaction", onBack = onNavigateBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
                        .background(if (isExpense) AkSpendTheme.finance.expense else Color.Transparent)
                        .selectable(selected = isExpense, role = Role.Tab) { isExpense = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isExpense) onAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(PillShape)
                        .background(if (!isExpense) AkSpendTheme.finance.income else Color.Transparent)
                        .selectable(selected = !isExpense, role = Role.Tab) { isExpense = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (!isExpense) onAccent else MaterialTheme.colorScheme.onSurfaceVariant
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
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
            
            // FlowRow equivalent or Column listing in grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Group categories by 3 in a Row for visual grid layout
                currentCategories.chunked(3).forEach { rowList ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowList.forEach { category ->
                            val selected = category == categorySelected
                            val chipColor = if (selected) accent else glass.fill
                            val textColor = if (selected) onAccent else MaterialTheme.colorScheme.onSurface

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(Spacing.touchTarget)
                                    .glassPanel(PillShape, glass, chipColor)
                                    .selectable(selected = selected, role = Role.RadioButton) { categorySelected = category },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        // Fill structural blank spaces to preserve even alignment
                        val blanks = 3 - rowList.size
                        if (blanks > 0) {
                            repeat(blanks) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // --- Description / Note Field ---
            OutlinedTextField(
                colors = glassTextFieldColors(),
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("e.g. Lunch, salary, bills, fuel...") },
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
                                    fontWeight = FontWeight.Bold
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
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                paymentMethods.chunked(3).forEach { rowMethods ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMethods.forEach { method ->
                            val isSelected = method == paymentMethod
                            val surface = if (isSelected) MaterialTheme.colorScheme.primary else glass.fill

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(Spacing.touchTarget)
                                    .glassPanel(PillShape, glass, surface)
                                    .selectable(selected = isSelected, role = Role.RadioButton) { paymentMethod = method }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = method,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

        }

        // --- Save button: pinned under the form so it's always reachable, even with the keyboard up ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter, vertical = Spacing.md)
        ) {
            Button(
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = onAccent
                )
            ) {
                Text(
                    text = if (isExpense) "Save expense" else "Save income",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

