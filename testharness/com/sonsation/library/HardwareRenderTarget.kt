package com.sonsation.library

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.view.View

/**
 * An offscreen, genuinely hardware accelerated render target.
 *
 * Robolectric and `Canvas(Bitmap)` both hand out software canvases, on which a display
 * list cannot be replayed - so neither can tell whether the RenderNode path works or
 * only its fallback does. This drives the real HWUI pipeline into an [ImageReader] and
 * reads the resulting pixels back.
 */
class HardwareRenderTarget(private val width: Int, private val height: Int) : AutoCloseable {

    private val imageReader = ImageReader.newInstance(
        width,
        height,
        PixelFormat.RGBA_8888,
        MAX_IMAGES,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
    )

    private val renderer = HardwareRenderer().apply {
        setSurface(imageReader.surface)
    }

    private val root = RenderNode("test-root").apply {
        setPosition(0, 0, width, height)
    }

    /** Records [block] onto a hardware canvas and presents it. */
    fun render(block: (Canvas) -> Unit) {
        val canvas = root.beginRecording()
        try {
            block(canvas)
        } finally {
            root.endRecording()
        }

        renderer.setContentRoot(root)
        renderer.createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()
    }

    /**
     * Renders [view] centred in the target, so its shadows have room to bleed out.
     *
     * [times] draws it repeatedly into the same frame. The harness costs the same either
     * way, so repeating amplifies the per draw cost above the presentation noise.
     */
    fun renderCentred(view: View, backgroundColor: Int, times: Int = 1) = render { canvas ->
        canvas.drawColor(backgroundColor)
        repeat(times) {
            canvas.save()
            canvas.translate((width - view.width) / 2f, (height - view.height) / 2f)
            view.draw(canvas)
            canvas.restore()
        }
    }

    /** Drops the presented frame without reading it back, to keep the queue moving. */
    fun discardFrame() {
        imageReader.acquireNextImage()?.close()
    }

    /** Copies the last presented frame into a software bitmap. */
    fun readBack(): Bitmap {
        val image = requireNotNull(imageReader.acquireNextImage()) { "no frame was presented" }
        try {
            val buffer = requireNotNull(image.hardwareBuffer) { "frame has no hardware buffer" }
            try {
                val wrapped = requireNotNull(
                    Bitmap.wrapHardwareBuffer(buffer, ColorSpace.get(ColorSpace.Named.SRGB))
                ) { "could not wrap the hardware buffer" }
                return wrapped.copy(Bitmap.Config.ARGB_8888, false)
            } finally {
                buffer.close()
            }
        } finally {
            image.close()
        }
    }

    override fun close() {
        renderer.destroy()
        imageReader.close()
        root.discardDisplayList()
    }

    companion object {
        private const val MAX_IMAGES = 3
    }
}
