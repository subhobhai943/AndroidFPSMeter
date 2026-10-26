package com.fpsmeter.app

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log

/**
 * Real-time in-game FPS monitor for non-rooted Android devices (Android 5.0 through Android 16+).
 *
 * Captures rendered frame presentations across any foreground application or game (PUBG, BGMI,
 * Minecraft, Call of Duty, Genshin Impact, etc.) without requiring root or PC setup.
 *
 * Uses MediaProjection with a minimal (64x64) auto-mirroring VirtualDisplay backed by an ImageReader.
 * SurfaceFlinger delivers each rendered frame to the virtual display, giving the exact in-game
 * frame presentation timestamps with negligible resource usage (~16KB per buffer).
 */
class ScreenCaptureFPSMonitor(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val onFpsUpdate: (fps: Double, frameTimeMs: Double) -> Unit,
    private val onStopped: (() -> Unit)? = null
) {

    companion object {
        private const val TAG = "FPSMeter_ScreenCapture"
        const val CAPTURE_WIDTH = 64
        const val CAPTURE_HEIGHT = 64
        const val UPDATE_INTERVAL_MS = 500L
        const val FRAME_WINDOW_NANOS = 1_000_000_000L // 1.0 second
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    @Volatile
    private var isRunning = false

    private val frameTimestamps = ArrayList<Long>()
    private val lock = Any()

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.d(TAG, "MediaProjection stopped by system")
            stop()
            onStopped?.let { mainHandler.post(it) }
        }
    }

    private val fpsUpdateRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            calculateAndPostFps()
            backgroundHandler?.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    /**
     * Start capturing frame presentations from the display compositor.
     */
    @Synchronized
    fun start() {
        if (isRunning) return
        Log.d(TAG, "Starting ScreenCaptureFPSMonitor")

        try {
            val thread = HandlerThread("ScreenCaptureFPSThread").apply { start() }
            handlerThread = thread
            val bgHandler = Handler(thread.looper)
            backgroundHandler = bgHandler

            // Android 14+ (API 34+) strictly requires registering callback BEFORE createVirtualDisplay
            mediaProjection.registerCallback(projectionCallback, bgHandler)

            // Minimal 64x64 RGBA_8888 ImageReader with 2 buffers
            val reader = ImageReader.newInstance(
                CAPTURE_WIDTH,
                CAPTURE_HEIGHT,
                PixelFormat.RGBA_8888,
                2
            )
            imageReader = reader

            reader.setOnImageAvailableListener({ ir ->
                try {
                    // Acquire latest image to flush queue and extract hardware timestamp
                    val image = ir.acquireLatestImage()
                    if (image != null) {
                        val timestampNanos = if (image.timestamp > 0L) image.timestamp else System.nanoTime()
                        image.close()
                        recordFrame(timestampNanos)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error acquiring frame image", e)
                }
            }, bgHandler)

            val densityDpi = context.resources.displayMetrics.densityDpi

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "FPSMeterCapture",
                CAPTURE_WIDTH,
                CAPTURE_HEIGHT,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                object : VirtualDisplay.Callback() {
                    override fun onPaused() {
                        Log.d(TAG, "VirtualDisplay paused")
                    }
                    override fun onResumed() {
                        Log.d(TAG, "VirtualDisplay resumed")
                    }
                    override fun onStopped() {
                        Log.d(TAG, "VirtualDisplay stopped")
                    }
                },
                bgHandler
            )

            isRunning = true
            synchronized(lock) {
                frameTimestamps.clear()
            }

            bgHandler.postDelayed(fpsUpdateRunnable, UPDATE_INTERVAL_MS)
            Log.d(TAG, "ScreenCaptureFPSMonitor started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ScreenCaptureFPSMonitor", e)
            stop()
            throw e
        }
    }

    fun recordFrame(timestampNanos: Long) {
        synchronized(lock) {
            frameTimestamps.add(timestampNanos)
            val cutoff = timestampNanos - FRAME_WINDOW_NANOS
            while (frameTimestamps.isNotEmpty() && frameTimestamps.first() < cutoff) {
                frameTimestamps.removeAt(0)
            }
        }
    }

    private fun calculateAndPostFps() {
        val nowNanos = System.nanoTime()
        val (fps, frameTimeMs) = synchronized(lock) {
            FPSCalculator.calculateWindowFPS(frameTimestamps, nowNanos)
        }

        mainHandler.post {
            if (isRunning) {
                onFpsUpdate(fps, frameTimeMs)
            }
        }
    }

    /**
     * Stop capturing and release all native display and reader resources.
     */
    @Synchronized
    fun stop() {
        if (!isRunning && virtualDisplay == null && imageReader == null) return
        Log.d(TAG, "Stopping ScreenCaptureFPSMonitor")
        isRunning = false

        backgroundHandler?.removeCallbacks(fpsUpdateRunnable)

        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing virtualDisplay", e)
        } finally {
            virtualDisplay = null
        }

        try {
            imageReader?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing imageReader", e)
        } finally {
            imageReader = null
        }

        try {
            mediaProjection.unregisterCallback(projectionCallback)
            mediaProjection.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping mediaProjection", e)
        }

        try {
            handlerThread?.quitSafely()
        } catch (e: Exception) {
            Log.e(TAG, "Error quitting handlerThread", e)
        } finally {
            handlerThread = null
            backgroundHandler = null
        }

        synchronized(lock) {
            frameTimestamps.clear()
        }
        Log.d(TAG, "ScreenCaptureFPSMonitor stopped")
    }
}
