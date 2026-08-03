package com.sonsation.library.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Region
import android.os.Build
import android.view.View

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

        outsets.compute(context)
        outsetLeft = outsets.left
        outsetTop = outsets.top
        resolution = context.bitmapResolution

        // A tiny view at a low resolution would otherwise round down to a zero sized bitmap.
        val cacheWidth = ((width + outsets.left + outsets.right) * resolution).toInt().coerceAtLeast(1)
        val cacheHeight = ((height + outsets.top + outsets.bottom) * resolution).toInt().coerceAtLeast(1)

        try {
            if (bitmap?.width != cacheWidth || bitmap?.height != cacheHeight) {
                bitmap?.recycle()
                bitmap = Bitmap.createBitmap(cacheWidth, cacheHeight, Bitmap.Config.ARGB_8888)
                bitmapCanvas = Canvas(bitmap!!)
            }
        } catch (e: OutOfMemoryError) {
            release()
            return false
        }

        val target = bitmap ?: return true

        target.eraseColor(Color.TRANSPARENT)

        bitmapCanvas?.let { cv ->
            cv.save()
            cv.scale(resolution, resolution)
            cv.translate(outsets.left - context.bounds.left, outsets.top - context.bounds.top)

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

    private fun release() {
        bitmap?.recycle()
        bitmap = null
        bitmapCanvas = null
    }
}
