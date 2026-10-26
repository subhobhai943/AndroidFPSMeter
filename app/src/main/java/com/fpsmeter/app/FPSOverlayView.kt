package com.fpsmeter.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
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
    private val choreographer: Choreographer = Choreographer.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private val fpsCalculator = FPSCalculator(maxFrameHistory = 60)
    private val fpsFormat = DecimalFormat("0.0")

    private var frameCount = 0
    private var lastUpdateTimeNanos = 0L
    @Volatile
    private var isMonitoring = false

    companion object {
        private const val TAG = "FPSMeter_OverlayView"
        // Update overlay display every 500ms or 30 frames for a smooth readout
        private const val UPDATE_INTERVAL_NANOS = 500_000_000L
    }

    init {
        try {
            orientation = VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.overlay_background)
            val paddingPx = (8 * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2)

            fpsTextView = TextView(context).apply {
                text = "FPS: --"
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setShadowLayer(3f, 1f, 1f, Color.BLACK)
            }

            addView(fpsTextView)
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
            lastUpdateTimeNanos = System.nanoTime()
            fpsCalculator.reset()
            post {
                fpsTextView.text = "FPS: --"
                fpsTextView.setTextColor(Color.YELLOW)
            }
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
            fpsCalculator.reset()
            post {
                fpsTextView.text = "FPS: Stopped"
                fpsTextView.setTextColor(Color.GRAY)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping FPS monitoring", e)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isMonitoring) return

        try {
            frameCount++
            fpsCalculator.onFrame(frameTimeNanos)

            if (frameCount % 15 == 0 || (frameTimeNanos - lastUpdateTimeNanos) >= UPDATE_INTERVAL_NANOS) {
                updateFPSDisplay(fpsCalculator.fps)
                lastUpdateTimeNanos = frameTimeNanos
            }

            if (isMonitoring) {
                choreographer.postFrameCallback(this)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in doFrame", e)
            post {
                fpsTextView.text = "FPS: Error"
                fpsTextView.setTextColor(Color.RED)
            }
        }
    }

    private fun updateFPSDisplay(fps: Double) {
        post {
            val fpsText = "FPS: ${fpsFormat.format(fps)}"
            fpsTextView.text = fpsText
            val color = when {
                fps >= 55.0 -> ContextCompat.getColor(context, R.color.fps_good)
                fps >= 30.0 -> ContextCompat.getColor(context, R.color.fps_medium)
                fps > 0.0 -> ContextCompat.getColor(context, R.color.fps_poor)
                else -> Color.GRAY
            }
            fpsTextView.setTextColor(color)
        }
    }

    override fun onDetachedFromWindow() {
        Log.d(TAG, "View detached from window")
        stopFPSMonitoring()
        super.onDetachedFromWindow()
    }
}
