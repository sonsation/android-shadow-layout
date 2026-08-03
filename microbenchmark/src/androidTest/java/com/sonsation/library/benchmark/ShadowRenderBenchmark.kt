package com.sonsation.library.benchmark

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sonsation.library.HardwareRenderTarget
import com.sonsation.library.ShadowLayout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Per frame cost of each render mode, measured on a real GPU canvas.
 *
 * Hand rolled timing could not resolve these differences: the shadows cost single digit
 * microseconds per draw and the run to run spread on an unrooted phone was larger than
 * the gap between modes. The benchmark library warms up, repeats until the numbers
 * stabilise, watches for thermal throttling and reports a spread, which is what it takes
 * to say anything at this magnitude.
 *
 * Each iteration draws the view [DRAWS_PER_FRAME] times into one frame, so the shadow
 * work dominates the fixed cost of presenting the frame. Divide a reported figure by that
 * to get the cost of drawing one view once.
 *
 * HARDWARE_LAYER is deliberately absent. A hardware layer is applied by the parent while
 * drawing an attached child, and this harness calls `View.draw(Canvas)` on a detached
 * view, so the layer would never be exercised and the numbers would be a copy of
 * DEFAULT's. Measuring it needs an attached hierarchy driven by a real ViewRootImpl.
 */
@RunWith(AndroidJUnit4::class)
class ShadowRenderBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun shadowLayout(mode: Int, configure: ShadowLayout.() -> Unit): ShadowLayout =
        ShadowLayout(context).apply {
            build {
                renderMode(mode)
                backgroundColor(Color.WHITE)
                radius {
                    topLeftRadius = 24f
                    topRightRadius = 24f
                    bottomLeftRadius = 24f
                    bottomRightRadius = 24f
                }
                stroke {
                    strokeColor = Color.DKGRAY
                    strokeWidth = 4f
                }
            }
            configure()
            measure(
                View.MeasureSpec.makeMeasureSpec(VIEW_SIZE, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(VIEW_SIZE, View.MeasureSpec.EXACTLY)
            )
            layout(0, 0, VIEW_SIZE, VIEW_SIZE)
        }

    /**
     * Measures the steady state: the shadows never change, so every iteration after the
     * first is a frame a caching mode gets to skip work on.
     */
    private fun measureSteadyState(mode: Int, configure: ShadowLayout.() -> Unit) {
        val layout = shadowLayout(mode, configure)

        HardwareRenderTarget(TARGET_SIZE, TARGET_SIZE).use { target ->
            // Build the cache and let the pipeline settle before measuring.
            repeat(4) {
                target.renderCentred(layout, Color.WHITE, DRAWS_PER_FRAME)
                target.discardFrame()
            }

            benchmarkRule.measureRepeated {
                target.renderCentred(layout, Color.WHITE, DRAWS_PER_FRAME)
                runWithTimingDisabled { target.discardFrame() }
            }
        }
    }

    private val oneShadow: ShadowLayout.() -> Unit = {
        // The attribute free constructor starts with no shadows at all.
        removeAllBackgroundShadows()
        addBackgroundShadow(24f, 6f, 10f, 4f, Color.argb(180, 0, 0, 0))
    }

    private val heavyShadows: ShadowLayout.() -> Unit = {
        removeAllBackgroundShadows()
        repeat(4) { i ->
            addBackgroundShadow(60f, i * 4f, i * 4f, 8f, Color.argb(120, 0, 0, 0))
        }
    }

    /** The harness alone: presenting a frame that draws nothing but a background colour. */
    @Test
    fun baselineEmptyFrame() {
        HardwareRenderTarget(TARGET_SIZE, TARGET_SIZE).use { target ->
            benchmarkRule.measureRepeated {
                target.render { canvas -> canvas.drawColor(Color.WHITE) }
                runWithTimingDisabled { target.discardFrame() }
            }
        }
    }

    @Test
    fun oneShadowDefault() = measureSteadyState(ShadowLayout.RENDER_MODE_DEFAULT, oneShadow)

    @Test
    fun oneShadowBitmapCache() = measureSteadyState(ShadowLayout.RENDER_MODE_BITMAP_CACHE, oneShadow)

    @Test
    fun oneShadowRenderNode() = measureSteadyState(ShadowLayout.RENDER_MODE_RENDER_NODE, oneShadow)

    @Test
    fun heavyShadowsDefault() = measureSteadyState(ShadowLayout.RENDER_MODE_DEFAULT, heavyShadows)

    @Test
    fun heavyShadowsBitmapCache() = measureSteadyState(ShadowLayout.RENDER_MODE_BITMAP_CACHE, heavyShadows)

    @Test
    fun heavyShadowsRenderNode() = measureSteadyState(ShadowLayout.RENDER_MODE_RENDER_NODE, heavyShadows)

    companion object {
        private const val VIEW_SIZE = 300
        private const val TARGET_SIZE = 500
        private const val DRAWS_PER_FRAME = 40
    }
}
