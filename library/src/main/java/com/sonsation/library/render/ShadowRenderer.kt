package com.sonsation.library.render

import android.graphics.Canvas
import android.graphics.Region
import android.os.Build
import android.view.View

/**
 * Strategy for getting a ShadowLayout's shadows onto the screen.
 *
 * The view keeps owning the geometry - paths, rects, paints - and hands it over through
 * a [ShadowRenderContext]. A renderer only decides *how* those shadows are painted:
 * straight onto the canvas, through a software bitmap cache, or through a GPU display
 * list. Adding a backend means adding an implementation here, not another branch in
 * `dispatchDraw`.
 */
internal interface ShadowRenderer {

    /**
     * The layer type the view has to be on for this renderer to work. Only the hardware
     * layer strategy needs one; the view applies it when the renderer is installed.
     */
    val preferredLayerType: Int
        get() = View.LAYER_TYPE_NONE

    /** Called once when this renderer becomes the view's active strategy. */
    fun onInstalled(view: View) = Unit

    /** Called when another renderer replaces this one. Must free any cached resource. */
    fun onUninstalled(view: View) = Unit

    /**
     * Rebuilds whatever this renderer caches. [cacheDirty] is true when the geometry or
     * the shadow paints changed since the last frame; a renderer holding no cache can
     * ignore it entirely.
     *
     * @return false when the renderer permanently gave up (it ran out of memory, say),
     *         in which case the view downgrades to [ShadowLayout.RENDER_MODE_DEFAULT].
     */
    fun prepare(context: ShadowRenderContext, cacheDirty: Boolean): Boolean = true

    /**
     * @return false when this renderer cannot draw on this particular canvas - the view
     *         then paints the shadows directly, for this frame only.
     */
    fun draw(canvas: Canvas, context: ShadowRenderContext): Boolean
}

/**
 * Paints every enabled shadow with the background shape cut out, so a translucent
 * background never shows the shadows stacked underneath it. Shared by the renderers
 * that paint shadows themselves instead of blitting a cache.
 */
internal fun Canvas.drawShadowsOutsideBackground(context: ShadowRenderContext) {
    save()
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            clipOutPath(context.backgroundPath)
        } else {
            @Suppress("DEPRECATION")
            clipPath(context.backgroundPath, Region.Op.DIFFERENCE)
        }

        context.shadows.forEach { shadow ->
            if (shadow.isEnable) {
                shadow.draw(this)
            }
        }
    } finally {
        restore()
    }
}
