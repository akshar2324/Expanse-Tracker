package com.akshar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.akshar.sms.BankSmsImport
import com.akshar.sms.SmsDraft
import com.akshar.sms.SmsImportResult
import com.akshar.ui.components.GlassCard
import com.akshar.ui.components.ScreenHeader
import com.akshar.ui.components.glassTextFieldColors
import com.akshar.ui.components.signedMoney
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.theme.Spacing
import com.akshar.ui.theme.money
import com.akshar.utils.CategoryMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns a pasted or shared bank SMS into a draft transaction. The message is read on the device
 * and never stored; only the transaction the user saves on the next screen is kept.
 */
@Composable
fun SmsImportScreen(
    currencySymbol: String,
    onBack: () -> Unit,
    onReview: (SmsDraft) -> Unit,
    loadCategoryMemory: suspend () -> CategoryMemory = { CategoryMemory.EMPTY },
    sharedText: String? = null,
    savedOne: Boolean = false,
    onSavedShown: () -> Unit = {},
) {
    val clipboard = LocalClipboardManager.current
    var message by rememberSaveable { mutableStateOf(sharedText.orEmpty()) }
    // The text that was last read. Kept separately so editing the field clears a stale result.
    var readText by rememberSaveable { mutableStateOf(sharedText?.takeIf { it.isNotBlank() }) }
    var justSaved by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(savedOne) {
        if (savedOne) {
            message = ""
            readText = null
            justSaved = true
            onSavedShown()
        }
    }

    val result by produceState<SmsImportResult?>(initialValue = null, readText) {
        val text = readText
        value = if (text == null) null else {
            // Categories the user picked before beat the built-in keyword hints.
            val memory = runCatching { loadCategoryMemory() }.getOrDefault(CategoryMemory.EMPTY)
            withContext(Dispatchers.Default) { BankSmsImport.parse(text, memory = memory) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader(
            title = "Import bank SMS",
            subtitle = "Read on this device. The message itself isn't saved.",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            if (!BankSmsImport.isSupported) {
                Notice("Reading bank messages needs Android 8.0 or newer. You can still add transactions by hand.")
                return@Column
            }

            if (justSaved) {
                Notice("Saved. Paste or share the next message to keep going.")
            }

            OutlinedTextField(
                value = message,
                onValueChange = {
                    message = it
                    justSaved = false
                    if (it != readText) readText = null
                },
                label = { Text("Bank message") },
                placeholder = { Text("Sent Rs.250.00 From HDFC Bank A/C *1234 To SWIGGY On 05/01/26 Ref 4012...") },
                minLines = 4,
                maxLines = 10,
                colors = glassTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = {
                    clipboard.getText()?.text?.takeIf { it.isNotBlank() }?.let {
                        message = it
                        justSaved = false
                        readText = it
                    }
                }) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Paste")
                }
                Button(
                    onClick = {
                        justSaved = false
                        readText = message
                    },
                    enabled = message.isNotBlank() && readText != message
                ) {
                    Text("Read message")
                }
            }

            when (val r = result) {
                null -> Notice(
                    "Tip: in your messages app, long-press a bank SMS, tap Share and choose AkSpend. " +
                        "Works best with HDFC, ICICI, SBI, Axis and SBI Card; other banks are a best guess."
                )
                is SmsImportResult.Found -> DraftCard(r.draft, currencySymbol, onReview = { onReview(r.draft) })
                is SmsImportResult.Skipped -> Notice(r.reason, live = true)
                SmsImportResult.NotFound -> Notice(
                    "No completed payment found. Check that you pasted the whole bank message.",
                    live = true
                )
                SmsImportResult.Unsupported -> Notice("Reading bank messages needs Android 8.0 or newer.", live = true)
            }
        }
    }
}

@Composable
private fun DraftCard(draft: SmsDraft, currencySymbol: String, onReview: () -> Unit) {
    val finance = AkSpendTheme.finance
    val accent = if (draft.isExpense) finance.expense else finance.income
    val dateText = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(draft.dateMillis))
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentPadding = PaddingValues(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            if (draft.isExpense) "Expense found" else "Income found",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            signedMoney(draft.amount.toDouble(), positive = !draft.isExpense, currencySymbol = currencySymbol, decimals = 2),
            style = MaterialTheme.typography.headlineMedium.money(),
            color = accent
        )
        Text(draft.description, style = MaterialTheme.typography.titleMedium)
        val details = listOfNotNull(
            dateText,
            draft.category,
            draft.paymentMethod,
            draft.accountTail?.let { "A/c ending $it" },
        ).joinToString(" · ")
        Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!draft.confident) {
            Text(
                "This bank's format isn't known yet, so this is a best guess. Check the details.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.width(Spacing.xs))
        Button(onClick = onReview, modifier = Modifier.fillMaxWidth()) {
            Text("Review and save")
        }
    }
}

@Composable
private fun Notice(text: String, live: Boolean = false) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (live) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
        contentPadding = PaddingValues(Spacing.lg)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
