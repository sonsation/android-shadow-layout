package com.sonsation.library.render

import android.graphics.Path
import android.graphics.RectF
import com.sonsation.library.effet.Shadow

/**
 * Everything a [ShadowRenderer] needs in order to paint.
 *
 * The rect, the path and the shadow list are *references* to the objects ShadowLayout
 * already owns and mutates in place, so a context is created once per view and never
 * allocates during a frame. The scalar fields are refreshed by the view right before
 * the renderer runs.
 */
internal class ShadowRenderContext(
    val bounds: RectF,
    val backgroundPath: Path,
    val shadows: List<Shadow>
) {

    /** How far the stroke pushes the shape outside [bounds]. */
    var strokeOutset = 0f

    /** Blur radius of the stroke, which bleeds outside the stroke itself. */
    var strokeBlur = 0f

    /** Downscale factor for renderers that rasterize into a bitmap. */
    var bitmapResolution = 1f
}
