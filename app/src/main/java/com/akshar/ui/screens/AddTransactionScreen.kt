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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshar.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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

    // Sync category reset if type swaps
    LaunchedEffect(isExpense) {
        categorySelected = currentCategories.first()
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader(title = "Add transaction", onBack = onNavigateBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.xxl),
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
                label = { Text("Amount ($currencySymbol)") },
                placeholder = { Text("0.00") },
                isError = amountError != null,
                supportingText = {
                    if (amountError != null) {
                        Text(text = amountError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.money(),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
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

            Spacer(modifier = Modifier.height(16.dp))

            // --- Save Button ---
            Button(
                onClick = {
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
                            paymentMethod = paymentMethod
                        )
                        onNavigateBack()
                    }
                },
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
