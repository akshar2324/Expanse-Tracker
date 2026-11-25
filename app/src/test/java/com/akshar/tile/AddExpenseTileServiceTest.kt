package com.akshar.tile

import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.TileService
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.akshar.AppNavigationRequests
import com.akshar.MainActivity
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowService
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 33, 34], shadows = [RecordingTileServiceShadow::class])
class AddExpenseTileServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun clearNavigationRequest() {
        AppNavigationRequests.consumed()
    }

    @Test
    fun `tap collapses the panel using the supported overload and opens Add`() {
        val controller = Robolectric.buildService(AddExpenseTileService::class.java).create()
        try {
            val service = controller.get()
            val shadow = Shadow.extract<RecordingTileServiceShadow>(service)
            service.onClick()

            val intent = if (Build.VERSION.SDK_INT >= 34) {
                assertNull(shadow.launchedIntent)
                val pending = requireNotNull(shadow.launchedPendingIntent)
                assertTrue(shadowOf(pending).isImmutable)
                assertTrue(shadowOf(pending).isActivity)
                shadowOf(pending).savedIntent
            } else {
                assertNull(shadow.launchedPendingIntent)
                requireNotNull(shadow.launchedIntent)
            }
            assertEquals(MainActivity::class.java.name, intent.component?.className)
            AppNavigationRequests.handle(intent)
            assertEquals(AppNavigationRequests.ADD_TRANSACTION, AppNavigationRequests.pendingTab.value)
        } finally {
            controller.destroy()
        }
    }

    @Test
    fun `tile intent explicitly opens the Add route through navigation requests`() {
        val intent = AddExpenseTileService.openAddIntent(context)

        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(
            AppNavigationRequests.ADD_TRANSACTION,
            intent.getStringExtra(AppNavigationRequests.EXTRA_OPEN_TAB)
        )
        AppNavigationRequests.handle(intent)
        assertEquals(AppNavigationRequests.ADD_TRANSACTION, AppNavigationRequests.pendingTab.value)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
    }
}

/** Captures the System UI boundary while exercising the real service click handler. */
@Implements(TileService::class)
class RecordingTileServiceShadow : ShadowService() {
    var launchedIntent: Intent? = null
    var launchedPendingIntent: PendingIntent? = null

    @Implementation
    fun startActivityAndCollapse(intent: Intent) {
        launchedIntent = intent
    }

    @Implementation(minSdk = 34)
    fun startActivityAndCollapse(intent: PendingIntent) {
        launchedPendingIntent = intent
    }
}
