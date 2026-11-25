package com.akshar.ui.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akshar.data.backup.AutoBackupFrequency
import com.akshar.data.backup.AutoBackupScheduler
import com.akshar.data.backup.AutoBackupSettings
import com.akshar.data.backup.AutoBackupStore
import com.akshar.data.backup.BackupCodec
import com.akshar.data.backup.BackupFiles
import com.akshar.data.backup.BackupPasswordRequiredException
import com.akshar.data.backup.SafBackupDestination
import com.akshar.data.backup.WrongBackupPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Steps of restoring a JSON backup file, plain or password protected. */
sealed class JsonRestoreState {
    data object Idle : JsonRestoreState()
    data object Working : JsonRestoreState()
    /** The file is encrypted; [wrongPassword] is true after a failed attempt. */
    data class NeedsPassword(val wrongPassword: Boolean = false) : JsonRestoreState()
    /** Decoded and validated; waiting for the user to confirm replacing their data. */
    data class Ready(val json: String, val transactionCount: Int, val accountCount: Int = 0) : JsonRestoreState()
    data class Error(val message: String) : JsonRestoreState()
}

/** Automatic backup settings and the JSON restore flow under More → Backup & restore. */
class AutoBackupViewModel(application: Application) : AndroidViewModel(application) {

    private val store = AutoBackupStore(application)

    private val _settings = MutableStateFlow(AutoBackupSettings())
    val settings: StateFlow<AutoBackupSettings> = _settings.asStateFlow()

    /** Null until checked; false when the chosen folder can no longer be written. */
    private val _folderAccessible = MutableStateFlow<Boolean?>(null)
    val folderAccessible: StateFlow<Boolean?> = _folderAccessible.asStateFlow()

    private val _restoreState = MutableStateFlow<JsonRestoreState>(JsonRestoreState.Idle)
    val restoreState: StateFlow<JsonRestoreState> = _restoreState.asStateFlow()

    /** Encrypted file text held only while the password dialog is open. */
    private var pendingEncrypted: String? = null
    private var restoreJob: Job? = null

    // The worker records its result in the same preferences, so the status updates live.
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    init {
        store.registerListener(listener)
        refresh()
    }

    override fun onCleared() {
        store.unregisterListener(listener)
        super.onCleared()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = store.read()
            _settings.value = current
            _folderAccessible.value = current.folderUri?.let { uri ->
                try {
                    SafBackupDestination(getApplication(), Uri.parse(uri)).isAccessible()
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    private fun update(change: AutoBackupStore.() -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            store.change()
            val next = store.read()
            AutoBackupScheduler.sync(getApplication(), next)
            refresh()
        }
    }

    fun setEnabled(enabled: Boolean) = update { setEnabled(enabled) }
    fun setFrequency(frequency: AutoBackupFrequency) = update { setFrequency(frequency) }
    fun setKeepCount(count: Int) = update { setKeepCount(count) }

    /** Keeps access to the picked folder across restarts and releases the previous one. */
    fun chooseFolder(uri: Uri, enable: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        val app = getApplication<Application>()
        try {
            app.contentResolver.takePersistableUriPermission(uri, SafBackupDestination.PERSISTED_FLAGS)
        } catch (e: SecurityException) {
            Log.e("AutoBackup", "Folder permission was not granted", e)
            update { recordError(System.currentTimeMillis(), "That folder can't be used for backups. Pick another one.") }
            return@launch
        }
        val previous = store.read().folderUri
        if (previous != null && previous != uri.toString()) {
            try {
                app.contentResolver.releasePersistableUriPermission(Uri.parse(previous), SafBackupDestination.PERSISTED_FLAGS)
            } catch (e: SecurityException) {
                // Already gone.
            }
        }
        update {
            setFolder(uri.toString())
            if (enable) setEnabled(true)
        }
    }

    fun setPassword(password: CharArray) = viewModelScope.launch(Dispatchers.IO) {
        try {
            store.setEncryption(password)
        } catch (e: Exception) {
            Log.e("AutoBackup", "Could not store backup password", e)
            store.recordError(System.currentTimeMillis(), "Could not save the backup password on this device.")
        } finally {
            password.fill('\u0000')
        }
        refresh()
    }

    fun turnOffEncryption() = update { setEncryption(null) }

    fun backUpNow() = AutoBackupScheduler.backUpNow(getApplication())

    // --- Restore from a JSON backup file ---

    fun openRestoreFile(uri: Uri) {
        restoreJob?.cancel()
        pendingEncrypted = null
        _restoreState.value = JsonRestoreState.Working
        restoreJob = viewModelScope.launch {
            val text = try {
                readText(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AutoBackup", "Could not read backup file", e)
                _restoreState.value = JsonRestoreState.Error("Could not read that file.")
                return@launch
            }
            try {
                val json = withContext(Dispatchers.Default) { BackupFiles.openBackup(text, null) }
                prepare(json)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupPasswordRequiredException) {
                pendingEncrypted = text
                _restoreState.value = JsonRestoreState.NeedsPassword()
            } catch (e: Exception) {
                _restoreState.value = JsonRestoreState.Error("That file is not an AkSpend backup.")
            }
        }
    }

    fun submitRestorePassword(password: CharArray) {
        val encrypted = pendingEncrypted ?: run { password.fill('\u0000'); return }
        restoreJob?.cancel()
        _restoreState.value = JsonRestoreState.Working
        restoreJob = viewModelScope.launch {
            try {
                // Wipe the typed password as soon as decryption is done, before any state change
                // can let the screen (or a test) move on while it is still in memory.
                val json = try {
                    withContext(Dispatchers.Default) { BackupFiles.openBackup(encrypted, password) }
                } finally {
                    password.fill('\u0000')
                }
                prepare(json)
                pendingEncrypted = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: WrongBackupPasswordException) {
                _restoreState.value = JsonRestoreState.NeedsPassword(wrongPassword = true)
            } catch (e: Exception) {
                pendingEncrypted = null
                _restoreState.value = JsonRestoreState.Error("That file is not an AkSpend backup.")
            }
        }
    }

    fun clearRestore() {
        restoreJob?.cancel()
        pendingEncrypted = null
        _restoreState.value = JsonRestoreState.Idle
    }

    private suspend fun prepare(json: String) {
        // Validate off the main thread before offering to replace anything.
        val snapshot = withContext(Dispatchers.Default) { BackupCodec.decode(json) }
        _restoreState.value = JsonRestoreState.Ready(json, snapshot.transactions.size, snapshot.accounts.size)
    }

    private suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val stream = resolver.openInputStream(uri) ?: throw java.io.IOException("No stream")
        stream.use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > BackupFiles.MAX_BYTES) throw java.io.IOException("File too large")
                out.write(buffer, 0, read)
            }
            out.toString("UTF-8")
        }
    }
}
