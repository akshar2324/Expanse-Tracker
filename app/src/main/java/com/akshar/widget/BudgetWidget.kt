package com.akshar.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as GlanceColor
import com.akshar.AppNavigationRequests
import com.akshar.ExpenseTrackerApp
import com.akshar.MainActivity
import com.akshar.data.repository.FinanceRepository
import com.akshar.utils.CurrencyUtils
import com.akshar.ui.theme.DarkFinanceColors
import com.akshar.ui.theme.DarkOnSurface
import com.akshar.ui.theme.DarkOnSurfaceVariant
import com.akshar.ui.theme.DarkOutlineVariant
import com.akshar.ui.theme.DarkPrimary
import com.akshar.ui.theme.LightFinanceColors
import com.akshar.ui.theme.LightOnSurface
import com.akshar.ui.theme.LightOnSurfaceVariant
import com.akshar.ui.theme.LightOutlineVariant
import com.akshar.ui.theme.LightPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * This period's spending against budgets, in the AkSpend palette. It follows the system
 * light/dark setting and opens the Budgets tab when tapped.
 */
class BudgetWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val openBudgets = Intent(context, MainActivity::class.java)
            .putExtra(AppNavigationRequests.EXTRA_OPEN_TAB, AppNavigationRequests.TAB_BUDGETS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        // With the app lock on, the home screen must not show what the lock protects.
        if (BudgetWidgetModel.isAppLockOn(context)) {
            provideContent { LockedWidgetContent(actionIntent = openBudgets) }
            return
        }

        val repository = (context.applicationContext as ExpenseTrackerApp).repository
        val symbol = CurrencyUtils.symbolFor(CurrencyUtils.selectedCountry(context))
        val initial = withContext(Dispatchers.IO) {
            BudgetWidgetModel.build(repository.allBudgets.first(), repository.allTransactions.first())
        }

        provideContent {
            // Live while the widget session is open; BudgetWidgetUpdater covers later changes.
            val flow = remember { stateFlow(repository) }
            val state by flow.collectAsState(initial)
            BudgetWidgetContent(state, symbol, actionIntent = openBudgets)
        }
    }

    private fun stateFlow(repository: FinanceRepository) =
        combine(repository.allBudgets, repository.allTransactions) { budgets, transactions ->
            BudgetWidgetModel.build(budgets, transactions)
        }.flowOn(Dispatchers.Default)

    companion object {
        private val SMALL = DpSize(160.dp, 160.dp)
        private val TALL = DpSize(240.dp, 300.dp)
    }
}

private object WidgetColors {
    val panel = ColorProvider(day = Color(0xF2FFFCF9), night = Color(0xF2221E1B))
    val title = ColorProvider(day = LightPrimary, night = DarkPrimary)
    val text = ColorProvider(day = LightOnSurface, night = DarkOnSurface)
    val muted = ColorProvider(day = LightOnSurfaceVariant, night = DarkOnSurfaceVariant)
    val track = ColorProvider(day = LightOutlineVariant, night = DarkOutlineVariant)
    val bar = ColorProvider(day = LightPrimary, night = DarkPrimary)
    val warning = ColorProvider(day = LightFinanceColors.warning, night = DarkFinanceColors.warning)
    val over = ColorProvider(day = LightFinanceColors.expense, night = DarkFinanceColors.expense)

    /** Teal while on track, amber from 90 %, red once over. */
    fun forUsage(fraction: Float, isOver: Boolean): GlanceColor = when {
        isOver -> over
        fraction >= 0.9f -> warning
        else -> bar
    }
}

@Composable
private fun BudgetWidgetContent(state: BudgetWidgetState, symbol: String, actionIntent: Intent) {
    val money = { amount: Double -> BudgetWidgetModel.formatShort(amount, symbol) }
    val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
    val showRows = LocalSize.current.height >= 300.dp
    val description = if (state.hasBudgets) {
        "Budgets: ${money(state.totalSpent)} spent of ${money(state.totalLimit)}. Open budgets."
    } else {
        "No budgets set. Open budgets."
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(WidgetColors.panel)
            .padding(14.dp)
            .clickable(actionStartActivity(actionIntent))
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = "Budgets",
                style = TextStyle(color = WidgetColors.title, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(text = month, style = TextStyle(color = WidgetColors.muted, fontSize = 12.sp), maxLines = 1)
        }
        Spacer(GlanceModifier.height(8.dp))

        if (!state.hasBudgets) {
            Text(
                text = "No budgets yet",
                style = TextStyle(color = WidgetColors.text, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            )
            Text(text = "Tap to set one", style = TextStyle(color = WidgetColors.muted, fontSize = 12.sp))
            return@Column
        }

        Text(
            text = money(state.totalSpent),
            style = TextStyle(color = WidgetColors.text, fontWeight = FontWeight.Bold, fontSize = 20.sp),
            maxLines = 1
        )
        Text(
            text = "of ${money(state.totalLimit)}",
            style = TextStyle(color = WidgetColors.muted, fontSize = 12.sp),
            maxLines = 1
        )
        Spacer(GlanceModifier.height(6.dp))
        LinearProgressIndicator(
            progress = state.fraction,
            modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp),
            color = WidgetColors.forUsage(state.fraction, state.isOver),
            backgroundColor = WidgetColors.track
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = if (state.isOver) "${money(state.overBy)} over" else "${money(state.remaining)} left",
            style = TextStyle(
                color = if (state.isOver) WidgetColors.over else WidgetColors.muted,
                fontWeight = if (state.isOver) FontWeight.Medium else FontWeight.Normal,
                fontSize = 12.sp
            ),
            maxLines = 1
        )

        if (showRows) {
            state.rows.forEach { row ->
                Spacer(GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = if (row.weekly) "${row.label} · week" else row.label,
                        style = TextStyle(color = WidgetColors.text, fontSize = 12.sp),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = "${money(row.spent)} / ${money(row.limit)}",
                        style = TextStyle(color = if (row.isOver) WidgetColors.over else WidgetColors.muted, fontSize = 12.sp),
                        maxLines = 1
                    )
                }
                Spacer(GlanceModifier.height(3.dp))
                LinearProgressIndicator(
                    progress = row.fraction,
                    modifier = GlanceModifier.fillMaxWidth().height(5.dp).cornerRadius(3.dp),
                    color = WidgetColors.forUsage(row.fraction, row.isOver),
                    backgroundColor = WidgetColors.track
                )
            }
        }
    }
}

/** Shown instead of any amounts while the app lock is on. */
@Composable
private fun LockedWidgetContent(actionIntent: Intent) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(WidgetColors.panel)
            .padding(14.dp)
            .clickable(actionStartActivity(actionIntent))
            .semantics { contentDescription = "Budgets. Locked. Open AkSpend to see them." },
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "Budgets",
            style = TextStyle(color = WidgetColors.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        )
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = "Locked",
            style = TextStyle(color = WidgetColors.text, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        )
        Text(
            text = "Amounts are hidden while the app lock is on. Tap to open.",
            style = TextStyle(color = WidgetColors.muted, fontSize = 12.sp)
        )
    }
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}
