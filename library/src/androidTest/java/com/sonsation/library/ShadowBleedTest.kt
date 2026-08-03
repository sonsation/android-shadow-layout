package com.sonsation.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sonsation.library.render.blurExtent
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * How far a shadow reaches outside the view, measured against the platform rather than
 * assumed.
 *
 * A cached shadow is only correct if it is the same shadow: a cache surface sized from the
 * blur radius clips it, because a `BlurMaskFilter` paints out to three sigma and its sigma
 * is over half again its radius. That went unnoticed until it was measured - the shadow
 * simply ended sooner in one render mode than another, with nothing to fail.
 */
@RunWith(AndroidJUnit4::class)
class ShadowBleedTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * The bound [blurExtent] promises has to hold on the device, or every surface sized
     * from it clips.
     */
    @Test
    fun blurStaysInsideTheExtentItIsSizedFor() {
        for (radius in floatArrayOf(1f, 4f, 10f, 20f, 40f)) {
            val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
            }

            Canvas(bitmap).drawPath(Path().apply {
                addRect(RectF(SHAPE, SHAPE, SIZE - SHAPE, SIZE - SHAPE), Path.Direction.CW)
            }, paint)

            val bleed = bleedPastEdge(bitmap, SIZE - SHAPE.toInt(), minAlpha = 1)
            bitmap.recycle()

            assertTrue(
                "a blur of $radius bleeds ${bleed}px, past the ${blurExtent(radius)} it is sized for",
                bleed <= blurExtent(radius)
            )
        }
    }

    /**
     * The bitmap cache has to hold the whole shadow, not the part that fits.
     *
     * Compared on a software canvas, where both modes render the same way and the only
     * difference left is the surface the cache rasterizes into. The comparison runs per
     * alpha threshold because the outer tail of a blur is a handful of alpha steps: a
     * clipped cache ends abruptly at one column at every threshold, a resampled one comes
     * up short only at the faintest.
     */
    @Test
    fun theBitmapCacheKeepsTheWholeShadow() {
        val direct = shadowProfile(ShadowLayout.RENDER_MODE_DEFAULT)
        val cached = shadowProfile(ShadowLayout.RENDER_MODE_BITMAP_CACHE)

        THRESHOLDS.forEachIndexed { i, minAlpha ->
            assertTrue(
                "at alpha >= $minAlpha the cached shadow reaches ${cached[i]}px against ${direct[i]}px drawn straight",
                cached[i] >= direct[i]
            )
        }
    }

    /** How far past the view's right edge the shadow reaches, at each of [THRESHOLDS]. */
    private fun shadowProfile(mode: Int): IntArray {
        val layout = ShadowLayout(context).apply {
            // The cache is compared to the direct render pixel for pixel, so it must not be
            // downscaled - that is a separate, deliberate loss.
            updateShadowBitmapResolution(1f)
            build {
                renderMode(mode)
                backgroundColor(Color.WHITE)
                shadow { blurSize = BLUR; shadowSpread = 0f; shadowColor = Color.BLACK }
            }
            measure(
                View.MeasureSpec.makeMeasureSpec(VIEW, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(VIEW, View.MeasureSpec.EXACTLY)
            )
            layout(0, 0, VIEW, VIEW)
        }

        // Centred, so the shadow has room to run out on every side.
        val bitmap = Bitmap.createBitmap(TARGET, TARGET, Bitmap.Config.ARGB_8888)
        val offset = (TARGET - VIEW) / 2f
        Canvas(bitmap).apply {
            translate(offset, offset)
            layout.draw(this)
        }

        val profile = IntArray(THRESHOLDS.size) { i ->
            bleedPastEdge(bitmap, (TARGET + VIEW) / 2, THRESHOLDS[i])
        }

        bitmap.recycle()

        return profile
    }

    /**
     * Scans the middle row of [bitmap] inwards from its right side for the last pixel that
     * is at least [minAlpha] opaque, and returns how far past [edge] that lands.
     */
    private fun bleedPastEdge(bitmap: Bitmap, edge: Int, minAlpha: Int): Int {
        val width = bitmap.width
        val row = IntArray(width)

        bitmap.getPixels(row, 0, width, 0, bitmap.height / 2, width, 1)

        for (x in width - 1 downTo edge) {
            if (Color.alpha(row[x]) >= minAlpha) {
                return x - edge + 1
            }
        }

        return 0
    }

    companion object {
        private val THRESHOLDS = intArrayOf(1, 2, 4, 8, 16)

        private const val BLUR = 20f
        private const val VIEW = 200
        private const val TARGET = 600

        private const val SIZE = 900
        private const val SHAPE = 100f
    }
}
