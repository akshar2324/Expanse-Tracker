package com.akshar.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.akshar.ExpenseTrackerApp
import com.akshar.TestViewModelOwner
import com.akshar.data.db.AppDatabase
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.theme.GlassBackdrop
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Layout checks for the Add screen problems seen in the 9 November phone recordings. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AddTransactionLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<ExpenseTrackerApp>()
    private val viewModels = TestViewModelOwner()

    @After fun close() {
        viewModels.clear()
        com.akshar.sms.inbox.ImportInboxDatabase.closeAndResetInstance()
        AppDatabase.closeAndResetInstance()
        RuntimeEnvironment.setFontScale(1f)
    }

    private fun show() {
        val vm = viewModels.finance(app)
        compose.setContent {
            AkSpendTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize()) {
                    GlassBackdrop()
                    AddTransactionScreen(viewModel = vm, onNavigateBack = {})
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun largeTextShortCategoriesShareALine() {
        RuntimeEnvironment.setFontScale(2f)
        show()
        // Before: one category per line, so 15 full-width rows pushed the rest of the form away.
        val food = compose.onNodeWithText("Food").getBoundsInRoot()
        val grocery = compose.onNodeWithText("Grocery").getBoundsInRoot()
        assertEquals("Food and Grocery should sit on the same line", food.top, grocery.top)
        compose.onNodeWithText("Entertainment").performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/screens/add-large-text.png")
    }

    @Test @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun landscapeScrollsTheTitleAndSaveButtonWithTheForm() {
        show()
        compose.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/screens/add-landscape-top.png")
        // The title scrolls away instead of staying pinned, leaving the form room to show.
        compose.onNodeWithText("Paid with").performScrollTo()
        compose.onNodeWithText("Save expense").performScrollTo().assertIsDisplayed()
        val title = compose.onNodeWithText("Add transaction").getBoundsInRoot()
        assertTrue("Title should have scrolled up off screen, was at ${title.top}", title.bottom.value <= 0f)
        compose.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/screens/add-landscape-bottom.png")
    }

    @Test fun portraitKeepsTheTitleAndSaveButtonPinned() {
        show()
        compose.onNodeWithText("Paid with").performScrollTo()
        compose.onNodeWithText("Add transaction").assertIsDisplayed()
        compose.onNodeWithText("Save expense").assertIsDisplayed()
    }
}
