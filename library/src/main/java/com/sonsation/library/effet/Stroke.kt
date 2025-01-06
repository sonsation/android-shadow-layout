package com.sonsation.library.effet

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import com.sonsation.library.model.StrokeType
import com.sonsation.library.utils.ViewHelper

class Stroke(var strokeWidth: Float = 0f,
             var strokeColor: Int = ViewHelper.NOT_SET_COLOR,
             var strokeType: StrokeType = StrokeType.INSIDE
    ) {

    var blur: Float = 0f
    var blurType = BlurMaskFilter.Blur.NORMAL
    val isEnable: Boolean
        get() = strokeWidth != 0f && strokeColor != ViewHelper.NOT_SET_COLOR

    fun updateStrokeWidth(strokeWidth: Float) {
        this.strokeWidth = strokeWidth
    }

    fun updateStrokeColor(color: Int) {
        this.strokeColor = color
    }
}