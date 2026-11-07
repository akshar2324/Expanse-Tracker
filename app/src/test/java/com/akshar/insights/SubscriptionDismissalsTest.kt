package com.akshar.insights

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubscriptionDismissalsTest {
    @Test
    fun recreationAndMultipleInstances_preserveAllDismissals() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("insight_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        val first = SubscriptionDismissals(context)
        val second = SubscriptionDismissals(context)
        first.dismiss("netflix")
        second.dismiss("cloud storage")
        second.dismiss("netflix")
        second.dismiss(" ")
        assertEquals(setOf("netflix", "cloud storage"), SubscriptionDismissals(context).keys.first())
        assertEquals(setOf("netflix", "cloud storage"), first.keys.first())
    }
}
