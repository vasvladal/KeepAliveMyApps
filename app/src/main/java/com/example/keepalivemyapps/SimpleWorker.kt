package com.example.keepalivemyapps

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class SimpleWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val TAG = "SimpleWorker"

        fun schedule(context: Context) {
            // Schedule periodic work every 15 minutes for health checks
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<SimpleWorker>(
                15, TimeUnit.MINUTES
            )
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()

            androidx.work.WorkManager.getInstance(context)
                .enqueue(workRequest)
            Log.d(TAG, "Worker scheduled")
        }
    }

    override fun doWork(): Result {
        Log.d(TAG, "Worker running - checking service health")

        // Log that worker is active (for debugging)
        Log.d(TAG, "Periodic check: Service should be monitoring calls")

        // Check if target app exists
        val targetPackage = "com.example.unwantedcallblocker"
        val packageInfo = try {
            applicationContext.packageManager.getPackageInfo(targetPackage, 0)
            true
        } catch (e: Exception) {
            false
        }

        Log.d(TAG, "Target app ($targetPackage) installed: $packageInfo")

        return Result.success()
    }
}