package com.sonsation.library.render

import android.graphics.Bitmap

/**
 * Builds an ARGB_8888 bitmap of [width] x [height], or returns null if it cannot be had.
 *
 * Every renderer here sizes a surface from numbers a caller supplies - a blur, a spread, an
 * offset, none of them capped - so every one of them can be asked for a bitmap that does not
 * exist. The three ways that goes wrong are collected here because one of them is not an
 * exception and cannot be discovered by handling exceptions:
 *
 * - Out of memory. The allocation is simply too large for the heap.
 * - An IllegalArgumentException, when the byte count overflows a 32 bit size.
 * - **A native abort.** Past [SIDE_LIMIT] the image has no valid description at all, and
 *   `Bitmap.createBitmap` does not throw - it kills the process from native code with
 *   `unknown bitmap configuration`. Nothing above can catch that, so the size has to be
 *   refused before the call rather than handled after it.
 *
 * @return null when the caller should fall back - drawing directly, or a lower render mode -
 *         rather than retry, because nothing about the next frame will be different.
 */
internal fun createShadowBitmap(width: Int, height: Int): Bitmap? {

    if (width <= 0 || height <= 0) {
        return null
    }

    if (width >= SIDE_LIMIT || height >= SIDE_LIMIT) {
        return null
    }

    return try {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    } catch (e: OutOfMemoryError) {
        null
    } catch (e: RuntimeException) {
        null
    }
}

/**
 * One past the widest bitmap the platform can describe, in pixels.
 *
 * Skia keeps a row's byte count in a signed 32 bit int, and a pixel costs four bytes, so a
 * side of 2^29 leaves no valid image info behind - which is the abort above, not an
 * exception. Measured on API 36: 536870911 throws OutOfMemoryError, which is ordinary and
 * handled; 536870912 takes the whole process down.
 */
private const val SIDE_LIMIT = 1 shl 29
