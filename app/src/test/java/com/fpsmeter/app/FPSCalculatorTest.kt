package com.fpsmeter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FPSCalculatorTest {

    private lateinit var calculator: FPSCalculator

    @Before
    fun setUp() {
        calculator = FPSCalculator(maxFrameHistory = 60)
    }

    @Test
    fun testInitialFPSIsZero() {
        assertEquals(0.0, calculator.fps, 0.001)
        assertEquals(0, calculator.getFrameCount())
    }

    @Test
    fun testSingleFrameReturnsZero() {
        val fps = calculator.onFrame(1_000_000_000L)
        assertEquals(0.0, fps, 0.001)
        assertEquals(1, calculator.getFrameCount())
    }

    @Test
    fun testExact60FPSTiming() {
        // At 60 FPS, each frame is ~16,666,666 ns (1/60th of a second)
        val frameDurationNanos = 1_000_000_000L / 60
        var currentTime = 0L

        for (i in 0 until 61) {
            calculator.onFrame(currentTime)
            currentTime += frameDurationNanos
        }

        // With 60 intervals of ~16.66ms over 1 second, calculated FPS should be 60.0
        assertEquals(60.0, calculator.fps, 0.5)
        assertEquals(60, calculator.getFrameCount())
    }

    @Test
    fun testExact120FPSTiming() {
        val frameDurationNanos = 1_000_000_000L / 120
        var currentTime = 0L

        for (i in 0 until 61) {
            calculator.onFrame(currentTime)
            currentTime += frameDurationNanos
        }

        assertEquals(120.0, calculator.fps, 1.0)
    }

    @Test
    fun testExact30FPSTiming() {
        val frameDurationNanos = 1_000_000_000L / 30
        var currentTime = 0L

        for (i in 0 until 31) {
            calculator.onFrame(currentTime)
            currentTime += frameDurationNanos
        }

        assertEquals(30.0, calculator.fps, 0.5)
    }

    @Test
    fun testResetClearsFrames() {
        calculator.onFrame(100L)
        calculator.onFrame(200L)
        assertTrue(calculator.getFrameCount() > 0)

        calculator.reset()
        assertEquals(0, calculator.getFrameCount())
        assertEquals(0.0, calculator.fps, 0.001)
    }

    @Test
    fun testUpperBoundaryCapAt240() {
        // Simulate impossibly fast frame delta (e.g. 1 nanosecond)
        calculator.onFrame(0L)
        calculator.onFrame(1L)
        assertTrue(calculator.fps <= 240.0)
    }

    @Test
    fun testCalculateWindowFPS60Fps() {
        val now = 10_000_000_000L
        val frameInterval = 1_000_000_000L / 60 // ~16.66ms
        val timestamps = mutableListOf<Long>()

        for (i in 0 until 60) {
            timestamps.add(now - (59 - i) * frameInterval)
        }

        val (fps, frameTimeMs) = FPSCalculator.calculateWindowFPS(timestamps, now)
        assertEquals(60.0, fps, 0.5)
        assertEquals(16.67, frameTimeMs, 0.5)
    }

    @Test
    fun testCalculateWindowFPSIdleScreen() {
        val now = 10_000_000_000L
        // Last frame was 900ms ago (> 800ms idle threshold)
        val timestamps = listOf(
            now - 1_500_000_000L,
            now - 900_000_000L
        )

        val (fps, frameTimeMs) = FPSCalculator.calculateWindowFPS(timestamps, now)
        assertEquals(0.0, fps, 0.001)
        assertEquals(0.0, frameTimeMs, 0.001)
    }

    @Test
    fun testCalculateWindowFPSEmptyAndSingle() {
        val now = 10_000_000_000L
        val (fpsEmpty, _) = FPSCalculator.calculateWindowFPS(emptyList(), now)
        assertEquals(0.0, fpsEmpty, 0.001)

        val (fpsSingle, _) = FPSCalculator.calculateWindowFPS(listOf(now - 100_000L), now)
        assertEquals(0.0, fpsSingle, 0.001)
    }
}
