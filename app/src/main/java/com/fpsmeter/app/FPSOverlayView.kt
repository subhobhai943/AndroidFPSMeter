package com.fpsmeter.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.DecimalFormat

class FPSOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), Choreographer.FrameCallback {

    private val fpsTextView: TextView
    private val appTextView: TextView
    private val gpuTextView: TextView

    private val choreographer: Choreographer = Choreographer.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private val fpsCalculator = FPSCalculator(maxFrameHistory = 60)
    private val fpsFormat = DecimalFormat("0.0")

    private var realTimeMonitor: RealTimeFPSMonitor? = null

    private var frameCount = 0
    private var lastUpdateTimeNanos = 0L
    @Volatile
    private var isMonitoring = false
    private var hasRootRealTime = false

    companion object {
        private const val TAG = "FPSMeter_OverlayView"
        private const val UPDATE_INTERVAL_NANOS = 500_000_000L
    }

    init {
        try {
            orientation = VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.overlay_background)
            val paddingH = (12 * resources.displayMetrics.density).toInt()
            val paddingV = (8 * resources.displayMetrics.density).toInt()
            setPadding(paddingH, paddingV, paddingH, paddingV)

            fpsTextView = TextView(context).apply {
                text = "FPS: --"
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(3f, 1f, 1f, Color.BLACK)
            }
            addView(fpsTextView)

            appTextView = TextView(context).apply {
                text = "Detecting Game..."
                setTextColor(Color.LTGRAY)
                textSize = 10f
                visibility = View.GONE
                setShadowLayer(2f, 1f, 1f, Color.BLACK)
            }
            addView(appTextView)

            gpuTextView = TextView(context).apply {
                setTextColor(Color.parseColor("#4FC3F7"))
                textSize = 10f
                visibility = View.GONE
                setShadowLayer(2f, 1f, 1f, Color.BLACK)
            }
            addView(gpuTextView)

            Log.d(TAG, "FPSOverlayView initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing FPSOverlayView", e)
            throw e
        }
    }

    fun startFPSMonitoring() {
        Log.d(TAG, "Starting FPS monitoring")
        try {
            if (isMonitoring) return
            isMonitoring = true
            frameCount = 0
            hasRootRealTime = false
            lastUpdateTimeNanos = System.nanoTime()
            fpsCalculator.reset()

            post {
                fpsTextView.text = "FPS: --"
                fpsTextView.setTextColor(Color.YELLOW)
                appTextView.visibility = View.GONE
                gpuTextView.visibility = View.GONE
            }

            // Start hardware-level real-time in-game monitor
            realTimeMonitor = RealTimeFPSMonitor { metrics ->
                if (!isMonitoring) return@RealTimeFPSMonitor
                hasRootRealTime = metrics.isRealTimeGameFPS
                updateFromRealTimeMetrics(metrics)
            }.apply {
                start()
            }

            // Also keep Choreographer running as baseline/fallback
            choreographer.postFrameCallback(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting FPS monitoring", e)
            isMonitoring = false
        }
    }

    fun stopFPSMonitoring() {
        Log.d(TAG, "Stopping FPS monitoring")
        try {
            if (!isMonitoring) return
            isMonitoring = false
            choreographer.removeFrameCallback(this)
            realTimeMonitor?.stop()
            realTimeMonitor = null
            fpsCalculator.reset()

            post {
                fpsTextView.text = "FPS: Stopped"
                fpsTextView.setTextColor(Color.GRAY)
                appTextView.visibility = View.GONE
                gpuTextView.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping FPS monitoring", e)
        }
    }

    private fun updateFromRealTimeMetrics(metrics: PerformanceMetrics) {
        if (!metrics.isRealTimeGameFPS) return

        val fps = metrics.fps
        val color = when {
            fps >= 55.0 -> ContextCompat.getColor(context, R.color.fps_good)
            fps >= 30.0 -> ContextCompat.getColor(context, R.color.fps_medium)
            fps > 0.0 -> ContextCompat.getColor(context, R.color.fps_poor)
            else -> Color.GRAY
        }

        fpsTextView.text = "FPS: ${fpsFormat.format(fps)}"
        fpsTextView.setTextColor(color)

        if (!metrics.foregroundApp.isNullOrEmpty()) {
            val shortName = metrics.foregroundApp.substringAfterLast('.')
            appTextView.text = "Game: $shortName"
            appTextView.visibility = View.VISIBLE
        } else {
            appTextView.text = "Real-Time FPS"
            appTextView.visibility = View.VISIBLE
        }

        if (metrics.gpuLoad != null) {
            gpuTextView.text = "GPU: ${metrics.gpuLoad}%"
            gpuTextView.visibility = View.VISIBLE
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isMonitoring) return

        try {
            frameCount++
            fpsCalculator.onFrame(frameTimeNanos)

            // If real-time in-game root monitor is active, it handles updates.
            // If not active, fall back to display refresh rate.
            if (!hasRootRealTime && (frameCount % 20 == 0 || (frameTimeNanos - lastUpdateTimeNanos) >= UPDATE_INTERVAL_NANOS)) {
                updateFallbackDisplay(fpsCalculator.fps)
                lastUpdateTimeNanos = frameTimeNanos
            }

            if (isMonitoring) {
                choreographer.postFrameCallback(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in doFrame", e)
        }
    }

    private fun updateFallbackDisplay(fps: Double) {
        post {
            val fpsText = "Display: ${fpsFormat.format(fps)} Hz"
            fpsTextView.text = fpsText
            val color = when {
                fps >= 55.0 -> ContextCompat.getColor(context, R.color.fps_good)
                fps >= 30.0 -> ContextCompat.getColor(context, R.color.fps_medium)
                fps > 0.0 -> ContextCompat.getColor(context, R.color.fps_poor)
                else -> Color.GRAY
            }
            fpsTextView.setTextColor(color)
            appTextView.text = "(Root for in-game FPS)"
            appTextView.visibility = View.VISIBLE
        }
    }

    override fun onDetachedFromWindow() {
        Log.d(TAG, "View detached from window")
        stopFPSMonitoring()
        super.onDetachedFromWindow()
    }
}
