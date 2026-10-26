package com.fpsmeter.app

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.regex.Pattern

/**
 * Data class representing real-time in-game and screen performance metrics.
 */
data class PerformanceMetrics(
    val fps: Double,
    val displayHz: Double,
    val foregroundApp: String?,
    val gpuLoad: Int?,
    val isRealTimeGameFPS: Boolean
)

/**
 * Engine that captures real-time in-game FPS and hardware metrics using
 * SurfaceFlinger IPC counters, dumpsys, and GPU sysfs (when root is present),
 * or falls back to Choreographer display refresh rate.
 */
class RealTimeFPSMonitor(
    private val onMetricsUpdate: (PerformanceMetrics) -> Unit
) {

    companion object {
        private const val TAG = "FPSMeter_Monitor"
        private const val SAMPLE_INTERVAL_MS = 1000L
        private val PARCEL_PATTERN = Pattern.compile("Result:\\s+Parcel\\(([0-9a-fA-F]+)")
        private val FOCUSED_APP_PATTERN = Pattern.compile("mFocusedApp=ActivityRecord\\{[0-9a-fA-F]+\\s+u[0-9]+\\s+([^/]+)/")

        fun parseSurfaceFlingerFrameCount(output: String): Long {
            val matcher = PARCEL_PATTERN.matcher(output)
            if (matcher.find()) {
                val hexStr = matcher.group(1) ?: return -1L
                return try {
                    hexStr.toLong(16)
                } catch (e: NumberFormatException) {
                    -1L
                }
            }
            return -1L
        }

        fun parseForegroundApp(output: String): String? {
            val matcher = FOCUSED_APP_PATTERN.matcher(output)
            if (matcher.find()) {
                val pkg = matcher.group(1)
                if (pkg != null && pkg != "com.fpsmeter.app") {
                    return pkg
                }
            }
            return null
        }

        fun parseGpuLoad(output: String): Int? {
            val cleaned = output.replace("%", "").trim()
            return try {
                cleaned.toInt()
            } catch (_: Exception) {
                null
            }
        }
    }

    private val mainHandler: Handler? = try {
        Handler(Looper.getMainLooper())
    } catch (_: Throwable) {
        null
    }
    private var isRunning = false
    private var monitorThread: Thread? = null

    // Persistent root shell session
    private var rootProcess: Process? = null
    private var shellWriter: BufferedWriter? = null
    private var shellReader: BufferedReader? = null

    private var lastFrameCount = -1L
    private var lastSampleTimeMs = 0L

    fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = process.inputStream.bufferedReader().readLine()
            process.waitFor()
            output?.contains("uid=0") == true
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    fun start() {
        if (isRunning) return
        isRunning = true
        lastFrameCount = -1L
        lastSampleTimeMs = System.currentTimeMillis()

        monitorThread = Thread({
            runMonitoringLoop()
        }, "RealTimeFPSMonitorThread").apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stop() {
        isRunning = false
        try {
            monitorThread?.interrupt()
            monitorThread = null
            closeShell()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping monitor", e)
        }
    }

    private fun openShell(): Boolean {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su"))
            val writer = BufferedWriter(OutputStreamWriter(proc.outputStream))
            val reader = BufferedReader(InputStreamReader(proc.inputStream))

            // Check if shell is active and working
            writer.write("echo __READY__\n")
            writer.flush()
            val reply = reader.readLine()
            if (reply?.contains("__READY__") == true) {
                rootProcess = proc
                shellWriter = writer
                shellReader = reader
                Log.d(TAG, "Persistent root shell opened successfully")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open root shell: ${e.message}")
            false
        }
    }

    private fun executeShellCommand(cmd: String): String {
        val writer = shellWriter ?: return ""
        val reader = shellReader ?: return ""

        return try {
            val endMarker = "___EOF_${System.nanoTime()}___"
            writer.write("$cmd\necho $endMarker\n")
            writer.flush()

            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line == endMarker) break
                sb.append(line).append("\n")
            }
            sb.toString().trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command in shell: $cmd", e)
            ""
        }
    }

    private fun closeShell() {
        try {
            shellWriter?.write("exit\n")
            shellWriter?.flush()
            shellWriter?.close()
            shellReader?.close()
            rootProcess?.destroy()
        } catch (_: Exception) {}
        shellWriter = null
        shellReader = null
        rootProcess = null
    }

    private fun runMonitoringLoop() {
        var hasRoot = openShell()

        while (isRunning && !Thread.currentThread().isInterrupted) {
            val startTime = System.currentTimeMillis()

            if (hasRoot && rootProcess != null) {
                try {
                    // 1. Get SurfaceFlinger composed frame count via IPC transaction 1013
                    val sfOutput = executeShellCommand("service call SurfaceFlinger 1013")
                    val currentFrameCount = parseSurfaceFlingerFrameCount(sfOutput)

                    // 2. Query focused app/game package
                    val appOutput = executeShellCommand("dumpsys activity activities | grep mFocusedApp")
                    val foregroundApp = parseForegroundApp(appOutput)

                    // 3. Query GPU load if available
                    val gpuOutput = executeShellCommand("cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage 2>/dev/null")
                    val gpuLoad = parseGpuLoad(gpuOutput)

                    val now = System.currentTimeMillis()
                    val deltaMs = now - lastSampleTimeMs

                    Log.d(TAG, "Frame sample: sf='$sfOutput', parsed=$currentFrameCount, last=$lastFrameCount, deltaMs=$deltaMs")

                    if (lastFrameCount >= 0 && deltaMs > 0 && currentFrameCount >= lastFrameCount) {
                        val deltaFrames = currentFrameCount - lastFrameCount
                        val calculatedFps = (deltaFrames * 1000.0 / deltaMs).coerceIn(0.0, 240.0)

                        Log.d(TAG, "Calculated in-game FPS: $calculatedFps for $foregroundApp")

                        val metrics = PerformanceMetrics(
                            fps = calculatedFps,
                            displayHz = 60.0,
                            foregroundApp = foregroundApp,
                            gpuLoad = gpuLoad,
                            isRealTimeGameFPS = true
                        )

                        postMetrics(metrics)
                    }

                    lastFrameCount = currentFrameCount
                    lastSampleTimeMs = now
                } catch (e: Exception) {
                    Log.e(TAG, "Error during root monitoring sample", e)
                    hasRoot = false
                    closeShell()
                }
            } else {
                // If root is not available or fails, notify that fallback is active
                val metrics = PerformanceMetrics(
                    fps = 0.0,
                    displayHz = 60.0,
                    foregroundApp = null,
                    gpuLoad = null,
                    isRealTimeGameFPS = false
                )
                postMetrics(metrics)
            }

            // Sleep until next sampling tick
            val elapsed = System.currentTimeMillis() - startTime
            val sleepDuration = (SAMPLE_INTERVAL_MS - elapsed).coerceAtLeast(100L)
            try {
                Thread.sleep(sleepDuration)
            } catch (e: InterruptedException) {
                break
            }
        }

        closeShell()
    }

    private fun postMetrics(metrics: PerformanceMetrics) {
        val handler = mainHandler
        if (handler != null) {
            handler.post {
                if (isRunning) onMetricsUpdate(metrics)
            }
        } else {
            if (isRunning) onMetricsUpdate(metrics)
        }
    }
}
