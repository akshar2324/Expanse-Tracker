package com.akshar.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akshar.data.backup.AutoBackupFrequency
import com.akshar.data.backup.AutoBackupRotation
import com.akshar.data.backup.AutoBackupSettings
import com.akshar.ui.components.GlassSegmentedControl
import com.akshar.ui.components.glassCardColors
import com.akshar.ui.components.glassTextFieldColors
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.viewmodel.AutoBackupViewModel
import com.akshar.ui.viewmodel.FinanceViewModel
import com.akshar.ui.viewmodel.JsonRestoreState
import com.akshar.utils.countLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class LocalBackupFolderPicker : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: Context, input: Uri?): Intent =
        super.createIntent(context, input).putExtra(Intent.EXTRA_LOCAL_ONLY, true)
}

private const val MIN_PASSWORD_LENGTH = 8

/** Scheduled backups and JSON restore, shown in More → Backup & restore. */
@Composable
fun AutoBackupSection(financeViewModel: FinanceViewModel, autoBackup: AutoBackupViewModel = viewModel()) {
    val settings by autoBackup.settings.collectAsStateWithLifecycle()
    val folderAccessible by autoBackup.folderAccessible.collectAsStateWithLifecycle()
    // The user can remove folder access in system settings while the app is in the background.
    LifecycleResumeEffect(Unit) {
        autoBackup.refresh()
        onPauseOrDispose { }
    }

    AutoBackupCard(settings, folderAccessible, autoBackup)
    JsonRestoreCard(financeViewModel, autoBackup)
}

@Composable
private fun AutoBackupCard(
    settings: AutoBackupSettings,
    folderAccessible: Boolean?,
    autoBackup: AutoBackupViewModel
) {
    val context = LocalContext.current
    var enableAfterPick by rememberSaveable { mutableStateOf(false) }
    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(remember { LocalBackupFolderPicker() }) { uri ->
        if (uri != null) autoBackup.chooseFolder(uri, enable = enableAfterPick)
        enableAfterPick = false
    }
    val pickFolder: (Boolean) -> Unit = { enable ->
        enableAfterPick = enable
        try {
            folderPicker.launch(null)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No folder picker is available on this device.", Toast.LENGTH_LONG).show()
        }
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = glassCardColors(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Automatic backups", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Saves a backup to a folder on this phone. Nothing is uploaded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { on ->
                        if (on && settings.folderUri == null) pickFolder(true) else autoBackup.setEnabled(on)
                    },
                    modifier = Modifier.semantics { contentDescription = "Automatic backups" }
                )
            }

            BackupStatus(settings, folderAccessible)

            if (settings.enabled || settings.folderUri != null) {
                if (settings.enabled) {
                    Text("How often", style = MaterialTheme.typography.labelLarge)
                    val frequencies = AutoBackupFrequency.entries
                    GlassSegmentedControl(
                        options = frequencies.map { it.label },
                        selectedIndex = frequencies.indexOf(settings.frequency),
                        onSelect = { autoBackup.setFrequency(frequencies[it]) }
                    )
                } else {
                    // A folder is chosen but the schedule is off: say so, rather than showing a
                    // schedule that looks active. The options below still apply to "Back up now".
                    Text(
                        "Automatic backups are off. You can still back up to this folder with Back up now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Keep the newest", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Older automatic backups are deleted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { autoBackup.setKeepCount(settings.keepCount - 1) },
                        enabled = settings.keepCount > AutoBackupRotation.MIN_KEEP
                    ) { Icon(Icons.Default.Remove, contentDescription = "Keep fewer backups") }
                    Text(
                        "${settings.keepCount}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { stateDescription = "${settings.keepCount} backups kept" }
                    )
                    IconButton(
                        onClick = { autoBackup.setKeepCount(settings.keepCount + 1) },
                        enabled = settings.keepCount < AutoBackupRotation.MAX_KEEP
                    ) { Icon(Icons.Default.Add, contentDescription = "Keep more backups") }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Password protect", style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (settings.encrypt) "Backups are encrypted. You need the password to restore them."
                            else "Encrypt backups with a password you choose.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.encrypt,
                        onCheckedChange = { on -> if (on) showPasswordDialog = true else autoBackup.turnOffEncryption() },
                        modifier = Modifier.semantics { contentDescription = "Password protect backups" }
                    )
                }
                if (settings.encrypt) {
                    TextButton(onClick = { showPasswordDialog = true }) { Text("Change password") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickFolder(false) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (settings.folderUri == null) "Choose folder" else "Change folder")
                    }
                    Button(
                        onClick = {
                            autoBackup.backUpNow()
                            Toast.makeText(context, "Backing up…", Toast.LENGTH_SHORT).show()
                        },
                        enabled = settings.folderUri != null && folderAccessible != false,
                        modifier = Modifier.weight(1f)
                    ) { Text("Back up now") }
                }
            }
        }
    }

    if (showPasswordDialog) {
        SetBackupPasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { password ->
                autoBackup.setPassword(password)
                showPasswordDialog = false
            }
        )
    }
}

