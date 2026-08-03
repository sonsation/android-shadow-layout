package com.sonsation.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * A gradient reaches the pixels through a `Shader`, whose coordinates live in the canvas
 * space at the moment of drawing. [com.sonsation.library.render.SoftwareBlurLayer] draws
 * into its own bitmap with the canvas translated, so the shader has to travel with the
 * path - if it does not, the gradient lands offset or collapses to one colour.
 *
 * Runs on every version: below API 28 the rasterization is in play, above it the same
 * assertions describe the native path, so the two are held to one standard.
 */
@RunWith(AndroidJUnit4::class)
class SoftwareBlurLayerShaderTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun render(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(TARGET, TARGET, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.translate((TARGET - view.width) / 2f, (TARGET - view.height) / 2f)
        view.draw(canvas)
        return bitmap
    }

    private fun gradientLayout(blur: Float): ShadowLayout = ShadowLayout(context).apply {
        build {
            backgroundColor(Color.WHITE)
            gradient {
                gradientStartColor = Color.RED
                gradientEndColor = Color.BLUE
                gradientAngle = 0 // left to right
            }
        }
        updateBackgroundBlur(blur)
        measure(
            View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, SIZE, SIZE)
    }

    /** Samples inside the shape, a quarter in from each side. */
    private fun sampleLeftAndRight(bitmap: Bitmap): Pair<Int, Int> {
        val viewLeft = (TARGET - SIZE) / 2
        val y = TARGET / 2
        return bitmap.getPixel(viewLeft + SIZE / 4, y) to bitmap.getPixel(viewLeft + SIZE * 3 / 4, y)
    }

    @Test
    fun aBlurredGradientKeepsItsColoursAndDirection() {

        val sharp = sampleLeftAndRight(render(gradientLayout(0f)))
        val blurred = sampleLeftAndRight(render(gradientLayout(BLUR)))

        Log.i(
            TAG,
            "API ${Build.VERSION.SDK_INT}: sharp L=#${Integer.toHexString(sharp.first)} " +
                    "R=#${Integer.toHexString(sharp.second)}, blurred " +
                    "L=#${Integer.toHexString(blurred.first)} R=#${Integer.toHexString(blurred.second)}"
        )

        // Whichever way round the angle puts them, the two ends must be different
        // colours - a shader left behind would flatten the shape to one.
        assertTrue(
            "the sharp gradient is a single colour",
            abs(Color.red(sharp.first) - Color.red(sharp.second)) > 40
        )
        assertTrue(
            "the blurred gradient collapsed to one colour - the shader was left behind",
            abs(Color.red(blurred.first) - Color.red(blurred.second)) > 40
        )

        // Blurring softens the edges; well inside the shape it must not move the gradient.
        val leftShift = abs(Color.red(sharp.first) - Color.red(blurred.first))
        val rightShift = abs(Color.red(sharp.second) - Color.red(blurred.second))
        assertTrue(
            "the blurred gradient shifted across the shape (left $leftShift, right $rightShift)",
            leftShift < 40 && rightShift < 40
        )
    }

    /** The non-NORMAL blur styles spread differently; none of them may be cut off. */
    @Test
    fun everyBlurStyleStaysInsideItsBitmap() {
        BlurMaskFilter.Blur.entries.forEach { style ->
            val layout = ShadowLayout(context).apply {
                build {
                    backgroundColor(Color.BLACK)
                    stroke {
                        strokeColor = Color.BLACK
                        strokeWidth = 10f
                        blur = BLUR
                        blurType = style
                    }
                }
                measure(
                    View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY)
                )
                layout(0, 0, SIZE, SIZE)
            }

            val rendered = render(layout)
            val centre = TARGET / 2
            var firstInkedRow = -1
            for (y in 0 until TARGET) {
                if (Color.red(rendered.getPixel(centre, y)) < 250) {
                    firstInkedRow = y
                    break
                }
            }

            val edge = if (firstInkedRow >= 0) Color.red(rendered.getPixel(centre, firstInkedRow)) else 255
            Log.i(TAG, "API ${Build.VERSION.SDK_INT} $style: starts at row $firstInkedRow, value $edge")

            val viewTop = (TARGET - SIZE) / 2

            assertTrue("$style drew nothing", firstInkedRow >= 0)
            assertTrue("$style ran off the render target", firstInkedRow > 0)

            if (style == BlurMaskFilter.Blur.INNER) {
                // INNER blurs towards the middle, so the outer edge is meant to be hard
                // and nothing should appear outside the shape at all.
                assertTrue("$style spilled outside the shape", firstInkedRow >= viewTop)
            } else {
                assertTrue("$style was cut off at the bitmap edge (value $edge)", edge >= 200)
            }
        }
    }

    companion object {
        private const val TAG = "BlurShaderTest"
        private const val SIZE = 160
        private const val TARGET = 320
        private const val BLUR = 15f
    }
}
