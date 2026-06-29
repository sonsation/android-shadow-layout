package com.sonsation.library

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import org.robolectric.RuntimeEnvironment
import com.sonsation.library.effet.Radius
import com.sonsation.library.utils.Corner
import com.sonsation.library.utils.ViewHelper
import com.sonsation.library.utils.ViewHelper.getIntAlpha
import com.sonsation.library.utils.ViewHelper.intToColorModel
import com.sonsation.library.utils.ViewHelper.onSetAlphaFromAlpha
import com.sonsation.library.utils.ViewHelper.onSetAlphaFromColor
import com.sonsation.library.utils.ViewHelper.parseGradientColors
import com.sonsation.library.utils.ViewHelper.parseGradientPositions
import com.sonsation.library.utils.ViewHelper.parseShadowArray
import com.sonsation.library.utils.ViewHelper.toPx
import com.sonsation.library.utils.ViewHelper.getInnerPath
import com.sonsation.library.utils.addSmoothRoundRect
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun testParseGradientColors() {
        assertNull(parseGradientColors(null))
        assertNull(parseGradientColors(""))
        
        val colors = parseGradientColors("#FF0000,#00FF00,  #0000FF")
        assertNotNull(colors)
        assertEquals(3, colors!!.size)
        assertEquals(Color.RED, colors[0])
        assertEquals(Color.GREEN, colors[1])
        assertEquals(Color.BLUE, colors[2])

        // Empty splits
        try {
            parseGradientColors(",")
            fail("Expected Exception")
        } catch (e: Exception) {
            // Expected
        }
    }

    @Test
    fun testParseGradientPositions() {
        assertNull(parseGradientPositions(null))
        assertNull(parseGradientPositions(""))

        val positions = parseGradientPositions("0.0, 0.5, 1.0")
        assertNotNull(positions)
        assertEquals(3, positions!!.size)
        assertEquals(0.0f, positions[0], 0.01f)
        assertEquals(0.5f, positions[1], 0.01f)
        assertEquals(1.0f, positions[2], 0.01f)

        try {
            parseGradientPositions(",")
            fail("Expected Exception")
        } catch (e: Exception) {
            // Expected
        }
    }

    @Test
    fun testParseShadowArray() {
        assertNull(parseShadowArray(context, null))
        assertNull(parseShadowArray(context, ""))

        // 4 elements case
        val shadows4 = parseShadowArray(context, "{10,5,5,#FF0000}")
        assertNotNull(shadows4)
        assertEquals(1, shadows4!!.size)
        val s4 = shadows4[0]
        assertEquals(10f.toPx(context), s4.blurSize, 0.01f)
        assertEquals(5f.toPx(context), s4.shadowOffsetX, 0.01f)
        assertEquals(5f.toPx(context), s4.shadowOffsetY, 0.01f)
        assertEquals(0f, s4.shadowSpread, 0.01f)
        assertEquals(Color.RED, s4.shadowColor)

        // 5 elements case
        val shadows5 = parseShadowArray(context, "{10,5,5,2,#00FF00}")
        assertNotNull(shadows5)
        assertEquals(1, shadows5!!.size)
        val s5 = shadows5[0]
        assertEquals(10f.toPx(context), s5.blurSize, 0.01f)
        assertEquals(5f.toPx(context), s5.shadowOffsetX, 0.01f)
        assertEquals(5f.toPx(context), s5.shadowOffsetY, 0.01f)
        assertEquals(2f.toPx(context), s5.shadowSpread, 0.01f)
        assertEquals(Color.GREEN, s5.shadowColor)

        // Multiple shadows
        val multiple = parseShadowArray(context, "{10,5,5,#FF0000}, {20,10,10,2,#00FF00}")
        assertNotNull(multiple)
        assertEquals(2, multiple!!.size)

        // NumberFormatException in 4 elements
        val badShadow4 = parseShadowArray(context, "{abc,5,5,#FF0000}")
        assertNotNull(badShadow4)
        assertEquals(Color.WHITE, badShadow4!![0].shadowColor)
        assertEquals(0f, badShadow4[0].blurSize, 0.01f)

        // NumberFormatException in 5 elements
        val badShadow5 = parseShadowArray(context, "{10,5,5,abc,#FF0000}")
        assertNotNull(badShadow5)
        assertEquals(Color.WHITE, badShadow5!![0].shadowColor)
        assertEquals(0f, badShadow5[0].blurSize, 0.01f)

        // Empty splits check
        val emptySplitResult = parseShadowArray(context, ",")
        assertNotNull(emptySplitResult)
        assertEquals(1, emptySplitResult!!.size)
        assertEquals(Color.WHITE, emptySplitResult[0].shadowColor)

        // Test IndexOutOfBoundsException trigger
        try {
            parseShadowArray(context, "{1.0,1.0}")
            fail("Expected IndexOutOfBoundsException")
        } catch (e: IndexOutOfBoundsException) {
            // Expected
        }
    }

    @Test
    fun testFloatToPx() {
        val density = context.resources.displayMetrics.density
        assertEquals(density * 10f, 10f.toPx(context), 0.01f)
    }

    @Test
    fun testIntToColorModel() {
        val color = Color.argb(200, 100, 150, 200)
        val argb = intToColorModel(color)
        assertEquals(200, argb.alpha)
        assertEquals(100, argb.red)
        assertEquals(150, argb.green)
        assertEquals(200, argb.blue)
    }

    @Test
    fun testOnSetAlphaFromAlpha() {
        // Out of bounds
        assertFalse(onSetAlphaFromAlpha(-0.1f, 100))
        assertFalse(onSetAlphaFromAlpha(1.1f, 100))

        // In bounds, evaluates true/false
        assertTrue(onSetAlphaFromAlpha(0.2f, 100)) // 0.2 * 255 = 51 < 100 -> true
        assertFalse(onSetAlphaFromAlpha(0.8f, 100)) // 0.8 * 255 = 204 >= 100 -> false
    }

    @Test
    fun testOnSetAlphaFromColor() {
        // Out of bounds
        assertFalse(onSetAlphaFromColor(-0.1f, Color.BLACK))
        assertFalse(onSetAlphaFromColor(1.1f, Color.BLACK))

        val color = Color.argb(100, 0, 0, 0)
        assertTrue(onSetAlphaFromColor(0.2f, color))
        assertFalse(onSetAlphaFromColor(0.8f, color))
    }

    @Test
    fun testGetStaticIntAlpha() {
        assertEquals(255, getIntAlpha(-0.1f))
        assertEquals(255, getIntAlpha(1.1f))
        assertEquals(127, getIntAlpha(0.5f))
    }

    @Test
    fun testGetInnerPath() {
        val path = Path().apply {
            addRect(0f, 0f, 100f, 100f, Path.Direction.CW)
        }
        val innerPath = path.getInnerPath(10f)
        assertNotNull(innerPath)
        assertFalse(innerPath.isEmpty)
    }

    @Test
    fun testCornerEnum() {
        assertEquals(Corner.TOP_LEFT, Corner.valueOf("TOP_LEFT"))
        assertEquals(4, Corner.entries.size)
    }

    @Test
    fun testAddSmoothRoundRect() {
        val rect = RectF(0f, 0f, 100f, 100f)
        val path = Path()

        // 1. smoothing == 0f
        val rZeroSmoothing = Radius(10f).apply { cornerSmoothing = 0f }
        path.addSmoothRoundRect(rect, rZeroSmoothing)
        assertFalse(path.isEmpty)

        // 2. smoothing > 0f
        val rWithSmoothing = Radius(10f).apply { cornerSmoothing = 0.5f }
        path.addSmoothRoundRect(rect, rWithSmoothing)
        assertFalse(path.isEmpty)

        // 3. radiusHalf = true with smoothing
        val rHalfSmoothing = Radius(10f).apply {
            cornerSmoothing = 0.5f
            radiusHalf = true
        }
        path.addSmoothRoundRect(rect, rHalfSmoothing)
        assertFalse(path.isEmpty)

        // 4. radius <= 0f corner logic
        val rZeroRadius = Radius(0f).apply { cornerSmoothing = 0.5f }
        path.addSmoothRoundRect(rect, rZeroRadius)
        assertFalse(path.isEmpty)
    }
}
