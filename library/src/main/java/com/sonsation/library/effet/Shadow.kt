package com.sonsation.library.effet

import android.graphics.*
import com.sonsation.library.utils.ViewHelper

class Shadow(
    var blurSize: Float = 0f,
    var shadowColor: Int = ViewHelper.NOT_SET_COLOR,
    var shadowOffsetX: Float = 0f,
    var shadowOffsetY: Float = 0f,
    var shadowSpread: Float = 0f
) {

    val paint by lazy { Paint() }
    val path by lazy { Path() }

    val isEnable: Boolean
        get() = (blurSize != 0f || shadowSpread != 0f) && shadowColor != ViewHelper.NOT_SET_COLOR

    fun updatePaint(alpha: Float) {

        paint.apply {
            isAntiAlias = true
            color = shadowColor

            if (ViewHelper.onSetAlphaFromColor(alpha, shadowColor)) {
                this.alpha = ViewHelper.getIntAlpha(alpha)
            }

            style = Paint.Style.FILL

            if (blurSize != 0f) {
                maskFilter = BlurMaskFilter(blurSize, BlurMaskFilter.Blur.NORMAL)
            } else {
                maskFilter = null
            }
        }
    }

    fun updatePath(offset: RectF, radius: Radius?) {

        val width = offset.width()
        val height = offset.height()

        val rect = RectF(
            shadowOffsetX,
            shadowOffsetY,
            width + shadowOffsetX,
            height + shadowOffsetY
        )

        if (shadowSpread != 0f) {
            rect.inset(-shadowSpread, -shadowSpread)
        }

        path.apply {
            reset()

            if (radius == null) {
                addRect(rect, Path.Direction.CW)
            } else {
                val height = rect.height()
                addRoundRect(rect, radius.getRadiusArray(height), Path.Direction.CW)
            }

            close()
        }
    }

    fun updateShadowColor(color: Int) {
        this.shadowColor = color
    }

    fun updateShadowOffsetX(offset: Float) {
        this.shadowOffsetX = offset
    }

    fun updateShadowOffsetY(offset: Float) {
        this.shadowOffsetY = offset
    }

    fun updateShadowSpread(spread: Float) {
        this.shadowSpread = spread
    }

    fun updateShadowBlurSize(size: Float) {
        this.blurSize = size
    }
}