package com.sonsation.library

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.sonsation.library.effet.Gradient
import com.sonsation.library.effet.Radius
import com.sonsation.library.effet.Shadow
import com.sonsation.library.effet.Stroke
import com.sonsation.library.model.Offset
import com.sonsation.library.model.StrokeType
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EffectTest {

    @Test
    fun testOffset() {
        val o1 = Offset()
        assertEquals(0f, o1.left, 0.01f)
        assertEquals(0f, o1.top, 0.01f)
        assertEquals(0f, o1.right, 0.01f)
        assertEquals(0f, o1.bottom, 0.01f)

        val o2 = Offset(1f, 2f, 3f, 4f)
        assertEquals(1f, o2.left, 0.01f)
        assertEquals(2f, o2.top, 0.01f)
        assertEquals(3f, o2.right, 0.01f)
        assertEquals(4f, o2.bottom, 0.01f)

        o2.left = 5f
        o2.top = 6f
        o2.right = 7f
        o2.bottom = 8f
        assertEquals(5f, o2.left, 0.01f)
        assertEquals(6f, o2.top, 0.01f)
        assertEquals(7f, o2.right, 0.01f)
        assertEquals(8f, o2.bottom, 0.01f)

        // data class auto-generated methods
        val o3 = o2.copy(left = 10f)
        assertEquals(10f, o3.left, 0.01f)
        assertEquals(6f, o3.top, 0.01f)

        assertEquals(o2, o2)
        assertNotEquals(o2, o3)
        assertNotEquals(o2, "string")
        assertEquals(o2.hashCode(), o2.hashCode())
        assertTrue(o2.toString().contains("Offset"))

        assertEquals(5f, o2.component1())
        assertEquals(6f, o2.component2())
        assertEquals(7f, o2.component3())
        assertEquals(8f, o2.component4())
    }

    @Test
    fun testStroke() {
        val s = Stroke()
        assertFalse(s.isEnable)
        assertEquals(0f, s.strokeWidth, 0.01f)
        assertEquals(-101, s.strokeColor)
        assertEquals(StrokeType.INSIDE, s.strokeType)
        assertEquals(100, s.strokeAlpha)
        assertFalse(s.drawAsOverlay)
        assertEquals(0f, s.blur, 0.01f)
        assertEquals(BlurMaskFilter.Blur.NORMAL, s.blurType)

        s.updateStrokeWidth(2f)
        assertEquals(2f, s.strokeWidth, 0.01f)

        s.updateStrokeColor(0xFF00FF00.toInt())
        assertEquals(0xFF00FF00.toInt(), s.strokeColor)
        assertTrue(s.isEnable)

        s.updateStrokeAlpha(200)
        assertEquals(200, s.strokeAlpha)

        s.drawAsOverlay = true
        s.blur = 1.5f
        s.blurType = BlurMaskFilter.Blur.OUTER
        assertEquals(1.5f, s.blur, 0.01f)
        assertEquals(BlurMaskFilter.Blur.OUTER, s.blurType)
        assertTrue(s.drawAsOverlay)
    }

    @Test
    fun testGradient() {
        val g = Gradient()
        assertFalse(g.isEnable)
        assertEquals(-101, g.gradientStartColor)
        assertEquals(-101, g.gradientCenterColor)
        assertEquals(-101, g.gradientEndColor)
        assertEquals(0, g.gradientAngle)
        assertEquals(0f, g.gradientOffsetX, 0.01f)
        assertEquals(0f, g.gradientOffsetY, 0.01f)
        assertNull(g.gradientColors)
        assertNull(g.gradientPositions)

        g.updateGradientColor(0xFF111111.toInt(), 0xFF222222.toInt(), 0xFF333333.toInt())
        assertEquals(0xFF111111.toInt(), g.gradientStartColor)
        assertEquals(0xFF222222.toInt(), g.gradientCenterColor)
        assertEquals(0xFF333333.toInt(), g.gradientEndColor)
        // isEnable is true because default gradientAngle is 0 (not -1)
        assertTrue(g.isEnable)
        g.updateGradientAngle(90)
        assertTrue(g.isEnable)

        g.updateGradientColor(0xFF444444.toInt(), 0xFF555555.toInt())
        assertEquals(0xFF444444.toInt(), g.gradientStartColor)
        assertEquals(-101, g.gradientCenterColor)
        assertEquals(0xFF555555.toInt(), g.gradientEndColor)

        g.updateGradientOffsetX(10f)
        g.updateGradientOffsetY(20f)
        assertEquals(10f, g.gradientOffsetX, 0.01f)
        assertEquals(20f, g.gradientOffsetY, 0.01f)

        val matrix = Matrix()
        g.updateLocalMatrix(matrix)

        val positions = floatArrayOf(0f, 1f)
        g.updateGradientPositions(positions)
        assertArrayEquals(positions, g.gradientPositions, 0.01f)

        val colors = intArrayOf(0xFF111111.toInt(), 0xFF222222.toInt())
        g.updateGradientColors(colors)
        assertArrayEquals(colors, g.gradientColors)

        val shader = g.getGradientShader(0f, 0f, 100f, 100f)
        assertNotNull(shader)

        // Test custom gradient shader override
        g.updateGradientShader(shader)
        assertEquals(shader, g.getGradientShader(0f, 0f, 100f, 100f))

        // Let's test different angles to cover all branches in getGradientShader
        val angles = listOf(-45, 0, 45, 90, 135, 180, 225, 270, 315, 360, 405)
        for (angle in angles) {
            val grad = Gradient(
                gradientStartColor = 0xFF111111.toInt(),
                gradientEndColor = 0xFF222222.toInt(),
                gradientAngle = angle
            )
            val sh = grad.getGradientShader(0f, 0f, 100f, 100f)
            assertNotNull(sh)
        }

        // Test with center color not set vs set
        val gradCenter = Gradient(
            gradientStartColor = 0xFF111111.toInt(),
            gradientCenterColor = 0xFF333333.toInt(),
            gradientEndColor = 0xFF222222.toInt(),
            gradientAngle = 90
        )
        assertNotNull(gradCenter.getGradientShader(0f, 0f, 100f, 100f))
    }

    @Test
    fun testShadow() {
        val s = Shadow()
        assertFalse(s.isEnable)
        assertEquals(0f, s.blurSize, 0.01f)
        assertEquals(-101, s.shadowColor)
        assertEquals(0f, s.shadowOffsetX, 0.01f)
        assertEquals(0f, s.shadowOffsetY, 0.01f)
        assertEquals(0f, s.shadowSpread, 0.01f)

        s.updateShadowColor(0xFF111111.toInt())
        s.updateShadowOffsetX(1f)
        s.updateShadowOffsetY(2f)
        s.updateShadowSpread(3f)
        s.updateShadowBlurSize(4f)

        assertEquals(0xFF111111.toInt(), s.shadowColor)
        assertEquals(1f, s.shadowOffsetX, 0.01f)
        assertEquals(2f, s.shadowOffsetY, 0.01f)
        assertEquals(3f, s.shadowSpread, 0.01f)
        assertEquals(4f, s.blurSize, 0.01f)
        assertTrue(s.isEnable)

        s.updatePaint()
        s.updateShadowBlurSize(0f)
        s.updatePaint() // test no blur path in updatePaint

        val offset = RectF(10f, 10f, 50f, 50f)
        val radius = Radius(5f)
        s.updatePath(offset, radius)

        // Spread non-zero
        s.updateShadowSpread(5f)
        s.updatePath(offset, radius)

        // Radius null / disabled
        s.updatePath(offset, null)

        val canvas = Canvas()
        s.draw(canvas)
    }
}
