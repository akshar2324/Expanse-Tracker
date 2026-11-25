package com.akshar.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import android.graphics.BitmapFactory
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A surface that animates from pressed back to flush must redraw flush, with no leftover well. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SoftSurfaceRedrawTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun depthReturningToZeroRedrawsFlush() {
        val soft = SoftPalettes.resolve(SoftPalette.LAVENDER, dark = false, 0f, 0f)
        var pressed by mutableStateOf(false)
        rule.setContent {
            val depth by animateFloatAsState(if (pressed) -1f else 0f, tween(200), label = "d")
            Box(Modifier.testTag("root").background(soft.background).padding(16.dp)) {
                Box(Modifier.size(80.dp).softSurface(RoundedCornerShape(20.dp), soft, distance = 6.dp) { depth })
            }
        }
        rule.waitForIdle()
        // Always record: without the record flag Roborazzi writes nothing, and a stale file from an
        // earlier run must never stand in for this one.
        fun shot(name: String) = java.io.File("build/outputs/roborazzi/soft-redraw-$name.png").let { file ->
            file.delete()
            rule.onNodeWithTag("root").captureRoboImage(file.path, RoborazziOptions(taskType = RoborazziTaskType.Record))
            checkNotNull(BitmapFactory.decodeFile(file.path)) { "no capture written for $name" }
        }
        val flush = shot("flush")
        pressed = true
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        val inset = shot("pressed")
        pressed = false
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        val back = shot("released")
        // Near the top-left inner edge: shaded while pressed, page colour again once released.
        val x = (16 * flush.width / 112) + 8
        val y = flush.height / 2
        assert(inset.getPixel(x, y) != flush.getPixel(x, y)) { "pressed state should draw a well" }
        assertEquals(flush.getPixel(x, y), back.getPixel(x, y))
    }
}
