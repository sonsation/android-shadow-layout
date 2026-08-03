package com.sonsation.library.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Region
import android.os.Build
import android.view.View
import kotlin.math.ceil

/**
 * Rasterizes the shadows once into a software bitmap and blits that bitmap every frame.
 *
 * This is the only mode that survives on API < 28, where a `BlurMaskFilter` cannot run on
 * a hardware canvas. The bitmap can be rendered smaller than the view through
 * [ShadowRenderContext.bitmapResolution] to cut memory - a blurred shadow has no detail
 * to lose, and bilinear filtering hides the upscale.
 */
internal class BitmapCacheShadowRenderer : ShadowRenderer {

    // Bilinear filtering smooths the cached shadow bitmap when it is scaled back up
    // from bitmapResolution (< 1) instead of showing hard pixel steps.
    private val blitPaint by lazy { Paint(Paint.FILTER_BITMAP_FLAG) }

    private val outsets = ShadowOutsets()

    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null

    private var outsetLeft = 0f
    private var outsetTop = 0f
    private var resolution = 1f

    override fun prepare(context: ShadowRenderContext, cacheDirty: Boolean): Boolean {

        if (!cacheDirty && bitmap != null) {
            return true
        }

        val width = context.bounds.width()
        val height = context.bounds.height()

        if (width <= 0f || height <= 0f) {
            return true
        }

        resolution = context.bitmapResolution

        outsets.compute(context, padding = EDGE_PADDING / resolution)

        // Rounded out to a whole bitmap pixel. Both the rasterization below and the blit in
        // [draw] shift by this, and at a fractional offset that blit has to resample the
        // cache against the transparent space around it - which eats the faint outer tail
        // of the blur, where a gaussian keeps most of its reach.
        outsetLeft = ceil(outsets.left * resolution) / resolution
        outsetTop = ceil(outsets.top * resolution) / resolution

        // Rounded up rather than truncated: the outsets are float, so a truncated size
        // would drop the fraction off the right and bottom edges - and a tiny view at a
        // low resolution would round all the way down to a zero sized bitmap.
        val cacheWidth = ceil((width + outsetLeft + outsets.right) * resolution).toInt()
        val cacheHeight = ceil((height + outsetTop + outsets.bottom) * resolution).toInt()

        if (bitmap?.width != cacheWidth || bitmap?.height != cacheHeight) {
            release()
            // Nothing caps the blur or the spread of a shadow, so this size can be anything
            // at all - see [createShadowBitmap] for what that costs and why it is checked
            // rather than caught.
            val created = createShadowBitmap(cacheWidth, cacheHeight) ?: return false
            bitmap = created
            bitmapCanvas = Canvas(created)
        }

        val target = bitmap ?: return true

        target.eraseColor(Color.TRANSPARENT)

        bitmapCanvas?.let { cv ->
            cv.save()
            cv.scale(resolution, resolution)
            cv.translate(outsetLeft - context.bounds.left, outsetTop - context.bounds.top)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                cv.clipOutPath(context.backgroundPath)
            } else {
                @Suppress("DEPRECATION")
                cv.clipPath(context.backgroundPath, Region.Op.DIFFERENCE)
            }

            context.shadows.forEach { shadow ->
                shadow.updatePaint()
                if (shadow.isEnable) {
                    shadow.draw(cv)
                }
            }
            cv.restore()
        }

        return true
    }

    override fun draw(canvas: Canvas, context: ShadowRenderContext): Boolean {

        val cached = bitmap ?: return false

        canvas.save()
        val inverseScale = 1f / resolution
        canvas.scale(inverseScale, inverseScale)
        canvas.drawBitmap(cached, -outsetLeft * resolution, -outsetTop * resolution, blitPaint)
        canvas.restore()

        return true
    }

    override fun onUninstalled(view: View) {
        release()
    }

    /**
     * Drops the cache instead of recycling it.
     *
     * The bitmap the last frame drew is still held by that frame's display list, which the
     * render thread may not be done with - `recycle()` here would pull the pixels out from
     * under it. Letting go of the reference frees the memory just as surely, once nothing
     * is drawing it any more.
     */
    private fun release() {
        bitmap = null
        bitmapCanvas = null
    }

    companion object {
        /**
         * Slack around the rasterized shadows, in *bitmap* pixels.
         *
         * An anti-aliased edge paints up to a pixel past the geometry it covers, and
         * [blitPaint] filters the bitmap on the way back up, which samples a pixel further
         * still - a shadow touching the edge of the bitmap would be smeared outwards by
         * the clamp. One transparent pixel on every side covers both.
         *
         * Divided by the resolution before it reaches [ShadowOutsets] because those
         * outsets are in view space. Added there as a constant instead, the slack would
         * shrink with the downscale and be worth less than the rounding above costs.
         */
        private const val EDGE_PADDING = 1f
    }
}
