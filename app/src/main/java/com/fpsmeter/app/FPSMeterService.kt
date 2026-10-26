package com.fpsmeter.app

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class FPSMeterService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: FPSOverlayView? = null
    private var isOverlayVisible = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private var screenCaptureMonitor: ScreenCaptureFPSMonitor? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "fps_meter_channel"
        private const val TAG = "FPSMeter_Service"

        const val ACTION_STOP = "com.fpsmeter.app.ACTION_STOP"
        const val ACTION_STATE_CHANGED = "com.fpsmeter.app.ACTION_STATE_CHANGED"
        const val EXTRA_IS_RUNNING = "extra_is_running"

        const val EXTRA_PROJECTION_RESULT_CODE = "extra_projection_result_code"
        const val EXTRA_PROJECTION_DATA = "extra_projection_data"

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate called")

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            createNotificationChannel()
            Log.d(TAG, "Service onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in service onCreate", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand called, action=${intent?.action}")

        if (intent?.action == ACTION_STOP) {
            Log.d(TAG, "Stop action received, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val resultCode = intent?.getIntExtra(EXTRA_PROJECTION_RESULT_CODE, Activity.RESULT_CANCELED)
                ?: Activity.RESULT_CANCELED

            val projectionData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra(EXTRA_PROJECTION_DATA)
            }

            val hasProjection = (resultCode == Activity.RESULT_OK && projectionData != null)
            val notification = createNotification(hasProjection)

            // Start foreground with proper FGS types
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val fgsType = if (hasProjection) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fgsType = if (hasProjection) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                } else {
                    0
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            isRunning = true
            broadcastState(true)

            // Show HUD overlay
            showOverlay(skipRootMonitor = hasProjection)

            // If MediaProjection credentials were provided, start non-root real-time frame engine
            if (hasProjection) {
                startScreenCaptureEngine(resultCode, projectionData!!)
            }

            return START_STICKY
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
            showToast("Failed to start FPS monitoring: ${e.message}")
            isRunning = false
            broadcastState(false)
            stopSelf()
            return START_NOT_STICKY
        }
    }

    private fun startScreenCaptureEngine(resultCode: Int, projectionData: Intent) {
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mediaProjection = projectionManager.getMediaProjection(resultCode, projectionData)

            if (mediaProjection != null) {
                screenCaptureMonitor = ScreenCaptureFPSMonitor(
                    context = this,
                    mediaProjection = mediaProjection,
                    onFpsUpdate = { fps, frameTimeMs ->
                        overlayView?.updateRealTimeFPS(fps, frameTimeMs, isScreenCapture = true)
                    },
                    onStopped = {
                        Log.d(TAG, "Screen capture projection stopped by system")
                        stopSelf()
                    }
                ).apply {
                    start()
                }
                Log.d(TAG, "ScreenCaptureFPSMonitor successfully initialized and running")
            } else {
                Log.w(TAG, "getMediaProjection returned null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ScreenCaptureFPSMonitor", e)
            showToast("Screen capture engine failed: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay(skipRootMonitor: Boolean) {
        Log.d(TAG, "Attempting to show overlay (skipRoot=$skipRootMonitor)")

        if (isOverlayVisible) {
            Log.w(TAG, "Overlay already visible")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Log.e(TAG, "Overlay permission not granted")
                showToast("Overlay permission not granted")
                stopSelf()
                return
            }

            overlayView = FPSOverlayView(this)

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 50
                y = 100
            }

            windowManager.addView(overlayView, params)
            isOverlayVisible = true
            overlayView?.startFPSMonitoring(skipRootMonitor = skipRootMonitor)
            Log.d(TAG, "Overlay added to WindowManager successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay", e)
            showToast("Failed to show overlay: ${e.message}")
            stopSelf()
        }
    }

    private fun hideOverlay() {
        Log.d(TAG, "Hiding overlay")

        if (!isOverlayVisible || overlayView == null) {
            return
        }

        try {
            overlayView?.stopFPSMonitoring()
            windowManager.removeView(overlayView)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay", e)
        } finally {
            isOverlayVisible = false
            overlayView = null
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy called")
        isRunning = false
        broadcastState(false)

        try {
            screenCaptureMonitor?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping screenCaptureMonitor", e)
        } finally {
            screenCaptureMonitor = null
        }

        hideOverlay()
        super.onDestroy()
    }

    private fun broadcastState(running: Boolean) {
        val intent = Intent(ACTION_STATE_CHANGED).apply {
            putExtra(EXTRA_IS_RUNNING, running)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "FPS Meter Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Displays real-time FPS overlay"
                    setShowBadge(false)
                }

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel", e)
            }
        }
    }

    private fun createNotification(isRealTimeScreenCapture: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val immutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag
        )

        val stopIntent = Intent(this, FPSMeterService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag
        )

        val modeDescription = if (isRealTimeScreenCapture) {
            "Real-time in-game FPS monitoring active"
        } else {
            "FPS monitoring active"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(modeDescription)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun showToast(message: String) {
        mainHandler.post {
            try {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing toast", e)
            }
        }
    }
}
