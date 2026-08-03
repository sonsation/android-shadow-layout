package com.sonsation.library

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import com.sonsation.library.effet.Shadow
import com.sonsation.library.render.BitmapCacheShadowRenderer
import com.sonsation.library.render.blurExtent
import com.sonsation.library.render.DirectShadowRenderer
import com.sonsation.library.render.HardwareLayerShadowRenderer
import com.sonsation.library.render.RenderNodeShadowRenderer
import com.sonsation.library.render.ShadowRenderContext
import com.sonsation.library.render.ShadowOutsets
import com.sonsation.library.render.ShadowRendererFactory
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RenderModeTest {

    private val context: android.content.Context get() = RuntimeEnvironment.getApplication()

    private fun activeRenderer(layout: ShadowLayout): Any =
        ShadowLayout::class.java.getDeclaredField("renderer")
            .apply { isAccessible = true }
            .get(layout)

    private fun draw(layout: ShadowLayout) {
        layout.layout(0, 0, 200, 200)
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        layout.draw(Canvas(bitmap))
    }

    private fun shadowLayout(mode: Int) = ShadowLayout(context).apply {
        build {
            renderMode(mode)
            backgroundColor(Color.WHITE)
            shadow {
                blurSize = 10f
                shadowColor = Color.GRAY
            }
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun renderNodeModeInstallsRenderNodeRenderer() {
        val layout = shadowLayout(ShadowLayout.RENDER_MODE_RENDER_NODE)

        assertEquals(ShadowLayout.RENDER_MODE_RENDER_NODE, layout.renderMode)
        assertTrue(activeRenderer(layout) is RenderNodeShadowRenderer)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun renderNodeModeFallsBackToBitmapCacheBelowApi29() {
        val layout = shadowLayout(ShadowLayout.RENDER_MODE_RENDER_NODE)

        // The reported mode is the one actually in effect, not the one requested.
        assertEquals(ShadowLayout.RENDER_MODE_BITMAP_CACHE, layout.renderMode)
        assertTrue(activeRenderer(layout) is BitmapCacheShadowRenderer)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun renderNodeModeDrawsOnSoftwareCanvasThroughFallback() {
        val layout = shadowLayout(ShadowLayout.RENDER_MODE_RENDER_NODE)

        // A software canvas cannot replay a display list; the view must still paint.
        draw(layout)

        assertTrue(activeRenderer(layout) is RenderNodeShadowRenderer)
    }

    @Test
    fun eachModeInstallsItsOwnStrategy() {
        assertTrue(activeRenderer(shadowLayout(ShadowLayout.RENDER_MODE_DEFAULT)) is DirectShadowRenderer)
        assertTrue(activeRenderer(shadowLayout(ShadowLayout.RENDER_MODE_BITMAP_CACHE)) is BitmapCacheShadowRenderer)
        assertTrue(activeRenderer(shadowLayout(ShadowLayout.RENDER_MODE_HARDWARE_LAYER)) is HardwareLayerShadowRenderer)
        assertTrue(activeRenderer(shadowLayout(ShadowLayout.RENDER_MODE_RENDER_NODE)) is RenderNodeShadowRenderer)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun defaultModeIsDirectFromApi28() {
        // RENDER_NODE is opt in: a view that asks for nothing still draws its shadows
        // every frame, the same as before the mode existed.
        val layout = ShadowLayout(context)

        assertEquals(ShadowLayout.RENDER_MODE_DEFAULT, layout.renderMode)
        assertTrue(activeRenderer(layout) is DirectShadowRenderer)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun defaultModeIsBitmapCacheBelowApi28() {
        // A blur cannot run on a hardware canvas here, so the shadows must be rasterized.
        val layout = ShadowLayout(context)

        assertEquals(ShadowLayout.RENDER_MODE_BITMAP_CACHE, layout.renderMode)
        assertTrue(activeRenderer(layout) is BitmapCacheShadowRenderer)
    }

    private fun softwareBlurLayer(layout: ShadowLayout, name: String): Any? =
        ShadowLayout::class.java.getDeclaredField(name)
            .apply { isAccessible = true }
            .get(layout)

    /**
     * Below API 28 a BlurMaskFilter is dropped on a hardware canvas, so the background and
     * the stroke rasterize themselves. Verified on real devices: on API 24 and 27 a stroke
     * or background blur renders pixel identical to no blur at all without this.
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun blurredStrokeAndBackgroundRasterizeBelowApi28() {
        val layout = ShadowLayout(context)
        draw(layout)
        assertNull(softwareBlurLayer(layout, "strokeBlurLayer"))
        assertNull(softwareBlurLayer(layout, "backgroundBlurLayer"))

        layout.build { stroke { strokeColor = Color.BLACK; strokeWidth = 4f; blur = 12f } }
        layout.updateBackgroundBlur(12f)
        draw(layout)
        assertNotNull(softwareBlurLayer(layout, "strokeBlurLayer"))
        assertNotNull(softwareBlurLayer(layout, "backgroundBlurLayer"))

        // Dropping the blurs releases the bitmaps again.
        layout.updateStrokeBlur(0f)
        layout.updateBackgroundBlur(0f)
        draw(layout)
        assertNull(softwareBlurLayer(layout, "strokeBlurLayer"))
        assertNull(softwareBlurLayer(layout, "backgroundBlurLayer"))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun blurIsLeftToTheHardwarePipelineFromApi28() {
        val layout = ShadowLayout(context)
        layout.build { stroke { strokeColor = Color.BLACK; strokeWidth = 4f; blur = 12f } }
        layout.updateBackgroundBlur(12f)
        draw(layout)

        assertNull(softwareBlurLayer(layout, "strokeBlurLayer"))
        assertNull(softwareBlurLayer(layout, "backgroundBlurLayer"))
    }

    /**
     * The view must never be pushed onto a software layer to get a blur: that layer's
     * bitmap is exactly the size of the view, so the shadows, the spread and an OUTSIDE
     * stroke would all be clipped off at the bounds.
     */
    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun aBlurNeverForcesTheViewOntoASoftwareLayer() {
        val layout = shadowLayout(ShadowLayout.RENDER_MODE_DEFAULT)
        layout.build { stroke { strokeColor = Color.BLACK; strokeWidth = 4f; blur = 12f } }
        layout.updateBackgroundBlur(12f)
        draw(layout)

        assertEquals(View.LAYER_TYPE_NONE, layout.layerType)
    }

    @Test
    fun hardwareLayerModeRestoresLayerTypeWhenReplaced() {
        val layout = shadowLayout(ShadowLayout.RENDER_MODE_HARDWARE_LAYER)
        draw(layout)
        assertEquals(View.LAYER_TYPE_HARDWARE, layout.layerType)

        layout.updateRenderMode(ShadowLayout.RENDER_MODE_DEFAULT)
        draw(layout)
        assertEquals(View.LAYER_TYPE_NONE, layout.layerType)
    }

    @Test
    fun switchingModesKeepsDrawingAndReleasesThePreviousCache() {
        val layout = shadowLayout(ShadowLayout.RENDER_MODE_BITMAP_CACHE)
        draw(layout)

        val cache = activeRenderer(layout) as BitmapCacheShadowRenderer
        val bitmapField = BitmapCacheShadowRenderer::class.java.getDeclaredField("bitmap")
            .apply { isAccessible = true }
        assertTrue("bitmap cache should have been built", bitmapField.get(cache) != null)

        layout.updateRenderMode(ShadowLayout.RENDER_MODE_DEFAULT)

        assertTrue(activeRenderer(layout) is DirectShadowRenderer)
        assertTrue("previous cache should be released", bitmapField.get(cache) == null)

        draw(layout)
    }

    @Test
    fun unknownModeResolvesToDefault() {
        assertEquals(ShadowLayout.RENDER_MODE_DEFAULT, ShadowRendererFactory.resolveMode(99))
        assertEquals(ShadowLayout.RENDER_MODE_DEFAULT, ShadowRendererFactory.resolveMode(-1))
    }

    @Test
    fun outsetsCoverBlurSpreadAndOffset() {
        val shadows = listOf(
            Shadow(blurSize = 10f, shadowColor = Color.GRAY, shadowOffsetX = 5f, shadowOffsetY = -3f, shadowSpread = 4f)
        )
        val context = ShadowRenderContext(RectF(0f, 0f, 100f, 100f), Path(), shadows).apply {
            strokeOutset = 2f
            strokeBlur = 1f
        }

        val outsets = ShadowOutsets().apply { compute(context) }

        // bleed = strokeOutset + how far the blur reaches + spread, shifted by the offset.
        val bleed = 2f + blurExtent(10f) + 4f

        assertEquals(bleed - 5f, outsets.left, 0.01f)
        assertEquals(bleed + 5f, outsets.right, 0.01f)
        assertEquals(bleed + 3f, outsets.top, 0.01f)
        assertEquals(bleed - 3f, outsets.bottom, 0.01f)
    }

    @Test
    fun outsetsNeverFallBelowTheStrokeBleed() {
        val context = ShadowRenderContext(RectF(0f, 0f, 100f, 100f), Path(), emptyList()).apply {
            strokeOutset = 8f
            strokeBlur = 4f
        }

        val outsets = ShadowOutsets().apply { compute(context) }

        assertEquals(8f + blurExtent(4f), outsets.left, 0.01f)
        assertEquals(8f + blurExtent(4f), outsets.right, 0.01f)
    }

    @Test
    fun outsetsCarryThePaddingTheCallerAsksFor() {
        val context = ShadowRenderContext(RectF(0f, 0f, 100f, 100f), Path(), emptyList()).apply {
            strokeOutset = 8f
        }

        val outsets = ShadowOutsets().apply { compute(context, padding = 4f) }

        assertEquals(12f, outsets.left, 0.01f)
        assertEquals(12f, outsets.top, 0.01f)
        assertEquals(12f, outsets.right, 0.01f)
        assertEquals(12f, outsets.bottom, 0.01f)
    }

    /**
     * A BlurMaskFilter paints well past its own radius - the platform reads that radius as
     * a Gaussian and draws out to three sigma. Sizing a cache from the radius alone cuts
     * the blur off, which is what [blurExtent] exists to stop.
     *
     * The expected bleeds are measured, not derived: each is how far past the edge of a
     * rect the last non transparent pixel sits, after filling that rect through a
     * BlurMaskFilter of that radius. `ShadowBleedTest` takes the same measurements on a
     * device, where they can move; these are the record of what was seen there.
     */
    @Test
    fun blurReachesFurtherThanItsRadius() {
        val measured = mapOf(1f to 3f, 4f to 8f, 10f to 19f, 20f to 36f, 40f to 70f)

        measured.forEach { (radius, bleed) ->
            val extent = blurExtent(radius)

            assertTrue(
                "radius $radius bleeds ${bleed}px, and $extent would clip it",
                extent >= bleed
            )
            assertTrue(
                "radius $radius bleeds ${bleed}px, and $extent wastes the difference",
                extent <= bleed + 2f
            )
        }
    }

    @Test
    fun noBlurReachesNowhere() {
        assertEquals(0f, blurExtent(0f), 0f)
        assertEquals(0f, blurExtent(-1f), 0f)
    }
}
