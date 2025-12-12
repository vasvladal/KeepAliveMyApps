package com.example.keepalivemyapps

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class CallInterceptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make it full-screen and show over lockscreen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        // Launch the target app immediately
        launchTargetApp()

        // Finish this activity immediately
        finish()
    }

    private fun launchTargetApp() {
        val targetPackage = "com.your.target.app"

        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                // For better compatibility
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
                }

                startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure screen turns off if we're done
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }
}