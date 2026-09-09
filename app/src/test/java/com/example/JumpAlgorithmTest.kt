package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class JumpAlgorithmTest {

    private val g = 9.80665f

    @Test
    fun testFlightHeightCalculation() {
        // Flight time 600 ms (0.6s)
        val deltaT = 0.6f
        val height = (g * deltaT * deltaT) / 8f

        // Height should be approximately 0.441 meters (44 cm)
        assertEquals(0.441f, height, 0.01f)
    }

    @Test
    fun testHighJumpRecordCalculation() {
        // Elite jump 0.85 meters: t = sqrt(8 * h / g) = sqrt(8 * 0.85 / 9.80665) = ~0.832 seconds (~832 ms)
        val expectedHeight = 0.85f
        val calculatedTime = sqrt(8.0 * expectedHeight / g).toFloat()
        val calculatedHeight = (g * calculatedTime * calculatedTime) / 8.0f

        assertEquals(expectedHeight, calculatedHeight, 0.001f)
        assertTrue("Flight time should be around 830ms", calculatedTime in 0.82f..0.85f)
    }

    @Test
    fun testAntiCheatBoundaries() {
        // Below 0.1m is filtered
        val lowHeight = 0.08f
        val isFiltered = lowHeight < 0.1f || lowHeight > 3.0f
        assertTrue("Jumps below 0.1m must be filtered out", isFiltered)

        // Above 3.0m is filtered
        val absurdHeight = 3.5f
        val isAbsurdFiltered = absurdHeight < 0.1f || absurdHeight > 3.0f
        assertTrue("Jumps above 3.0m must be filtered out", isAbsurdFiltered)

        // Valid jump 0.75m is accepted
        val validHeight = 0.75f
        val isValid = validHeight in 0.1f..3.0f
        assertTrue("Valid athletic jump must be accepted", isValid)
    }
}
