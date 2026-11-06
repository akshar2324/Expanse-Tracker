package com.akshar.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.akshar.utils.CurrencyUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BudgetWidgetSettingsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val lockPrefs get() = context.getSharedPreferences(BudgetWidgetModel.LOCK_PREFS, Context.MODE_PRIVATE)
    private val countryPrefs get() = context.getSharedPreferences(CurrencyUtils.PREFS, Context.MODE_PRIVATE)

    @After
    fun tearDown() {
        lockPrefs.edit().clear().commit()
        countryPrefs.edit().clear().commit()
    }

    @Test
    fun `widget hides amounts unless the user turned the app lock off`() {
        assertTrue("the lock is on by default", BudgetWidgetModel.isAppLockOn(context))
        lockPrefs.edit().putBoolean(BudgetWidgetModel.KEY_LOCK_ENABLED, false).commit()
        assertFalse(BudgetWidgetModel.isAppLockOn(context))
        lockPrefs.edit().putBoolean(BudgetWidgetModel.KEY_LOCK_ENABLED, true).commit()
        assertTrue(BudgetWidgetModel.isAppLockOn(context))
    }

    @Test
    fun `widget reads the same region setting as the app`() {
        assertEquals("IN", CurrencyUtils.selectedCountry(context))
        countryPrefs.edit().putString(CurrencyUtils.KEY_COUNTRY, "JP").commit()
        assertEquals("JP", CurrencyUtils.selectedCountry(context))
        assertEquals("¥", CurrencyUtils.symbolFor(CurrencyUtils.selectedCountry(context)))
    }
}
