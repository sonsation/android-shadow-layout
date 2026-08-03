package com.sonsation.library.render

/**
 * How far the shadows bleed past the view bounds on each side.
 *
 * Blur, spread and offset all push a shadow outside the layout rect, and a cache that
 * only covered the bounds would clip it. Shared by every renderer that has to size an
 * offscreen surface around the view.
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

    fun compute(context: ShadowRenderContext) {

        val base = context.strokeOutset + context.strokeBlur

        left = base
        top = base
        right = base
        bottom = base

        context.shadows.forEach { shadow ->
            if (shadow.isEnable) {
                val bleed = context.strokeOutset + shadow.blurSize + shadow.shadowSpread
                val ox = shadow.shadowOffsetX
                val oy = shadow.shadowOffsetY

                if (bleed - ox > left) left = bleed - ox
                if (bleed + ox > right) right = bleed + ox
                if (bleed - oy > top) top = bleed - oy
                if (bleed + oy > bottom) bottom = bleed + oy
            }
        }

        left += EDGE_PADDING
        top += EDGE_PADDING
        right += EDGE_PADDING
        bottom += EDGE_PADDING
    }

    companion object {
        /** Slack so anti-aliased edges are never cut off by rounding. */
        private const val EDGE_PADDING = 2f
    }
}
