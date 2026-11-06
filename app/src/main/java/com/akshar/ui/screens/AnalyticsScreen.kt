package com.akshar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.akshar.ui.components.EmptyState
import com.akshar.ui.components.GlassCard
import com.akshar.ui.components.GlassSegmentedControl
import com.akshar.ui.components.LocalFloatingBarInset
import com.akshar.ui.components.ScreenHeader
import com.akshar.ui.theme.*
import com.akshar.ui.viewmodel.FinanceViewModel
import com.akshar.ui.screens.analytics.AdaptiveAnalyticsCard
import com.akshar.ui.screens.analytics.CashFlowBarChart
import com.akshar.ui.screens.analytics.PieSliceChart
import com.akshar.ui.screens.analytics.TrendLineChart

private val analyticsTabs = listOf("EXPENSES" to "Spending", "INCOME" to "Income", "CASHFLOW" to "Cash flow")

@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val transactions = allTransactions.filter { it.transferId == null }
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.getCurrencySymbol()

    var selectedTab by rememberSaveable { mutableStateOf("EXPENSES") } // "EXPENSES", "INCOME", "CASHFLOW"

    val expenses = transactions.filter { it.type == "EXPENSE" }
    val incomes = transactions.filter { it.type == "INCOME" }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            bottom = LocalFloatingBarInset.current + Spacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item {
            ScreenHeader(
                title = "Insights",
                subtitle = "Where your money goes, worked out on this device",
                insetHorizontally = false
            )
        }
        item {
            GlassSegmentedControl(
                options = analyticsTabs.map { it.second },
                selectedIndex = analyticsTabs.indexOfFirst { it.first == selectedTab },
                onSelect = { selectedTab = analyticsTabs[it].first }
            )
        }

        if (transactions.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.BarChart,
                    title = "Charts appear as you log",
                    message = "Add a few expenses or income entries and your trends will show up here."
                )
            }
        } else {
            item {
                AdaptiveAnalyticsCard(transactions = transactions, currencySymbol = currencySymbol)
            }

            when (selectedTab) {
                "EXPENSES" -> {
                    item { ChartCard("Spending by category") { PieSliceChart(transactions = expenses) } }
                    item {
                        ChartCard("Last 7 days") {
                            TrendLineChart(transactions = expenses, lineColor = AkSpendTheme.finance.expense)
                        }
                    }
                }
                "INCOME" -> {
                    item { ChartCard("Income by source") { PieSliceChart(transactions = incomes) } }
                    item {
                        ChartCard("Last 7 days") {
                            TrendLineChart(transactions = incomes, lineColor = AkSpendTheme.finance.income)
                        }
                    }
                }
                "CASHFLOW" -> {
                    item { ChartCard("Money in vs out") { CashFlowBarChart(transactions = transactions) } }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        content()
    }
}
