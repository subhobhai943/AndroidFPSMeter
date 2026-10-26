package com.fpsmeter.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var permissionHelper: PermissionHelper

    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == FPSMeterService.ACTION_STATE_CHANGED) {
                val isRunning = intent.getBooleanExtra(
                    FPSMeterService.EXTRA_IS_RUNNING,
                    FPSMeterService.isRunning
                )
                Log.d(TAG, "Service state changed broadcast received: isRunning=$isRunning")
                updateUI(isRunning)
            }
        }
    }

    private var isReceiverRegistered = false

    companion object {
        private const val TAG = "FPSMeter_MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        try {
            setContentView(R.layout.activity_main)
            initViews()
            permissionHelper = PermissionHelper(this)

            registerServiceStateReceiver()
            updateUI(FPSMeterService.isRunning)
            Log.d(TAG, "onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showError("Failed to initialize app: ${e.message}")
        }
    }

    private fun initViews() {
        statusText = findViewById(R.id.status_text)
        toggleButton = findViewById(R.id.toggle_button)
    }

    private fun registerServiceStateReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(FPSMeterService.ACTION_STATE_CHANGED)
            ContextCompat.registerReceiver(
                this,
                serviceStateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true
        }
    }

    private fun unregisterServiceStateReceiver() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(serviceStateReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            } finally {
                isReceiverRegistered = false
            }
        }
    }

    /**
     * Centralized UI update based on permissions and service running state.
     */
    private fun updateUI(isRunning: Boolean) {
        if (!permissionHelper.hasOverlayPermission()) {
            statusText.text = getString(R.string.overlay_permission_required)
            toggleButton.text = "Grant Overlay Permission"
            toggleButton.isEnabled = true
            toggleButton.setOnClickListener {
                Log.d(TAG, "Requesting overlay permission")
                permissionHelper.requestOverlayPermission()
            }
        } else {
            if (isRunning) {
                statusText.text = getString(R.string.fps_monitoring_active)
                toggleButton.text = "Stop FPS Meter"
                toggleButton.isEnabled = true
                toggleButton.setOnClickListener {
                    stopFPSMeter()
                }
            } else {
                statusText.text = "Ready to start FPS monitoring"
                toggleButton.text = "Start FPS Meter"
                toggleButton.isEnabled = true
                toggleButton.setOnClickListener {
                    startFPSMeter()
                }
            }
        }
    }

    private fun startFPSMeter() {
        Log.d(TAG, "Starting FPS meter...")

        try {
            if (!permissionHelper.hasOverlayPermission()) {
                Log.w(TAG, "Overlay permission not available when starting")
                updateUI(false)
                permissionHelper.requestOverlayPermission()
                return
            }

            // Request notification permission if needed on Android 13+
            if (!permissionHelper.hasNotificationPermission()) {
                permissionHelper.requestNotificationPermission()
            }

            statusText.text = "Starting FPS monitoring..."
            toggleButton.isEnabled = false

            val intent = Intent(this, FPSMeterService::class.java)
            ContextCompat.startForegroundService(this, intent)
            Log.d(TAG, "Foreground service start requested")

            // Update UI optimistically; receiver will confirm
            updateUI(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting FPS meter", e)
            showError("Failed to start FPS monitoring: ${e.message}")
            updateUI(false)
        }
    }

    private fun stopFPSMeter() {
        Log.d(TAG, "Stopping FPS meter...")

        try {
            statusText.text = "Stopping FPS monitoring..."
            toggleButton.isEnabled = false

            val intent = Intent(this, FPSMeterService::class.java)
            stopService(intent)

            updateUI(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping FPS meter", e)
            showError("Failed to stop FPS monitoring: ${e.message}")
            updateUI(FPSMeterService.isRunning)
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            statusText.text = message
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        updateUI(FPSMeterService.isRunning)
    }

    override fun onDestroy() {
        unregisterServiceStateReceiver()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode")

        if (requestCode == PermissionHelper.OVERLAY_PERMISSION_REQUEST_CODE) {
            updateUI(FPSMeterService.isRunning)
        }
    }
}
