package com.parka.mdive

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherIconTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun render(resource: Int, size: Int = 108): Bitmap {
        val drawable = checkNotNull(context.getDrawable(resource))
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(it))
        }
    }

    @Test
    fun adaptiveForegroundStaysInsideSafeZoneAndPreservesCounters() {
        assertTrue(context.getDrawable(R.mipmap.ic_launcher) is AdaptiveIconDrawable)
        val foreground = render(R.drawable.ic_launcher_foreground, 324)
        var visible = 0
        for (y in 0 until 324) for (x in 0 until 324) {
            if (Color.alpha(foreground.getPixel(x, y)) > 20) {
                visible++
                assertTrue("Foreground must fit the centered 66dp safe zone", x in 63..260 && y in 63..260)
                val dx = x - 162.0
                val dy = y - 162.0
                assertTrue("Circle mask must retain the emblem", dx * dx + dy * dy < 108.0 * 108.0)
            }
        }
        assertTrue(visible > 8_000)
        // Open spaces between m arches and inside d stay transparent, not dark patches.
        assertEquals(0, Color.alpha(foreground.getPixel(98, 138)))
        assertEquals(0, Color.alpha(foreground.getPixel(138, 138)))
        assertEquals(0, Color.alpha(foreground.getPixel(196, 188)))
        foreground.recycle()
    }

    @Test
    fun legacyRoundAndMonochromeResourcesRender() {
        val round = render(R.drawable.ic_launcher_legacy_round)
        assertEquals(0, Color.alpha(round.getPixel(0, 0)))
        assertTrue(Color.alpha(round.getPixel(54, 54)) > 0)
        val square = render(R.drawable.ic_launcher_legacy)
        assertEquals(Color.rgb(13, 29, 39), square.getPixel(0, 0))
        val monochrome = render(R.drawable.ic_launcher_monochrome)
        var pixels = 0
        for (y in 0 until 108) for (x in 0 until 108) {
            val color = monochrome.getPixel(x, y)
            if (Color.alpha(color) > 240) {
                pixels++
                assertEquals(255, Color.red(color))
                assertEquals(255, Color.green(color))
                assertEquals(255, Color.blue(color))
            }
        }
        assertTrue(pixels > 500)
        listOf(round, square, monochrome).forEach(Bitmap::recycle)
    }
}
