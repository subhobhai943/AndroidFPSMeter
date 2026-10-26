package com.fpsmeter.app

/**
 * Helper class to calculate real-time FPS using sliding window of frame timestamps.
 */
class FPSCalculator(private val maxFrameHistory: Int = 60) {

    private val frameTimes = mutableListOf<Long>()

    var fps: Double = 0.0
        private set

    /**
     * Record a frame rendered at [frameTimeNanos] and update current FPS.
     * @return Calculated FPS value.
     */
    fun onFrame(frameTimeNanos: Long): Double {
        frameTimes.add(frameTimeNanos)
        if (frameTimes.size > maxFrameHistory) {
            frameTimes.removeAt(0)
        }
        fps = calculate()
        return fps
    }

    /**
     * Calculate FPS based on timestamps in the sliding window.
     */
    fun calculate(): Double {
        if (frameTimes.size < 2) {
            return 0.0
        }
        val startTime = frameTimes.first()
        val endTime = frameTimes.last()
        val durationNanos = endTime - startTime
        return if (durationNanos > 0) {
            ((frameTimes.size - 1) * 1_000_000_000.0 / durationNanos).coerceIn(0.0, 240.0)
        } else {
            0.0
        }
    }

    /**
     * Reset recorded frames and calculated FPS.
     */
    fun reset() {
        frameTimes.clear()
        fps = 0.0
    }

    fun getFrameCount(): Int = frameTimes.size
}
