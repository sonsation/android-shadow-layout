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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Below API 28 the background and the stroke are rasterized into their own bitmaps so a
 * blur survives the hardware pipeline. Rasterizing a blurred shape is the expensive part
 * of drawing there, so a change that only touches one of them must not redo the other.
 *
 * This has to run on a device: Robolectric hands out bitmaps that no drawing ever reaches,
 * so every comparison between them passes whether the work was skipped or not.
 */
@RunWith(AndroidJUnit4::class)
class SoftwareBlurLayerSeparationTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun requireSoftwareRasterization() {
        assumeTrue(
            "the software blur layers only exist below API 28",
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P
        )
    }

    private fun blurredLayout(): ShadowLayout = ShadowLayout(context).apply {
        build {
            backgroundColor(Color.RED)
            // Without this the Radius is null and updateRadius quietly does nothing.
            radius { topLeftRadius = 4f; topRightRadius = 4f; bottomLeftRadius = 4f; bottomRightRadius = 4f }
            stroke {
                strokeColor = Color.BLACK
                strokeWidth = 6f
                blur = 10f
            }
            shadow {
                blurSize = 8f
                shadowColor = Color.GRAY
            }
        }
        updateBackgroundBlur(10f)
        measure(
            View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, SIZE, SIZE)
    }

    private fun draw(view: View) {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
    }

    /** The bitmap the named software blur layer currently holds. */
    private fun blurBitmap(layout: ShadowLayout, field: String): Bitmap {
        val layer = ShadowLayout::class.java.getDeclaredField(field)
            .apply { isAccessible = true }
            .get(layout)
        assertNotNull("$field was never created", layer)
        return layer!!.javaClass.getDeclaredField("bitmap")
            .apply { isAccessible = true }
            .get(layer) as Bitmap
    }

    /** The bitmaps are reused in place, so the contents have to be copied to compare. */
    private fun snapshot(layout: ShadowLayout, field: String): Bitmap =
        blurBitmap(layout, field).copy(Bitmap.Config.ARGB_8888, false)

    @Test
    fun aBackgroundChangeLeavesTheStrokeRasterizationAlone() {
        val layout = blurredLayout()
        draw(layout)

        val stroke = snapshot(layout, "strokeBlurLayer")
        val background = snapshot(layout, "backgroundBlurLayer")

        layout.updateBackgroundColor(Color.BLUE)
        draw(layout)

        assertTrue(
            "a background colour change re-rasterized the stroke",
            stroke.sameAs(blurBitmap(layout, "strokeBlurLayer"))
        )
        assertFalse(
            "the background rasterization did not pick the change up",
            background.sameAs(blurBitmap(layout, "backgroundBlurLayer"))
        )
    }

    @Test
    fun aStrokeChangeLeavesTheBackgroundRasterizationAlone() {
        val layout = blurredLayout()
        draw(layout)

        val stroke = snapshot(layout, "strokeBlurLayer")
        val background = snapshot(layout, "backgroundBlurLayer")

        layout.updateStrokeColor(Color.GREEN)
        draw(layout)

        assertTrue(
            "a stroke colour change re-rasterized the background",
            background.sameAs(blurBitmap(layout, "backgroundBlurLayer"))
        )
        assertFalse(
            "the stroke rasterization did not pick the change up",
            stroke.sameAs(blurBitmap(layout, "strokeBlurLayer"))
        )
    }

    @Test
    fun aShadowChangeRasterizesNeither() {
        val layout = blurredLayout()
        draw(layout)

        val stroke = snapshot(layout, "strokeBlurLayer")
        val background = snapshot(layout, "backgroundBlurLayer")

        layout.updateShadowBlurType(BlurMaskFilter.Blur.SOLID)
        draw(layout)

        assertTrue(
            "a shadow change re-rasterized the stroke",
            stroke.sameAs(blurBitmap(layout, "strokeBlurLayer"))
        )
        assertTrue(
            "a shadow change re-rasterized the background",
            background.sameAs(blurBitmap(layout, "backgroundBlurLayer"))
        )
    }

    /** A geometry change moves both paths, so both have to be redone. */
    @Test
    fun aGeometryChangeRasterizesBoth() {
        val layout = blurredLayout()
        draw(layout)

        val stroke = snapshot(layout, "strokeBlurLayer")
        val background = snapshot(layout, "backgroundBlurLayer")

        layout.updateRadius(20f)
        draw(layout)

        val strokeRedone = !stroke.sameAs(blurBitmap(layout, "strokeBlurLayer"))
        val backgroundRedone = !background.sameAs(blurBitmap(layout, "backgroundBlurLayer"))

        Log.i(TAG, "radius change: stroke redone=$strokeRedone, background redone=$backgroundRedone")

        assertTrue("a radius change did not reach the stroke", strokeRedone)
        assertTrue("a radius change did not reach the background", backgroundRedone)
    }

    companion object {
        private const val TAG = "BlurSeparationTest"
        private const val SIZE = 200
    }
}
