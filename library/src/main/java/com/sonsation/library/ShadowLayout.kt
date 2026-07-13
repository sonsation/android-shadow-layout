package com.sonsation.library

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout
import com.sonsation.library.effet.*
import com.sonsation.library.model.Padding
import com.sonsation.library.model.StrokeType
import com.sonsation.library.utils.ViewHelper
import com.sonsation.library.utils.ViewHelper.getInnerPath
import com.sonsation.library.utils.addSmoothRoundRect
import kotlin.math.abs


class ShadowLayout : FrameLayout {

    private val outlineRect by lazy {
        RectF()
    }

    private val shadowRect by lazy {
        RectF()
    }

    private val outlinePaint by lazy {
        Paint()
    }

    private val outlinePath by lazy {
        Path()
    }

    private val backgroundPaint by lazy {
        Paint()
    }

    private val backgroundPath by lazy {
        Path()
    }

    private val layoutRect by lazy {
        RectF()
    }

    private val padding by lazy {
        Padding(0, 0, 0, 0)
    }

    private val targetRect by lazy {
        RectF()
    }

    companion object {
        const val RENDER_MODE_DEFAULT = 0
        const val RENDER_MODE_BITMAP_CACHE = 1
        const val RENDER_MODE_HARDWARE_LAYER = 2
    }

    var renderMode = RENDER_MODE_DEFAULT
        private set

    private var cachedBitmap: Bitmap? = null
    private var cacheCanvas: Canvas? = null

    private var isPathDirty = true

    var autoAdjustPadding = false
        private set
    var backgroundColor = ViewHelper.NOT_SET_COLOR
        private set
    var backgroundBlur = 0f
        private set
    var backgroundBlurType = BlurMaskFilter.Blur.NORMAL
        private set
    var radius: Radius? = null
        private set
    var stroke: Stroke? = null
        private set
    var gradient: Gradient? = null
        private set
    var strokeGradient: Gradient? = null
        private set
    val shadows by lazy {
        mutableListOf<Shadow>()
    }

    var clipOutLine = false
        private set

    private var isInitialized = false


