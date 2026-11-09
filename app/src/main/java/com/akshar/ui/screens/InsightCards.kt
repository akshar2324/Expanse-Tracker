package com.akshar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import com.akshar.ui.components.HorizontalDivider
import com.akshar.ui.components.TextButton
import com.akshar.ui.components.FilledTonalButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akshar.ui.viewmodel.InsightsViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.akshar.insights.InsightFormat
import com.akshar.insights.MonthEndForecast
import com.akshar.insights.SubscriptionCandidate
import com.akshar.ui.components.*
import com.akshar.ui.theme.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Bind the insights to the current application instead of a cached global Android factory. */
@Composable
internal fun rememberInsightsViewModel(): InsightsViewModel {
    val application = LocalContext.current.applicationContext as Application
    val factory = remember(application) { ViewModelProvider.AndroidViewModelFactory(application) }
    return viewModel(factory = factory)
}

/** Money with two decimals and an explicit sign, for breakdown lines. */
private fun exactMoney(value: BigDecimal, currencySymbol: String, sign: Boolean = false): String {
    val body = currencySymbol + String.format(Locale.getDefault(), "%,.2f", value.abs())
    return when {
        value.signum() == 0 -> body
        sign && value.signum() > 0 -> "+$body"
        value.signum() < 0 -> "-$body"
        else -> body
    }
}

private fun shortDate(ms: Long): String = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ms))

/** Headline for a ready forecast: "About ₹12,400 left on 30 Nov" or "About ₹2,300 short by 30 Nov". */
fun forecastHeadline(forecast: MonthEndForecast.Ready, currencySymbol: String): String {
    val rounded = InsightFormat.approximate(forecast.projectedBalance)
    val amount = InsightFormat.wholeMoney(rounded, currencySymbol)
    val date = shortDate(forecast.monthEndDayMs)
    return if (forecast.isShortfall) "About $amount short by $date" else "About $amount left on $date"
}

/**
 * Month-end forecast card. Shows a short "not enough data" note for a brand-new user
 * until there is enough history; otherwise a one-line projection that expands into the breakdown.
 */
