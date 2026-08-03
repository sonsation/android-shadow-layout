package com.sonsation.library.render

import android.graphics.Canvas

/**
 * Paints the shadows straight onto the view's canvas every frame.
 *
 * No cache, no extra memory. On API 28+ the blur runs on the GPU, which makes this the
 * cheapest option for shadows that change often; below that a `BlurMaskFilter` forces
 * the whole draw back into software, which is why the view defaults to
 * [BitmapCacheShadowRenderer] there.
 *
 * Also serves as the per-frame fallback for renderers that cannot draw on a given
 * canvas, so it must never fail.
 */
internal class DirectShadowRenderer : ShadowRenderer {

    override fun draw(canvas: Canvas, context: ShadowRenderContext): Boolean {
        canvas.drawShadowsOutsideBackground(context)
        return true
    }
}
