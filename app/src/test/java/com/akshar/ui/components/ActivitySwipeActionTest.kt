package com.akshar.ui.components

import org.junit.Assert.*
import org.junit.Test

class ActivitySwipeActionTest {
    @Test fun `right edits and left deletes at the distance threshold`() {
        assertEquals(ActivitySwipeAction.EDIT, activitySwipeAction(100f, 100f))
        assertEquals(ActivitySwipeAction.DELETE, activitySwipeAction(-100f, 100f))
        assertNull(activitySwipeAction(99f, 100f))
        assertNull(activitySwipeAction(-99f, 100f))
        assertNull(activitySwipeAction(0f, 100f))
    }
}
