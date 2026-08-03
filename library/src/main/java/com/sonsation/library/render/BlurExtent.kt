package com.sonsation.library.render

/**
 * How far a `BlurMaskFilter` of [radius] actually paints outside the shape it blurs.
 *
 * Not [radius]. The platform turns the radius into a Gaussian sigma and then paints out to
 * three of them, so the real bleed is a little over 1.7x what the caller asked for. Sizing
 * an offscreen surface from the radius alone clips the outer third of every blur: measured
 * on a device, a radius of 20 bleeds 36px, and a bitmap cache built for 22 cut it there
 * while the same shadow drawn straight onto the view canvas kept all 36.
 *
 * Three sigma is where the maths says a gaussian ends, and sweeping every radius from 1 to
 * 80 on a device agrees to within half a pixel either way - the mask is rasterized onto the
 * pixel grid, so it can land just past the continuous figure (the worst seen was 0.47px
 * over, at radii 11, 26 and 67). Rounding a whole pixel out covers that, and makes this an
 * upper bound rather than a fit: what a surface sized from it can afford is being a pixel
 * too large, never a pixel too small.
 */
internal fun blurExtent(radius: Float): Float {

    if (radius <= 0f) {
        return 0f
    }

    return (radius * SIGMA_PER_RADIUS + SIGMA_FLOOR) * SIGMA_TO_EXTENT + RASTER_SLACK
}

/** `SkBlurMask::ConvertRadiusToSigma`, which is what the platform applies to the radius. */
private const val SIGMA_PER_RADIUS = 0.57735f
private const val SIGMA_FLOOR = 0.5f

/** The Gaussian is cut off at three sigma; past that there is nothing left to draw. */
private const val SIGMA_TO_EXTENT = 3f

/** The pixel the rasterized mask can spill into past that continuous cut off. */
private const val RASTER_SLACK = 1f
