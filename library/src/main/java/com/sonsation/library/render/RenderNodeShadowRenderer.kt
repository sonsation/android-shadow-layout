package com.sonsation.library.render

import android.graphics.Canvas
import android.graphics.RenderNode
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Records the shadows into a [RenderNode] and replays that node every frame.
 *
 * Compared to [BitmapCacheShadowRenderer] nothing is allocated on the Java heap, nothing
 * is rasterized on the CPU, no texture is uploaded per rebuild, and the shadows keep full
 * resolution instead of being downscaled. Compared to [HardwareLayerShadowRenderer] only
 * the shadows are cached, so a child invalidation replays the node instead of
 * re-rasterizing the view.
 *
 * The node is deliberately *not* promoted to a compositing layer. That looks like the
 * obvious thing to do - rasterize the blur once into a texture instead of re-running it -
 * but measuring it says otherwise: a layer backed node costs about 7us per draw against
 * 1.2us for a plain display list, and the cost does not vary with how many shadows or how
 * large the blur is. HWUI already caches the blurred mask, so there is nothing left for a
 * layer to save, and all it adds is an offscreen render pass per draw.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class RenderNodeShadowRenderer : ShadowRenderer {

    private val renderNode = RenderNode("ShadowLayout")
    private val outsets = ShadowOutsets()

    private var isRecorded = false

    override fun prepare(context: ShadowRenderContext, cacheDirty: Boolean): Boolean {

        if (!cacheDirty && isRecorded && renderNode.hasDisplayList()) {
            return true
        }

        val width = context.bounds.width()
        val height = context.bounds.height()

        if (width <= 0f || height <= 0f) {
            isRecorded = false
            return true
        }

        outsets.compute(context)

        // The node covers the view plus everything the shadows bleed outside it, and sits
        // at that offset in the parent's coordinate space.
        val left = floor(context.bounds.left - outsets.left).toInt()
        val top = floor(context.bounds.top - outsets.top).toInt()
        val right = ceil(context.bounds.right + outsets.right).toInt()
        val bottom = ceil(context.bounds.bottom + outsets.bottom).toInt()

        renderNode.setPosition(left, top, right, bottom)

        val canvas = renderNode.beginRecording()

        try {
            // The recording starts at the node's own origin; shift back so the shadow
            // paths, which are built in view coordinates, land in the right place.
            canvas.translate(-left.toFloat(), -top.toFloat())
            canvas.drawShadowsOutsideBackground(context)
        } finally {
            renderNode.endRecording()
        }

        isRecorded = true

        return true
    }

    override fun draw(canvas: Canvas, context: ShadowRenderContext): Boolean {

        // A software canvas - a screenshot, a print job, a unit test - cannot replay a
        // display list. Falling back for that frame only, the node stays valid.
        if (!isRecorded || !renderNode.hasDisplayList() || !canvas.isHardwareAccelerated) {
            return false
        }

        canvas.drawRenderNode(renderNode)

        return true
    }

    override fun onUninstalled(view: View) {
        renderNode.discardDisplayList()
        isRecorded = false
    }
}
