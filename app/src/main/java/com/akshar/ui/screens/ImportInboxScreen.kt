package com.akshar.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.Inbox
import com.akshar.ui.components.Button
import androidx.compose.material3.MaterialTheme
import com.akshar.ui.components.Switch
import androidx.compose.material3.Text
import com.akshar.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.akshar.sms.BankSmsImport
import com.akshar.sms.inbox.AutoImportSettings
import com.akshar.sms.inbox.ImportInboxViewModel
import com.akshar.sms.inbox.PendingImport
import com.akshar.ui.components.EmptyState
import com.akshar.ui.components.GlassCard
import com.akshar.ui.components.ScreenHeader
import com.akshar.ui.components.signedMoney
import com.akshar.ui.components.rememberConfirmDeleteState
import com.akshar.ui.components.ConfirmDeleteDialog
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.theme.Spacing
import com.akshar.ui.theme.money
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Payments picked up automatically from bank, UPI and messaging notifications. Each one is
 * reviewed in the normal add form before it becomes a transaction.
 */
@Composable
fun ImportInboxScreen(
    viewModel: ImportInboxViewModel,
    currencySymbol: String,
    onBack: () -> Unit,
    onReview: (PendingImport) -> Unit,
) {
    val context = LocalContext.current
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    var hasAccess by remember { mutableStateOf(AutoImportSettings.hasAccess(context)) }
    var enabled by remember { mutableStateOf(AutoImportSettings.isEnabled(context)) }
    val confirm = rememberConfirmDeleteState()

    // The grant happens in system settings; re-check whenever the user comes back.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            hasAccess = AutoImportSettings.hasAccess(context)
        }
    }

    fun openAccessSettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    ConfirmDeleteDialog(confirm)

    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        ScreenHeader(
            title = "To review",
            subtitle = "Payments read from your notifications, on this device",
            onBack = onBack,
            actions = if (pending.size > 1) {
                {
                    TextButton(onClick = {
                        confirm.request(
                            title = "Dismiss all ${pending.size}?",
                            message = "They won't be added. New payments will still show up here.",
                            onConfirm = viewModel::dismissAll
                        )
                    }) { Text("Dismiss all") }
                }
            } else null
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item(key = "setup") {
                AutoImportSetupCard(
                    supported = BankSmsImport.isSupported,
                    hasAccess = hasAccess,
                    enabled = enabled,
                    onGrant = ::openAccessSettings,
                    onToggle = {
                        enabled = it
                        AutoImportSettings.setEnabled(context, it)
                    }
                )
            }
            if (pending.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = androidx.compose.material.icons.Icons.Rounded.Inbox,
                        title = "Nothing to review",
                        message = if (hasAccess && enabled) "New bank and UPI payments will appear here."
                        else "Turn on auto-import above, or share a bank SMS to AkSpend."
                    )
                }
            } else {
                items(pending, key = { it.id }) { item ->
                    PendingRow(
                        item = item,
                        currencySymbol = currencySymbol,
                        onReview = { onReview(item) },
                        onDismiss = { viewModel.dismiss(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoImportSetupCard(
    supported: Boolean,
    hasAccess: Boolean,
    enabled: Boolean,
    onGrant: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        when {
            !supported -> Text(
                "Auto-import needs Android 8.0 or newer.",
                style = MaterialTheme.typography.bodyMedium
            )
            !hasAccess -> {
                Text("Auto-import payments", style = MaterialTheme.typography.titleMedium)
                Text(
                    "AkSpend can read payment notifications from your messages, bank and UPI apps and " +
                        "list them here for you to confirm. It only looks at those apps, keeps no message " +
                        "text, and nothing leaves your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) { Text("Allow notification access") }
            }
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Auto-import", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (enabled) "On. Watching messages, bank and UPI apps." else "Paused. Nothing new is picked up.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics { contentDescription = "Auto-import" }
                )
            }
        }
    }
}

@Composable
private fun PendingRow(item: PendingImport, currencySymbol: String, onReview: () -> Unit, onDismiss: () -> Unit) {
    val finance = AkSpendTheme.finance
    val date = remember(item.dateMillis) { SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(item.dateMillis)) }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.description, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    listOfNotNull(date, item.category, item.source).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                signedMoney(item.amount.toDouble(), positive = !item.isExpense, currencySymbol = currencySymbol, decimals = 2),
                style = MaterialTheme.typography.titleMedium.money(),
                color = if (item.isExpense) finance.expense else finance.income
            )
        }
        if (!item.confident) {
            Text(
                "Best guess. Check the details.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.padding(top = Spacing.xs)) {
            Button(onClick = onReview) { Text("Review") }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}
