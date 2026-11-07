package com.akshar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.akshar.data.db.AppDatabase
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.viewmodel.FinanceViewModel
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdaptiveDraftTest {
    @get:Rule val compose = createComposeRule()

    private val viewModels = TestViewModelOwner()

    @After fun close() {
        viewModels.clear()
        com.akshar.sms.inbox.ImportInboxDatabase.closeAndResetInstance()
        AppDatabase.closeAndResetInstance()
    }

    @Test fun draftSurvivesSwitchingBetweenBottomBarAndNavigationRail() {
        val wide = mutableStateOf(false)
        val vm = viewModels.finance(ApplicationProvider.getApplicationContext<ExpenseTrackerApp>())
        compose.setContent {
            Box(Modifier.requiredSize(if (wide.value) 800.dp else 400.dp, 900.dp)) {
                AkSpendTheme { AppScaffold(vm) }
            }
        }
        compose.onNodeWithContentDescription("Add transaction").performClick()
        compose.onNodeWithText("Amount").performTextInput("77")
        compose.onNodeWithText("Grocery").performScrollTo().performClick()
        compose.onNodeWithText("Description").performScrollTo().performTextInput("Demo rotation")

        compose.runOnIdle { wide.value = true }
        compose.onNodeWithText("77").assertExists()
        compose.onNodeWithText("Grocery").assertIsSelected()
        compose.onNodeWithText("Demo rotation").assertExists()

        compose.runOnIdle { wide.value = false }
        compose.onNodeWithText("77").assertExists()
        compose.onNodeWithText("Grocery").assertIsSelected()
        compose.onNodeWithText("Demo rotation").assertExists()
    }

    @Test fun doubleFontScaleKeepsFinancialLabelsAndCategoryNamesReadable() {
        val vm = viewModels.finance(ApplicationProvider.getApplicationContext<ExpenseTrackerApp>())
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                Box(Modifier.requiredSize(400.dp, 900.dp)) {
                    AkSpendTheme { AppScaffold(vm) }
                }
            }
        }
        assertTextFits("In this month")
        assertTextFits("Out this month")
        assertTextFits("Transfer")
        assertTextFits("Home")
        compose.onNode(hasText("Activity") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).performClick()
        assertTextFits("Activity")
        compose.onNode(hasText("Home") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).performClick()
        compose.onNodeWithContentDescription("Add transaction").performScrollTo().performClick()
        compose.onNodeWithText("Transportation").performScrollTo()
        assertTextFits("Transportation")
        compose.onNodeWithText("Entertainment").performScrollTo()
        assertTextFits("Entertainment")
        compose.onNodeWithText("Subscription").performScrollTo()
        assertTextFits("Subscription")
    }

    private fun assertTextFits(text: String) {
        val nodes = compose.onAllNodes(
            hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true
        )
        val count = nodes.fetchSemanticsNodes().size
        assertTrue("No rendered text found for $text", count > 0)
        repeat(count) { index ->
            val layouts = mutableListOf<TextLayoutResult>()
            nodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue("No text measurements for $text", layouts.isNotEmpty())
            // Check painted line bounds and complete text, rather than paragraph width:
            // Compose may retain a wider paragraph allocation for naturally short labels.
            assertFalse("$text is clipped or ellipsized", layouts.any { result ->
                result.lineCount == 0 ||
                    result.getLineEnd(result.lineCount - 1, visibleEnd = true) < result.layoutInput.text.length ||
                    (0 until result.lineCount).any { line ->
                        result.isLineEllipsized(line) || result.getLineLeft(line) < -1f ||
                            result.getLineRight(line) > result.size.width + 1f ||
                            result.getLineBottom(line) > result.size.height + 1f
                    }
            })
        }
    }

}
