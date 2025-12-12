package com.example.keepalivemyapps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

object CallStateManager {

    private var callReceiver: BroadcastReceiver? = null

    fun registerCallReceiver(context: Context) {
        // Note: READ_PHONE_STATE is heavily restricted on Android 12+
        // This approach may not work on all devices

        callReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

                    if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                        // Incoming call detected!
                        launchTargetApp(context)
                    }
                }
            }
        }

        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        ContextCompat.registerReceiver(
            context,
            callReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun unregisterCallReceiver(context: Context) {
        callReceiver?.let {
            context.unregisterReceiver(it)
            callReceiver = null
        }
    }

    private fun launchTargetApp(context: Context) {
        val targetPackage = "com.your.target.app"

        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                // Create a full-screen intent to ensure it launches
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                // For Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
                }

                context.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}