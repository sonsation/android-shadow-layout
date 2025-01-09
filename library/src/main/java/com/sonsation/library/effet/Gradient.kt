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
        get() = ((gradientStartColor != ViewHelper.NOT_SET_COLOR && gradientEndColor != ViewHelper.NOT_SET_COLOR)
                || (gradientColors != null && gradientColors?.isNotEmpty() == true)) && gradientAngle != -1

    private var localMatrix: Matrix? = null

    fun getGradientShader(offsetLeft: Float, offsetTop: Float, offsetRight: Float, offsetBottom: Float): LinearGradient {

        val colors = if (gradientColors != null && gradientColors?.isNotEmpty() == true) {
            gradientColors!!
        } else {
            if (gradientCenterColor == ViewHelper.NOT_SET_COLOR) {
                intArrayOf(gradientStartColor, gradientEndColor)
            } else {
                intArrayOf(gradientStartColor, gradientCenterColor, gradientEndColor)
            }
        }

        var realAngle = 0

        if (gradientAngle > 0) {
            val trueAngle = gradientAngle % 360
            realAngle = trueAngle + 360
        }

        val trueAngle = realAngle % 360

        val width = offsetRight - offsetLeft
        val height = offsetBottom - offsetTop

        return when (trueAngle / 45) {
            0 -> {
                val x = offsetRight + gradientOffsetX
                LinearGradient(x, 0f, offsetLeft, 0f, colors, gradientPositions, Shader.TileMode.CLAMP)
            }
            1 -> {
                val x = offsetRight + gradientOffsetX
                val y = offsetTop + gradientOffsetY
                LinearGradient(x, offsetTop, offsetLeft, y, colors, gradientPositions, Shader.TileMode.CLAMP)
            }
            2 -> {
                val y = offsetTop + gradientOffsetY
                LinearGradient(0f, y, 0f, offsetBottom, colors, gradientPositions, Shader.TileMode.CLAMP)
            }
            3 -> {
                val x = width + gradientOffsetX
                val y = (height * 2) + gradientOffsetY
                LinearGradient(0f, y, x, offsetBottom, colors, gradientPositions, Shader.TileMode.CLAMP)
            }
            4 -> {
                val y = offsetBottom + gradientOffsetY
                LinearGradient(0f, y, 0f, 0f, colors, gradientPositions, Shader.TileMode.CLAMP)
            }
            5 -> {
                val x = offsetRight + gradientOffsetX
                val y = offsetTop + gradientOffsetY
                LinearGradient(0f, y, x, offsetTop, colors, gradientPositions, Shader.TileMode.CLAMP)
            }
            6 -> {
                val x = offsetTop + gradientOffsetX
                LinearGradient(x, 0f, offsetRight, 0f, colors, gradientPositions, Shader.TileMode.CLAMP)
            }
            else -> {
                val x = offsetRight + gradientOffsetX
                val y = offsetTop + gradientOffsetY
                LinearGradient(0f, y, x, offsetBottom, colors, gradientPositions, Shader.TileMode.CLAMP)
            }
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
    }

    fun updateGradientColor(startColor: Int, endColor: Int) {
        this.updateGradientColor(startColor, ViewHelper.NOT_SET_COLOR, endColor)
    }

    fun updateGradientAngle(angle: Int) {
        this.gradientAngle = angle
    }

    fun updateGradientOffsetX(offset: Float) {
        this.gradientOffsetX = offset
    }

    fun updateGradientOffsetY(offset: Float) {
        this.gradientOffsetY = offset
    }

    fun updateLocalMatrix(matrix: Matrix?) {
        this.localMatrix = matrix
    }

    fun updateGradientPositions(positions: FloatArray) {
        this.gradientPositions = positions
    }

    fun updateGradientColors(colors: IntArray) {
        this.gradientColors = colors
    }
}