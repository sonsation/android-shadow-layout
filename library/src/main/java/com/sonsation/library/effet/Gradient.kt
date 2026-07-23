package com.sonsation.library.effet

import android.graphics.*
import com.sonsation.library.utils.ViewHelper

class Gradient(
    var gradientStartColor: Int = ViewHelper.NOT_SET_COLOR,
    var gradientCenterColor: Int = ViewHelper.NOT_SET_COLOR,
    var gradientEndColor: Int = ViewHelper.NOT_SET_COLOR,
    var gradientAngle: Int = 0,
    var gradientOffsetX: Float = 0f,
    var gradientOffsetY: Float = 0f,
    var gradientColors: IntArray? = null,
    var gradientPositions: FloatArray? = null
) {

    val isEnable: Boolean
        get() = gradientShader != null || (((gradientStartColor != ViewHelper.NOT_SET_COLOR && gradientEndColor != ViewHelper.NOT_SET_COLOR)
                || (gradientColors != null && gradientColors?.isNotEmpty() == true)) && gradientAngle != -1)

    private var localMatrix: Matrix? = null

    private var gradientShader: LinearGradient? = null

    // Cache of the last built shader, reused while neither the gradient params nor
    // the target bounds change (localMatrix is reapplied on every return, so it is
    // not part of the cache key and never invalidates it).
    private var cachedShader: LinearGradient? = null
    private var cacheLeft = 0f
    private var cacheTop = 0f
    private var cacheRight = 0f
    private var cacheBottom = 0f

    private fun invalidateShaderCache() {
        cachedShader = null
    }

    fun getGradientShader(offsetLeft: Float, offsetTop: Float, offsetRight: Float, offsetBottom: Float): LinearGradient {

        if (gradientShader != null) {
            return gradientShader!!.apply {
                setLocalMatrix(localMatrix)
            }
        }

        cachedShader?.let {
            if (cacheLeft == offsetLeft && cacheTop == offsetTop && cacheRight == offsetRight && cacheBottom == offsetBottom) {
                return it.apply { setLocalMatrix(localMatrix) }
            }
        }

        val rawColors = if (gradientColors != null && gradientColors?.isNotEmpty() == true) {
            gradientColors!!
        } else {
            if (gradientCenterColor == ViewHelper.NOT_SET_COLOR) {
                intArrayOf(gradientStartColor, gradientEndColor)
            } else {
                intArrayOf(gradientStartColor, gradientCenterColor, gradientEndColor)
            }
        }

        // LinearGradient requires at least two colors; duplicate a single color so a
        // one-element gradient_colors input renders as a solid fill instead of crashing.
        val colors = if (rawColors.size < 2) {
            intArrayOf(rawColors.first(), rawColors.first())
        } else {
            rawColors
        }

        // positions must be null or exactly match the color count, otherwise
        // LinearGradient throws. Drop mismatched positions to stay safe.
        val positions = gradientPositions?.takeIf { it.size == colors.size }

        val trueAngle = if (gradientAngle > 0) gradientAngle % 360 else 0

        val width = offsetRight - offsetLeft
        val height = offsetBottom - offsetTop

        return when (trueAngle / 45) {
            0 -> {
                val x = offsetRight + gradientOffsetX
                LinearGradient(x, 0f, offsetLeft, 0f, colors, positions, Shader.TileMode.CLAMP)
            }
            1 -> {
                val x = offsetRight + gradientOffsetX
                val y = offsetTop + gradientOffsetY
                LinearGradient(x, offsetTop, offsetLeft, y, colors, positions, Shader.TileMode.CLAMP)
            }
            2 -> {
                val y = offsetTop + gradientOffsetY
                LinearGradient(0f, y, 0f, offsetBottom, colors, positions, Shader.TileMode.CLAMP)
            }
            3 -> {
                val x = width + gradientOffsetX
                val y = (height * 2) + gradientOffsetY
                LinearGradient(0f, y, x, offsetBottom, colors, positions, Shader.TileMode.CLAMP)
            }
            4 -> {
                val y = offsetBottom + gradientOffsetY
                LinearGradient(0f, y, 0f, 0f, colors, positions, Shader.TileMode.CLAMP)
            }
            5 -> {
                val x = offsetRight + gradientOffsetX
                val y = offsetTop + gradientOffsetY
                LinearGradient(0f, y, x, offsetTop, colors, positions, Shader.TileMode.CLAMP)
            }
            6 -> {
                val x = offsetTop + gradientOffsetX
                LinearGradient(x, 0f, offsetRight, 0f, colors, positions, Shader.TileMode.CLAMP)
            }
            else -> {
                val x = offsetRight + gradientOffsetX
                val y = offsetTop + gradientOffsetY
                LinearGradient(0f, y, x, offsetBottom, colors, positions, Shader.TileMode.CLAMP)
            }
        }.also { built ->
            cachedShader = built
            cacheLeft = offsetLeft
            cacheTop = offsetTop
            cacheRight = offsetRight
            cacheBottom = offsetBottom
        }.apply {
            if (localMatrix != null) {
                setLocalMatrix(localMatrix)
            }
        }
    }

    fun updateGradientColor(startColor: Int, centerColor: Int, endColor: Int) {
        this.gradientStartColor = startColor
        this.gradientCenterColor = centerColor
        this.gradientEndColor = endColor
        invalidateShaderCache()
    }

    fun updateGradientColor(startColor: Int, endColor: Int) {
        this.updateGradientColor(startColor, ViewHelper.NOT_SET_COLOR, endColor)
    }

    fun updateGradientAngle(angle: Int) {
        this.gradientAngle = angle
        invalidateShaderCache()
    }

    fun updateGradientOffsetX(offset: Float) {
        this.gradientOffsetX = offset
        invalidateShaderCache()
    }

    fun updateGradientOffsetY(offset: Float) {
        this.gradientOffsetY = offset
        invalidateShaderCache()
    }

    fun updateLocalMatrix(matrix: Matrix?) {
        this.localMatrix = matrix
    }

    fun updateGradientPositions(positions: FloatArray?) {
        this.gradientPositions = positions
        invalidateShaderCache()
    }

    fun updateGradientColors(colors: IntArray?) {
        this.gradientColors = colors
        invalidateShaderCache()
    }

    fun updateGradientShader(shader: LinearGradient?) {
        this.gradientShader = shader
    }
}