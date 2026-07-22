package com.sonsation.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.view.LayoutInflater
import com.sonsation.library.ShadowLayout
import com.sonsation.library.model.StrokeType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShadowLayoutTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun testSimpleConstructor() {
        val layout = ShadowLayout(context)
        assertEquals(ShadowLayout.RENDER_MODE_DEFAULT, layout.renderMode)
        assertFalse(layout.autoAdjustPadding)
        assertFalse(layout.clipOutLine)
        assertEquals(com.sonsation.library.utils.ViewHelper.NOT_SET_COLOR, layout.backgroundColor)
        assertNotNull(layout.shadows)
    }

    @Test
    fun testXmlConstructorWithVariousAttributes() {
        val layoutId = context.resources.getIdentifier("test_shadow_layout", "layout", context.packageName)
        val viewId = context.resources.getIdentifier("shadow_layout_1", "id", context.packageName)
        val inflater = LayoutInflater.from(context)
        val root = inflater.inflate(layoutId, null)
        val layout = root.findViewById<ShadowLayout>(viewId)
        assertNotNull(layout)
        assertTrue(layout.autoAdjustPadding)
        assertTrue(layout.clipOutLine)
        assertEquals(ShadowLayout.RENDER_MODE_BITMAP_CACHE, layout.renderMode)

        val radius = layout.getRadiusInfo()
        assertNotNull(radius)
        assertEquals(1.2f, radius!!.radiusWeight, 0.01f)
        assertEquals(0.5f, radius.cornerSmoothing, 0.01f)

        val stroke = layout.getStrokeInfo()
        assertNotNull(stroke)
        assertEquals(StrokeType.CENTER, stroke!!.strokeType)
        assertEquals(128, stroke.strokeAlpha)
        assertEquals(BlurMaskFilter.Blur.SOLID, stroke.blurType)


        val gradient = layout.getGradientInfo()
        assertNotNull(gradient)
        assertEquals(Color.parseColor("#FF0000FF"), gradient!!.gradientStartColor)
        assertEquals(90, gradient.gradientAngle)

        assertEquals(1, layout.shadows.size)
        assertEquals(Color.parseColor("#FF888888"), layout.shadows[0].shadowColor)
    }

    @Test
    fun testXmlConstructorWithIndividualRadiiAndShadowArray() {
        val layoutId = context.resources.getIdentifier("test_shadow_layout", "layout", context.packageName)
        val viewId = context.resources.getIdentifier("shadow_layout_2", "id", context.packageName)
        val inflater = LayoutInflater.from(context)
        val root = inflater.inflate(layoutId, null)
        val layout = root.findViewById<ShadowLayout>(viewId)
        assertNotNull(layout)
        val radius = layout.getRadiusInfo()
        assertNotNull(radius)
        assertEquals(2f * context.resources.displayMetrics.density, radius!!.topLeftRadius, 0.01f)
        assertEquals(4f * context.resources.displayMetrics.density, radius.topRightRadius, 0.01f)
        assertEquals(6f * context.resources.displayMetrics.density, radius.bottomLeftRadius, 0.01f)
        assertEquals(8f * context.resources.displayMetrics.density, radius.bottomRightRadius, 0.01f)

        assertEquals(3, layout.shadows.size)
    }

    @Test
    fun testSettersAndUpdates() {
        val layout = ShadowLayout(context)
        layout.build {
            backgroundColor(Color.BLUE)
            backgroundBlur(10f)
            backgroundBlurType(BlurMaskFilter.Blur.INNER)
            renderMode(ShadowLayout.RENDER_MODE_HARDWARE_LAYER)
            radius {
                topLeftRadius = 5f
            }
            stroke {
                strokeWidth = 2f
            }
            gradient {
                gradientStartColor = Color.RED
                gradientEndColor = Color.GREEN
                gradientAngle = 45
            }
            strokeGradient {
                gradientStartColor = Color.YELLOW
                gradientEndColor = Color.CYAN
                gradientAngle = 90
            }
        }

        assertEquals(Color.BLUE, layout.backgroundColor)
        assertEquals(10f, layout.backgroundBlur, 0.01f)
        assertEquals(BlurMaskFilter.Blur.INNER, layout.backgroundBlurType)
        assertEquals(ShadowLayout.RENDER_MODE_HARDWARE_LAYER, layout.renderMode)
        assertEquals(5f, layout.getRadiusInfo()?.topLeftRadius ?: 0f, 0.01f)
        assertEquals(2f, layout.getStrokeInfo()?.strokeWidth ?: 0f, 0.01f)
        assertEquals(Color.RED, layout.getGradientInfo()?.gradientStartColor)

        // Individual updates
        layout.updateBackgroundColor(Color.BLACK)
        assertEquals(Color.BLACK, layout.backgroundColor)

        layout.updateRadius(15f)
        assertEquals(15f, layout.getRadiusInfo()?.topLeftRadius ?: 0f, 0.01f)

        layout.updateRadius(1f, 2f, 3f, 4f)
        assertEquals(1f, layout.getRadiusInfo()?.topLeftRadius ?: 0f, 0.01f)

        layout.updateCornerSmoothing(0.8f)
        assertEquals(0.8f, layout.getRadiusInfo()?.cornerSmoothing ?: 0f, 0.01f)

        layout.updateStrokeWidth(5f)
        assertEquals(5f, layout.getStrokeInfo()?.strokeWidth ?: 0f, 0.01f)

        layout.updateStrokeColor(Color.MAGENTA)
        assertEquals(Color.MAGENTA, layout.getStrokeInfo()?.strokeColor)

        layout.updateStrokeType(StrokeType.OUTSIDE)
        assertEquals(StrokeType.OUTSIDE, layout.getStrokeInfo()?.strokeType ?: StrokeType.INSIDE)

        layout.updateStrokeAlpha(150)
        assertEquals(150, layout.getStrokeInfo()?.strokeAlpha)

        layout.updateStrokeBlur(4f)
        assertEquals(4f, layout.getStrokeInfo()?.blur ?: 0f, 0.01f)

        layout.updateStrokeBlurType(BlurMaskFilter.Blur.OUTER)
        assertEquals(BlurMaskFilter.Blur.OUTER, layout.getStrokeInfo()?.blurType)



        layout.updateBackgroundRadiusHalf(true)
        assertTrue(layout.getRadiusInfo()?.radiusHalf ?: false)

        layout.updateBackgroundBlur(12f)
        assertEquals(12f, layout.backgroundBlur, 0.01f)

        layout.updateBackgroundBlurType(BlurMaskFilter.Blur.SOLID)
        assertEquals(BlurMaskFilter.Blur.SOLID, layout.backgroundBlurType)

        // Shadow modifications
        layout.removeAllBackgroundShadows()
        assertEquals(0, layout.shadows.size)

        layout.addBackgroundShadow(10f, 2f, 2f, Color.GRAY)
        assertEquals(1, layout.shadows.size)

        layout.addBackgroundShadow(12f, 3f, 3f, 1f, Color.RED)
        assertEquals(2, layout.shadows.size)

        layout.removeBackgroundShadowLast()
        assertEquals(1, layout.shadows.size)

        layout.addBackgroundShadow(15f, 4f, 4f, Color.BLUE)
        layout.removeBackgroundShadowFirst()
        assertEquals(1, layout.shadows.size)

        layout.addBackgroundShadow(20f, 5f, 5f, Color.GREEN)
        layout.removeBackgroundShadow(0)
        assertEquals(1, layout.shadows.size)

        val newShadow = com.sonsation.library.effet.Shadow(blurSize = 10f, shadowColor = Color.DKGRAY)
        layout.updateBackgroundShadow(0, newShadow)
        assertEquals(Color.DKGRAY, layout.shadows[0].shadowColor)

        layout.updateBackgroundShadow(0, 8f, 1f, 1f, Color.LTGRAY)
        assertEquals(Color.LTGRAY, layout.shadows[0].shadowColor)

        layout.updateBackgroundShadow(0, 8f, 1f, 1f, 2f, Color.RED)
        assertEquals(2f, layout.shadows[0].shadowSpread, 0.01f)

        // Create a new instance to prevent in-place mutation of the previously assigned newShadow
        layout.updateBackgroundShadow(com.sonsation.library.effet.Shadow(blurSize = 10f, shadowColor = Color.DKGRAY))
        assertEquals(Color.DKGRAY, layout.shadows[0].shadowColor)

        layout.updateBackgroundShadow(5f, 2f, 2f, Color.YELLOW)
        assertEquals(Color.YELLOW, layout.shadows[0].shadowColor)

        layout.updateBackgroundShadow(5f, 2f, 2f, 1f, Color.CYAN)
        assertEquals(1f, layout.shadows[0].shadowSpread, 0.01f)

        // Gradient updates
        layout.updateGradientColor(Color.RED, Color.GREEN, Color.BLUE)
        assertEquals(Color.GREEN, layout.getGradientInfo()?.gradientCenterColor)

        layout.updateGradientColor(Color.YELLOW, Color.CYAN)
        assertEquals(Color.YELLOW, layout.getGradientInfo()?.gradientStartColor)

        layout.updateGradientAngle(180)
        assertEquals(180, layout.getGradientInfo()?.gradientAngle)

        layout.updateGradientColors(intArrayOf(Color.RED, Color.BLUE))
        assertArrayEquals(intArrayOf(Color.RED, Color.BLUE), layout.getGradientInfo()?.gradientColors)

        layout.updateGradientPositions(floatArrayOf(0.1f, 0.9f))
        assertArrayEquals(floatArrayOf(0.1f, 0.9f), layout.getGradientInfo()?.gradientPositions, 0.01f)

        val matrix = Matrix()
        layout.updateLocalMatrix(matrix)

        layout.updateGradientOffsetX(4f)
        layout.updateGradientOffsetY(6f)
        assertEquals(4f, layout.getGradientInfo()?.gradientOffsetX ?: 0f, 0.01f)
        assertEquals(6f, layout.getGradientInfo()?.gradientOffsetY ?: 0f, 0.01f)

        // Stroke gradient updates
        layout.updateStrokeGradientColor(Color.RED, Color.GREEN, Color.BLUE)
        layout.updateStrokeGradientColor(Color.YELLOW, Color.CYAN)
        layout.updateStrokeGradientAngle(270)
        layout.updateStrokeGradientColors(intArrayOf(Color.BLACK, Color.WHITE))
        layout.updateStrokeGradientPositions(floatArrayOf(0f, 1f))
        layout.updateStrokeLocalMatrix(matrix)
        layout.updateStrokeGradientOffsetX(2f)
        layout.updateStrokeGradientOffsetY(2f)

        val strokeGrad = layout.strokeGradient
        assertNotNull(strokeGrad)
        assertEquals(270, strokeGrad!!.gradientAngle)
        assertArrayEquals(intArrayOf(Color.BLACK, Color.WHITE), strokeGrad.gradientColors)

        // Shader overrides
        layout.updateGradientShader(null)
        layout.updateStrokeGradientShader(null)
    }

    @Test
    fun testAutoAdjustPadding() {
        val layout = ShadowLayout(context)
        layout.build {
            stroke {
                strokeColor = Color.BLACK // Set valid color so stroke.isEnable is true
            }
        }
        layout.setAutoAdjustPadding(true)
        layout.updateStrokeWidth(10f)

        // StrokeType INSIDE: offset is strokeWidth = 10
        layout.updateStrokeType(StrokeType.INSIDE)
        layout.setPadding(0, 0, 0, 0)
        assertEquals(10, layout.paddingLeft)
        assertEquals(10, layout.paddingTop)

        // StrokeType CENTER: offset is strokeWidth - strokeWidth/2 = 5
        layout.updateStrokeType(StrokeType.CENTER)
        layout.setPadding(0, 0, 0, 0)
        assertEquals(5, layout.paddingLeft)

        // StrokeType OUTSIDE: offset is 0
        layout.updateStrokeType(StrokeType.OUTSIDE)
        layout.setPadding(0, 0, 0, 0)
        assertEquals(0, layout.paddingLeft)

        // Relative padding
        layout.updateStrokeType(StrokeType.INSIDE)
        layout.setPaddingRelative(0, 0, 0, 0)
        assertEquals(10, layout.paddingStart)
    }

    @Test
    fun testDrawAndLayoutCombinations() {
        val layout = ShadowLayout(context)
        layout.build {
            backgroundColor(Color.WHITE)
            radius {
                topLeftRadius = 10f
                radiusWeight = 1f
            }
            stroke {
                strokeColor = Color.BLACK
                strokeWidth = 4f
                strokeType = StrokeType.INSIDE
            }
            shadow {
                blurSize = 10f
                shadowColor = Color.GRAY
                shadowOffsetX = 5f
                shadowOffsetY = 5f
            }
            gradient {
                gradientStartColor = Color.RED
                gradientEndColor = Color.BLUE
                gradientAngle = 45
            }
        }

        // Layout and draw simple mode
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        layout.measure(200, 200)
        layout.layout(0, 0, 200, 200)
        layout.draw(canvas)

        // With builder update to BITMAP_CACHE render mode
        layout.build {
            renderMode(ShadowLayout.RENDER_MODE_BITMAP_CACHE)
        }
        layout.invalidate()
        layout.layout(0, 0, 200, 200)
        layout.draw(canvas)

        // Draw with clipToOutline enabled
        val layoutId = context.resources.getIdentifier("test_shadow_layout", "layout", context.packageName)
        val viewId = context.resources.getIdentifier("shadow_layout_3", "id", context.packageName)
        val inflater = LayoutInflater.from(context)
        val root = inflater.inflate(layoutId, null)
        val clipLayout = root.findViewById<ShadowLayout>(viewId)
        assertNotNull(clipLayout)
        clipLayout.build {
            stroke {
                strokeColor = Color.BLACK
                strokeWidth = 2f

            }
        }
        clipLayout.layout(0, 0, 200, 200)
        clipLayout.draw(canvas)

        // Test hasOverlappingRendering
        assertFalse(layout.hasOverlappingRendering())
        val simpleLayout = ShadowLayout(context)
        assertTrue(simpleLayout.hasOverlappingRendering())

        // Test onDetachedFromWindow via reflection
        try {
            val method = java.lang.Class.forName("android.view.View").getDeclaredMethod("onDetachedFromWindow")
            method.isAccessible = true
            method.invoke(layout)
        } catch (e: Exception) {
            fail("onDetachedFromWindow reflection failed: ${e.message}")
        }
    }

    @Test
    fun testBuilderDslEdgeCases() {
        val layout = ShadowLayout(context)
        layout.build {
            clearShadows()
            shadow(10) {
                blurSize = 5f
            }
        }
        assertEquals(1, layout.shadows.size)

        // Check compile of builder commit and invalid inputs
        layout.build {
            renderMode(-1)
        }
    }

    @Test
    @Config(sdk = [21])
    fun testDrawLegacySdk() {
        val layout = ShadowLayout(context)
        layout.build {
            backgroundColor(Color.WHITE)
            radius {
                topLeftRadius = 10f
            }
            stroke {
                strokeColor = Color.BLACK
                strokeWidth = 4f
                strokeType = StrokeType.CENTER
            }
            shadow {
                blurSize = 10f
                shadowColor = Color.GRAY
            }
        }
        layout.layout(0, 0, 200, 200)
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        layout.draw(canvas)
    }

    @Test
    fun testDrawStrokeOutsideAndGradiens() {
        val layout = ShadowLayout(context)
        layout.build {
            backgroundColor(Color.WHITE)
            radius {
                topLeftRadius = 10f
            }
            stroke {
                strokeColor = Color.BLACK
                strokeWidth = 4f
                strokeType = StrokeType.OUTSIDE
            }
            strokeGradient {
                gradientStartColor = Color.RED
                gradientEndColor = Color.BLUE
                gradientAngle = 90
            }
            gradient {
                gradientStartColor = Color.YELLOW
                gradientEndColor = Color.GREEN
                gradientAngle = 180
            }
        }
        layout.layout(0, 0, 200, 200)
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        layout.draw(canvas)
    }

    @Test
    fun testLargeStrokeWidthWithShadowNotClipped() {
        val layout = ShadowLayout(context)
        layout.build {
            renderMode(ShadowLayout.RENDER_MODE_BITMAP_CACHE)
            backgroundColor(Color.WHITE)
            stroke {
                strokeColor = Color.BLACK
                strokeWidth = 50f
                strokeType = StrokeType.OUTSIDE
            }
            shadow {
                blurSize = 20f
                shadowColor = Color.GRAY
            }
        }
        layout.layout(0, 0, 200, 200)
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        layout.draw(canvas)

        // For OUTSIDE stroke of width 50 and blur 20, maxOutset is at least 50 + 20 = 70.
        // Unscaled width (200 + 70*2 + outsets) with resolution 0.5 should result in cache width > (200 + 140) * 0.5 = 170.
        val cachedField = ShadowLayout::class.java.getDeclaredField("cachedBitmap").apply { isAccessible = true }
        val cached = cachedField.get(layout) as? Bitmap
        assertNotNull(cached)
        assertTrue("Cached bitmap width should include stroke outset", cached!!.width >= 170)

        // Also test CENTER stroke type
        layout.updateStrokeType(StrokeType.CENTER)
        layout.invalidate()
        layout.draw(canvas)
        val cachedCenter = cachedField.get(layout) as? Bitmap
        assertNotNull(cachedCenter)
        // For CENTER stroke of width 50, stroke outset is 25. Outset is at least 25 + 20 = 45.
        assertTrue("Cached bitmap width for CENTER stroke should include half stroke width", cachedCenter!!.width >= 145)
    }

    @Test
    fun testClipToOutlineWithOutsideStroke() {
        val layout = ShadowLayout(context)
        layout.build {
            backgroundColor(Color.WHITE)
            stroke {
                strokeColor = Color.BLACK
                strokeWidth = 20f
                strokeType = StrokeType.OUTSIDE
            }
        }
        layout.layout(0, 0, 200, 200)
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Ensure clipToOutline draws cleanly without exception when clipOutLine is true
        layout.draw(canvas)

        val contentPathField = ShadowLayout::class.java.getDeclaredField("contentPath").apply { isAccessible = true }
        val contentPath = contentPathField.get(layout) as? android.graphics.Path
        assertNotNull(contentPath)
        assertFalse(contentPath!!.isEmpty)

        val bounds = android.graphics.RectF()
        contentPath.computeBounds(bounds, true)
        // For OUTSIDE stroke, contentPath bounds should match the view bounds [0, 0, 200, 200]
        assertEquals(0f, bounds.left, 0.01f)
        assertEquals(0f, bounds.top, 0.01f)
        assertEquals(200f, bounds.right, 0.01f)
        assertEquals(200f, bounds.bottom, 0.01f)
    }
}
