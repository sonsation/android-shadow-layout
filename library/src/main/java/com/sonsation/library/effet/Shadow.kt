package com.sonsation.library.effet

import android.graphics.*
import com.sonsation.library.utils.ViewHelper
import com.sonsation.library.utils.addSmoothRoundRect

class Shadow(
    var blurSize: Float = 0f,
    var shadowColor: Int = ViewHelper.NOT_SET_COLOR,
    var shadowOffsetX: Float = 0f,
    var shadowOffsetY: Float = 0f,
    var shadowSpread: Float = 0f
) {

    private val paint by lazy { Paint() }
    private val path by lazy { Path() }
    private val rect by lazy { RectF() }

    val isEnable: Boolean
        get() = (blurSize != 0f || shadowSpread != 0f) && shadowColor != ViewHelper.NOT_SET_COLOR

    fun updatePaint() {

        paint.apply {
            isAntiAlias = true
            color = shadowColor
            style = Paint.Style.FILL

            if (blurSize != 0f) {
                maskFilter = BlurMaskFilter(blurSize, BlurMaskFilter.Blur.NORMAL)
            } else {
                maskFilter = null
            }
        }
    }

    fun updatePath(offset: RectF, radius: Radius?) {

        rect.set(
            offset.left + shadowOffsetX,
            offset.top + shadowOffsetY,
            offset.right + shadowOffsetX,
            offset.bottom + shadowOffsetY
        )

        if (shadowSpread != 0f) {
            rect.inset(-shadowSpread, -shadowSpread)
        }

        path.apply {
            reset()

            if (radius?.isEnable == true) {
                addSmoothRoundRect(rect, radius)
            } else {
                addRect(rect, Path.Direction.CW)
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

    fun draw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }
}