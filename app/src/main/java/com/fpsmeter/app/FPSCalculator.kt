package com.fpsmeter.app

/**
 * Helper class to calculate real-time FPS using sliding window of frame timestamps.
 */
class FPSCalculator(private val maxFrameHistory: Int = 60) {

    companion object {
        const val DEFAULT_WINDOW_NANOS = 1_000_000_000L // 1.0 second
        const val DEFAULT_IDLE_THRESHOLD_NANOS = 800_000_000L // 800 ms

        /**
         * Pure function to calculate FPS and frame time from a list of frame timestamps.
         *
         * @param timestamps List of monotonic frame presentation timestamps in nanoseconds.
         * @param nowNanos Current monotonic time in nanoseconds.
         * @param windowDurationNanos Window duration (default 1 second).
         * @param idleThresholdNanos Duration after last frame beyond which display is considered idle (0 FPS).
         * @return Pair of (fps: Double, frameTimeMs: Double).
         */
        fun calculateWindowFPS(
            timestamps: List<Long>,
            nowNanos: Long = System.nanoTime(),
            windowDurationNanos: Long = DEFAULT_WINDOW_NANOS,
            idleThresholdNanos: Long = DEFAULT_IDLE_THRESHOLD_NANOS
        ): Pair<Double, Double> {
            val cutoff = nowNanos - windowDurationNanos
            val validTimestamps = timestamps.filter { it in cutoff..nowNanos }

            if (validTimestamps.size < 2) {
                return Pair(0.0, 0.0)
            }

            val last = validTimestamps.last()
            if ((nowNanos - last) > idleThresholdNanos) {
                // No frame delivered recently; screen or game is idle
                return Pair(0.0, 0.0)
            }

            val first = validTimestamps.first()
            val durationNanos = last - first
            if (durationNanos <= 0) {
                return Pair(0.0, 0.0)
            }

            val calculatedFps = ((validTimestamps.size - 1) * 1_000_000_000.0 / durationNanos).coerceIn(0.0, 240.0)
            val frameTimeMs = if (calculatedFps > 0.0) (1000.0 / calculatedFps) else 0.0
            return Pair(calculatedFps, frameTimeMs)
        }
    }

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
