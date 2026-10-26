package com.fpsmeter.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
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

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "fps_meter_channel"
        private const val TAG = "FPSMeter_Service"

        const val ACTION_STOP = "com.fpsmeter.app.ACTION_STOP"
        const val ACTION_STATE_CHANGED = "com.fpsmeter.app.ACTION_STATE_CHANGED"
        const val EXTRA_IS_RUNNING = "extra_is_running"

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
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            isRunning = true
            broadcastState(true)
            showOverlay()

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

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlay() {
        Log.d(TAG, "Attempting to show overlay")

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
            overlayView?.startFPSMonitoring()
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

    private fun createNotification(): Notification {
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Monitoring FPS in real-time")
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