@Composable
private fun BackupStatus(settings: AutoBackupSettings, folderAccessible: Boolean?) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val error = AkSpendTheme.finance.expense
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (settings.folderUri != null && folderAccessible == false) {
            Text(
                "AkSpend lost access to the backup folder. Choose the folder again to keep backing up.",
                style = MaterialTheme.typography.bodySmall,
                color = error
            )
        }
        Text(
            if (settings.lastSuccessAt > 0) "Last backup: ${relative(settings.lastSuccessAt)}" else "No automatic backup yet",
            style = MaterialTheme.typography.bodySmall,
            color = muted
        )
        settings.lastError?.let { message ->
            Text(
                "Last problem (${relative(settings.lastErrorAt)}): $message",
                style = MaterialTheme.typography.bodySmall,
                color = error
            )
        }
    }
}

private fun relative(time: Long): String = relativeBackupTime(time, System.currentTimeMillis())

/** "Just now" for the last minute (DateUtils would say "0 minutes ago"), then "5 minutes ago" and so on. */
internal fun relativeBackupTime(time: Long, now: Long): String =
    if (now - time in 0 until DateUtils.MINUTE_IN_MILLIS) "Just now"
    else DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS).toString()

internal fun restoreConfirmation(transactionCount: Int, accountCount: Int): String {
    val contents = buildList {
        add(countLabel(transactionCount, "transaction"))
        if (accountCount > 0) add(countLabel(accountCount, "account"))
    }.joinToString(" and ")
    return "This backup has $contents. Everything in the app now, including budgets, goals and " +
        "reminders, will be replaced by it. This can't be undone."
}

@Composable
private fun SetBackupPasswordDialog(onDismiss: () -> Unit, onConfirm: (CharArray) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val tooShort = password.length < MIN_PASSWORD_LENGTH
    val mismatch = confirm.isNotEmpty() && confirm != password
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "There is no way to recover this password. Without it, encrypted backups can't be restored.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (at least $MIN_PASSWORD_LENGTH characters)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = glassTextFieldColors()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Repeat password") },
                    singleLine = true,
                    isError = mismatch,
                    supportingText = if (mismatch) ({ Text("Passwords don't match") }) else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = glassTextFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password.toCharArray()) },
                enabled = !tooShort && confirm == password
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun JsonRestoreCard(financeViewModel: FinanceViewModel, autoBackup: AutoBackupViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val restoreState by autoBackup.restoreState.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) autoBackup.openRestoreFile(uri)
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = glassCardColors(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Restore an automatic backup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(
                "Pick an akspend-auto .json file. Password protected backups ask for their password. This replaces everything currently in the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    try {
                        picker.launch("*/*")
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "No file picker is available on this device.", Toast.LENGTH_LONG).show()
                    }
                },
                enabled = restoreState !is JsonRestoreState.Working,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.outlineVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (restoreState is JsonRestoreState.Working) {
                    CircularProgressIndicator(Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Restore, contentDescription = null)
                }
                Spacer(Modifier.width(10.dp))
                Text("Choose .json backup", fontWeight = FontWeight.Bold)
            }
        }
    }

    when (val state = restoreState) {
        is JsonRestoreState.NeedsPassword -> RestorePasswordDialog(
            wrongPassword = state.wrongPassword,
            onDismiss = autoBackup::clearRestore,
            onSubmit = autoBackup::submitRestorePassword
        )
        is JsonRestoreState.Ready -> AlertDialog(
            onDismissRequest = autoBackup::clearRestore,
            title = { Text("Replace all data?") },
            text = {
                Text(restoreConfirmation(state.transactionCount, state.accountCount))
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.Default) { financeViewModel.restoreDataFromJson(state.json) }
                        autoBackup.clearRestore()
                    }
                }) { Text("Replace", color = AkSpendTheme.finance.expense) }
            },
            dismissButton = { TextButton(onClick = autoBackup::clearRestore) { Text("Cancel") } }
        )
        is JsonRestoreState.Error -> AlertDialog(
            onDismissRequest = autoBackup::clearRestore,
            title = { Text("Can't restore") },
            text = { Text(state.message) },
            confirmButton = { TextButton(onClick = autoBackup::clearRestore) { Text("OK") } }
        )
        JsonRestoreState.Idle, JsonRestoreState.Working -> Unit
    }
}

@Composable
private fun RestorePasswordDialog(wrongPassword: Boolean, onDismiss: () -> Unit, onSubmit: (CharArray) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter backup password") },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                isError = wrongPassword,
                supportingText = if (wrongPassword) ({ Text("Wrong password, or the file is damaged.") }) else null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = glassTextFieldColors()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(password.toCharArray()) }, enabled = password.isNotEmpty()) { Text("Unlock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
