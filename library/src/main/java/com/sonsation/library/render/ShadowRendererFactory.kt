package com.sonsation.library.render

import android.os.Build
import com.sonsation.library.ShadowLayout

/**
 * Maps a public `RENDER_MODE_*` constant to the strategy that implements it.
 */
internal object ShadowRendererFactory {

    /**
     * The mode a view gets when nothing asks for one.
     *
     * The order of preference is DEFAULT, then RENDER_NODE, then BITMAP_CACHE: draw the
     * shadows as they come, cache them on the GPU if that is not possible, and only
     * rasterize them into a bitmap if neither works.
     *
     * Only the first and last rung can ever be reached, because of when the platform
     * gained each piece. A blur cannot run on a hardware canvas before API 28, which is
     * the one thing that rules DEFAULT out - and RenderNode only arrived in API 29. So
     * there is no version where DEFAULT fails and RENDER_NODE could step in, and adding
     * it here would be a branch that never runs.
     *
     * RENDER_NODE therefore stays opt in, which is also where the measurements left it:
     * level with drawing the shadows every frame rather than reliably ahead of it, and
     * the view's own display list already caches an unchanged ShadowLayout.
     */
    fun defaultMode(): Int = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> ShadowLayout.RENDER_MODE_BITMAP_CACHE
        else -> ShadowLayout.RENDER_MODE_DEFAULT
    }

    /**
     * Narrows [mode] to what this device can actually run, so `ShadowLayout.renderMode`
     * always reports the mode in effect rather than the one that was asked for.
     */
    fun resolveMode(mode: Int): Int = when (mode) {
        ShadowLayout.RENDER_MODE_RENDER_NODE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ShadowLayout.RENDER_MODE_RENDER_NODE
            } else {
                ShadowLayout.RENDER_MODE_BITMAP_CACHE
            }
        }

        ShadowLayout.RENDER_MODE_BITMAP_CACHE -> ShadowLayout.RENDER_MODE_BITMAP_CACHE
        ShadowLayout.RENDER_MODE_HARDWARE_LAYER -> ShadowLayout.RENDER_MODE_HARDWARE_LAYER
        else -> ShadowLayout.RENDER_MODE_DEFAULT
    }

    /** [mode] must already have been through [resolveMode]. */
    fun create(mode: Int): ShadowRenderer = when (mode) {
        ShadowLayout.RENDER_MODE_RENDER_NODE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RenderNodeShadowRenderer()
        } else {
            BitmapCacheShadowRenderer()
        }
        ShadowLayout.RENDER_MODE_BITMAP_CACHE -> BitmapCacheShadowRenderer()
        ShadowLayout.RENDER_MODE_HARDWARE_LAYER -> HardwareLayerShadowRenderer()
        else -> DirectShadowRenderer()
    }
}
