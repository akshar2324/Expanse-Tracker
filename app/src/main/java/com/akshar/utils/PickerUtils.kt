package com.akshar.utils

import android.content.ActivityNotFoundException
import androidx.activity.result.ActivityResultLauncher

/**
 * Launches a system file picker. Some devices and profiles have no app for GET_CONTENT, and an
 * unguarded launch crashes the app; show a message instead.
 */
fun ActivityResultLauncher<String>.launchFilePicker(mimeType: String, onUnavailable: (String) -> Unit) {
    try {
        launch(mimeType)
    } catch (_: ActivityNotFoundException) {
        onUnavailable("No file picker is available on this device.")
    }
}
