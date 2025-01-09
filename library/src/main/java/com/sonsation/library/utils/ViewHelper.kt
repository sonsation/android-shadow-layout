package com.sonsation.library.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import com.sonsation.library.effet.*
import com.sonsation.library.model.ARGB
import java.lang.NumberFormatException
import kotlin.math.sqrt

object ViewHelper {

    const val NOT_SET_COLOR = -101

    fun parseGradientColors(arrays: String?): List<Int>? {

        if (arrays.isNullOrEmpty())
            return null

        val list = mutableListOf<Int>()

        val split = arrays.split(",").map {
            val text = it
            text.trim()
        }


        if (split.isEmpty())
            return null

        split.map { it.trim() }

        split.forEach {
            list.add(Color.parseColor(it))
        }

        return list
    }

    fun parseGradientPositions(arrays: String?): List<Float>? {

        if (arrays.isNullOrEmpty())
            return null

        val list = mutableListOf<Float>()

        val split = arrays.split(",").map {
            val text = it
            text.trim()
        }


        if (split.isEmpty())
            return null

        split.map { it.trim() }

        split.forEach {
            list.add(it.toFloat())
        }

        return list
    }

    fun parseShadowArray(context: Context, arrays: String?): List<Shadow>? {

        if (arrays.isNullOrEmpty()) {
            return null
        }

        val list = mutableListOf<Shadow>()
        val split = arrays.split("},").map {
            var text = it
            text = text.replace("{", "")
            text = text.replace("}", "")
            text.trim()
        }

        if (split.isEmpty()) {
            return null
        }

        split.map { it.trim() }

        split.forEach {

            val splitArray = it.split(",")

            val shadow = if (splitArray.size == 4) {
                try {
                    val blurSize = splitArray[0].toFloat().toPx(context)
                    val offsetX = splitArray[1].toFloat().toPx(context)
                    val offsetY = splitArray[2].toFloat().toPx(context)
                    val color = Color.parseColor(splitArray[3])

                    Shadow(blurSize, color, offsetX, offsetY, 0f)
                } catch (e: NumberFormatException) {
                    Shadow(0f, Color.WHITE, 0f, 0f, 0f)
                }
            } else {
                try {
                    val blurSize = splitArray[0].toFloat().toPx(context)
                    val offsetX = splitArray[1].toFloat().toPx(context)
                    val offsetY = splitArray[2].toFloat().toPx(context)
                    val spread = splitArray[3].toFloat().toPx(context)
                    val color = Color.parseColor(splitArray[4])
                    Shadow(blurSize, color, offsetX, offsetY, spread)
                } catch (e: NumberFormatException) {
                    Shadow(0f, Color.WHITE, 0f, 0f, 0f)
                }
            }

            list.add(shadow)
        }

        return list
    }

    fun Float.toPx(context: Context): Float {
        return context.resources.displayMetrics.density * this
    }

    fun intToColorModel(color: Int): ARGB {

        val alpha = Color.alpha(color)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        return ARGB(alpha, red, green, blue)
    }

    fun onSetAlphaFromAlpha(alpha: Float, currentAlpha: Int): Boolean {

        if (alpha !in 0f..1f) {
            return false
        }

        return (alpha * 255) < currentAlpha
    }

    fun onSetAlphaFromColor(alpha: Float, color: Int): Boolean {

        if (alpha !in 0f..1f) {
            return false
        }

        return (alpha * 255) < Color.alpha(color)
    }

    fun getIntAlpha(alpha: Float): Int {

        if (alpha !in 0f..1f) {
            return 255
        }

        return (255 * alpha).toInt()
    }

    fun Path.getInnerPath(strokeWidth: Float): Path {

        val offset = strokeWidth / 2
        val rect = RectF().apply {
            computeBounds(this, true)
            inset(offset, offset)
        }
        val pathMeasure = PathMeasure(this, false)

        return Path().apply {

            for (distance in 0 until pathMeasure.length.toInt()) {

                val pos = FloatArray(2)
                val tan = FloatArray(2)

                pathMeasure.getPosTan(distance.toFloat(), pos, tan)

                val dx = tan[0]
                val dy = tan[1]

                var normalX = -dy
                var normalY = dx

                val lengthNormal = sqrt((normalX * normalX + normalY * normalY).toDouble()).toFloat()

                normalX /= lengthNormal
                normalY /= lengthNormal

                val innerX = pos[0] + normalX * offset
                val innerY = pos[1] + normalY * offset

                if (innerX < rect.left || innerX > rect.right) {
                    continue
                }

                if (innerY < rect.top || innerY > rect.bottom) {
                    continue
                }

                if (distance == 0) {
                    moveTo(innerX, innerY)
                } else {
                    lineTo(innerX, innerY)
                }
            }
        }
    }
}