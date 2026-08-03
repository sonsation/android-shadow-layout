package com.sonsation.library.render

/**
 * How far the shadows bleed past the view bounds on each side.
 *
 * Blur, spread and offset all push a shadow outside the layout rect, and a cache that
 * only covered the bounds would clip it. Shared by every renderer that has to size an
 * offscreen surface around the view.
 *
 * What comes out is the geometry alone. Whatever slack a particular surface needs for its
 * own rounding depends on that surface - a display list rounds outwards for free, a
 * downscaled bitmap does not - so it is passed in rather than assumed here.
 */
internal class ShadowOutsets {

    var left = 0f
        private set
    var top = 0f
        private set
    var right = 0f
        private set
    var bottom = 0f
        private set

    /**
     * @param padding extra slack on every side, for a surface that needs room for its own
     *        rounding or for an anti-aliased edge. In the same units as the bounds, so a
     *        renderer that rasterizes at a reduced resolution has to scale it up to get a
     *        whole pixel out of it.
     */
    fun compute(context: ShadowRenderContext, padding: Float = 0f) {

        val base = context.strokeOutset + blurExtent(context.strokeBlur)

        left = base
        top = base
        right = base
        bottom = base

        context.shadows.forEach { shadow ->
            if (shadow.isEnable) {
                val bleed = context.strokeOutset + blurExtent(shadow.blurSize) + shadow.shadowSpread
                val ox = shadow.shadowOffsetX
                val oy = shadow.shadowOffsetY

                if (bleed - ox > left) left = bleed - ox
                if (bleed + ox > right) right = bleed + ox
                if (bleed - oy > top) top = bleed - oy
                if (bleed + oy > bottom) bottom = bleed + oy
            }
        }

        left += padding
        top += padding
        right += padding
        bottom += padding
    }
}
