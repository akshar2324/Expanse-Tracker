package com.akshar.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akshar.alerts.BudgetAlertWorker
import com.akshar.ui.components.glassCardColors
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.viewmodel.BudgetAlertsViewModel

/** Permission is requested only from an explicit switch-on action. */
@Composable
fun BudgetAlertsSection(model: BudgetAlertsViewModel = viewModel()) {
    val context = LocalContext.current
    val enabled by model.enabled.collectAsStateWithLifecycle()
    var permissionDenied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val allowed = granted && BudgetAlertWorker.canNotify(context)
        model.setEnabled(allowed)
        permissionDenied = !allowed
    }
    Card(colors = glassCardColors(AkSpendTheme.glass.fillStrong), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Monthly budget alerts", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = enabled,
                    onCheckedChange = { value ->
                        permissionDenied = false
                        when {
                            !value -> model.setEnabled(false)
                            BudgetAlertWorker.canNotify(context) -> model.setEnabled(true)
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else -> permissionDenied = true
                        }
                    },
                    modifier = Modifier.semantics { contentDescription = "Monthly budget alerts" }
                )
            }
            Text("Notify at 80% and 100% of monthly category budgets, once per level each month. Checked daily and after ledger changes. Off by default.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (permissionDenied) Text("Notifications are blocked. Allow notifications for AkSpend in Android Settings, then turn this switch on.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
