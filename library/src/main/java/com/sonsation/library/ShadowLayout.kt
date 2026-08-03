package com.sonsation.library

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout
import com.sonsation.library.effet.*
import com.sonsation.library.model.Padding
import com.sonsation.library.model.StrokeOrigin
import com.sonsation.library.model.StrokeType
import com.sonsation.library.render.DirectShadowRenderer
import com.sonsation.library.render.ShadowRenderContext
import com.sonsation.library.render.ShadowRenderer
import com.sonsation.library.render.ShadowRendererFactory
import com.sonsation.library.render.SoftwareBlurLayer
import com.sonsation.library.utils.ViewHelper
import com.sonsation.library.utils.addSmoothRoundRect
import com.sonsation.library.utils.isBlurUsable
import java.util.Collections
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

    private val strokePath by lazy {
        Path()
    }

    private val innerClipPath by lazy {
        Path()
    }

    private val innerClipRect by lazy {
        RectF()
    }

    private val pathMeasure by lazy {
        PathMeasure()
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
        const val RENDER_MODE_RENDER_NODE = 3

        // Lets the square outline go through the same origin aware builder as the rounded one.
        private val NO_RADIUS = Radius(0f)

        /**
         * First release whose hardware pipeline honours a `BlurMaskFilter`. Below this a
         * blurred stroke or background has to be rasterized on the CPU first, which costs
         * a bitmap - so the line is measured, not assumed. See
         * `StrokeBlurHardwareCanvasTest`.
         */
        private const val FIRST_SDK_WITH_HARDWARE_BLUR = Build.VERSION_CODES.P
    }

    var renderMode = ShadowRendererFactory.defaultMode()
        private set

    // How the shadows reach the screen. The view owns the geometry and hands it to the
    // renderer through renderContext; swapping the strategy is the only thing a render
    // mode change does.
    private var renderer: ShadowRenderer = ShadowRendererFactory.create(renderMode)

    private val renderContext by lazy {
        ShadowRenderContext(layoutRect, backgroundPath, shadows)
    }

    // Used when the active renderer cannot draw on the current canvas (a software canvas
    // cannot replay a display list, for instance). Allocated only if that happens.
    private var fallbackRenderer: DirectShadowRenderer? = null

    // Cached BlurMaskFilter inputs so the filter is only reallocated when the blur
    // actually changes (mirrors the caching Shadow.updatePaint already does).
    private var cachedStrokeBlur: Float? = null
    private var cachedStrokeBlurType: BlurMaskFilter.Blur? = null
    private var cachedBackgroundBlur: Float? = null
    private var cachedBackgroundBlurType: BlurMaskFilter.Blur? = null

    private var isPathDirty = true
    private var isPaintDirty = false
    // A renderer's cache only holds shadows. It must be regenerated on any geometry
    // change (always implied by isPathDirty) or when a shadow's paint changes, but
    // NOT for background/stroke/gradient color changes that never touch the cache.
    private var isCacheDirty = true

    // Which element a paint change belongs to, so the software blur rasterizations can be
    // redone one at a time instead of both together. A setter that marks isPaintDirty
    // without naming a scope - and isCacheDirty names the shadows - is treated as
    // touching everything, so forgetting one of these costs work rather than correctness.
    private var isBackgroundPaintDirty = false
    private var isStrokePaintDirty = false

    // The stroke width actually used for both geometry and painting. Guards against
    // negative values and caps INSIDE/CENTER strokes so they cannot exceed the view
    // (a width larger than the shape would otherwise overflow it). OUTSIDE grows
    // outward, so it is capped to the larger view dimension to keep the shadow
    // bitmap cache from exploding into an OutOfMemoryError on extreme inputs.
    private val safeStrokeWidth: Float
        get() {
            val s = stroke ?: return 0f
            if (!s.isEnable) return 0f
            val requested = s.strokeWidth.coerceAtLeast(0f)
            val width = layoutRect.width()
            val height = layoutRect.height()
            return when (s.strokeType) {
                StrokeType.INSIDE -> minOf(requested, width / 2f, height / 2f)
                StrokeType.CENTER -> minOf(requested, width, height)
                StrokeType.OUTSIDE -> minOf(requested, maxOf(width, height))
            }
        }

    private val strokeOutset: Float
        get() = if (stroke?.isEnable == true) {
            when (stroke!!.strokeType) {
                StrokeType.INSIDE -> 0f
                StrokeType.CENTER -> safeStrokeWidth / 2f
                StrokeType.OUTSIDE -> safeStrokeWidth
            }
        } else {
            0f
        }

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
    private val mutableShadows by lazy {
        mutableListOf<Shadow>()
    }

    /**
     * The shadows, in paint order.
     *
     * Read only, for two reasons. A shadow added straight to the list would draw without
     * marking the geometry dirty or invalidating, so the view would go on using the paths
     * and the cache it built for the old list. And the list is walked while the view is
     * drawing, where a mutation from anywhere else is a ConcurrentModificationException.
     *
     * [addBackgroundShadow] and the other setters do both halves of the job.
     *
     * Wrapped rather than merely typed read only, because Kotlin's `List` is only a promise
     * to the compiler - a Java caller, or a cast, reaches the same mutable list underneath.
     */
    val shadows: List<Shadow>
        get() = readOnlyShadows

    private val readOnlyShadows: List<Shadow> by lazy {
        Collections.unmodifiableList(mutableShadows)
    }

    var clipOutLine = false
        private set

    var shadowBitmapResolution = 0.5f
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
            shadowBitmapResolution = a.getFloat(R.styleable.ShadowLayout_shadow_bitmap_resolution, 0.5f).coerceIn(0.01f, 1.0f)
            renderMode = ShadowRendererFactory.resolveMode(
                a.getInt(
                    R.styleable.ShadowLayout_shadow_render_mode,
                    ShadowRendererFactory.defaultMode()
                )
            )
            installRenderer()
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
                this.strokeStart = a.getFloat(R.styleable.ShadowLayout_stroke_start, 0f)
                this.strokeProgress = a.getFloat(R.styleable.ShadowLayout_stroke_progress, 1f)
                this.strokeOrigin = StrokeOrigin.from(
                    a.getInt(R.styleable.ShadowLayout_stroke_origin, StrokeOrigin.TOP.value)
                )
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

            mutableShadows.add(shadow)

            val shadows = ViewHelper.parseShadowArray(
                context,
                a.getString(R.styleable.ShadowLayout_shadow_array)
            )

            if (!shadows.isNullOrEmpty()) {
                mutableShadows.addAll(shadows)
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
        val resolved = ShadowRendererFactory.resolveMode(mode)

        if (resolved == renderMode) {
            return
        }

        renderMode = resolved
        installRenderer()
        isPathDirty = true
        invalidate()
    }

    /** Swaps in the strategy for [renderMode], letting the previous one release its cache. */
    private fun installRenderer() {
        renderer.onUninstalled(this)
        renderer = ShadowRendererFactory.create(renderMode)
        renderer.onInstalled(this)
        // The layer only ever follows the renderer, so this is the only moment it can
        // change - no need to reconsider it on every invalidation.
        applyLayerType()
    }

    /**
     * Before API 28 a `BlurMaskFilter` is silently dropped on a hardware canvas - the
     * shape is drawn sharp, with no error. Measured on API 24 and 27: a stroke or
     * background blur produces pixels identical to no blur at all, while API 36 blurs
     * normally.
     *
     * The shadows escape this through [BitmapCacheShadowRenderer], the default below
     * API 28. The background and the stroke have no such cover, so they rasterize
     * themselves through [SoftwareBlurLayer] instead.
     */
    private val isBlurLostOnHardware: Boolean
        get() = Build.VERSION.SDK_INT < FIRST_SDK_WITH_HARDWARE_BLUR

    // A blur the platform would reject is no blur at all, so it is not worth a bitmap either.
    private val needsSoftwareBackgroundBlur: Boolean
        get() = isBlurLostOnHardware && isBlurUsable(backgroundBlur)

    private val needsSoftwareStrokeBlur: Boolean
        get() = isBlurLostOnHardware && isBlurUsable(stroke?.takeIf { it.isEnable }?.blur ?: 0f)

    private var backgroundBlurLayer: SoftwareBlurLayer? = null
    private var strokeBlurLayer: SoftwareBlurLayer? = null

    private fun applyLayerType() {
        val target = renderer.preferredLayerType

        if (layerType != target) {
            setLayerType(target, null)
        }
    }

    /**
     * Rebuilds the software rasterizations of the background and the stroke, and drops
     * the ones that are no longer needed. No-op on the platforms that blur on hardware.
     */
    private fun updateSoftwareBlurLayers(rebuildBackground: Boolean, rebuildStroke: Boolean) {

        if (needsSoftwareBackgroundBlur) {
            val layer = backgroundBlurLayer ?: SoftwareBlurLayer().also { backgroundBlurLayer = it }
            // A layer that has nothing in it yet has to rasterize whatever the flags say.
            if (rebuildBackground || !layer.isReady) {
                // A failed rebuild is not worth retrying every frame; drop back to painting
                // the shape directly, blur and all.
                if (!layer.rebuild(backgroundPath, backgroundPaint, backgroundBlur)) {
                    layer.release()
                    backgroundBlurLayer = null
                }
            }
        } else {
            backgroundBlurLayer?.release()
            backgroundBlurLayer = null
        }

        if (needsSoftwareStrokeBlur) {
            val layer = strokeBlurLayer ?: SoftwareBlurLayer().also { strokeBlurLayer = it }
            if (rebuildStroke || !layer.isReady) {
                if (!layer.rebuild(strokePath, outlinePaint, stroke?.blur ?: 0f)) {
                    layer.release()
                    strokeBlurLayer = null
                }
            }
        } else {
            strokeBlurLayer?.release()
            strokeBlurLayer = null
        }
    }

    private fun drawShadows(canvas: Canvas) {

        if (renderer.draw(canvas, renderContext)) {
            return
        }

        // The renderer passed on this frame - a display list cannot be replayed onto a
        // software canvas, say. Paint the shadows straight onto it instead.
        val fallback = fallbackRenderer ?: DirectShadowRenderer().also { fallbackRenderer = it }
        fallback.draw(canvas, renderContext)
    }

    override fun dispatchDraw(canvas: Canvas) {

        val isCacheStale = isPathDirty || isCacheDirty
        val wasDirty = isPathDirty || isPaintDirty

        // A geometry change rebuilds both paths, so both rasterizations follow it. A paint
        // change only needs the element it belongs to - unless no setter said which, in
        // which case both are redone rather than risk leaving a stale one on screen.
        val isPaintScopeKnown = isBackgroundPaintDirty || isStrokePaintDirty || isCacheDirty
        val unscopedPaintChange = isPaintDirty && !isPaintScopeKnown
        val rebuildBackgroundBlur = isPathDirty || isBackgroundPaintDirty || unscopedPaintChange
        val rebuildStrokeBlur = isPathDirty || isStrokePaintDirty || unscopedPaintChange

        if (wasDirty) {
            if (isPathDirty) {
                setOutlineAndBackground(layoutRect)
            } else {
                updatePaintsOnly()
            }
            shadows.forEach { shadow ->
                if (isPathDirty) {
                    shadow.updatePath(shadowRect, radius, strokeOutset)
                }
                shadow.updatePaint()
            }
            isPathDirty = false
            isPaintDirty = false
            isBackgroundPaintDirty = false
            isStrokePaintDirty = false

            updateSoftwareBlurLayers(rebuildBackgroundBlur, rebuildStrokeBlur)
        }

        // Refreshed whenever the renderer is about to read it, rather than every frame.
        if (wasDirty || isCacheStale) {
            renderContext.strokeOutset = strokeOutset
            renderContext.strokeBlur = stroke?.takeIf { it.isEnable }?.blur ?: 0f
            renderContext.bitmapResolution = shadowBitmapResolution
        }

        if (!renderer.prepare(renderContext, isCacheStale)) {
            // The renderer gave up for good (it ran out of memory building its cache).
            renderMode = RENDER_MODE_DEFAULT
            installRenderer()
        }

        isCacheDirty = false

        drawShadows(canvas)

        val background = backgroundBlurLayer
        if (background != null && background.isReady) {
            background.draw(canvas)
        } else {
            canvas.drawPath(backgroundPath, backgroundPaint)
        }

        val forceInnerClip = autoAdjustPadding && stroke?.isEnable == true

        if (clipOutLine || forceInnerClip) {
            canvas.save()
            if (forceInnerClip) {
                canvas.clipPath(innerClipPath)
            } else {
                canvas.clipPath(backgroundPath)
            }
            super.dispatchDraw(canvas)
            canvas.restore()
        } else {
            super.dispatchDraw(canvas)
        }

        if (stroke?.isEnable == true) {
            val strokeLayer = strokeBlurLayer
            if (strokeLayer != null && strokeLayer.isReady) {
                strokeLayer.draw(canvas)
            } else {
                canvas.drawPath(strokePath, outlinePaint)
            }
        }
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

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)
        // A start/end stroke origin mirrors with the layout direction.
        isPathDirty = true
        invalidate()
    }


    private fun updatePadding() {
        // Use the relative variant so start/end map to the correct physical side
        // under RTL (the stored padding is start/end, not left/right).
        setPaddingRelative(padding.start, padding.top, padding.end, padding.bottom)
    }

    fun updateBackgroundColor(color: Int) {
        backgroundColor = color
        isPaintDirty = true
        isBackgroundPaintDirty = true
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
        mutableShadows.add(shadow)
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
        mutableShadows.add(shadow)
        isPathDirty = true
        invalidate()
    }

    fun removeBackgroundShadowLast() {
        mutableShadows.removeLastOrNull()
        isPathDirty = true
        invalidate()
    }

    fun removeBackgroundShadowFirst() {
        mutableShadows.removeFirstOrNull()
        isPathDirty = true
        invalidate()
    }

    fun removeAllBackgroundShadows() {
        mutableShadows.clear()
        isPathDirty = true
        invalidate()
    }

    /**
     * Whether [position] names a shadow that exists.
     *
     * The shadow list starts empty unless the view was inflated from XML, and any of the
     * remove calls can empty it again, so a position is a request rather than a fact. The
     * setters below ignore one that names nothing, which is how every other setter on this
     * view treats a target that is not there - see [updateStrokeWidth] with no stroke set.
     * [addBackgroundShadow] is the call that creates one.
     */
    private fun hasShadowAt(position: Int) = position >= 0 && position < mutableShadows.size

    fun removeBackgroundShadow(position: Int) {
        if (!hasShadowAt(position)) {
            return
        }
        mutableShadows.removeAt(position)
        isPathDirty = true
        invalidate()
    }

    fun updateBackgroundShadow(position: Int, shadow: Shadow) {
        if (!hasShadowAt(position)) {
            return
        }
        mutableShadows[position] = shadow
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
        if (!hasShadowAt(position)) {
            return
        }
        updateBackgroundShadow(position, blurSize, offsetX, offsetY, mutableShadows[position].shadowSpread, color)
    }

    fun updateBackgroundShadow(
        position: Int,
        blurSize: Float,
        offsetX: Float,
        offsetY: Float,
        spread: Float,
        color: Int
    ) {
        if (!hasShadowAt(position)) {
            return
        }

        val shadow = mutableShadows[position]
        val wasEnable = shadow.isEnable
        val isEnableChanged = wasEnable != (color != ViewHelper.NOT_SET_COLOR)
        val geometryChanged = shadow.blurSize != blurSize || shadow.shadowOffsetX != offsetX || shadow.shadowOffsetY != offsetY || shadow.shadowSpread != spread || isEnableChanged
        
        shadow.apply {
            this.blurSize = blurSize
            this.shadowColor = color
            this.shadowOffsetX = offsetX
            this.shadowOffsetY = offsetY
            this.shadowSpread = spread
        }
        
        if (geometryChanged) {
            isPathDirty = true
        } else {
            // Only the shadow color changed: repaint the cached shadow bitmap.
            isPaintDirty = true
            isCacheDirty = true
        }
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

    fun updateShadowBlurType(blurType: BlurMaskFilter.Blur) {
        this.shadows.forEach { it.updateShadowBlurType(blurType) }
        isPaintDirty = true
        isCacheDirty = true
        invalidate()
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
                }
            }
            isPathDirty = true
            invalidate()
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

        padding.setPadding(start, top, end, bottom)

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
                }
            }
            isPathDirty = true
            invalidate()
            return
        }

        super.setPaddingRelative(start, top, end, bottom)
    }

    fun updateStrokeColor(color: Int) {
        val wasEnable = stroke?.isEnable == true
        stroke?.updateStrokeColor(color)
        
        if (wasEnable != (stroke?.isEnable == true)) {
            isPathDirty = true
            if (autoAdjustPadding) {
                updatePadding()
            }
        }
        
        isPaintDirty = true
        isStrokePaintDirty = true
        invalidate()
    }

    fun updateGradientColor(startColor: Int, centerColor: Int, endColor: Int) {
        this.gradient?.updateGradientColor(startColor, centerColor, endColor)
        isPaintDirty = true
        isBackgroundPaintDirty = true
        invalidate()
    }

    fun updateGradientColor(startColor: Int, endColor: Int) {
        this.gradient?.updateGradientColor(startColor, endColor)
        isPaintDirty = true
        isBackgroundPaintDirty = true
        invalidate()
    }

    fun updateGradientAngle(angle: Int) {
        this.gradient?.updateGradientAngle(angle)
        isPaintDirty = true
        isBackgroundPaintDirty = true
        invalidate()
    }

    fun updateGradientColors(colors: IntArray?) {
        this.gradient?.updateGradientColors(colors)
        isPaintDirty = true
        isBackgroundPaintDirty = true
        invalidate()
    }

    fun updateGradientPositions(positions: FloatArray?) {
        this.gradient?.updateGradientPositions(positions)
        isPaintDirty = true
        isBackgroundPaintDirty = true
        invalidate()
    }

    fun updateLocalMatrix(matrix: Matrix?) {
        this.gradient?.updateLocalMatrix(matrix)
        isPaintDirty = true
        isBackgroundPaintDirty = true
        invalidate()
    }

    fun updateGradientShader(shader: LinearGradient?) {
        gradient?.updateGradientShader(shader)
        isPaintDirty = true
        isBackgroundPaintDirty = true
        invalidate()
    }

    fun updateGradientOffsetX(offset: Float) {
        this.gradient?.updateGradientOffsetX(offset)
        isPaintDirty = true
        isBackgroundPaintDirty = true
        invalidate()
    }

    fun updateGradientOffsetY(offset: Float) {
        this.gradient?.updateGradientOffsetY(offset)
        isPaintDirty = true
        isBackgroundPaintDirty = true
        invalidate()
    }

    fun updateStrokeGradientColor(startColor: Int, centerColor: Int, endColor: Int) {
        this.strokeGradient?.updateGradientColor(startColor, centerColor, endColor)
        isPaintDirty = true
        isStrokePaintDirty = true
        invalidate()
    }

    fun updateStrokeGradientColor(startColor: Int, endColor: Int) {
        this.strokeGradient?.updateGradientColor(startColor, endColor)
        isPaintDirty = true
        isStrokePaintDirty = true
        invalidate()
    }

    fun updateStrokeGradientAngle(angle: Int) {
        this.strokeGradient?.updateGradientAngle(angle)
        isPaintDirty = true
        isStrokePaintDirty = true
        invalidate()
    }

    fun updateStrokeGradientColors(colors: IntArray?) {
        this.strokeGradient?.updateGradientColors(colors)
        isPaintDirty = true
        isStrokePaintDirty = true
        invalidate()
    }

    fun updateStrokeGradientPositions(positions: FloatArray?) {
        this.strokeGradient?.updateGradientPositions(positions)
        isPaintDirty = true
        isStrokePaintDirty = true
        invalidate()
    }

    fun updateStrokeLocalMatrix(matrix: Matrix?) {
        this.strokeGradient?.updateLocalMatrix(matrix)
        isPaintDirty = true
        isStrokePaintDirty = true
        invalidate()
    }

    fun updateStrokeGradientShader(shader: LinearGradient?) {
        this.strokeGradient?.updateGradientShader(shader)
        isPaintDirty = true
        isStrokePaintDirty = true
        invalidate()
    }

    fun updateStrokeGradientOffsetX(offset: Float) {
        this.strokeGradient?.updateGradientOffsetX(offset)
        isPaintDirty = true
        isStrokePaintDirty = true
        invalidate()
    }

    fun updateStrokeGradientOffsetY(offset: Float) {
        this.strokeGradient?.updateGradientOffsetY(offset)
        isPaintDirty = true
        isStrokePaintDirty = true
        invalidate()
    }

    fun updateBackgroundRadiusHalf(enable: Boolean) {
        this.radius?.radiusHalf = enable
        isPathDirty = true
        invalidate()
    }

    fun updateBackgroundBlur(blur: Float) {
        this.backgroundBlur = blur
        isPaintDirty = true
        isBackgroundPaintDirty = true
        invalidate()
    }

    fun updateBackgroundBlurType(blurType: BlurMaskFilter.Blur) {
        this.backgroundBlurType = blurType
        isPaintDirty = true
        isBackgroundPaintDirty = true
        invalidate()
    }

    fun updateStrokeBlur(blur: Float) {
        this.stroke?.blur = blur
        isPaintDirty = true
        isStrokePaintDirty = true
        invalidate()
    }

    fun updateStrokeBlurType(blurType: BlurMaskFilter.Blur) {
        this.stroke?.blurType = blurType
        isPaintDirty = true
        isStrokePaintDirty = true
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
        isPaintDirty = true
        isStrokePaintDirty = true
        invalidate()
    }

    fun updateStrokeStart(start: Float) {
        this.stroke?.strokeStart = start
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeProgress(progress: Float) {
        this.stroke?.strokeProgress = progress
        isPathDirty = true
        invalidate()
    }

    fun updateStrokeOrigin(origin: StrokeOrigin) {
        this.stroke?.strokeOrigin = origin
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

    private fun applyStrokeBlur() {
        val blur = stroke?.blur ?: 0f
        val type = stroke?.blurType ?: BlurMaskFilter.Blur.NORMAL
        if (cachedStrokeBlur == blur && cachedStrokeBlurType == type) return
        // A radius the platform will not take costs the blur, not the frame - see [isBlurUsable].
        outlinePaint.maskFilter = if (isBlurUsable(blur)) BlurMaskFilter(blur, type) else null
        cachedStrokeBlur = blur
        cachedStrokeBlurType = type
    }

    private fun applyBackgroundBlur() {
        if (cachedBackgroundBlur == backgroundBlur && cachedBackgroundBlurType == backgroundBlurType) return
        backgroundPaint.maskFilter =
            if (isBlurUsable(backgroundBlur)) BlurMaskFilter(backgroundBlur, backgroundBlurType) else null
        cachedBackgroundBlur = backgroundBlur
        cachedBackgroundBlurType = backgroundBlurType
    }

    private fun updatePaintsOnly() {
        if (stroke?.isEnable == true) {
            with(outlinePaint) {
                isAntiAlias = true
                val targetColor = if (strokeGradient?.isEnable == true) Color.WHITE else stroke!!.strokeColor
                style = Paint.Style.STROKE
                color = targetColor
                alpha = stroke!!.strokeAlpha
                strokeWidth = safeStrokeWidth
                shader = if (strokeGradient?.isEnable == true) {
                    strokeGradient?.getGradientShader(
                        outlineRect.left, outlineRect.top, outlineRect.right, outlineRect.bottom
                    )
                } else null
            }
            applyStrokeBlur()
        }
        with(backgroundPaint) {
            val targetColor = if (gradient?.isEnable == true) Color.WHITE else backgroundColor
            isAntiAlias = true
            color = targetColor
            style = Paint.Style.FILL
            shader = if (gradient?.isEnable == true) {
                gradient?.getGradientShader(
                    targetRect.left, targetRect.top, targetRect.right, targetRect.bottom
                )
            } else null
        }
        applyBackgroundBlur()
    }

    private fun setOutlineAndBackground(offset: RectF) {

        val safeStrokeWidth = this.safeStrokeWidth
        val safeStrokeWidthHalf = safeStrokeWidth / 2f

        if (stroke?.isEnable == true) {

            when (stroke!!.strokeType) {
                StrokeType.INSIDE -> {
                    outlineRect.set(
                        offset.left + safeStrokeWidthHalf,
                        offset.top + safeStrokeWidthHalf,
                        offset.right - safeStrokeWidthHalf,
                        offset.bottom - safeStrokeWidthHalf
                    )
                    shadowRect.set(
                        offset.left,
                        offset.top,
                        offset.right,
                        offset.bottom
                    )
                }

                StrokeType.CENTER -> {
                    outlineRect.set(offset)
                    shadowRect.set(
                        offset.left - safeStrokeWidthHalf,
                        offset.top - safeStrokeWidthHalf,
                        offset.right + safeStrokeWidthHalf,
                        offset.bottom + safeStrokeWidthHalf
                    )
                }

                StrokeType.OUTSIDE -> {
                    outlineRect.set(
                        offset.left - safeStrokeWidthHalf,
                        offset.top - safeStrokeWidthHalf,
                        offset.right + safeStrokeWidthHalf,
                        offset.bottom + safeStrokeWidthHalf
                    )
                    shadowRect.set(
                        offset.left - safeStrokeWidth,
                        offset.top - safeStrokeWidth,
                        offset.right + safeStrokeWidth,
                        offset.bottom + safeStrokeWidth
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
                strokeWidth = safeStrokeWidth
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

            }
            applyStrokeBlur()
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
        }
        applyBackgroundBlur()

        val strokeRadiusOffset = if (stroke?.isEnable == true) {
            when (stroke!!.strokeType) {
                StrokeType.INSIDE -> -safeStrokeWidthHalf
                StrokeType.CENTER -> 0f
                StrokeType.OUTSIDE -> safeStrokeWidthHalf
            }
        } else {
            0f
        }

        // outlinePath only feeds the stroke, so it is built starting from the stroke origin - the
        // shape is unchanged, but distance 0 of pathMeasure then lands on the requested anchor.
        val strokeOrigin = (stroke?.strokeOrigin ?: StrokeOrigin.TOP)
            .resolve(layoutDirection == LAYOUT_DIRECTION_RTL)

        outlinePath.apply {
            reset()

            if (radius?.isEnable == true) {
                addSmoothRoundRect(outlineRect, radius!!, strokeRadiusOffset, strokeOrigin)
            } else {
                addSmoothRoundRect(outlineRect, NO_RADIUS, 0f, strokeOrigin)
            }

            close()
        }

        strokePath.reset()

        if (stroke?.isEnable == true) {
            val startRatio = stroke!!.strokeStart.coerceIn(0f, 1f)
            val lengthRatio = stroke!!.strokeProgress.coerceIn(0f, 1f)

            if (startRatio == 0f && lengthRatio == 1f) {
                strokePath.set(outlinePath)
            } else if (lengthRatio > 0f) {
                pathMeasure.setPath(outlinePath, false)
                val length = pathMeasure.length

                // A full turn lands back on the origin, so ratio 1 has to wrap to distance 0.
                // Leaving it at `length` would make both getSegment calls below degenerate and
                // the stroke would vanish instead of drawing the whole ring.
                val startDistance = if (startRatio == 1f) 0f else startRatio * length
                val endDistance = startDistance + lengthRatio * length

                if (endDistance > length) {
                    pathMeasure.getSegment(startDistance, length, strokePath, true)
                    pathMeasure.getSegment(0f, endDistance % length, strokePath, false)
                } else {
                    pathMeasure.getSegment(startDistance, endDistance, strokePath, true)
                }

                if (lengthRatio == 1f) {
                    strokePath.close()
                }
            }
        }

        backgroundPath.apply {

            reset()

            targetRect.set(offset)
            if (radius?.isEnable == true) {
                addSmoothRoundRect(offset, radius!!)
            } else {
                addRect(offset, Path.Direction.CW)
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

        innerClipPath.apply {
            reset()
            innerClipRect.set(offset)
            var innerRadiusOffset = 0f

            if (stroke?.isEnable == true && autoAdjustPadding) {
                when (stroke!!.strokeType) {
                    StrokeType.INSIDE -> {
                        innerClipRect.inset(safeStrokeWidth, safeStrokeWidth)
                        innerRadiusOffset = -safeStrokeWidth
                    }
                    StrokeType.CENTER -> {
                        innerClipRect.inset(safeStrokeWidthHalf, safeStrokeWidthHalf)
                        innerRadiusOffset = -safeStrokeWidthHalf
                    }
                    StrokeType.OUTSIDE -> {
                        // No inset needed for OUTSIDE
                    }
                }
            }

            if (radius?.isEnable == true) {
                addSmoothRoundRect(innerClipRect, radius!!, innerRadiusOffset)
            } else {
                addRect(innerClipRect, Path.Direction.CW)
            }

            close()
        }
    }

    fun setAutoAdjustPadding(isEnable: Boolean) {
        autoAdjustPadding = isEnable
        isPathDirty = true
        updatePadding()
        invalidate()
    }

    inner class Builder {
        fun backgroundColor(color: Int) = apply { this@ShadowLayout.backgroundColor = color }
        fun backgroundBlur(blur: Float) = apply { this@ShadowLayout.backgroundBlur = blur }
        fun backgroundBlurType(type: BlurMaskFilter.Blur) = apply { this@ShadowLayout.backgroundBlurType = type }
        fun renderMode(mode: Int) = apply { this@ShadowLayout.updateRenderMode(mode) }
        
        fun radius(block: Radius.() -> Unit) = apply {
            if (this@ShadowLayout.radius == null) this@ShadowLayout.radius = Radius()
            this@ShadowLayout.radius?.block()
        }

        fun shadow(index: Int = 0, block: Shadow.() -> Unit) = apply {
            if (this@ShadowLayout.hasShadowAt(index)) {
                this@ShadowLayout.mutableShadows[index].block()
            } else {
                val shadow = Shadow()
                shadow.block()
                this@ShadowLayout.mutableShadows.add(shadow)
            }
        }
        
        fun clearShadows() = apply { this@ShadowLayout.mutableShadows.clear() }

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

        fun shadowBitmapResolution(resolution: Float) = apply {
            this@ShadowLayout.updateShadowBitmapResolution(resolution)
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

    fun updateShadowBitmapResolution(resolution: Float) {
        this.shadowBitmapResolution = resolution.coerceIn(0.01f, 1.0f)
        isPathDirty = true
        invalidate()
    }
}