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

        split.forEach {
            try {
                list.add(Color.parseColor(it))
            } catch (e: Exception) {
                // Color.parseColor throws IllegalArgumentException for bad hex and
                // StringIndexOutOfBoundsException for empty strings; skip either.
            }
        }

        return list.ifEmpty { null }
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

        split.forEach {
            try {
                list.add(it.toFloat())
            } catch (e: Exception) {
                // Skip malformed position tokens instead of crashing.
            }
        }

        return list.ifEmpty { null }
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

        split.forEach { entry ->

            val splitArray = entry.split(",").map { it.trim() }

            // Guard against malformed entries: a wrong field count or an invalid
            // number/color token skips the entry instead of crashing (bad token
            // types throw NumberFormatException / IllegalArgumentException, and a
            // short field list would otherwise throw IndexOutOfBoundsException).
            try {
                val shadow = when {
                    splitArray.size >= 5 -> {
                        val blurSize = splitArray[0].toFloat().toPx(context)
                        val offsetX = splitArray[1].toFloat().toPx(context)
                        val offsetY = splitArray[2].toFloat().toPx(context)
                        val spread = splitArray[3].toFloat().toPx(context)
                        val color = Color.parseColor(splitArray[4])
                        Shadow(blurSize, color, offsetX, offsetY, spread)
                    }
                    splitArray.size == 4 -> {
                        val blurSize = splitArray[0].toFloat().toPx(context)
                        val offsetX = splitArray[1].toFloat().toPx(context)
                        val offsetY = splitArray[2].toFloat().toPx(context)
                        val color = Color.parseColor(splitArray[3])
                        Shadow(blurSize, color, offsetX, offsetY, 0f)
                    }
                    else -> null
                }

                if (shadow != null) {
                    list.add(shadow)
                }
            } catch (e: Exception) {
                // Any bad token (bad number, bad/empty color) skips this entry
                // instead of crashing. Field count is already guarded above.
            }
        }

        return list.ifEmpty { null }
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
}

enum class Corner {
    TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT
}

fun Path.addSmoothRoundRect(rect: RectF, radius: Radius, radiusOffset: Float = 0f) {
    reset()

    val smoothing = radius.cornerSmoothing.coerceIn(0f, 1f)
    val height = rect.height()

    val targetTopLeftRadius = if (radius.radiusHalf) {
        height.div(2f)
    } else {
        radius.topLeftRadius * radius.radiusWeight + radiusOffset
    }
    val targetTopRightRadius = if (radius.radiusHalf) {
        height.div(2f)
    } else {
        radius.topRightRadius * radius.radiusWeight + radiusOffset
    }
    val targetBottomLeftRadius = if (radius.radiusHalf) {
        height.div(2f)
    } else {
        radius.bottomLeftRadius * radius.radiusWeight + radiusOffset
    }
    val targetBottomRightRadius = if (radius.radiusHalf) {
        height.div(2f)
    } else {
        radius.bottomRightRadius * radius.radiusWeight + radiusOffset
    }

    val width = rect.width()
    val maxRadius = minOf(width, height) / 2f

    val tl = maxOf(0f, minOf(targetTopLeftRadius, maxRadius))
    val tr = maxOf(0f, minOf(targetTopRightRadius, maxRadius))
    val br = maxOf(0f, minOf(targetBottomRightRadius, maxRadius))
    val bl = maxOf(0f, minOf(targetBottomLeftRadius, maxRadius))

    if (smoothing == 0f) {
        moveTo(rect.left + tl, rect.top)
        
        if (tr > 0) arcTo(rect.right - 2 * tr, rect.top, rect.right, rect.top + 2 * tr, -90f, 90f, false)
        else lineTo(rect.right, rect.top)
        
        if (br > 0) arcTo(rect.right - 2 * br, rect.bottom - 2 * br, rect.right, rect.bottom, 0f, 90f, false)
        else lineTo(rect.right, rect.bottom)
        
        if (bl > 0) arcTo(rect.left, rect.bottom - 2 * bl, rect.left + 2 * bl, rect.bottom, 90f, 90f, false)
        else lineTo(rect.left, rect.bottom)
        
        if (tl > 0) arcTo(rect.left, rect.top, rect.left + 2 * tl, rect.top + 2 * tl, 180f, 90f, false)
        else lineTo(rect.left, rect.top)
        
        close()
        return
    }


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