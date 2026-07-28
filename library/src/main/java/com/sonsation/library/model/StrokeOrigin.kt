package com.sonsation.library.model

/**
 * The point on the outline where [com.sonsation.library.effet.Stroke.strokeStart] and
 * [com.sonsation.library.effet.Stroke.strokeProgress] are measured from.
 *
 * The first token names an edge, the second token names which end of that edge. A corner is
 * therefore bracketed by two origins: [TOP_END] is where the top edge stops (right before the
 * top-end corner radius begins) and [END_TOP] is where that same radius finishes.
 *
 * Values are declared clockwise starting from the center of the top edge.
 */
enum class StrokeOrigin(val value: Int) {
    TOP(0),
    TOP_END(1),
    END_TOP(2),
    END(3),
    END_BOTTOM(4),
    BOTTOM_END(5),
    BOTTOM(6),
    BOTTOM_START(7),
    START_BOTTOM(8),
    START(9),
    START_TOP(10),
    TOP_START(11);

    /**
     * Mirrors start/end horizontally when the layout direction is RTL. In the returned value
     * `START` always means the left edge and `END` always means the right edge.
     */
    fun resolve(isRtl: Boolean): StrokeOrigin = if (!isRtl) {
        this
    } else {
        when (this) {
            TOP -> TOP
            BOTTOM -> BOTTOM
            TOP_END -> TOP_START
            TOP_START -> TOP_END
            END_TOP -> START_TOP
            START_TOP -> END_TOP
            END -> START
            START -> END
            END_BOTTOM -> START_BOTTOM
            START_BOTTOM -> END_BOTTOM
            BOTTOM_END -> BOTTOM_START
            BOTTOM_START -> BOTTOM_END
        }
    }

    companion object {
        fun from(value: Int): StrokeOrigin = entries.find { it.value == value } ?: TOP
    }
}
