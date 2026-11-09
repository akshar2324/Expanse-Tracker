package com.akshar.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.theme.LocalReducedMotion
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Verifies Compose gesture/semantics wiring; real TalkBack is a separate device check. */
@MediumTest
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ActivitySwipeRowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `row click and custom accessibility actions use the same callbacks`() {
        var edits = 0
        var deletes = 0
        compose.setContent {
            AkSpendTheme {
                ActivitySwipeRow({ edits++ }, { deletes++ }) { modifier ->
                    GlassListRow("Sample transaction", modifier, onClick = { edits++ })
                }
            }
        }
        val row = compose.onNodeWithText("Sample transaction")
        row.performClick()
        val actions = row.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Edit", "Delete"), actions.map { it.label })
        compose.runOnIdle {
            assertTrue(actions[0].action())
            assertTrue(actions[1].action())
            assertEquals(2, edits)
            assertEquals(1, deletes)
        }
    }

    @Test fun `reduced motion swipes invoke once and settle without a fling`() = verifySwipes(LayoutDirection.Ltr)

    @Test fun `right and left retain their physical meanings in RTL`() = verifySwipes(LayoutDirection.Rtl)

    private fun verifySwipes(layoutDirection: LayoutDirection) {
        var edits = 0
        var deletes = 0
        compose.setContent {
            AkSpendTheme {
                CompositionLocalProvider(LocalReducedMotion provides true, LocalLayoutDirection provides layoutDirection) {
                    Box(Modifier.requiredWidth(300.dp)) {
                        ActivitySwipeRow({ edits++ }, { deletes++ }) { modifier ->
                            GlassListRow("Sample transaction", modifier, onClick = { edits++ })
                        }
                    }
                }
            }
        }
        val row = compose.onNodeWithText("Sample transaction")
        val before = row.fetchSemanticsNode().boundsInRoot
        row.performTouchInput { swipeRight() }
        compose.runOnIdle { assertEquals(1, edits); assertEquals(0, deletes) }
        assertEquals(before, row.fetchSemanticsNode().boundsInRoot)
        row.performTouchInput { swipeLeft() }
        compose.runOnIdle { assertEquals(1, edits); assertEquals(1, deletes) }
        assertEquals(before, row.fetchSemanticsNode().boundsInRoot)
    }
}
