package com.akshar.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshar.ui.theme.ExpenseRed
import com.akshar.ui.theme.IncomeGreen
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

    val paymentMethods = remember {
        listOf("Cash", "UPI", "Credit Card", "Debit Card", "Bank Transfer", "Wallet")
    }

    // Date formatting helper
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Error states
    var amountError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- Type Slider (Income vs Expense Toggle) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isExpense) ExpenseRed else Color.Transparent)
                        .clickable { isExpense = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isExpense) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (!isExpense) IncomeGreen else Color.Transparent)
                        .clickable { isExpense = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (!isExpense) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // --- Amount Field (Numeric with Validation) ---
            OutlinedTextField(
                value = amountStr,
                onValueChange = {
                    amountStr = it
                    amountError = null
                },
                label = { Text("Amount (₹)") },
                placeholder = { Text("0.00") },
                isError = amountError != null,
                supportingText = {
                    if (amountError != null) {
                        Text(text = amountError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // --- Main Category Selector ---
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
                            val chipColor = if (selected) {
                                if (isExpense) ExpenseRed else IncomeGreen
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                            val textColor = if (selected) {
                                MaterialTheme.colorScheme.onSecondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(chipColor)
                                    .clickable { categorySelected = category },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    ),
                                    maxLines = 1
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
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("e.g. Lunch, salary, bills, fuel...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // --- Date Picker Dialog Access button ---
            OutlinedCard(
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
                            contentDescription = "Calendar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Transaction Date",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
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
                        text = "CHANGE",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // --- Payment Method Selector (Horizontal List) ---
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                paymentMethods.chunked(3).forEach { rowMethods ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMethods.forEach { method ->
                            val isSelected = method == paymentMethod
                            val surface = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            val stroke = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(surface)
                                    .clickable { paymentMethod = method }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = method,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1
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
                    val amt = amountStr.toDoubleOrNull()
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
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isExpense) ExpenseRed else IncomeGreen
                )
            ) {
                Text(
                    text = "Save Transaction",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                )
            }
        }
    }
}
