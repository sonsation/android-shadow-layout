package com.sonsation.library.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import com.sonsation.library.effet.*
import com.sonsation.library.model.ARGB
import com.sonsation.library.model.StrokeOrigin
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

/**
 * Builds the rounded rect outline clockwise, starting from [origin].
 *
 * The shape is identical whichever origin is used - only the point the contour starts from moves,
 * which is what makes distance 0 of a PathMeasure line up with the requested anchor. [origin] must
 * already be resolved for the layout direction (see [StrokeOrigin.resolve]).
 */
fun Path.addSmoothRoundRect(
    rect: RectF,
    radius: Radius,
    radiusOffset: Float = 0f,
    origin: StrokeOrigin = StrokeOrigin.TOP_START
) {
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

    // How far from each rect corner the straight edge hands over to the corner itself. With
    // smoothing the handover starts earlier than the radius, which is why the two differ.
    val tlOffset = if (smoothing == 0f) tl else getCornerOffset(tl, smoothing, maxRadius)
    val trOffset = if (smoothing == 0f) tr else getCornerOffset(tr, smoothing, maxRadius)
    val brOffset = if (smoothing == 0f) br else getCornerOffset(br, smoothing, maxRadius)
    val blOffset = if (smoothing == 0f) bl else getCornerOffset(bl, smoothing, maxRadius)

    // Contour elements clockwise: 0 top edge, 1 top-right corner, 2 right edge,
    // 3 bottom-right corner, 4 bottom edge, 5 bottom-left corner, 6 left edge, 7 top-left corner.
    // Every origin sits on one of the four edges, so the anchor only ever splits a straight run.
    // No corner offset can exceed maxRadius (half the shorter side), which is why the edge centers
    // below always land on the straight part and never need clamping into the corner.
    val anchorEdge: Int
    val anchorX: Float
    val anchorY: Float

    when (origin) {
        StrokeOrigin.TOP_START -> {
            anchorEdge = 0
            anchorX = rect.left + tlOffset
            anchorY = rect.top
        }
        StrokeOrigin.TOP -> {
            anchorEdge = 0
            anchorX = rect.centerX()
            anchorY = rect.top
        }
        StrokeOrigin.TOP_END -> {
            anchorEdge = 0
            anchorX = rect.right - trOffset
            anchorY = rect.top
        }
        StrokeOrigin.END_TOP -> {
            anchorEdge = 2
            anchorX = rect.right
            anchorY = rect.top + trOffset
        }
        StrokeOrigin.END -> {
            anchorEdge = 2
            anchorX = rect.right
            anchorY = rect.centerY()
        }
        StrokeOrigin.END_BOTTOM -> {
            anchorEdge = 2
            anchorX = rect.right
            anchorY = rect.bottom - brOffset
        }
        StrokeOrigin.BOTTOM_END -> {
            anchorEdge = 4
            anchorX = rect.right - brOffset
            anchorY = rect.bottom
        }
        StrokeOrigin.BOTTOM -> {
            anchorEdge = 4
            anchorX = rect.centerX()
            anchorY = rect.bottom
        }
        StrokeOrigin.BOTTOM_START -> {
            anchorEdge = 4
            anchorX = rect.left + blOffset
            anchorY = rect.bottom
        }
        StrokeOrigin.START_BOTTOM -> {
            anchorEdge = 6
            anchorX = rect.left
            anchorY = rect.bottom - blOffset
        }
        StrokeOrigin.START -> {
            anchorEdge = 6
            anchorX = rect.left
            anchorY = rect.centerY()
        }
        StrokeOrigin.START_TOP -> {
            anchorEdge = 6
            anchorX = rect.left
            anchorY = rect.top + tlOffset
        }
    }

    moveTo(anchorX, anchorY)

    // The anchor's own edge is drawn first (its tail) and the walk lands back on that edge's
    // start point, so the leading part of it closes the contour.
    for (step in 0 until 8) {
        when ((anchorEdge + step) % 8) {
            0 -> lineTo(rect.right - trOffset, rect.top)
            1 -> drawCorner(rect.right, rect.top, tr, trOffset, Corner.TOP_RIGHT, smoothing)
            2 -> lineTo(rect.right, rect.bottom - brOffset)
            3 -> drawCorner(rect.right, rect.bottom, br, brOffset, Corner.BOTTOM_RIGHT, smoothing)
            4 -> lineTo(rect.left + blOffset, rect.bottom)
            5 -> drawCorner(rect.left, rect.bottom, bl, blOffset, Corner.BOTTOM_LEFT, smoothing)
            6 -> lineTo(rect.left, rect.top + tlOffset)
            else -> drawCorner(rect.left, rect.top, tl, tlOffset, Corner.TOP_LEFT, smoothing)
        }
    }

    lineTo(anchorX, anchorY)
    close()
}

private fun Path.drawCorner(
    cornerX: Float, cornerY: Float,
    radius: Float, offset: Float,
    corner: Corner,
    smoothing: Float
) {
    // A zero radius corner has no length: the edge before it already ended on the corner point
    // and the edge after it starts there, so emitting anything would just be a degenerate segment.
    if (radius <= 0f) {
        return
    }

    if (smoothing != 0f) {
        drawSmoothCorner(cornerX, cornerY, radius, offset, corner)
        return
    }

    val diameter = 2 * radius
    when (corner) {
        Corner.TOP_RIGHT -> arcTo(
            cornerX - diameter, cornerY, cornerX, cornerY + diameter, -90f, 90f, false
        )
        Corner.BOTTOM_RIGHT -> arcTo(
            cornerX - diameter, cornerY - diameter, cornerX, cornerY, 0f, 90f, false
        )
        Corner.BOTTOM_LEFT -> arcTo(
            cornerX, cornerY - diameter, cornerX + diameter, cornerY, 90f, 90f, false
        )
        Corner.TOP_LEFT -> arcTo(
            cornerX, cornerY, cornerX + diameter, cornerY + diameter, 180f, 90f, false
        )
    }
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