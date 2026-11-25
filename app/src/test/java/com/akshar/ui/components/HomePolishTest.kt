package com.akshar.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.akshar.ui.theme.AkSpendTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomePolishTest {
    @get:Rule val compose = createComposeRule()

    @Test fun rollingBalanceExposesTheExactAmountAndSettlesOnTheNewOne() {
        var value by mutableDoubleStateOf(524_723.88)
        var text by mutableStateOf("₹524,723.88")
        compose.setContent {
            AkSpendTheme {
                RollingMoneyText(
                    text = text,
                    value = value,
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    accessibilityLabel = "Net balance $text"
                )
            }
        }
        compose.onNodeWithText("₹524,723.88").assertContentDescriptionEquals("Net balance ₹524,723.88")
        value = 524_673.88
        text = "₹524,673.88"
        compose.waitForIdle()
        compose.onNodeWithText("₹524,673.88").assertContentDescriptionEquals("Net balance ₹524,673.88")
    }

    @Test fun budgetDialReportsProgressAndCapsTheArc() {
        compose.setContent {
            AkSpendTheme {
                BudgetDial(usedFraction = 1.25f, color = Color.Red, trackColor = Color.Gray, accessibilityLabel = "Monthly budget 125% used")
            }
        }
        val node = compose.onNodeWithContentDescription("Monthly budget 125% used").fetchSemanticsNode()
        assertEquals(1f, node.config[SemanticsProperties.ProgressBarRangeInfo].current, 0f)
    }

    @Test fun percentLabelRoundsDownAndNeverGoesNegative() {
        assertEquals("0%", budgetPercentLabel(-0.2f))
        assertEquals("99%", budgetPercentLabel(0.999f))
        assertEquals("100%", budgetPercentLabel(1f))
        assertEquals("125%", budgetPercentLabel(1.25f))
    }
}
