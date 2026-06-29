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

enum class Corner {
    TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT
}

fun Path.addSmoothRoundRect(rect: RectF, radius: Radius) {
    reset()

    val smoothing = radius.cornerSmoothing.coerceIn(0f, 1f)
    if (smoothing == 0f) {
        val height = rect.height()
        addRoundRect(rect, radius.getRadiusArray(height), Path.Direction.CW)
        return
    }

    val width = rect.width()
    val height = rect.height()
    val maxRadius = minOf(width, height) / 2f

    val tl = minOf(radius.topLeftRadius * radius.radiusWeight, maxRadius)
    val tr = minOf(radius.topRightRadius * radius.radiusWeight, maxRadius)
    val br = minOf(radius.bottomRightRadius * radius.radiusWeight, maxRadius)
    val bl = minOf(radius.bottomLeftRadius * radius.radiusWeight, maxRadius)

    val tlOffset = getCornerOffset(tl, smoothing, maxRadius)
    moveTo(rect.left + tlOffset, rect.top)

    val trOffset = getCornerOffset(tr, smoothing, maxRadius)
    lineTo(rect.right - trOffset, rect.top)
    drawSmoothCorner(
        rect.right, rect.top,
        tr, trOffset,
        Corner.TOP_RIGHT
    )

    val brOffset = getCornerOffset(br, smoothing, maxRadius)
    lineTo(rect.right, rect.bottom - brOffset)
    drawSmoothCorner(
        rect.right, rect.bottom,
        br, brOffset,
        Corner.BOTTOM_RIGHT
    )

    val blOffset = getCornerOffset(bl, smoothing, maxRadius)
    lineTo(rect.left + blOffset, rect.bottom)
    drawSmoothCorner(
        rect.left, rect.bottom,
        bl, blOffset,
        Corner.BOTTOM_LEFT
    )

    lineTo(rect.left, rect.top + tlOffset)
    drawSmoothCorner(
        rect.left, rect.top,
        tl, tlOffset,
        Corner.TOP_LEFT
    )

    close()
}

private fun getCornerOffset(radius: Float, smoothing: Float, maxOffset: Float): Float {
    return minOf(radius * (1f + smoothing * 0.5286f), maxOffset)
}

private fun Path.drawSmoothCorner(
    cornerX: Float, cornerY: Float,
    radius: Float, offset: Float,
    corner: Corner
) {
    if (radius <= 0f) {
        lineTo(cornerX, cornerY)
        return
    }

    val p = offset
    val m = radius * 0.2928932f
    val c_a = radius * 0.734784f
    val k = radius * 0.187536f

    when (corner) {
        Corner.TOP_RIGHT -> {
            cubicTo(
                cornerX - c_a, cornerY,
                cornerX - (m + k), cornerY + (m - k),
                cornerX - m, cornerY + m
            )
            cubicTo(
                cornerX - (m - k), cornerY + (m + k),
                cornerX, cornerY + c_a,
                cornerX, cornerY + p
            )
        }
        Corner.BOTTOM_RIGHT -> {
            cubicTo(
                cornerX, cornerY - c_a,
                cornerX - (m - k), cornerY - (m + k),
                cornerX - m, cornerY - m
            )
            cubicTo(
                cornerX - (m + k), cornerY - (m - k),
                cornerX - c_a, cornerY,
                cornerX - p, cornerY
            )
        }
        Corner.BOTTOM_LEFT -> {
            cubicTo(
                cornerX + c_a, cornerY,
                cornerX + (m + k), cornerY - (m - k),
                cornerX + m, cornerY - m
            )
            cubicTo(
                cornerX + (m - k), cornerY - (m + k),
                cornerX, cornerY - c_a,
                cornerX, cornerY - p
            )
        }
        Corner.TOP_LEFT -> {
            cubicTo(
                cornerX, cornerY + c_a,
                cornerX + (m - k), cornerY + (m + k),
                cornerX + m, cornerY + m
            )
            cubicTo(
                cornerX + (m + k), cornerY + (m - k),
                cornerX + c_a, cornerY,
                cornerX + p, cornerY
            )
        }
    }
}