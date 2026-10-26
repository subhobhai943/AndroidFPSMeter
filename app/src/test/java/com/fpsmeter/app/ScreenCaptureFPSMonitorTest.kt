package com.fpsmeter.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenCaptureFPSMonitorTest {

    @Test
    fun testMonitorConstants() {
        assertEquals(64, ScreenCaptureFPSMonitor.CAPTURE_WIDTH)
        assertEquals(64, ScreenCaptureFPSMonitor.CAPTURE_HEIGHT)
        assertEquals(500L, ScreenCaptureFPSMonitor.UPDATE_INTERVAL_MS)
        assertEquals(1_000_000_000L, ScreenCaptureFPSMonitor.FRAME_WINDOW_NANOS)
    }

    @Test
    fun testRealTimeGameFPSAt90Hz() {
        val now = 20_000_000_000L
        val frameInterval = 1_000_000_000L / 90 // 90 FPS
        val timestamps = mutableListOf<Long>()

        for (i in 0 until 90) {
            timestamps.add(now - (89 - i) * frameInterval)
        }

        val (fps, frameTimeMs) = FPSCalculator.calculateWindowFPS(timestamps, now)
        assertEquals(90.0, fps, 1.0)
        assertEquals(11.11, frameTimeMs, 0.5)
    }

    @Test
    fun testRealTimeGameFPSVariableStutter() {
        val now = 30_000_000_000L
        // 45 frames delivered over 1 second (e.g. game stuttering at 45 FPS)
        val frameInterval = 1_000_000_000L / 45
        val timestamps = mutableListOf<Long>()

        for (i in 0 until 45) {
            timestamps.add(now - (44 - i) * frameInterval)
        }

        val (fps, frameTimeMs) = FPSCalculator.calculateWindowFPS(timestamps, now)
        assertEquals(45.0, fps, 0.5)
        assertEquals(22.22, frameTimeMs, 0.5)
    }
}