@Composable
fun MonthEndForecastCard(
    forecast: MonthEndForecast,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    when (forecast) {
        MonthEndForecast.NoData -> Unit
        is MonthEndForecast.NotEnoughData -> GlassCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = Icons.Rounded.Insights, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 40.dp)
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text("MONTH-END FORECAST", style = MaterialTheme.typography.labelSmall.eyebrow(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Not enough data yet", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        if (forecast.moreDaysNeeded > 0) {
                            "Keep logging for about ${forecast.moreDaysNeeded} more ${if (forecast.moreDaysNeeded == 1) "day" else "days"} to see where the month ends."
                        } else {
                            "Log a few more everyday expenses to see where the month ends."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is MonthEndForecast.Ready -> ReadyForecastCard(forecast, currencySymbol, modifier, initiallyExpanded)
    }
}

@Composable
private fun ReadyForecastCard(
    forecast: MonthEndForecast.Ready,
    currencySymbol: String,
    modifier: Modifier,
    initiallyExpanded: Boolean,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val finance = AkSpendTheme.finance
    val headline = forecastHeadline(forecast, currencySymbol)
    val tint = if (forecast.isShortfall) finance.expense else finance.income

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = if (expanded) "Breakdown shown" else "Breakdown hidden" },
        onClick = { expanded = !expanded },
        onClickLabel = if (expanded) "Hide forecast breakdown" else "Show forecast breakdown",
        contentPadding = PaddingValues(Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Rounded.Insights, tint = tint, size = 44.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("MONTH-END FORECAST", style = MaterialTheme.typography.labelSmall.eyebrow(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (forecast.isShortfall) finance.expense else MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(motionSpec()) + expandVertically(motionSpec()),
            exit = fadeOut(motionSpec()) + shrinkVertically(motionSpec())
        ) {
            Column(Modifier.padding(top = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                BreakdownLine("Balance now", exactMoney(forecast.currentBalance, currencySymbol), MaterialTheme.colorScheme.onSurface)
                if (forecast.upcomingIncome.signum() > 0) {
                    BreakdownLine("Expected income", exactMoney(forecast.upcomingIncome, currencySymbol, sign = true), finance.income)
                }
                BreakdownLine(
                    "Bills and reminders due",
                    exactMoney(forecast.upcomingBills.negate(), currencySymbol),
                    if (forecast.upcomingBills.signum() > 0) finance.expense else MaterialTheme.colorScheme.onSurfaceVariant
                )
                BreakdownLine(
                    "Everyday spending remaining",
                    exactMoney(forecast.expectedVariableSpend.negate(), currencySymbol),
                    if (forecast.expectedVariableSpend.signum() > 0) finance.expense else MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                BreakdownLine("Projected on ${shortDate(forecast.monthEndDayMs)}", exactMoney(forecast.projectedBalance, currencySymbol), tint, emphasised = true)
                Text(
                    buildString {
                        append("Daily spending averages ${exactMoney(forecast.dailyVariableSpend, currencySymbol)} over the last ${forecast.basedOnDays} days")
                        if (forecast.excludedLargeCount > 0) {
                            append(", leaving out ${forecast.excludedLargeCount} large one-off ")
                            append(if (forecast.excludedLargeCount == 1) "payment" else "payments")
                            append(" (${exactMoney(forecast.excludedLargeTotal, currencySymbol)}) such as rent")
                        }
                        append(". Includes only today’s spending still expected. Bills come from reminders and future entries. Estimates can change.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BreakdownLine(label: String, value: String, valueColor: Color, emphasised: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = if (emphasised) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = if (emphasised) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            value,
            style = (if (emphasised) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium).money(),
            color = valueColor,
            maxLines = 1
        )
    }
}

/** Detected repeating payments with one-tap reminder creation and a remembered dismiss. */
@Composable
fun SubscriptionSuggestions(
    candidates: List<SubscriptionCandidate>,
    currencySymbol: String,
    onMakeReminder: (SubscriptionCandidate) -> Unit,
    onDismiss: (SubscriptionCandidate) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (candidates.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Possible subscriptions · ${candidates.size}")
        Text(
            "These repeat on a schedule. Make a reminder to include them in your forecast.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.sm))
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xs)) {
            candidates.forEachIndexed { index, candidate ->
                if (index > 0) GlassDivider()
                SubscriptionRow(candidate, currencySymbol, onMakeReminder, onDismiss)
            }
        }
    }
}

@Composable
private fun SubscriptionRow(
    candidate: SubscriptionCandidate,
    currencySymbol: String,
    onMakeReminder: (SubscriptionCandidate) -> Unit,
    onDismiss: (SubscriptionCandidate) -> Unit,
) {
    val amountText = currencySymbol + String.format(Locale.getDefault(), "%,.2f", candidate.amount)
    val next = shortDate(candidate.nextExpectedMs)
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md)
                .semantics(mergeDescendants = true) {
                    contentDescription = "${candidate.displayName}, $amountText ${candidate.frequency.label.lowercase()}, next expected $next, seen ${candidate.occurrences} times"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon = Icons.Rounded.Autorenew, tint = categoryTint(candidate.category))
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(candidate.displayName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${candidate.frequency.label} · next about $next · seen ${candidate.occurrences}×",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Text(amountText, style = MaterialTheme.typography.titleSmall.money(), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.lg, end = Spacing.sm, bottom = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            FilledTonalButton(
                onClick = { onMakeReminder(candidate) },
                shape = PillShape,
                modifier = Modifier
                    .heightIn(min = Spacing.touchTarget)
                    .semantics { contentDescription = "Make a reminder for ${candidate.displayName}" }
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Make a reminder", style = MaterialTheme.typography.labelLarge)
            }
            TextButton(
                onClick = { onDismiss(candidate) },
                modifier = Modifier
                    .heightIn(min = Spacing.touchTarget)
                    .semantics { contentDescription = "${candidate.displayName} is not a subscription" }
            ) {
                Text("Not a subscription", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
