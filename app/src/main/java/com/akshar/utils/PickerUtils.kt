package com.akshar.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher

/**
 * Launches a system file picker. Some devices and profiles have no app for GET_CONTENT, and an
 * unguarded launch crashes the app; show a message instead.
 */
fun ActivityResultLauncher<String>.launchFilePicker(context: Context, mimeType: String) {
    try {
        launch(mimeType)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No file picker is available on this device.", Toast.LENGTH_LONG).show()
    }
}
