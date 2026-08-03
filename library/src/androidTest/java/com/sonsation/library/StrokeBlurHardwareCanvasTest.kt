package com.sonsation.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sonsation.library.model.StrokeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Does a stroke blur survive a hardware canvas?
 *
 * `BlurMaskFilter` reaches the screen through `Paint.setMaskFilter`, which the hardware
 * pipeline did not support on older releases - it drops the filter instead of failing, so
 * the stroke simply comes out sharp. The shadows dodge this in BITMAP_CACHE mode because
 * they are rasterized into a software bitmap first, but the stroke is always painted
 * straight onto the view's canvas, so nothing shields it.
 *
 * The check does not need a reference image: render the same view with the blur off and
 * with it on, and see whether the two differ. Running it on a software canvas as well
 * proves the setup produces a visible difference in the first place.
 */
@RunWith(AndroidJUnit4::class)
class StrokeBlurHardwareCanvasTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun strokedLayout(blur: Float): ShadowLayout = layout {
        updateStrokeBlur(blur)
    }

    private fun backgroundBlurredLayout(blur: Float): ShadowLayout = layout {
        updateBackgroundBlur(blur)
    }

    private fun layout(
        mode: Int? = null,
        configure: ShadowLayout.() -> Unit
    ): ShadowLayout = ShadowLayout(context).apply {
        build {
            if (mode != null) renderMode(mode)
            backgroundColor(Color.BLACK)
            stroke {
                strokeColor = Color.BLACK
                strokeWidth = 8f
            }
        }
        configure()
        measure(
            View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, SIZE, SIZE)
    }

    private fun drawCentred(canvas: Canvas, view: View) {
        canvas.drawColor(Color.WHITE)
        canvas.save()
        canvas.translate((TARGET - view.width) / 2f, (TARGET - view.height) / 2f)
        view.draw(canvas)
        canvas.restore()
    }

    /** Renders through a Surface, which hands out a genuinely hardware accelerated canvas. */
    private fun renderOnHardwareCanvas(view: View): Bitmap {
        val reader = ImageReader.newInstance(TARGET, TARGET, PixelFormat.RGBA_8888, 2)
        val readerThread = HandlerThread("image-reader").apply { start() }
        try {
            // The frame is posted asynchronously; acquiring straight after unlocking
            // returns nothing.
            val frame = CountDownLatch(1)
            reader.setOnImageAvailableListener({ frame.countDown() }, Handler(readerThread.looper))

            val surface = reader.surface
            val canvas = surface.lockHardwareCanvas()
            assertTrue(
                "lockHardwareCanvas did not return a hardware accelerated canvas",
                canvas.isHardwareAccelerated
            )
            try {
                drawCentred(canvas, view)
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }

            assertTrue("no frame was produced within 5s", frame.await(5, TimeUnit.SECONDS))

            val image = requireNotNull(reader.acquireNextImage()) { "no frame produced" }
            try {
                val plane = image.planes[0]
                val padded = Bitmap.createBitmap(
                    plane.rowStride / plane.pixelStride,
                    TARGET,
                    Bitmap.Config.ARGB_8888
                )
                padded.copyPixelsFromBuffer(plane.buffer)
                return Bitmap.createBitmap(padded, 0, 0, TARGET, TARGET)
            } finally {
                image.close()
            }
        } finally {
            reader.close()
            readerThread.quitSafely()
        }
    }

    private fun renderOnSoftwareCanvas(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(TARGET, TARGET, Bitmap.Config.ARGB_8888)
        drawCentred(Canvas(bitmap), view)
        return bitmap
    }

    /** Fraction of pixels that differ by more than a rounding error. */
    private fun differenceRatio(a: Bitmap, b: Bitmap): Float {
        var differing = 0
        for (y in 0 until TARGET) {
            for (x in 0 until TARGET) {
                val pa = a.getPixel(x, y)
                val pb = b.getPixel(x, y)
                val delta = maxOf(
                    abs(Color.red(pa) - Color.red(pb)),
                    abs(Color.green(pa) - Color.green(pb)),
                    abs(Color.blue(pa) - Color.blue(pb))
                )
                if (delta > 8) differing++
            }
        }
        return differing.toFloat() / (TARGET * TARGET)
    }

    /** Control: the blurs do reach the pixels when nothing about the canvas is in the way. */
    @Test
    fun blurAlwaysReachesTheOutputOnASoftwareCanvas() {

        val stroke = differenceRatio(
            renderOnSoftwareCanvas(strokedLayout(0f)),
            renderOnSoftwareCanvas(strokedLayout(BLUR))
        )
        val background = differenceRatio(
            renderOnSoftwareCanvas(backgroundBlurredLayout(0f)),
            renderOnSoftwareCanvas(backgroundBlurredLayout(BLUR))
        )

        Log.i(TAG, "software canvas: stroke ${pct(stroke)}, background ${pct(background)}")

        assertTrue("the stroke blur has no effect even in software", stroke > 0.001f)
        assertTrue("the background blur has no effect even in software", background > 0.001f)
    }

    /**
     * The blur has to reach the pixels on every version, hardware canvas or not.
     *
     * Before the fix this failed below API 28: measured on API 24 and API 27, a blurred
     * stroke or background came out pixel identical to no blur at all. The library now
     * rasterizes those two through [com.sonsation.library.render.SoftwareBlurLayer] there.
     */
    @Test
    fun blurReachesTheOutputOnAHardwareCanvasToo() {
        assumeTrue("lockHardwareCanvas needs API 23", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

        val stroke = differenceRatio(
            renderOnHardwareCanvas(strokedLayout(0f)),
            renderOnHardwareCanvas(strokedLayout(BLUR))
        )
        val background = differenceRatio(
            renderOnHardwareCanvas(backgroundBlurredLayout(0f)),
            renderOnHardwareCanvas(backgroundBlurredLayout(BLUR))
        )

        Log.i(
            TAG,
            "hardware canvas on API ${Build.VERSION.SDK_INT}: " +
                    "stroke ${pct(stroke)}, background ${pct(background)}"
        )

        assertTrue("the stroke blur was dropped", stroke > 0.001f)
        assertTrue("the background blur was dropped", background > 0.001f)
    }

    /**
     * The regression the software-layer approach would have caused.
     *
     * `LAYER_TYPE_SOFTWARE` would have made the blur work, but its bitmap is exactly the
     * size of the view, so everything drawn outside the bounds - the shadows above all -
     * would be clipped away. This checks that a view carrying a blurred stroke still
     * paints its shadow outside its own bounds.
     */
    @Test
    fun aBlurredStrokeDoesNotClipTheOuterShadow() {
        assumeTrue("lockHardwareCanvas needs API 23", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

        val withShadow = layout {
            updateStrokeBlur(BLUR)
            addBackgroundShadow(18f, 0f, 0f, 10f, Color.BLACK)
        }

        val rendered = renderOnHardwareCanvas(withShadow)

        // A band just outside the view's bounds, which only the shadow can reach.
        val outside = (TARGET - SIZE) / 2 - 6
        val centre = TARGET / 2
        val shadowPixel = rendered.getPixel(centre, outside)

        Log.i(
            TAG,
            "API ${Build.VERSION.SDK_INT}: pixel ${outside}px above the view bounds = " +
                    "#${Integer.toHexString(shadowPixel)}"
        )

        assertTrue(
            "nothing was drawn outside the view bounds - the shadow got clipped",
            Color.red(shadowPixel) < 240
        )
    }

    /**
     * The render mode decides how the *shadows* are cached; the stroke and the background
     * are painted onto the view's canvas either way. This checks that claim instead of
     * assuming it, by measuring every mode rather than whichever one the device defaults
     * to.
     */
    @Test
    fun blurReachesTheOutputInEveryRenderMode() {
        assumeTrue("lockHardwareCanvas needs API 23", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

        val modes = listOf(
            "DEFAULT" to ShadowLayout.RENDER_MODE_DEFAULT,
            "BITMAP_CACHE" to ShadowLayout.RENDER_MODE_BITMAP_CACHE,
            "HARDWARE_LAYER" to ShadowLayout.RENDER_MODE_HARDWARE_LAYER
        )

        modes.forEach { (name, mode) ->
            val sharp = renderOnHardwareCanvas(layout(mode) { updateStrokeBlur(0f) })
            val blurred = renderOnHardwareCanvas(layout(mode) { updateStrokeBlur(BLUR) })
            val stroke = differenceRatio(sharp, blurred)

            val backgroundSharp = renderOnHardwareCanvas(layout(mode) { updateBackgroundBlur(0f) })
            val backgroundBlurred = renderOnHardwareCanvas(layout(mode) { updateBackgroundBlur(BLUR) })
            val background = differenceRatio(backgroundSharp, backgroundBlurred)

            Log.i(
                TAG,
                "API ${Build.VERSION.SDK_INT} $name: stroke ${pct(stroke)}, background ${pct(background)}"
            )

            assertTrue("$name dropped the stroke blur", stroke > 0.001f)
            assertTrue("$name dropped the background blur", background > 0.001f)
        }
    }

    /**
     * A blurred OUTSIDE stroke reaches well past the view bounds. The software
     * rasterization sizes its bitmap from the path, so if that arithmetic is wrong the
     * outer edge is silently cut - which a blur/no-blur diff would not catch, because
     * both sides would be cut the same way.
     *
     * Measures how far the drawing actually extends and compares it against the geometry.
     */
    @Test
    fun aBlurredOutsideStrokeIsNotCutOffAtTheBitmapEdge() {
        assumeTrue("lockHardwareCanvas needs API 23", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

        val strokeWidth = 20f
        val rendered = renderOnHardwareCanvas(
            ShadowLayout(context).apply {
                build {
                    backgroundColor(Color.WHITE)
                    stroke {
                        strokeColor = Color.BLACK
                        this.strokeWidth = strokeWidth
                        strokeType = StrokeType.OUTSIDE
                        blur = BLUR
                    }
                }
                measure(
                    View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY)
                )
                layout(0, 0, SIZE, SIZE)
            }
        )

        // Topmost row holding anything other than the white background, down the centre.
        val centre = TARGET / 2
        var firstInkedRow = -1
        for (y in 0 until TARGET) {
            if (Color.red(rendered.getPixel(centre, y)) < 250) {
                firstInkedRow = y
                break
            }
        }

        val viewTop = (TARGET - SIZE) / 2
        val reach = viewTop - firstInkedRow

        Log.i(
            TAG,
            "API ${Build.VERSION.SDK_INT}: an OUTSIDE stroke of $strokeWidth blurred by $BLUR " +
                    "reaches ${reach}px past the bounds"
        )

        assertTrue("nothing was drawn at all", firstInkedRow >= 0)
        // The stroke alone already occupies strokeWidth outside the bounds; the blur adds
        // to that. Anything short of the stroke means the bitmap clipped it.
        assertTrue(
            "the blurred stroke only reached ${reach}px past the bounds, expected more than $strokeWidth",
            reach > strokeWidth
        )
    }

    /**
     * Whether the outermost drawn pixel is a fade or a cut.
     *
     * The rasterization sizes its bitmap as the blur radius, but a `BlurMaskFilter`
     * spreads further than its radius, so the tail could in principle run off the edge.
     * A blur that ends naturally trails off to near the background; one that was clipped
     * stops on a pixel that is still clearly dark.
     */
    @Test
    fun aLargeBlurFadesOutInsteadOfBeingCutOff() {
        assumeTrue("lockHardwareCanvas needs API 23", Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

        val bigBlur = 60f
        // A small view leaves room for the tail to end inside the target instead of
        // running off it, which would make the measurement meaningless.
        val smallSize = 80
        val rendered = renderOnHardwareCanvas(
            ShadowLayout(context).apply {
                build {
                    backgroundColor(Color.WHITE)
                    stroke {
                        strokeColor = Color.BLACK
                        strokeWidth = 12f
                        strokeType = StrokeType.OUTSIDE
                        blur = bigBlur
                    }
                }
                measure(
                    View.MeasureSpec.makeMeasureSpec(smallSize, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(smallSize, View.MeasureSpec.EXACTLY)
                )
                layout(0, 0, smallSize, smallSize)
            }
        )

        val centre = TARGET / 2
        var firstInkedRow = -1
        for (y in 0 until TARGET) {
            if (Color.red(rendered.getPixel(centre, y)) < 250) {
                firstInkedRow = y
                break
            }
        }

        assertTrue("nothing was drawn at all", firstInkedRow >= 0)
        assertTrue("the tail ran off the render target, so this proves nothing", firstInkedRow > 0)

        val edgeValue = Color.red(rendered.getPixel(centre, firstInkedRow))
        Log.i(
            TAG,
            "API ${Build.VERSION.SDK_INT}: blur $bigBlur ends at row $firstInkedRow " +
                    "with value $edgeValue (255 = background)"
        )

        // A natural tail is barely distinguishable from the background at its outer edge.
        assertTrue(
            "the outermost pixel is $edgeValue, too dark to be a fade - the blur was cut off",
            edgeValue >= 235
        )
    }

    private fun pct(ratio: Float) = "${"%.3f".format(ratio * 100)}%"

    companion object {
        private const val TAG = "StrokeBlurTest"
        private const val SIZE = 200
        private const val TARGET = 300
        private const val BLUR = 20f
    }
}
