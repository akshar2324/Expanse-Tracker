package com.akshar

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Screens that outside entry points (home-screen widgets, the Quick Settings tile) ask the app to
 * open. The request is held until the main scaffold is showing, so it still applies after the
 * lock screen.
 */
object AppNavigationRequests {

    const val EXTRA_OPEN_TAB = "com.akshar.akspend.OPEN_TAB"
    const val TAB_BUDGETS = "budgets"

    /** The Add transaction form. Not a tab: it opens on top of Home, like the in-app Add button. */
    const val ADD_TRANSACTION = "add_transaction"

    /** Only these routes may be opened from an intent. */
    private val allowedTabs = setOf(TAB_BUDGETS, ADD_TRANSACTION)

    /** An intent that opens the app (behind the lock, if it is on) straight to [route]. */
    fun openIntent(context: Context, route: String): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_TAB, route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    private val _pendingTab = MutableStateFlow<String?>(null)
    val pendingTab: StateFlow<String?> = _pendingTab.asStateFlow()

    fun handle(intent: Intent?) {
        val tab = intent?.getStringExtra(EXTRA_OPEN_TAB) ?: return
        if (tab in allowedTabs) _pendingTab.value = tab
    }

    fun consumed() {
        _pendingTab.value = null
    }
}
