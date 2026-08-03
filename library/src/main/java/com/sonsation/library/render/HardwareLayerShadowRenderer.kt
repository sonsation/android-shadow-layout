package com.sonsation.library.render

import android.graphics.Canvas
import android.view.View

/**
 * Draws like [DirectShadowRenderer], but promotes the whole view into a hardware layer
 * so the result is rasterized once and reused while the view is untouched.
 *
 * The layer covers the children too, so any child invalidation re-rasterizes everything.
 * For a static view that is the fastest mode; for a view whose children animate,
 * [RenderNodeShadowRenderer] caches only the shadows and leaves the children alone.
 */
internal class HardwareLayerShadowRenderer : ShadowRenderer {

    override val preferredLayerType: Int
        get() = View.LAYER_TYPE_HARDWARE

    override fun draw(canvas: Canvas, context: ShadowRenderContext): Boolean {
        canvas.drawShadowsOutsideBackground(context)
        return true
    }
}
