package com.sonsation.library.effet

class Radius(
    var topLeftRadius: Float = 0f,
    var topRightRadius: Float = 0f,
    var bottomLeftRadius: Float = 0f,
    var bottomRightRadius: Float = 0f
) {

    constructor(radius: Float) : this(radius, radius, radius, radius)

    val isEnable =
        topLeftRadius > 0f || topRightRadius > 0f || bottomLeftRadius > 0f || bottomRightRadius > 0f
    var radiusHalf = false
    var radiusWeight = 1f

    fun updateRadius(radius: Float) {
        this.topLeftRadius = radius
        this.topRightRadius = radius
        this.bottomLeftRadius = radius
        this.bottomRightRadius = radius
    }

    fun updateRadius(tl: Float, tr: Float, bl: Float, br: Float) {
        this.topLeftRadius = tl
        this.topRightRadius = tr
        this.bottomLeftRadius = bl
        this.bottomRightRadius = br
    }

    fun getRadiusArray(height: Float): FloatArray {

        val targetTopLeftRadius = if (radiusHalf) {
            height.div(2f)
        } else {
            topLeftRadius * radiusWeight
        }
        val targetTopRightRadius = if (radiusHalf) {
            height.div(2f)
        } else {
            topRightRadius * radiusWeight
        }
        val targetBottomLeftRadius = if (radiusHalf) {
            height.div(2f)
        } else {
            bottomLeftRadius * radiusWeight
        }
        val targetBottomRightRadius = if (radiusHalf) {
            height.div(2f)
        } else {
            bottomRightRadius * radiusWeight
        }

        return floatArrayOf(
            targetTopLeftRadius,
            targetTopLeftRadius,
            targetTopRightRadius,
            targetTopRightRadius,
            targetBottomRightRadius,
            targetBottomRightRadius,
            targetBottomLeftRadius,
            targetBottomLeftRadius
        )
    }

    fun getRadiusArray(): FloatArray {

        val targetTopLeftRadius = topLeftRadius * radiusWeight
        val targetTopRightRadius = topRightRadius * radiusWeight
        val targetBottomLeftRadius = bottomLeftRadius * radiusWeight
        val targetBottomRightRadius = bottomRightRadius * radiusWeight

        return floatArrayOf(
            targetTopLeftRadius,
            targetTopLeftRadius,
            targetTopRightRadius,
            targetTopRightRadius,
            targetBottomRightRadius,
            targetBottomRightRadius,
            targetBottomLeftRadius,
            targetBottomLeftRadius
        )
    }
}