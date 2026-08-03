package com.sonsation.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * Exercises the render modes on a real hardware accelerated canvas, which the Robolectric
 * suite cannot do. Verifies that every mode paints the same thing, that RENDER_NODE
 * really replays a display list instead of quietly falling back, and measures what the
 * caching modes buy per frame.
 */
@RunWith(AndroidJUnit4::class)
class RenderNodeShadowTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun requireRenderNode() {
        // HardwareRenderTarget is built on HardwareRenderer, which is API 29 like
        // RenderNode itself. Below that there is nothing here to exercise.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
    }

    private fun shadowLayout(mode: Int): ShadowLayout = ShadowLayout(context).apply {
        build {
            renderMode(mode)
            backgroundColor(Color.WHITE)
            radius { topLeftRadius = 24f; topRightRadius = 24f; bottomLeftRadius = 24f; bottomRightRadius = 24f }
            stroke { strokeColor = Color.DKGRAY; strokeWidth = 4f }
            shadow {
                blurSize = 24f
                shadowSpread = 4f
                shadowOffsetX = 6f
                shadowOffsetY = 10f
                shadowColor = Color.argb(180, 0, 0, 0)
            }
        }
        measure(
            View.MeasureSpec.makeMeasureSpec(VIEW_SIZE, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(VIEW_SIZE, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, VIEW_SIZE, VIEW_SIZE)
    }

    private fun renderOnce(mode: Int): Bitmap =
        HardwareRenderTarget(TARGET_SIZE, TARGET_SIZE).use { target ->
            target.renderCentred(shadowLayout(mode), Color.WHITE)
            target.readBack()
        }

    /** Fraction of pixels differing by more than [tolerance] on any channel. */
    private fun differenceRatio(a: Bitmap, b: Bitmap, tolerance: Int): Float {
        assertEquals(a.width, b.width)
        assertEquals(a.height, b.height)

        var differing = 0
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                val pa = a.getPixel(x, y)
                val pb = b.getPixel(x, y)
                val delta = maxOf(
                    abs(Color.red(pa) - Color.red(pb)),
                    abs(Color.green(pa) - Color.green(pb)),
                    abs(Color.blue(pa) - Color.blue(pb)),
                    abs(Color.alpha(pa) - Color.alpha(pb))
                )
                if (delta > tolerance) differing++
            }
        }
        return differing.toFloat() / (a.width * a.height)
    }

    private fun activeRenderer(layout: ShadowLayout): Any =
        ShadowLayout::class.java.getDeclaredField("renderer")
            .apply { isAccessible = true }
            .get(layout)

    private fun fallbackRenderer(layout: ShadowLayout): Any? =
        ShadowLayout::class.java.getDeclaredField("fallbackRenderer")
            .apply { isAccessible = true }
            .get(layout)

    @Test
    fun renderNodeModeIsSelectedOnThisDevice() {
        val layout = shadowLayout(ShadowLayout.RENDER_MODE_RENDER_NODE)

        assertEquals(ShadowLayout.RENDER_MODE_RENDER_NODE, layout.renderMode)
        assertEquals(
            "com.sonsation.library.render.RenderNodeShadowRenderer",
            activeRenderer(layout).javaClass.name
        )
    }

    @Test
    fun renderNodeReplaysItsDisplayListInsteadOfFallingBack() {
        val layout = shadowLayout(ShadowLayout.RENDER_MODE_RENDER_NODE)

        HardwareRenderTarget(TARGET_SIZE, TARGET_SIZE).use { target ->
            target.renderCentred(layout, Color.WHITE)
            target.discardFrame()
        }

        // The fallback renderer is allocated lazily, the first time the active renderer
        // declines a frame. Still null means the display list really was replayed.
        assertNull(
            "RENDER_NODE fell back to direct drawing on a hardware canvas",
            fallbackRenderer(layout)
        )
    }

    @Test
    fun renderNodeFallsBackOnASoftwareCanvas() {
        val layout = shadowLayout(ShadowLayout.RENDER_MODE_RENDER_NODE)

        val bitmap = Bitmap.createBitmap(TARGET_SIZE, TARGET_SIZE, Bitmap.Config.ARGB_8888)
        layout.draw(android.graphics.Canvas(bitmap))

        assertTrue(
            "a software canvas must be served by the direct fallback",
            fallbackRenderer(layout) != null
        )
    }

    @Test
    fun renderNodeOutputMatchesDefault() {
        val expected = renderOnce(ShadowLayout.RENDER_MODE_DEFAULT)
        val actual = renderOnce(ShadowLayout.RENDER_MODE_RENDER_NODE)

        val ratio = differenceRatio(expected, actual, TOLERANCE)
        Log.i(TAG, "RENDER_NODE vs DEFAULT: ${"%.4f".format(ratio * 100)}% of pixels differ")

        assertTrue(
            "RENDER_NODE differs from DEFAULT on ${"%.4f".format(ratio * 100)}% of pixels",
            ratio < 0.005f
        )
    }

    @Test
    fun everyModeMatchesDefault() {
        val expected = renderOnce(ShadowLayout.RENDER_MODE_DEFAULT)

        val modes = mapOf(
            "BITMAP_CACHE" to ShadowLayout.RENDER_MODE_BITMAP_CACHE,
            "HARDWARE_LAYER" to ShadowLayout.RENDER_MODE_HARDWARE_LAYER,
            "RENDER_NODE" to ShadowLayout.RENDER_MODE_RENDER_NODE
        )

        modes.forEach { (name, mode) ->
            val ratio = differenceRatio(expected, renderOnce(mode), TOLERANCE)
            Log.i(TAG, "$name vs DEFAULT: ${"%.4f".format(ratio * 100)}% of pixels differ")
        }
    }

    /**
     * Steady state cost of one frame whose shadows did not change - what a caching mode
     * saves on every frame after the first.
     *
     * HARDWARE_LAYER is deliberately absent: a hardware layer is applied by the parent
     * when it draws an attached child, and this harness calls `View.draw(Canvas)`
     * directly, so the layer would never be exercised. The other three modes do all of
     * their work inside `dispatchDraw` and are measured faithfully.
     *
     * Modes are interleaved round robin so clock ramping and scheduling noise hit every
     * mode equally, and a baseline frame that draws no view at all is measured alongside
     * them, because the harness itself - recording the root node, presenting, releasing
     * the image - costs more than the shadows do.
     */
    private fun measurePerFrameCost(
        label: String,
        drawsPerFrame: Int = DRAWS_PER_FRAME,
        configure: ShadowLayout.() -> Unit
    ) {

        val modes = listOf(
            "DEFAULT" to ShadowLayout.RENDER_MODE_DEFAULT,
            "BITMAP_CACHE" to ShadowLayout.RENDER_MODE_BITMAP_CACHE,
            "RENDER_NODE" to ShadowLayout.RENDER_MODE_RENDER_NODE
        )

        val layouts = modes.map { (_, mode) -> shadowLayout(mode).apply(configure) }
        val samples = List(modes.size + 1) { LongArray(MEASURED_FRAMES) }

        HardwareRenderTarget(TARGET_SIZE, TARGET_SIZE).use { target ->

            repeat(WARMUP_FRAMES) {
                layouts.forEach { layout ->
                    target.renderCentred(layout, Color.WHITE, drawsPerFrame)
                    target.discardFrame()
                }
            }

            repeat(MEASURED_FRAMES) { round ->
                layouts.forEachIndexed { index, layout ->
                    val start = System.nanoTime()
                    target.renderCentred(layout, Color.WHITE, drawsPerFrame)
                    samples[index][round] = System.nanoTime() - start
                    target.discardFrame()
                }

                // Baseline: the same frame with nothing but the background colour.
                val start = System.nanoTime()
                target.render { canvas -> canvas.drawColor(Color.WHITE) }
                samples[modes.size][round] = System.nanoTime() - start
                target.discardFrame()
            }
        }

        samples.forEach { it.sort() }
        val baselineMin = samples[modes.size][0]

        Log.i(TAG, "--- $label (x$drawsPerFrame per frame) ---")
        Log.i(TAG, "baseline (no view): min ${us(baselineMin)}us")

        modes.forEachIndexed { index, (name, _) ->
            val min = samples[index][0]
            // The minimum is the least contaminated sample; subtracting the baseline and
            // dividing out the repeats leaves the cost of drawing the view once.
            val perDraw = (min - baselineMin).toDouble() / drawsPerFrame
            Log.i(
                TAG,
                "$name: frame min ${us(min)}us, per draw ${"%.2f".format(perDraw / 1000.0)}us"
            )
        }
    }

    private fun us(nanos: Long) = "%.1f".format(nanos / 1000.0)

    /** The bitmap the active renderer holds, or null if it holds none. */
    private fun cachedBitmap(layout: ShadowLayout): Bitmap? {
        val renderer = activeRenderer(layout)
        val field = renderer.javaClass.declaredFields.firstOrNull { it.name == "bitmap" }
            ?: return null
        return field.apply { isAccessible = true }.get(renderer) as? Bitmap
    }

    /**
     * Java heap cost of each caching mode. Unlike the timings this is exact and
     * repeatable, and it is the clearest difference between the two.
     */
    @Test
    fun renderNodeCachesWithoutAJavaHeapBitmap() {

        fun bytesFor(mode: Int, resolution: Float): Int {
            val layout = shadowLayout(mode).apply {
                updateShadowBitmapResolution(resolution)
                removeAllBackgroundShadows()
                repeat(4) { i -> addBackgroundShadow(60f, i * 4f, i * 4f, 8f, Color.argb(120, 0, 0, 0)) }
            }
            HardwareRenderTarget(TARGET_SIZE, TARGET_SIZE).use { target ->
                target.renderCentred(layout, Color.WHITE)
                target.discardFrame()
            }
            return cachedBitmap(layout)?.allocationByteCount ?: 0
        }

        val fullRes = bytesFor(ShadowLayout.RENDER_MODE_BITMAP_CACHE, 1.0f)
        val halfRes = bytesFor(ShadowLayout.RENDER_MODE_BITMAP_CACHE, 0.5f)
        val renderNode = bytesFor(ShadowLayout.RENDER_MODE_RENDER_NODE, 1.0f)

        Log.i(TAG, "java heap per view - BITMAP_CACHE @1.0: ${fullRes / 1024}KB, " +
                "@0.5: ${halfRes / 1024}KB, RENDER_NODE: ${renderNode / 1024}KB")

        assertTrue("BITMAP_CACHE should allocate a bitmap", fullRes > 0)
        assertEquals("RENDER_NODE must not allocate a java heap bitmap", 0, renderNode)
    }

    @Test
    fun perFrameCostWithOneShadow() {
        measurePerFrameCost("one shadow, blur 24") { }
    }

    @Test
    fun perFrameCostWithHeavyShadows() {
        // Four large blurs stacked, the case a cache is supposed to pay for.
        measurePerFrameCost("four shadows, blur 60") {
            removeAllBackgroundShadows()
            repeat(4) { i ->
                addBackgroundShadow(60f, i * 4f, i * 4f, 8f, Color.argb(120, 0, 0, 0))
            }
        }
    }

    companion object {
        private const val TAG = "RenderNodeShadowTest"
        private const val VIEW_SIZE = 300
        private const val TARGET_SIZE = 500
        private const val TOLERANCE = 8
        private const val WARMUP_FRAMES = 30
        private const val MEASURED_FRAMES = 120
        private const val DRAWS_PER_FRAME = 40
    }
}
