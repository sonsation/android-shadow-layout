package com.sonsation.library

import com.sonsation.library.effet.Radius
import com.sonsation.library.model.ARGB
import com.sonsation.library.model.Padding
import com.sonsation.library.model.StrokeType
import org.junit.Assert.*
import org.junit.Test

class ModelTest {

    @Test
    fun testARGB() {
        val argb = ARGB(255, 10, 20, 30)
        assertEquals(255, argb.alpha)
        assertEquals(10, argb.red)
        assertEquals(20, argb.green)
        assertEquals(30, argb.blue)

        // Test equality and toString for data class
        val argb2 = ARGB(255, 10, 20, 30)
        assertEquals(argb, argb2)
        assertEquals(argb.hashCode(), argb2.hashCode())
        assertTrue(argb.toString().contains("ARGB"))
    }

    @Test
    fun testStrokeType() {
        assertEquals(0, StrokeType.INSIDE.type)
        assertEquals(1, StrokeType.CENTER.type)
        assertEquals(2, StrokeType.OUTSIDE.type)

        assertEquals(StrokeType.INSIDE, StrokeType.valueOf("INSIDE"))
        assertEquals(3, StrokeType.entries.size)
    }

    @Test
    fun testPadding() {
        val padding = Padding(1, 2, 3, 4)
        assertEquals(1, padding.start)
        assertEquals(2, padding.top)
        assertEquals(3, padding.end)
        assertEquals(4, padding.bottom)

        padding.setPadding(5)
        assertEquals(5, padding.start)
        assertEquals(5, padding.top)
        assertEquals(5, padding.end)
        assertEquals(5, padding.bottom)

        padding.setPadding(6, 7, 8, 9)
        assertEquals(6, padding.start)
        assertEquals(7, padding.top)
        assertEquals(8, padding.end)
        assertEquals(9, padding.bottom)

        // data class methods
        val padding2 = padding.copy(start = 10)
        assertEquals(10, padding2.start)
        assertEquals(7, padding2.top)

        assertEquals(6, padding.component1())
        assertEquals(7, padding.component2())
        assertEquals(8, padding.component3())
        assertEquals(9, padding.component4())

        assertEquals(padding, padding)
        assertNotEquals(padding, padding2)
        assertNotEquals(padding, "string")
        assertEquals(padding.hashCode(), padding.hashCode())
        assertTrue(padding.toString().contains("Padding"))

        // test all equals branches
        assertNotEquals(padding, padding.copy(start = 99))
        assertNotEquals(padding, padding.copy(top = 99))
        assertNotEquals(padding, padding.copy(end = 99))
        assertNotEquals(padding, padding.copy(bottom = 99))
    }

    @Test
    fun testRadius() {
        val r0 = Radius()
        assertEquals(0f, r0.topLeftRadius, 0.01f)
        assertEquals(0f, r0.topRightRadius, 0.01f)
        assertEquals(0f, r0.bottomLeftRadius, 0.01f)
        assertEquals(0f, r0.bottomRightRadius, 0.01f)
        assertFalse(r0.isEnable)
        assertEquals(1f, r0.radiusWeight, 0.01f)
        assertEquals(0f, r0.cornerSmoothing, 0.01f)

        val r1 = Radius(10f)
        assertEquals(10f, r1.topLeftRadius, 0.01f)
        assertEquals(10f, r1.topRightRadius, 0.01f)
        assertEquals(10f, r1.bottomLeftRadius, 0.01f)
        assertEquals(10f, r1.bottomRightRadius, 0.01f)
        assertTrue(r1.isEnable)

        // Test all isEnable branches
        assertTrue(Radius(topLeftRadius = 5f).isEnable)
        assertTrue(Radius(topRightRadius = 5f).isEnable)
        assertTrue(Radius(bottomLeftRadius = 5f).isEnable)
        assertTrue(Radius(bottomRightRadius = 5f).isEnable)
        assertTrue(Radius().apply { radiusHalf = true }.isEnable)

        val r2 = Radius(0f, 0f, 0f, 0f)
        assertFalse(r2.isEnable)

        r2.radiusHalf = true
        assertTrue(r2.isEnable)
        r2.radiusHalf = false

        r2.updateRadius(5f)
        assertEquals(5f, r2.topLeftRadius, 0.01f)
        assertTrue(r2.isEnable)

        r2.updateRadius(1f, 2f, 3f, 4f)
        assertEquals(1f, r2.topLeftRadius, 0.01f)
        assertEquals(2f, r2.topRightRadius, 0.01f)
        assertEquals(3f, r2.bottomLeftRadius, 0.01f)
        assertEquals(4f, r2.bottomRightRadius, 0.01f)

        val arrayNormal = r1.getRadiusArray(100f)
        // radiusWeight is 1f, so it should be 10f for all elements
        val expectedNormal = floatArrayOf(10f, 10f, 10f, 10f, 10f, 10f, 10f, 10f)
        assertArrayEquals(expectedNormal, arrayNormal, 0.01f)

        r1.radiusHalf = true
        val arrayHalf = r1.getRadiusArray(100f)
        val expectedHalf = floatArrayOf(50f, 50f, 50f, 50f, 50f, 50f, 50f, 50f)
        assertArrayEquals(expectedHalf, arrayHalf, 0.01f)
    }
}
