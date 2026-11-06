package com.akshar

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tabs that outside entry points (home-screen widgets) ask the app to open. The request is
 * held until the main scaffold is showing, so it still applies after the lock screen.
 */
object AppNavigationRequests {

    const val EXTRA_OPEN_TAB = "com.akshar.akspend.OPEN_TAB"
    const val TAB_BUDGETS = "budgets"

    /** Only these routes may be opened from an intent. */
    private val allowedTabs = setOf(TAB_BUDGETS)

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
