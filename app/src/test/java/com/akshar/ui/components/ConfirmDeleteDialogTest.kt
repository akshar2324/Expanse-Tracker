package com.akshar.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.akshar.ui.theme.AkSpendTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConfirmDeleteDialogTest {

    @get:Rule val composeTestRule = createComposeRule()

    private var deletes = 0

    private fun setUp() {
        composeTestRule.setContent {
            AkSpendTheme(darkTheme = false) {
                val confirmDelete = rememberConfirmDeleteState()
                ConfirmDeleteDialog(confirmDelete)
                Button(onClick = { confirmDelete.request("Delete Food budget?") { deletes++ } }) {
                    Text("Bin")
                }
            }
        }
    }

    @Test
    fun `tapping delete icon only asks, cancel keeps the item`() {
        setUp()
        composeTestRule.onNodeWithText("Bin").performClick()
        composeTestRule.onNodeWithText("Delete Food budget?").assertExists()
        assertEquals(0, deletes)

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Delete Food budget?").assertDoesNotExist()
        assertEquals(0, deletes)
    }

    @Test
    fun `confirming deletes exactly once and closes the dialog`() {
        setUp()
        composeTestRule.onNodeWithText("Bin").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Delete Food budget?").assertDoesNotExist()
        assertEquals(1, deletes)
    }
}