    constructor(context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init(context, attributeSet, 0)
    }

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        init(context, attributeSet, defStyleAttr)
    }

    private fun init(context: Context, attributeSet: AttributeSet?, defStyle: Int) {

        if (attributeSet == null) {
            return
        }

        initAttrsLayout(context, attributeSet, defStyle)
    }

    private fun initAttrsLayout(context: Context, attributeSet: AttributeSet, defStyle: Int) {

        val a = context.obtainStyledAttributes(attributeSet, R.styleable.ShadowLayout, defStyle, 0)

        try {
            autoAdjustPadding = a.getBoolean(R.styleable.ShadowLayout_autoAdjustPadding, false)
            clipOutLine = a.getBoolean(R.styleable.ShadowLayout_clipToOutline, false)
            renderMode = a.getInt(R.styleable.ShadowLayout_shadow_render_mode, RENDER_MODE_DEFAULT)
            applyRenderMode()
            stroke = Stroke(
                strokeColor =
                a.getColor(R.styleable.ShadowLayout_stroke_color, ViewHelper.NOT_SET_COLOR),
                strokeWidth = a.getDimension(R.styleable.ShadowLayout_stroke_width, 0f),
                strokeType = StrokeType.entries.find {
                    it.ordinal == a.getInteger(
                        R.styleable.ShadowLayout_stroke_type,
                        StrokeType.INSIDE.type
                    )
                } ?: StrokeType.INSIDE,
                strokeAlpha = a.getInteger(R.styleable.ShadowLayout_stroke_alpha, 255)
            ).apply {
                this.blurType = BlurMaskFilter.Blur.entries.find {
                    it.ordinal == a.getInteger(
                        R.styleable.ShadowLayout_stroke_blur_type,
                        BlurMaskFilter.Blur.NORMAL.ordinal
                    )
                } ?: BlurMaskFilter.Blur.NORMAL
                this.blur = a.getDimension(R.styleable.ShadowLayout_stroke_blur, 0f)
                this.drawAsOverlay = a.getBoolean(R.styleable.ShadowLayout_stroke_draw_as_overlay, false)
            }

            val allRadius = a.getDimension(R.styleable.ShadowLayout_background_radius, 0f)
            val radiusHalf = a.getBoolean(R.styleable.ShadowLayout_background_radius_half, false)
            val radiusWeight = a.getFloat(R.styleable.ShadowLayout_background_radius_weight, 1f)
            val cornerSmoothing = a.getFloat(R.styleable.ShadowLayout_background_corner_smoothing, 0f)

            radius = if (allRadius == 0f) {
                val topLeftRadius =
                    a.getDimension(R.styleable.ShadowLayout_background_top_left_radius, 0f)
                val topRightRadius =
                    a.getDimension(R.styleable.ShadowLayout_background_top_right_radius, 0f)
                val bottomLeftRadius =
                    a.getDimension(R.styleable.ShadowLayout_background_bottom_left_radius, 0f)
                val bottomRightRadius =
                    a.getDimension(R.styleable.ShadowLayout_background_bottom_right_radius, 0f)

                Radius(topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius).apply {
                    this.radiusHalf = radiusHalf
                    this.radiusWeight = radiusWeight
                    this.cornerSmoothing = cornerSmoothing
                }
            } else {
                Radius(allRadius).apply {
                    this.radiusHalf = radiusHalf
                    this.radiusWeight = radiusWeight
                    this.cornerSmoothing = cornerSmoothing
                }
            }

            gradient = Gradient(
                gradientStartColor = a.getColor(
                    R.styleable.ShadowLayout_gradient_start_color,
                    ViewHelper.NOT_SET_COLOR
                ),
                gradientCenterColor = a.getColor(
                    R.styleable.ShadowLayout_gradient_center_color,
                    ViewHelper.NOT_SET_COLOR
                ),
                gradientEndColor = a.getColor(
                    R.styleable.ShadowLayout_gradient_end_color,
                    ViewHelper.NOT_SET_COLOR
                ),
                gradientAngle = a.getInt(R.styleable.ShadowLayout_gradient_angle, -1),
                gradientOffsetX = a.getDimension(R.styleable.ShadowLayout_gradient_offset_x, 0f),
                gradientOffsetY = a.getDimension(R.styleable.ShadowLayout_gradient_offset_y, 0f),
                gradientColors = ViewHelper.parseGradientColors(a.getString(R.styleable.ShadowLayout_gradient_colors))
                    ?.toIntArray(),
                gradientPositions = ViewHelper.parseGradientPositions(a.getString(R.styleable.ShadowLayout_gradient_positions))
                    ?.toFloatArray()
            )

            strokeGradient = Gradient(
                gradientStartColor = a.getColor(
                    R.styleable.ShadowLayout_stroke_gradient_start_color,
                    ViewHelper.NOT_SET_COLOR
                ),
                gradientCenterColor = a.getColor(
                    R.styleable.ShadowLayout_stroke_gradient_center_color,
                    ViewHelper.NOT_SET_COLOR
                ),
                gradientEndColor = a.getColor(
                    R.styleable.ShadowLayout_stroke_gradient_end_color,
                    ViewHelper.NOT_SET_COLOR
                ),
                gradientAngle = a.getInt(R.styleable.ShadowLayout_stroke_gradient_angle, -1),
                gradientOffsetX = a.getDimension(
                    R.styleable.ShadowLayout_stroke_gradient_offset_x,
                    0f
                ),
                gradientOffsetY = a.getDimension(
                    R.styleable.ShadowLayout_stroke_gradient_offset_y,
                    0f
                ),
                gradientColors = ViewHelper.parseGradientColors(a.getString(R.styleable.ShadowLayout_stroke_gradient_colors))
                    ?.toIntArray(),
                gradientPositions = ViewHelper.parseGradientPositions(a.getString(R.styleable.ShadowLayout_stroke_gradient_positions))
                    ?.toFloatArray()
            )

            backgroundColor = if (a.hasValue(R.styleable.ShadowLayout_background_color)) {
                a.getColor(
                    R.styleable.ShadowLayout_background_color,
                    Color.parseColor("#ffffffff")
                )
            } else {
                Color.parseColor("#ffffffff")
            }

            backgroundBlur = a.getDimension(R.styleable.ShadowLayout_background_blur, 0f)

            backgroundBlurType = BlurMaskFilter.Blur.entries.find {
                it.ordinal == a.getInteger(
                    R.styleable.ShadowLayout_background_blur_type,
                    BlurMaskFilter.Blur.NORMAL.ordinal
                )
            } ?: BlurMaskFilter.Blur.NORMAL

            val shadow = Shadow(
                blurSize = a.getDimension(R.styleable.ShadowLayout_shadow_blur, 0f),
                shadowColor = a.getColor(
                    R.styleable.ShadowLayout_shadow_color,
                    ViewHelper.NOT_SET_COLOR
                ),
                shadowOffsetX = a.getDimension(R.styleable.ShadowLayout_shadow_offset_x, 0f),
                shadowOffsetY = a.getDimension(R.styleable.ShadowLayout_shadow_offset_y, 0f),
                shadowSpread = a.getDimension(R.styleable.ShadowLayout_shadow_spread, 0f)
            )

            shadows.add(shadow)

            val shadows = ViewHelper.parseShadowArray(
                context,
                a.getString(R.styleable.ShadowLayout_shadow_array)
            )

            if (!shadows.isNullOrEmpty()) {
                this.shadows.addAll(shadows)
            }
        } finally {
            a.recycle()
            isInitialized = true
            padding.setPadding(paddingStart, paddingTop, paddingEnd, paddingBottom)
            updatePadding()
        }
    }

    override fun hasOverlappingRendering(): Boolean {
        return if (stroke?.isEnable == true ||
            shadows.any { it.isEnable } ||
            backgroundBlur != 0f
        ) {
            false
        } else {
            super.hasOverlappingRendering()
        }
    }

    fun updateRenderMode(mode: Int) {
        renderMode = mode
        applyRenderMode()
        isPathDirty = true
        invalidate()
    }

    private fun applyRenderMode() {
        if (renderMode == RENDER_MODE_HARDWARE_LAYER) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        } else {
            setLayerType(LAYER_TYPE_NONE, null)
        }
    }

    private var cacheOutsetLeft = 0f
    private var cacheOutsetTop = 0f

    private fun updateBitmapCache() {
        val w = layoutRect.width()
        val h = layoutRect.height()
        if (w <= 0f || h <= 0f) return

        var maxOutsetLeft = 0f
        var maxOutsetTop = 0f
        var maxOutsetRight = 0f
        var maxOutsetBottom = 0f

        shadows.forEach { shadow ->
            if (shadow.isEnable) {
                val spread = shadow.shadowSpread
                val blur = shadow.blurSize
                val ox = shadow.shadowOffsetX
                val oy = shadow.shadowOffsetY

                val leftBleed = (blur + spread) - ox
                val rightBleed = (blur + spread) + ox
                val topBleed = (blur + spread) - oy
                val bottomBleed = (blur + spread) + oy

                if (leftBleed > maxOutsetLeft) maxOutsetLeft = leftBleed
                if (rightBleed > maxOutsetRight) maxOutsetRight = rightBleed
                if (topBleed > maxOutsetTop) maxOutsetTop = topBleed
                if (bottomBleed > maxOutsetBottom) maxOutsetBottom = bottomBleed
            }
        }

        maxOutsetLeft += 2f
        maxOutsetTop += 2f
        maxOutsetRight += 2f
        maxOutsetBottom += 2f

        cacheOutsetLeft = maxOutsetLeft
        cacheOutsetTop = maxOutsetTop

        val cacheW = (w + maxOutsetLeft + maxOutsetRight).toInt()
        val cacheH = (h + maxOutsetTop + maxOutsetBottom).toInt()

        try {
            if (cachedBitmap?.width != cacheW || cachedBitmap?.height != cacheH) {
                cachedBitmap?.recycle()
                cachedBitmap = Bitmap.createBitmap(cacheW, cacheH, Bitmap.Config.ARGB_8888)
                cacheCanvas = Canvas(cachedBitmap!!)
            }
        } catch (e: OutOfMemoryError) {
            renderMode = RENDER_MODE_DEFAULT
            cachedBitmap = null
            cacheCanvas = null
            return
        }

        cachedBitmap!!.eraseColor(Color.TRANSPARENT)
        
        cacheCanvas?.let { cv ->
            cv.save()
            cv.translate(cacheOutsetLeft, cacheOutsetTop)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                cv.clipOutPath(backgroundPath)
            } else {
                @Suppress("DEPRECATION")
                cv.clipPath(backgroundPath, Region.Op.DIFFERENCE)
            }

            shadows.forEach { shadow ->
                shadow.updatePaint()
                if (shadow.isEnable) {
                    shadow.draw(cv)
                }
            }
            cv.restore()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cachedBitmap?.recycle()
        cachedBitmap = null
        cacheCanvas = null
        isPathDirty = true
    }

    override fun dispatchDraw(canvas: Canvas) {

        if (isPathDirty) {
            setOutlineAndBackground(layoutRect)
            shadows.forEach { shadow ->
                shadow.updatePath(shadowRect, radius)
                shadow.updatePaint()
            }
            if (renderMode == RENDER_MODE_BITMAP_CACHE) {
                updateBitmapCache()
            }
            isPathDirty = false
        }

        if (renderMode == RENDER_MODE_BITMAP_CACHE && cachedBitmap != null) {
            canvas.drawBitmap(cachedBitmap!!, -cacheOutsetLeft, -cacheOutsetTop, null)
        } else {
            try {
                canvas.save()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    canvas.clipOutPath(backgroundPath)
                } else {
                    @Suppress("DEPRECATION")
                    canvas.clipPath(backgroundPath, Region.Op.DIFFERENCE)
                }

                shadows.forEach { shadow ->
                    if (shadow.isEnable) {
                        shadow.draw(canvas)
                    }
                }
            } finally {
                canvas.restore()
            }
        }

        canvas.drawPath(backgroundPath, backgroundPaint)

        if (stroke?.isEnable == true) {

            if (stroke?.drawAsOverlay == true) {
                canvas.drawPath(outlinePath, outlinePaint)
                canvas.clipPath(backgroundPath)
                super.dispatchDraw(canvas)
                return
            } else {
                canvas.drawPath(outlinePath, outlinePaint)
            }
        }

        if (clipOutLine) {
            canvas.clipPath(outlinePath)
        }

        super.dispatchDraw(canvas)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (!changed) {
            return
        }

        val width = abs(right - left).toFloat()
        val height = abs(bottom - top).toFloat()
        layoutRect.set(0f, 0f, width, height)
        isPathDirty = true
    }


    private fun updatePadding() {
        setPadding(padding.start, padding.top, padding.end, padding.bottom)
    }

    fun updateBackgroundColor(color: Int) {
        backgroundColor = color
        isPathDirty = true
        invalidate()
    }

    fun updateRadius(radius: Float) {
        this.radius?.updateRadius(radius)
        isPathDirty = true
        invalidate()
    }

    fun updateRadius(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) {
        this.radius?.updateRadius(topLeft, topRight, bottomLeft, bottomRight)
        isPathDirty = true
        invalidate()
    }

    fun updateCornerSmoothing(smoothing: Float) {
        this.radius?.cornerSmoothing = smoothing
        isPathDirty = true
        invalidate()
    }

    fun addBackgroundShadow(blurSize: Float, offsetX: Float, offsetY: Float, shadowColor: Int) {
        val shadow = Shadow(blurSize, shadowColor, offsetX, offsetY, 0f)
        shadows.add(shadow)
        isPathDirty = true
        invalidate()
    }

    fun addBackgroundShadow(
        blurSize: Float,
        offsetX: Float,
        offsetY: Float,
        spread: Float,
        shadowColor: Int
    ) {
        val shadow = Shadow(blurSize, shadowColor, offsetX, offsetY, spread)
        shadows.add(shadow)
        isPathDirty = true
        invalidate()
    }

    fun removeBackgroundShadowLast() {
        shadows.removeLastOrNull()
        isPathDirty = true
        invalidate()
    }

    fun removeBackgroundShadowFirst() {
        shadows.removeFirstOrNull()
        isPathDirty = true
        invalidate()
    }

    fun removeAllBackgroundShadows() {
        shadows.clear()
        isPathDirty = true
        invalidate()
    }

    fun removeBackgroundShadow(position: Int) {
        shadows.removeAt(position)
        isPathDirty = true
        invalidate()
    }

    fun updateBackgroundShadow(position: Int, shadow: Shadow) {
        shadows[position] = shadow
        isPathDirty = true
        invalidate()
    }

    fun updateBackgroundShadow(
        position: Int,
        blurSize: Float,
        offsetX: Float,
        offsetY: Float,
        color: Int
    ) {
        shadows[position].apply {
            this.blurSize = blurSize
            this.shadowColor = color
            this.shadowOffsetX = offsetX
            this.shadowOffsetY = offsetY
        }
        isPathDirty = true
        invalidate()
    }

    fun updateBackgroundShadow(
        position: Int,
        blurSize: Float,
        offsetX: Float,
        offsetY: Float,
        spread: Float,
        color: Int
    ) {
        shadows[position].apply {
            this.blurSize = blurSize
            this.shadowColor = color
            this.shadowOffsetX = offsetX
            this.shadowOffsetY = offsetY
            this.shadowSpread = spread
        }
        isPathDirty = true
        invalidate()
    }

    fun updateBackgroundShadow(shadow: Shadow) {
        updateBackgroundShadow(0, shadow)
    }

    fun updateBackgroundShadow(blurSize: Float, offsetX: Float, offsetY: Float, color: Int) {
        updateBackgroundShadow(0, blurSize, offsetX, offsetY, color)
    }

    fun updateBackgroundShadow(
        blurSize: Float,
        offsetX: Float,
        offsetY: Float,
        spread: Float,
        color: Int
    ) {
        updateBackgroundShadow(0, blurSize, offsetX, offsetY, spread, color)
    }

    fun updateStrokeWidth(strokeWidth: Float) {
        stroke?.updateStrokeWidth(strokeWidth)
        isPathDirty = true
        if (autoAdjustPadding) {
            updatePadding()
        }
        invalidate()
    }

    override fun setPadding(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {

        padding.setPadding(left, top, right, bottom)

        if (autoAdjustPadding && stroke?.isEnable == true) {
            val strokeWidth = stroke?.takeIf { it.isEnable }?.strokeWidth ?: 0f

            when (stroke!!.strokeType) {
                StrokeType.INSIDE -> {
                    val offset = strokeWidth.toInt()
                    super.setPadding(left + offset, top + offset, right + offset, bottom + offset)
                }
                StrokeType.CENTER -> {
                    val offset = (strokeWidth - strokeWidth.div(2f)).toInt()
                    super.setPadding(left + offset, top + offset, right + offset, bottom + offset)
                }
                StrokeType.OUTSIDE -> {
                    super.setPadding(left, top, right, bottom)
                    isPathDirty = true
                    invalidate()
                }
            }
            return
        }

        super.setPadding(left, top, right, bottom)
    }

    override fun setPaddingRelative(
        start: Int,
        top: Int,
        end: Int,
        bottom: Int
    ) {

        padding.setPadding(left, top, right, bottom)

        if (autoAdjustPadding && stroke?.isEnable == true) {
            val strokeWidth = stroke?.takeIf { it.isEnable }?.strokeWidth ?: 0f

            when (stroke!!.strokeType) {
                StrokeType.INSIDE -> {
                    val offset = strokeWidth.toInt()
                    super.setPaddingRelative(start + offset, top + offset, end + offset, bottom + offset)
                }
                StrokeType.CENTER -> {
                    val offset = (strokeWidth - strokeWidth.div(2f)).toInt()
                    super.setPaddingRelative(start + offset, top + offset, end + offset, bottom + offset)
                }
                StrokeType.OUTSIDE -> {
                    super.setPaddingRelative(start, top, end, bottom)
                    isPathDirty = true
                    invalidate()
                }
            }
            return
        }

        super.setPaddingRelative(start, top, end, bottom)
    }

    fun updateStrokeColor(color: Int) {
        stroke?.updateStrokeColor(color)
        isPathDirty = true
        invalidate()
    }

    fun updateGradientColor(startColor: Int, centerColor: Int, endColor: Int) {
        this.gradient?.updateGradientColor(startColor, centerColor, endColor)
        isPathDirty = true
        invalidate()
    }

    fun updateGradientColor(startColor: Int, endColor: Int) {
        this.gradient?.updateGradientColor(startColor, endColor)
        isPathDirty = true
        invalidate()
    }

    fun updateGradientAngle(angle: Int) {
        this.gradient?.updateGradientAngle(angle)
        isPathDirty = true
        invalidate()
    }

    fun updateGradientColors(colors: IntArray?) {
        this.gradient?.updateGradientColors(colors)
        isPathDirty = true
        invalidate()
    }

    fun updateGradientPositions(positions: FloatArray?) {
        this.gradient?.gradientPositions = positions
        isPathDirty = true
        invalidate()
    }

    fun updateLocalMatrix(matrix: Matrix?) {
        this.gradient?.updateLocalMatrix(matrix)
        isPathDirty = true
        invalidate()
    }

    fun updateGradientShader(shader: LinearGradient?) {
        gradient?.updateGradientShader(shader)
        isPathDirty = true
        invalidate()
    }

    fun updateGradientOffsetX(offset: Float) {
        this.gradient?.updateGradientOffsetX(offset)
        isPathDirty = true
        invalidate()
    }

    fun updateGradientOffsetY(offset: Float) {
        this.gradient?.updateGradientOffsetY(offset)
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeGradientColor(startColor: Int, centerColor: Int, endColor: Int) {
        this.strokeGradient?.updateGradientColor(startColor, centerColor, endColor)
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeGradientColor(startColor: Int, endColor: Int) {
        this.strokeGradient?.updateGradientColor(startColor, endColor)
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeGradientAngle(angle: Int) {
        this.strokeGradient?.updateGradientAngle(angle)
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeGradientColors(colors: IntArray?) {
        this.strokeGradient?.updateGradientColors(colors)
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeGradientPositions(positions: FloatArray?) {
        this.strokeGradient?.updateGradientPositions(positions)
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeLocalMatrix(matrix: Matrix?) {
        this.strokeGradient?.updateLocalMatrix(matrix)
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeGradientShader(shader: LinearGradient?) {
        this.strokeGradient?.updateGradientShader(shader)
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeGradientOffsetX(offset: Float) {
        this.strokeGradient?.updateGradientOffsetX(offset)
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeGradientOffsetY(offset: Float) {
        this.strokeGradient?.updateGradientOffsetY(offset)
        isPathDirty = true
        invalidate()
    }

    fun updateBackgroundRadiusHalf(enable: Boolean) {
        this.radius?.radiusHalf = enable
        isPathDirty = true
        invalidate()
    }

    fun updateBackgroundBlur(blur: Float) {
        this.backgroundBlur = blur
        isPathDirty = true
        invalidate()
    }

    fun updateBackgroundBlurType(blurType: BlurMaskFilter.Blur) {
        this.backgroundBlurType = blurType
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeBlur(blur: Float) {
        this.stroke?.blur = blur
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeBlurType(blurType: BlurMaskFilter.Blur) {
        this.stroke?.blurType = blurType
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeDrawAsOverlay(drawAsOverlay: Boolean) {
        this.stroke?.drawAsOverlay = drawAsOverlay
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeType(strokeType: StrokeType) {
        this.stroke?.strokeType = strokeType
        isPathDirty = true
        if (autoAdjustPadding) {
            updatePadding()
        }
        invalidate()
    }

    fun updateStrokeAlpha(alpha: Int) {
        this.stroke?.strokeAlpha = alpha
        isPathDirty = true
        invalidate()
    }

    fun getGradientInfo(): Gradient? {
        return this.gradient
    }

    fun getRadiusInfo(): Radius? {
        return this.radius
    }

    fun getStrokeInfo(): Stroke? {
        return this.stroke
    }

    private fun setOutlineAndBackground(offset: RectF) {

        if (stroke?.isEnable == true) {

            val width = abs(offset.right - offset.left)
            val height = abs(offset.bottom - offset.top)

            when (stroke!!.strokeType) {
                StrokeType.INSIDE -> {

                    val maxAllowedWidth = width / 2f
                    val maxAllowedHeight = height / 2f

                    stroke!!.strokeWidth =
                        minOf(stroke!!.strokeWidth, maxAllowedWidth, maxAllowedHeight)

                    val calStrokeWidth = stroke!!.strokeWidth.div(2f)

                    outlineRect.set(
                        RectF(
                            offset.left + calStrokeWidth,
                            offset.top + calStrokeWidth,
                            offset.right - calStrokeWidth,
                            offset.bottom - calStrokeWidth
                        )
                    )
                    shadowRect.set(
                        RectF(
                            offset.left,
                            offset.top,
                            offset.right,
                            offset.bottom
                        )
                    )
                }

                StrokeType.CENTER -> {

                    val calStrokeWidth = stroke!!.strokeWidth.div(2f)

                    outlineRect.set(offset)
                    shadowRect.set(
                        RectF(
                            offset.left - calStrokeWidth,
                            offset.top - calStrokeWidth,
                            offset.right + calStrokeWidth,
                            offset.bottom + calStrokeWidth
                        )
                    )
                }

                StrokeType.OUTSIDE -> {

                    val calStrokeWidth = stroke!!.strokeWidth.div(2f)

                    outlineRect.set(
                        RectF(
                            offset.left - calStrokeWidth,
                            offset.top - calStrokeWidth,
                            offset.right + calStrokeWidth,
                            offset.bottom + calStrokeWidth
                        )
                    )
                    shadowRect.set(
                        RectF(
                            offset.left - stroke!!.strokeWidth,
                            offset.top - stroke!!.strokeWidth,
                            offset.right + stroke!!.strokeWidth,
                            offset.bottom + stroke!!.strokeWidth
                        )
                    )
                }
            }
        } else {
            outlineRect.set(offset)
            shadowRect.set(offset)
        }

        if (stroke?.isEnable == true) {

            with(outlinePaint) {

                isAntiAlias = true

                val targetColor = if (strokeGradient?.isEnable == true) {
                    Color.WHITE
                } else {
                    stroke!!.strokeColor
                }
                style = Paint.Style.STROKE
                color = targetColor
                alpha = stroke!!.strokeAlpha
                strokeWidth = stroke!!.strokeWidth
                shader = if (strokeGradient?.isEnable == true) {
                    strokeGradient?.getGradientShader(
                        outlineRect.left,
                        outlineRect.top,
                        outlineRect.right,
                        outlineRect.bottom
                    )
                } else {
                    null
                }

                maskFilter = if (stroke!!.blur != 0f) {
                    BlurMaskFilter(stroke!!.blur, stroke!!.blurType)
                } else {
                    null
                }
            }
        }

        with(backgroundPaint) {
            val targetColor = if (gradient?.isEnable == true) {
                Color.WHITE
            } else {
                backgroundColor
            }
            isAntiAlias = true
            color = targetColor
            style = Paint.Style.FILL

            maskFilter = if (backgroundBlur != 0f) {
                BlurMaskFilter(backgroundBlur, backgroundBlurType)
            } else {
                null
            }
        }

        outlinePath.apply {
            reset()

            if (radius?.isEnable == true) {
                addSmoothRoundRect(outlineRect, radius!!)
            } else {
                addRect(outlineRect, Path.Direction.CW)
            }

            close()
        }

        backgroundPath.apply {

            reset()

            if (stroke?.isEnable == true) {
                val newPath = outlinePath.getInnerPath(stroke!!.strokeWidth)
                newPath.computeBounds(targetRect, true)
                addPath(newPath)
            } else {
                targetRect.set(outlineRect)
                if (radius?.isEnable == true) {
                    addSmoothRoundRect(outlineRect, radius!!)
                } else {
                    addRect(outlineRect, Path.Direction.CW)
                }
            }

            backgroundPaint.shader = if (gradient?.isEnable == true) {
                gradient?.getGradientShader(
                    targetRect.left,
                    targetRect.top,
                    targetRect.right,
                    targetRect.bottom
                )
            } else {
                null
            }

            close()
        }
    }

    fun setAutoAdjustPadding(isEnable: Boolean) {
        autoAdjustPadding = isEnable
        updatePadding()
    }

    inner class Builder {
        fun backgroundColor(color: Int) = apply { this@ShadowLayout.backgroundColor = color }
        fun backgroundBlur(blur: Float) = apply { this@ShadowLayout.backgroundBlur = blur }
        fun backgroundBlurType(type: BlurMaskFilter.Blur) = apply { this@ShadowLayout.backgroundBlurType = type }
        fun renderMode(mode: Int) = apply { this@ShadowLayout.renderMode = mode; this@ShadowLayout.applyRenderMode() }
        
        fun radius(block: Radius.() -> Unit) = apply {
            if (this@ShadowLayout.radius == null) this@ShadowLayout.radius = Radius()
            this@ShadowLayout.radius?.block()
        }

        fun shadow(index: Int = 0, block: Shadow.() -> Unit) = apply {
            if (index >= 0 && index < this@ShadowLayout.shadows.size) {
                this@ShadowLayout.shadows[index].block()
            } else {
                val shadow = Shadow()
                shadow.block()
                this@ShadowLayout.shadows.add(shadow)
            }
        }
        
        fun clearShadows() = apply { this@ShadowLayout.shadows.clear() }

        fun stroke(block: Stroke.() -> Unit) = apply {
            if (this@ShadowLayout.stroke == null) this@ShadowLayout.stroke = Stroke()
            this@ShadowLayout.stroke?.block()
        }

        fun gradient(block: Gradient.() -> Unit) = apply {
            if (this@ShadowLayout.gradient == null) this@ShadowLayout.gradient = Gradient()
            this@ShadowLayout.gradient?.block()
        }

        fun strokeGradient(block: Gradient.() -> Unit) = apply {
            if (this@ShadowLayout.strokeGradient == null) this@ShadowLayout.strokeGradient = Gradient()
            this@ShadowLayout.strokeGradient?.block()
        }

        fun commit() {
            isPathDirty = true
            if (this@ShadowLayout.autoAdjustPadding) {
                this@ShadowLayout.updatePadding()
            }
            this@ShadowLayout.invalidate()
        }
    }

    fun build(block: Builder.() -> Unit) {
        Builder().apply(block).commit()
    }
}