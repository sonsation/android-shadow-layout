package com.sonsation.library.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.sonsation.library.utils.blurExtent
import kotlin.math.ceil

/**
 * Draws one blurred shape through a software bitmap, for the platforms that drop a
 * `BlurMaskFilter` on a hardware canvas (below API 28 it is ignored outright - the shape
 * comes out sharp, with no error).
 *
 * Putting the whole view on `LAYER_TYPE_SOFTWARE` would also work, and is what the
 * platform docs suggest, but its bitmap is exactly the size of the view: everything that
 * bleeds past the bounds - shadows, spread, an OUTSIDE stroke - gets clipped away. This
 * sizes its bitmap to the shape *plus* what the blur spills outside it and blits at the
 * matching offset, so nothing is lost. It is the same trick that keeps
 * [BitmapCacheShadowRenderer] from clipping the shadows.
 */
internal class SoftwareBlurLayer {

    private var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null

    private val contentBounds = RectF()

    private var left = 0f
    private var top = 0f

    val isReady: Boolean
        get() = bitmap != null

    /**
     * Rasterizes [path] with [paint] into a bitmap just large enough to hold the result.
     *
     * The size is taken from the path itself rather than from the caller, so there is no
     * outset arithmetic to get wrong: the path's own bounds, grown by half the stroke
     * width the paint will add and by how far [blur] spreads.
     *
     * @return false if the bitmap could not be built, in which case the caller should
     *         paint the shape directly and accept the missing blur.
     */
    fun rebuild(path: Path, paint: Paint, blur: Float): Boolean {

        path.computeBounds(contentBounds, true)

        if (contentBounds.isEmpty) {
            release()
            return false
        }

        // A stroked paint paints half its width to either side of the path.
        val strokeSpill = if (paint.style == Paint.Style.FILL) 0f else paint.strokeWidth / 2f
        val outset = strokeSpill + blurExtent(blur) + EDGE_PADDING

        val width = ceil(contentBounds.width() + outset * 2f).toInt()
        val height = ceil(contentBounds.height() + outset * 2f).toInt()

        left = contentBounds.left - outset
        top = contentBounds.top - outset

        if (bitmap?.width != width || bitmap?.height != height) {
            release()
            // Nothing caps how far a blur may spread the shape, so this size can be
            // anything at all - see [createShadowBitmap] for what that costs.
            val created = createShadowBitmap(width, height) ?: return false
            bitmap = created
            bitmapCanvas = Canvas(created)
        }

        bitmap?.eraseColor(Color.TRANSPARENT)

        bitmapCanvas?.apply {
            save()
            translate(-left, -top)
            drawPath(path, paint)
            restore()
        }

        return true
    }

    /** Blits at the offset [rebuild] recorded, so the shape lands where it was drawn. */
    fun draw(canvas: Canvas) {
        val ready = bitmap ?: return
        canvas.drawBitmap(ready, left, top, null)
    }

    /**
     * Drops the rasterization instead of recycling it, for the same reason
     * [BitmapCacheShadowRenderer] does: the last frame's display list may still be holding
     * this bitmap on the render thread.
     */
    fun release() {
        bitmap = null
        bitmapCanvas = null
    }

    companion object {
        /**
         * Slack for the anti-aliased edge, which paints up to a pixel past the geometry it
         * covers. One pixel is enough here because this bitmap is 1:1 with the view - it is
         * never downscaled the way [BitmapCacheShadowRenderer]'s is.
         */
        private const val EDGE_PADDING = 1f
    }
}
