package com.example.keepalivemyapps

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

class IncomingCallService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "incoming_call_service"
        private const val TAG = "IncomingCallService"

        fun startService(context: Context) {
            val intent = Intent(context, IncomingCallService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, IncomingCallService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var notificationManager: NotificationManager
    private lateinit var telephonyManager: TelephonyManager
    private var phoneStateListener: PhoneStateListener? = null
    private var isMonitoring = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startCallMonitoring()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoring incoming calls"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val launchAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Monitor Active")
            .setContentText("Ready to detect incoming calls")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun startCallMonitoring() {
        Log.d(TAG, "Starting call monitoring")

        // Check permission
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "READ_PHONE_STATE permission denied")
            showPermissionNotification()
            return
        }

        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)

                Log.d(TAG, "Call state: $state, Number: ${phoneNumber ?: "Unknown"}")

                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        Log.d(TAG, "INCOMING CALL DETECTED")
                        showCallDetectedNotification(phoneNumber)
                        launchTargetApp()
                    }

                    TelephonyManager.CALL_STATE_IDLE -> {
                        Log.d(TAG, "Call ended")
                    }

                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        Log.d(TAG, "Call answered")
                    }
                }
            }
        }

        // Start listening
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        isMonitoring = true
        Log.d(TAG, "Call monitoring active")
    }

    private fun showPermissionNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Permission Required")
            .setContentText("Grant phone permission for call detection")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showCallDetectedNotification(phoneNumber: String?) {
        val numberText = phoneNumber ?: "Unknown"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📞 Call Detected!")
            .setContentText("Launching app...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun launchTargetApp() {
        val targetPackage = "com.example.unwantedcallblocker"

        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
                }

                startActivity(launchIntent)
                Log.d(TAG, "App launched successfully: $targetPackage")
            } else {
                Log.e(TAG, "App not found: $targetPackage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Launch error: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        if (!isMonitoring) {
            startCallMonitoring()
        }

        return START_STICKY
    }

    private fun stopCallMonitoring() {
        Log.d(TAG, "Stopping call monitoring")

        phoneStateListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
        phoneStateListener = null
        isMonitoring = false
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        stopCallMonitoring()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}