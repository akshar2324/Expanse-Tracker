package com.akshar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akshar.data.model.MerchantCategory
import com.akshar.ui.components.ConfirmDeleteDialog
import com.akshar.ui.components.EmptyState
import com.akshar.ui.components.GlassCard
import com.akshar.ui.components.GlassListRow
import com.akshar.ui.components.IconBadge
import com.akshar.ui.components.ScreenHeader
import com.akshar.ui.components.categoryIcon
import com.akshar.ui.components.categoryTint
import com.akshar.ui.components.rememberConfirmDeleteState
import com.akshar.ui.theme.Spacing
import com.akshar.ui.viewmodel.FinanceViewModel

/**
 * Lists the merchant → category mappings learned from saved transactions, with a way to forget
 * one or all of them. They are stored only on this device.
 */
@Composable
fun LearnedCategoriesScreen(viewModel: FinanceViewModel, onBack: () -> Unit) {
    val mappings by viewModel.learnedCategories.collectAsStateWithLifecycle()
    LearnedCategoriesContent(
        mappings = mappings,
        onBack = onBack,
        onForget = viewModel::forgetLearnedCategory,
        onClearAll = viewModel::clearLearnedCategories,
    )
}

@Composable
internal fun LearnedCategoriesContent(
    mappings: List<MerchantCategory>,
    onBack: () -> Unit,
    onForget: (MerchantCategory) -> Unit,
    onClearAll: () -> Unit,
) {
    val confirm = rememberConfirmDeleteState()
    ConfirmDeleteDialog(confirm)

    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        ScreenHeader(
            title = "Learned categories",
            subtitle = "When you pick a category for a shop or payee, it's suggested next time. Kept on this device.",
            onBack = onBack,
            actions = if (mappings.isEmpty()) null else {
                {
                    TextButton(onClick = {
                        confirm.request(
                            title = "Forget all ${mappings.size} learned categories?",
                            message = "Your transactions stay as they are. New ones go back to the built-in guesses.",
                            onConfirm = onClearAll
                        )
                    }) { Text("Clear all") }
                }
            }
        )
        if (mappings.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Psychology,
                title = "Nothing learned yet",
                message = "Save a transaction with a shop or payee in the description, and the category you pick is remembered for next time.",
                modifier = Modifier.padding(horizontal = Spacing.gutter)
            )
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            items(mappings, key = { "${it.type}:${it.merchantKey}" }) { mapping ->
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    GlassListRow(
                        title = mapping.displayName,
                        subtitle = "${if (mapping.type == "EXPENSE") "Expense" else "Income"} → ${mapping.category}",
                        leading = { IconBadge(icon = categoryIcon(mapping.category), tint = categoryTint(mapping.category)) },
                        trailing = {
                            IconButton(onClick = {
                                confirm.request(
                                    title = "Forget ${mapping.displayName}?",
                                    message = "It won't be filled in as ${mapping.category} any more.",
                                    onConfirm = { onForget(mapping) }
                                )
                            }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Forget ${mapping.displayName}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
